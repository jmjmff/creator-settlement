package com.example.creatorsettlement.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)                          // 404
                .body(ErrorResponse.builder().status(404).message(e.getMessage()).build());
    }

    @ExceptionHandler(AlreadyCancelledException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyCancelled(AlreadyCancelledException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)                           // 409: 이미 취소된 판매
                .body(ErrorResponse.builder().status(409).message(e.getMessage()).build());
    }

    // 동일 creatorId + yearMonth 조합으로 정산이 이미 존재할 때 발생
    @ExceptionHandler(DuplicateSettlementException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSettlement(DuplicateSettlementException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)                           // 409: 중복 정산
                .body(ErrorResponse.builder().status(409).message(e.getMessage()).build());
    }

    // PENDING이 아닌데 confirm(), 또는 CONFIRMED가 아닌데 pay()를 호출할 때 발생
    @ExceptionHandler(InvalidSettlementStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSettlementStatus(InvalidSettlementStatusException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)               // 422: 상태 전이 불가
                .body(ErrorResponse.builder().status(422).message(e.getMessage()).build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)                        // 400: 요청 파라미터 오류
                .body(ErrorResponse.builder().status(400).message(message).build());
    }
}
