package com.example.creatorsettlement.controller;

import com.example.creatorsettlement.dto.response.AdminSettlementResponse;
import com.example.creatorsettlement.dto.response.CreatorSettlementResponse;
import com.example.creatorsettlement.dto.response.SettlementResponse;
import com.example.creatorsettlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    // ─── [기존] 실시간 계산 조회 ───────────────────────────────────────

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

    // ─── [신규] 정산 상태 관리 (PENDING → CONFIRMED → PAID) ───────────

    // POST /api/settlements?creatorId=1&yearMonth=2025-03
    // 정산을 PENDING 상태로 생성; 동일 creatorId+yearMonth 중복이면 409 반환
    @PostMapping
    public ResponseEntity<SettlementResponse> createSettlement(
            @RequestParam Long creatorId,
            @RequestParam String yearMonth) {
        SettlementResponse response = settlementService.createSettlement(creatorId, yearMonth);
        return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201 Created
    }

    // GET /api/settlements/1
    @GetMapping("/{settlementId}")
    public ResponseEntity<SettlementResponse> getSettlement(@PathVariable Long settlementId) {
        return ResponseEntity.ok(settlementService.getSettlement(settlementId));
    }

    // PATCH /api/settlements/1/confirm
    // PENDING → CONFIRMED; 다른 상태이면 422 반환
    @PatchMapping("/{settlementId}/confirm")
    public ResponseEntity<SettlementResponse> confirmSettlement(@PathVariable Long settlementId) {
        return ResponseEntity.ok(settlementService.confirmSettlement(settlementId));
    }

    // PATCH /api/settlements/1/pay
    // CONFIRMED → PAID; 다른 상태이면 422 반환
    @PatchMapping("/{settlementId}/pay")
    public ResponseEntity<SettlementResponse> paySettlement(@PathVariable Long settlementId) {
        return ResponseEntity.ok(settlementService.paySettlement(settlementId));
    }

    // ─── [신규] CSV 다운로드 ───────────────────────────────────────────

    // GET /api/settlements/admin/csv
    // 전체 정산 내역을 UTF-8 BOM CSV로 반환; Excel에서 바로 열기 가능
    @GetMapping("/admin/csv")
    public ResponseEntity<byte[]> exportCsv() {
        byte[] csv = settlementService.exportCsv();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        // Content-Disposition: attachment → 브라우저가 파일 저장 다이얼로그를 열도록 강제
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"settlements.csv\"");
        return ResponseEntity.ok().headers(headers).body(csv);
    }
}
