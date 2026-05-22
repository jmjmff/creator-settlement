package com.example.creatorsettlement.controller;

import com.example.creatorsettlement.dto.request.CancelRecordCreateRequest;
import com.example.creatorsettlement.dto.request.SaleRecordCreateRequest;
import com.example.creatorsettlement.dto.response.SaleRecordResponse;
import com.example.creatorsettlement.service.SaleRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SaleRecordController {

    private final SaleRecordService saleRecordService;

    @PostMapping("/sale-records")
    public ResponseEntity<SaleRecordResponse> createSaleRecord(
            @Valid @RequestBody SaleRecordCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saleRecordService.createSaleRecord(request));
    }

    @PostMapping("/cancel-records")
    public ResponseEntity<SaleRecordResponse> createCancelRecord(
            @Valid @RequestBody CancelRecordCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saleRecordService.createCancelRecord(request));
    }

    @GetMapping("/sale-records")
    public ResponseEntity<List<SaleRecordResponse>> getSaleRecords(
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(saleRecordService.getSaleRecords(creatorId, startDate, endDate));
    }
}
