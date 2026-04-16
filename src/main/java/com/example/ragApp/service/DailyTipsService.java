package com.example.ragApp.service;

import com.example.ragApp.data.DailyTips;
import com.example.ragApp.repository.DailyTipsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class DailyTipsService {

    private static final LocalDate TIP_BASE_DATE = LocalDate.of(2026, 3, 26);

    private final DailyTipsRepository dailyTipsRepository;

    public DailyTipsService(DailyTipsRepository dailyTipsRepository) {
        this.dailyTipsRepository = dailyTipsRepository;
    }

    @Transactional
    public DailyTips createTip(String tip) {
        if (tip == null || tip.trim().isEmpty()) {
            throw new RuntimeException("tip is required");
        }

        DailyTips dailyTip = new DailyTips();
        dailyTip.setTip(tip.trim());
        return dailyTipsRepository.save(dailyTip);
    }

    @Transactional
    public void deleteTip(Long id) {
        if (id == null) {
            throw new RuntimeException("id is required");
        }

        if (!dailyTipsRepository.existsById(id)) {
            throw new RuntimeException("Daily tip not found");
        }

        dailyTipsRepository.deleteById(id);
    }

    public DailyTips getRandomTip() {
        long totalTips = dailyTipsRepository.count();
        if (totalTips == 0) {
            throw new RuntimeException("No daily tips available");
        }

        long daysSinceBase = ChronoUnit.DAYS.between(TIP_BASE_DATE, LocalDate.now());
        int tipIndex = (int) Math.floorMod(daysSinceBase, totalTips);

        return dailyTipsRepository.findAll(PageRequest.of(tipIndex, 1, Sort.by(Sort.Direction.ASC, "id")))
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No daily tips available"));
    }

    public DailyTips getRefreshedTip(Long currentTipId) {
        if (dailyTipsRepository.count() == 0) {
            throw new RuntimeException("No daily tips available");
        }

        if (currentTipId == null) {
            return dailyTipsRepository.findFirstByOrderByIdAsc()
                    .orElseThrow(() -> new RuntimeException("No daily tips available"));
        }

        return dailyTipsRepository.findFirstByIdGreaterThanOrderByIdAsc(currentTipId)
                .or(() -> dailyTipsRepository.findFirstByOrderByIdAsc())
                .orElseThrow(() -> new RuntimeException("No daily tips available"));
    }
}

