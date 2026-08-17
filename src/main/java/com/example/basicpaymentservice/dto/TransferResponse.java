package com.example.basicpaymentservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID transactionId,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        BigDecimal sourceBalance,
        Instant createdAt
) {
}
