package com.joe.taskira.dashboard.controller;

import com.joe.taskira.dashboard.dto.DashboardSummaryResponse;
import com.joe.taskira.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/dashboard/summary")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }
}
