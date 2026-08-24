package com.pvp.travelmatch.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String,String>> handleRuntimeException(RuntimeException ex){

        return ResponseEntity.badRequest().body(
                Map.of(
                        "message", ex.getMessage()
                )
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String,String>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex){

        return ResponseEntity.badRequest().body(
                Map.of(
                        "message", "Photo is too large to upload"
                )
        );
    }

}