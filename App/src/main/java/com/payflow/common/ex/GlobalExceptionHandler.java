package com.payflow.common.ex;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends RuntimeException {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handle(Exception e){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error",e.getMessage()));
    }
}
