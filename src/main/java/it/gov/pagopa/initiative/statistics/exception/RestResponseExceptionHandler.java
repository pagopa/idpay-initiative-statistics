package it.gov.pagopa.initiative.statistics.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class RestResponseExceptionHandler {

    // API
    @ExceptionHandler({InitiativeStatException.class})
    public ResponseEntity<ErrorDTO> handleInitiativeException(InitiativeStatException ex) {
        return new ResponseEntity<>(new ErrorDTO(ex.getCode(), ex.getMessage()),
                ex.getHttpStatus());
    }

    @ExceptionHandler({MerchantStatException.class})
    public ResponseEntity<ErrorDTO> handleMerchantException(MerchantStatException ex) {
        return new ResponseEntity<>(new ErrorDTO(ex.getCode(), ex.getMessage()),
                ex.getHttpStatus());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorDTO> handleMethodArgumentNotValidExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldErrorInput) {
                String fieldName = fieldErrorInput.getField();
                String errorMessage = error.getDefaultMessage();
                errors.add(String.format("[%s]: %s", fieldName, errorMessage));
            } else {
                String objectName = error.getObjectName();
                String errorMessage = error.getDefaultMessage();
                errors.add(String.format("[%s]: %s", objectName, errorMessage));
            }
        });
        String message = String.join(" - ", errors);
        return new ResponseEntity<>(
                new ErrorDTO(HttpStatus.BAD_REQUEST.name(), message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<ErrorDTO> handleHttpMessageNotReadableException(Exception ex) {
        return new ResponseEntity<>(
                new ErrorDTO(HttpStatus.INTERNAL_SERVER_ERROR.name(), ex.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
    }

}
