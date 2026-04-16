package com.example.ragApp.service;

import com.example.ragApp.data.CommunityVideo;
import com.example.ragApp.dto.CommunityVideoRequest;
import com.example.ragApp.dto.CommunityVideoResponse;
import com.example.ragApp.repository.CommunityVideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CommunityVideoService {

    private final CommunityVideoRepository communityVideoRepository;

    public CommunityVideoService(CommunityVideoRepository communityVideoRepository) {
        this.communityVideoRepository = communityVideoRepository;
    }

    @Transactional
    public CommunityVideoResponse createVideo(CommunityVideoRequest request) {
        validateRequest(request);

        CommunityVideo video = new CommunityVideo();
        applyValues(video, request, true);
        return toResponse(communityVideoRepository.save(video));
    }

    @Transactional
    public CommunityVideoResponse updateVideo(Long id, CommunityVideoRequest request) {
        validateRequest(request);
        CommunityVideo video = getVideoEntity(id);

        applyValues(video, request, false);
        return toResponse(communityVideoRepository.save(video));
    }

    @Transactional
    public void deactivateVideo(Long id) {
        CommunityVideo video = getVideoEntity(id);
        video.setActive(false);
        communityVideoRepository.save(video);
    }

    @Transactional(readOnly = true)
    public List<CommunityVideoResponse> getAllForAdmin() {
        return communityVideoRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CommunityVideoResponse getByIdForAdmin(Long id) {
        return toResponse(getVideoEntity(id));
    }

    @Transactional(readOnly = true)
    public List<CommunityVideoResponse> getActiveVideos(String category) {
        List<CommunityVideo> videos;
        if (category == null || category.trim().isEmpty()) {
            videos = communityVideoRepository.findByActiveTrueOrderByCreatedAtDesc();
        } else {
            videos = communityVideoRepository.findByActiveTrueAndCategoryIgnoreCaseOrderByCreatedAtDesc(category.trim());
        }

        return videos.stream().map(this::toResponse).toList();
    }

    private CommunityVideo getVideoEntity(Long id) {
        if (id == null) {
            throw new RuntimeException("id is required");
        }

        return communityVideoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Community video not found"));
    }

    private void validateRequest(CommunityVideoRequest request) {
        if (request == null) {
            throw new RuntimeException("request body is required");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new RuntimeException("title is required");
        }
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new RuntimeException("description is required");
        }
        if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
            throw new RuntimeException("category is required");
        }
        if (request.getYoutubeUrl() == null || request.getYoutubeUrl().trim().isEmpty()) {
            throw new RuntimeException("youtubeUrl is required");
        }

        String normalizedUrl = request.getYoutubeUrl().trim().toLowerCase(Locale.ROOT);
        if (!(normalizedUrl.contains("youtube.com/watch") || normalizedUrl.contains("youtu.be/"))) {
            throw new RuntimeException("youtubeUrl must be a valid YouTube link");
        }
    }

    private void applyValues(CommunityVideo video, CommunityVideoRequest request, boolean isCreate) {
        video.setTitle(request.getTitle().trim());
        video.setDescription(request.getDescription().trim());
        video.setCategory(request.getCategory().trim());
        video.setYoutubeUrl(request.getYoutubeUrl().trim());

        if (isCreate) {
            video.setActive(request.getActive() == null ? true : request.getActive());
        } else if (request.getActive() != null) {
            video.setActive(request.getActive());
        }
    }

    private CommunityVideoResponse toResponse(CommunityVideo video) {
        CommunityVideoResponse response = new CommunityVideoResponse();
        response.setId(video.getId());
        response.setTitle(video.getTitle());
        response.setDescription(video.getDescription());
        response.setCategory(video.getCategory());
        response.setYoutubeUrl(video.getYoutubeUrl());
        response.setActive(video.getActive());
        response.setCreatedAt(video.getCreatedAt());
        response.setUpdatedAt(video.getUpdatedAt());
        return response;
    }
}
