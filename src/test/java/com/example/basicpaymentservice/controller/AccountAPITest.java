package com.example.basicpaymentservice.controller;

import com.example.basicpaymentservice.dto.AccountResponse;
import com.example.basicpaymentservice.dto.TransactionResponse;
import com.example.basicpaymentservice.exception.AccountNotFoundException;
import com.example.basicpaymentservice.service.AccountService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountAPI.class)
class AccountAPITest {

    @MockitoBean
    private AccountService accountService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAccount_returnsOkWithBalance() throws Exception {
        when(accountService.getAccount("ACC-1"))
                .thenReturn(new AccountResponse("ACC-1", new BigDecimal("1000.00")));

        mockMvc.perform(get("/api/accounts/ACC-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("ACC-1"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    void getAccount_unknownAccount_returnsNotFound() throws Exception {
        when(accountService.getAccount(anyString()))
                .thenThrow(new AccountNotFoundException("NotFoundId"));

        mockMvc.perform(get("/api/accounts/NotFoundId"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Account not found"));
    }

    @Test
    void getTransactions_returnsOkWithTransactions() throws Exception {
        UUID transactionId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        when(accountService.getTransactions("ACC-1")).thenReturn(List.of(new TransactionResponse(
                transactionId, "ACC-1", "ACC-2", new BigDecimal("10.00"),
                Instant.parse("2026-01-01T00:00:00Z"))));

        mockMvc.perform(get("/api/accounts/ACC-1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(transactionId.toString()))
                .andExpect(jsonPath("$[0].sourceAccountId").value("ACC-1"))
                .andExpect(jsonPath("$[0].destinationAccountId").value("ACC-2"))
                .andExpect(jsonPath("$[0].amount").value(10.00));
    }

    @Test
    void getTransactions_noTransactions_returnsOkWithEmptyArray() throws Exception {
        when(accountService.getTransactions("ACC-3")).thenReturn(List.of());

        mockMvc.perform(get("/api/accounts/ACC-3/transactions"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getTransactions_unknownAccount_returnsNotFound() throws Exception {
        when(accountService.getTransactions(anyString()))
                .thenThrow(new AccountNotFoundException("NotFoundId"));

        mockMvc.perform(get("/api/accounts/NotFoundId/transactions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Account not found"));
    }
}
