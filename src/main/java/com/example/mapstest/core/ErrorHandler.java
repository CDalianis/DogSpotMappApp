package com.example.mapstest.core;

import com.example.mapstest.core.exceptions.AppGenericException;
import com.example.mapstest.core.exceptions.EntityAlreadyExistsException;
import com.example.mapstest.core.exceptions.EntityInvalidArgumentException;
import com.example.mapstest.core.exceptions.EntityNotFoundException;
import com.example.mapstest.core.exceptions.FileUploadException;
import com.example.mapstest.core.exceptions.ValidationException;
import com.example.mapstest.dto.ErrorResponseDTO;
import com.example.mapstest.dto.ValidationErrorResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class ErrorHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponseDTO> handleValidationException(ValidationException e) {
        log.warn("Validation Failed. Message={}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return new ResponseEntity<>(
                new ValidationErrorResponseDTO(e.getCode(), e.getMessage(), errors),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleEntityNotFoundException(EntityNotFoundException e) {
        log.warn("Entity not found. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDTO(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(EntityInvalidArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidArgumentException(EntityInvalidArgumentException e) {
        log.warn("Invalid Argument. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleEntityAlreadyExistsException(EntityAlreadyExistsException e) {
        log.warn("Entity already exists. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponseDTO(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponseDTO> handleFileUploadException(FileUploadException e) {
        log.warn("File upload failed. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(AppGenericException.class)
    public ResponseEntity<ErrorResponseDTO> handleAppGenericException(AppGenericException e) {
        log.warn("Application error. code={} message={}", e.getCode(), e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception e) {
        log.warn("Unexpected error. Message={}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO("INTERNAL_SERVER_ERROR", "An unexpected error occurred."));
    }
}
