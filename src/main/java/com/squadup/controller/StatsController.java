package com.squadup.controller;

import com.squadup.aop.TrackExecutionTime;
import com.squadup.dto.PlatformStats;
import com.squadup.jdbc.PlatformStatsDao;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Platform statistics from {@link PlatformStatsDao} (Spring JDBC).
 * {@code @CrossOrigin} shows CORS configured on a single controller, next to the global rules in WebConfig.
 */
@RestController
@RequestMapping("/api/v1/stats")
@CrossOrigin(origins = "*", methods = org.springframework.web.bind.annotation.RequestMethod.GET)
public class StatsController {

    private final PlatformStatsDao statsDao;

    public StatsController(PlatformStatsDao statsDao) {
        this.statsDao = statsDao;
    }

    @GetMapping
    @TrackExecutionTime
    public ResponseEntity<PlatformStats> stats(@RequestParam(defaultValue = "5") @Min(1) @Max(20) int topGames) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)))
                .body(statsDao.snapshot(topGames));
    }
}
