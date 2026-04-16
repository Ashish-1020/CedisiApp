package com.example.ragApp.controller;

import com.example.ragApp.data.DailyTips;
import com.example.ragApp.dto.DailyTipRequest;
import com.example.ragApp.service.DailyTipsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/daily-tips")
public class DailyTipsController {

    private final DailyTipsService dailyTipsService;

    public DailyTipsController(DailyTipsService dailyTipsService) {
        this.dailyTipsService = dailyTipsService;
    }

    @PostMapping
    public ResponseEntity<DailyTips> createTip(@RequestBody DailyTipRequest request) {
        return ResponseEntity.ok(dailyTipsService.createTip(request.getTip()));
    }

    @GetMapping
    public ResponseEntity<DailyTips> getTodayTip() {
        return ResponseEntity.ok(dailyTipsService.getRandomTip());
    }

    @GetMapping("/refresh")
    public ResponseEntity<DailyTips> refreshTip(@RequestParam(required = false) Long currentTipId) {
        return ResponseEntity.ok(dailyTipsService.getRefreshedTip(currentTipId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTip(@PathVariable Long id) {
        dailyTipsService.deleteTip(id);
        return ResponseEntity.ok("Daily tip deleted successfully");
    }
}

