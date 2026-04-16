package com.example.ragApp.service;

import com.example.ragApp.data.EmailOtp;
import com.example.ragApp.data.Subscription;
import com.example.ragApp.data.User;
import com.example.ragApp.dto.*;
import com.example.ragApp.exception.AuthException;
import com.example.ragApp.exception.InvalidPasswordException;
import com.example.ragApp.repository.EmailOTPRepository;
import com.example.ragApp.repository.SubscriptionPlanRepository;
import com.example.ragApp.repository.SubscriptionRepository;
import com.example.ragApp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final EmailOTPRepository otpRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final PaymentService paymentService;
    private final FcmTokenService fcmTokenService;


    public AuthService(UserRepository userRepository,
                       EmailOTPRepository otpRepository,
                       SubscriptionRepository subscriptionRepository,
                       SubscriptionPlanRepository subscriptionPlanRepository,
                       PasswordEncoder passwordEncoder,
                       MailService mailService,
                       PaymentService paymentService,
                       FcmTokenService fcmTokenService) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.paymentService = paymentService;
        this.fcmTokenService = fcmTokenService;
    }

    // 🔹 Signup
    public String signup(SignupRequest request) throws Exception {


        validateEmail(request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("AUTH_EMAIL_ALREADY_REGISTERED", "Email already registered", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setMobile(request.getMobile());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        user.setRole("USER");
        user.setVerified(false);
        user.setUserStatus("ACTIVE");
        user.setNoOfTokensConsumed(0);
        user.setSubscriptionStatus("FREE");
        user.setRegisteredOn(LocalDateTime.now());
        user.setNoDeviceLoggedIn(0);

        userRepository.save(user);

        paymentService.ensureFreeSubscription(user.getId());


        // 🔥 Generate OTP
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        EmailOtp emailOtp = new EmailOtp();
        emailOtp.setId(UUID.randomUUID().toString());
        emailOtp.setEmail(request.getEmail());
        emailOtp.setOtp(otp);
        emailOtp.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        emailOtp.setUsed(false);

        otpRepository.save(emailOtp);

        // TODO: Send email
        mailService.sendOtpEmail(request.getEmail(), otp);
        System.out.println("OTP: " + otp);

        return "Signup successful. Verify OTP.";
    }

    // 🔹 Verify OTP
    public String verifyOtp(VerifyOtpRequest request) {
        validateOtp(request.getOtp());

        EmailOtp record = otpRepository
                .findTopByEmailOrderByExpiryTimeDesc(request.getEmail())
                .orElseThrow(() -> new AuthException("AUTH_OTP_NOT_FOUND", "OTP not found", HttpStatus.NOT_FOUND));

        if (record.getUsed()) {
            throw new AuthException("AUTH_OTP_ALREADY_USED", "OTP already used", HttpStatus.CONFLICT);
        }

        if (record.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new AuthException("AUTH_OTP_EXPIRED", "OTP expired", HttpStatus.BAD_REQUEST);
        }

        if (!record.getOtp().equals(request.getOtp())) {
            throw new AuthException("AUTH_OTP_INVALID", "Invalid OTP", HttpStatus.BAD_REQUEST);
        }

        record.setUsed(true);
        otpRepository.save(record);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("AUTH_USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND));

        user.setVerified(true);
        userRepository.save(user);

        try {
            mailService.sendWelcomeEmail(user.getEmail());
        } catch (Exception ex) {
            log.warn("Failed to send welcome email for userId={} email={}: {}", user.getId(), user.getEmail(), ex.getMessage());
        }

        return "Email verified successfully";
    }

    // 🔹 Login
    public LoginResponse login(LoginRequest request) {
        validateEmail(request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("AUTH_USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException();
        }

        if (!user.getVerified()) {
            throw new AuthException("AUTH_EMAIL_NOT_VERIFIED", "Email not verified", HttpStatus.FORBIDDEN);
        }

        if (!"ACTIVE".equals(user.getUserStatus())) {
            throw new AuthException("AUTH_USER_INACTIVE", "User is " + user.getUserStatus(), HttpStatus.FORBIDDEN);
        }

        Subscription latestSubscription = subscriptionRepository
                .findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .orElse(null);

        String subscriptionPlanId = latestSubscription != null
                ? latestSubscription.getPlanId()
                : user.getSubscriptionPlanId();

        LocalDateTime subscriptionExpiration = latestSubscription != null
                ? latestSubscription.getEndDate()
                : user.getSubscriptionEnd();

        Integer tokenLimit = null;
        if (subscriptionPlanId != null && !subscriptionPlanId.isBlank()) {
            tokenLimit = subscriptionPlanRepository.findByPlanCodeIgnoreCase(subscriptionPlanId.trim())
                    .map(plan -> plan.getTokenLimit())
                    .orElse(null);
        }

        Integer tokensConsumed = user.getNoOfTokensConsumed() == null ? 0 : user.getNoOfTokensConsumed();

        // Optional in-login sync so frontend can avoid an extra round trip.
        if (request.getFcmToken() != null && !request.getFcmToken().trim().isEmpty()) {
            try {
                RegisterPushTokenRequest pushTokenRequest = new RegisterPushTokenRequest();
                pushTokenRequest.setUserId(user.getId());
                pushTokenRequest.setFcmToken(request.getFcmToken());
                pushTokenRequest.setDeviceId(request.getDeviceId());
                pushTokenRequest.setPlatform(request.getPlatform());
                pushTokenRequest.setAppVersion(request.getAppVersion());
                fcmTokenService.registerToken(pushTokenRequest);
            } catch (Exception ex) {
                log.warn("Failed to sync push token during login for userId={}: {}", user.getId(), ex.getMessage());
            }
        }

        return new LoginResponse(
                "Login successful",
                user.getEmail(),
                user.getRole(),
                user.getId(),
                user.getSubscriptionStatus(),
                subscriptionPlanId,
                subscriptionExpiration,
                tokenLimit,
                tokensConsumed
        );
    }


    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new AuthException("AUTH_EMAIL_EMPTY", "Email cannot be empty", HttpStatus.BAD_REQUEST);
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

        if (!email.matches(emailRegex)) {
            throw new AuthException("AUTH_EMAIL_INVALID_FORMAT", "Invalid email format", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateOtp(String otp) {
        if (otp == null || otp.trim().isEmpty()) {
            throw new AuthException("AUTH_OTP_EMPTY", "OTP cannot be empty", HttpStatus.BAD_REQUEST);
        }

        if (!otp.matches("\\d{6}")) {
            throw new AuthException("AUTH_OTP_INVALID_FORMAT", "OTP must be exactly 6 digits", HttpStatus.BAD_REQUEST);
        }
    }
}
