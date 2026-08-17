package com.example.basicpaymentservice.dto;

import java.math.BigDecimal;

public record AccountResponse(
        String id,
        BigDecimal balance
) {
}
