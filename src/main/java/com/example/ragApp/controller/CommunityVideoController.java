package com.example.ragApp.controller;

import com.example.ragApp.dto.ApiMessageResponse;
import com.example.ragApp.dto.CommunityVideoRequest;
import com.example.ragApp.dto.CommunityVideoResponse;
import com.example.ragApp.service.CommunityVideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community-videos")
public class CommunityVideoController {

    private final CommunityVideoService communityVideoService;

    public CommunityVideoController(CommunityVideoService communityVideoService) {
        this.communityVideoService = communityVideoService;
    }

    @PostMapping("/admin")
    public ResponseEntity<CommunityVideoResponse> createVideo(@RequestBody CommunityVideoRequest request) {
        return ResponseEntity.ok(communityVideoService.createVideo(request));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<CommunityVideoResponse>> getAllForAdmin() {
        return ResponseEntity.ok(communityVideoService.getAllForAdmin());
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<CommunityVideoResponse> getByIdForAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(communityVideoService.getByIdForAdmin(id));
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<CommunityVideoResponse> updateVideo(@PathVariable Long id,
                                                              @RequestBody CommunityVideoRequest request) {
        return ResponseEntity.ok(communityVideoService.updateVideo(id, request));
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<ApiMessageResponse> deleteVideo(@PathVariable Long id) {
        communityVideoService.deactivateVideo(id);
        return ResponseEntity.ok(new ApiMessageResponse("Community video deactivated"));
    }

    @GetMapping
    public ResponseEntity<List<CommunityVideoResponse>> getActiveVideos(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(communityVideoService.getActiveVideos(category));
    }
}

