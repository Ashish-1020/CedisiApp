package com.example.ragApp.service;



import com.example.ragApp.data.UserProfile;
import com.example.ragApp.exception.AuthException;
import com.example.ragApp.helper.ProfileSection;
import com.example.ragApp.repository.UserProfileRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    // ---------------- CREATE PROFILE FIRST TIME ----------------

    public UserProfile createProfile(UserProfile profile) {

        if (userProfileRepository.existsById(profile.getUserId())) {
            throw new AuthException(
                    "USER_PROFILE_ALREADY_EXISTS",
                    "Profile already exists for this user",
                    HttpStatus.CONFLICT
            );
        }

        return userProfileRepository.save(profile);
    }


    // ---------------- UPDATE PROFILE ----------------

    public UserProfile updateProfile(String userId, UserProfile updatedProfile) {

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new AuthException(
                        "USER_PROFILE_NOT_FOUND",
                        "User profile not found",
                        HttpStatus.NOT_FOUND
                ));

        BeanUtils.copyProperties(updatedProfile, profile, getNullPropertyNames(updatedProfile));

        return userProfileRepository.save(profile);
    }


    // ---------------- CHECK SECTION COMPLETION ----------------

    public boolean isSectionFilled(String userId, ProfileSection section) {

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new AuthException(
                        "USER_PROFILE_NOT_FOUND",
                        "User profile not found",
                        HttpStatus.NOT_FOUND
                ));

        switch (section) {

            case BUSINESS_IDENTITY:
                return profile.getName() != null
                        && profile.getYearOfIncorporation() != null
                        && profile.getLegalStructure() != null
                        && profile.getPrimaryBusinessCategory() != null
                        && profile.getProductCategory() != null
                        && profile.getRegisteredOfficeState() != null
                        && profile.getRegisteredOfficeCity() != null;

            case FINANCE:
                return profile.getTurnoverYear1() != null
                        && profile.getTurnoverYear2() != null
                        && profile.getTurnoverYear3() != null;

            case MANUFACTURING:
                return profile.getManufacturingType() != null;

            case TRADING:
                return profile.getTradingType() != null;

            case SALES_MARKET:
                return profile.getPrimaryCustomerType() != null
                        && !profile.getPrimaryCustomerType().isEmpty();

            case WORKING_CAPITAL:
                return profile.getTotalLoanTakenRange() != null;

            case GROWTH:
                return profile.getGrowthPlan() != null
                        && !profile.getGrowthPlan().isEmpty();

            default:
                return false;
        }
    }


    // ---------------- HELPER METHOD ----------------

    private String[] getNullPropertyNames(Object source) {

        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();

        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }

        return emptyNames.toArray(new String[0]);
    }

    public UserProfile getUserProfile(String userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new AuthException(
                        "USER_PROFILE_NOT_FOUND",
                        "User profile not found",
                        HttpStatus.NOT_FOUND
                ));
    }
}

