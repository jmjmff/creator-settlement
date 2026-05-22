package com.example.creatorsettlement.controller;

import com.example.creatorsettlement.dto.response.AdminSettlementResponse;
import com.example.creatorsettlement.dto.response.CreatorSettlementResponse;
import com.example.creatorsettlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    // GET /api/settlements/creator/1?yearMonth=2025-03
    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<CreatorSettlementResponse> getCreatorSettlement(
            @PathVariable Long creatorId,
            @RequestParam String yearMonth) {
        return ResponseEntity.ok(settlementService.getCreatorSettlement(creatorId, yearMonth));
    }

    // GET /api/settlements/admin?startDate=2025-01-01&endDate=2025-03-31
    @GetMapping("/admin")
    public ResponseEntity<AdminSettlementResponse> getAdminSettlement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(settlementService.getAdminSettlement(startDate, endDate));
    }
}
