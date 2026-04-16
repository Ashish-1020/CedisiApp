package com.example.ragApp.repository;

import com.example.ragApp.data.CommunityVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityVideoRepository extends JpaRepository<CommunityVideo, Long> {

    List<CommunityVideo> findByActiveTrueOrderByCreatedAtDesc();

    List<CommunityVideo> findByActiveTrueAndCategoryIgnoreCaseOrderByCreatedAtDesc(String category);
}

