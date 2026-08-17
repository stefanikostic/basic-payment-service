package com.example.basicpaymentservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        Instant createdAt
) {
}
