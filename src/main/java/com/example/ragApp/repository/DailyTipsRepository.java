package com.example.ragApp.repository;

import com.example.ragApp.data.DailyTips;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DailyTipsRepository extends JpaRepository<DailyTips, Long> {

	Optional<DailyTips> findFirstByIdGreaterThanOrderByIdAsc(Long id);

	Optional<DailyTips> findFirstByOrderByIdAsc();
}

