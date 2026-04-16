package com.example.ragApp.controller;

import com.example.ragApp.data.UserProfile;
import com.example.ragApp.dto.UserProfileRequest;

import com.example.ragApp.helper.ProfileSection;
import com.example.ragApp.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @PostMapping("/create")
    public UserProfile createProfile(@RequestBody UserProfile profile) {
        return userProfileService.createProfile(profile);
    }

    @PutMapping("/update/{userId}")
    public UserProfile updateProfile(
            @PathVariable String userId,
            @RequestBody UserProfile profile) {

        return userProfileService.updateProfile(userId, profile);
    }



    @GetMapping("/section-status/{userId}/{section}")
    public boolean checkSection(
            @PathVariable String userId,
            @PathVariable ProfileSection section) {

        return userProfileService.isSectionFilled(userId, section);
    }

    @GetMapping("/{userId}")
    public UserProfile getUserProfile(@PathVariable String userId) {
        return userProfileService.getUserProfile(userId);
    }

}
