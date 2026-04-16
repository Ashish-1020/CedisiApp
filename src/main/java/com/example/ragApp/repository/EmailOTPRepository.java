package com.example.ragApp.repository;


import com.example.ragApp.data.EmailOtp;
import com.example.ragApp.data.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailOTPRepository extends JpaRepository<EmailOtp, String> {
    Optional<EmailOtp> findTopByEmailOrderByExpiryTimeDesc(String email);
}
