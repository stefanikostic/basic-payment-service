package com.example.basicpaymentservice.controller.advice;

import com.example.basicpaymentservice.dto.ErrorResponse;
import com.example.basicpaymentservice.exception.AccountNotFoundException;
import com.example.basicpaymentservice.exception.InsufficientFundsException;
import com.example.basicpaymentservice.exception.InvalidPaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleAccountNotFound(AccountNotFoundException exception) {
        log.error(exception.getMessage());
        return new ErrorResponse("Account not found");
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ErrorResponse handleInsufficientFunds(InsufficientFundsException exception) {
        log.error(exception.getMessage());
        return new ErrorResponse("Insufficient funds");
    }

    @ExceptionHandler(InvalidPaymentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidPayment(InvalidPaymentException exception) {
        log.error(exception.getMessage());
        return new ErrorResponse("Invalid payment");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationFailure(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        log.error(exception.getMessage());

        String errorMessage = "Validation failed due to invalid fields: " + errors.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(","));
        return new ErrorResponse(errorMessage);
    }
}
