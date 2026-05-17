package com.learnpath.version1.exception;

import com.learnpath.version1.dto.ErrorDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AiResponseParsingException.class)
    public ResponseEntity<ErrorDetail> handleParsingError (AiResponseParsingException ex){
        ErrorDetail error = new ErrorDetail(
                "AI response could not be parsed",
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
