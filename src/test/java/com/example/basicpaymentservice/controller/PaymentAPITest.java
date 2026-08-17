package com.example.basicpaymentservice.controller;

import com.example.basicpaymentservice.dto.TransferRequest;
import com.example.basicpaymentservice.dto.TransferResponse;
import com.example.basicpaymentservice.exception.AccountNotFoundException;
import com.example.basicpaymentservice.exception.InsufficientFundsException;
import com.example.basicpaymentservice.exception.InvalidPaymentException;
import com.example.basicpaymentservice.service.PaymentService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentAPI.class)
class PaymentAPITest {

    public static final String TRANSFERS_API = "/api/transfers";
    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void transfer_validRequest_returnsCreatedWithTransferDetails() throws Exception {
        UUID transactionId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        when(paymentService.transfer(any())).thenReturn(new TransferResponse(
                transactionId, "ACC-1", "ACC-2", new BigDecimal("10.00"), new BigDecimal("990.00"),
                Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(postTransfer("""
                        {
                            "sourceAccountId":"ACC-1",
                            "destinationAccountId":"ACC-2",
                            "amount":10.00
                        }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.sourceAccountId").value("ACC-1"))
                .andExpect(jsonPath("$.destinationAccountId").value("ACC-2"))
                .andExpect(jsonPath("$.amount").value(10.00))
                .andExpect(jsonPath("$.sourceBalance").value(990.00));
    }

    @Test
    void transfer_invalidPayment_returnsBadRequest() throws Exception {
        when(paymentService.transfer(any()))
                .thenThrow(new InvalidPaymentException("Source and destination accounts must differ"));

        mockMvc.perform(postTransfer("""
                        {
                            "sourceAccountId":"ACC-1",
                            "destinationAccountId":"ACC-1",
                            "amount":10.00
                        }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Invalid payment"));
    }

    @Test
    void transfer_unknownAccount_returnsNotFound() throws Exception {
        when(paymentService.transfer(any())).thenThrow(new AccountNotFoundException("NOPE"));

        mockMvc.perform(postTransfer("""
                        {
                            "sourceAccountId":"ACC-1",
                            "destinationAccountId":"NOPE",
                            "amount":10.00
                        }"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Account not found"));
    }

    @Test
    void transfer_insufficientFunds_returnsUnprocessableContent() throws Exception {
        when(paymentService.transfer(any()))
                .thenThrow(new InsufficientFundsException("Insufficient funds in account ACC-3"));

        mockMvc.perform(postTransfer("""
                        {
                            "sourceAccountId":"ACC-3",
                            "destinationAccountId":"ACC-2",
                            "amount":10.00
                        }"""))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.errorMessage").value("Insufficient funds"));
    }

    @Test
    void transfer_amountBelowMinimum_returnsBadRequestNamingTheField() throws Exception {
        mockMvc.perform(postTransfer("""
                        {
                            "sourceAccountId":"ACC-1",
                            "destinationAccountId":"ACC-2",
                            "amount":0.001
                        }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage", Matchers.containsString("amount")));

        verify(paymentService, never()).transfer(any(TransferRequest.class));
    }

    @Test
    void transfer_missingAmount_returnsBadRequestNamingTheField() throws Exception {
        mockMvc.perform(postTransfer("""
                        {
                            "sourceAccountId":"ACC-1",
                            "destinationAccountId":"ACC-2"
                        }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage", Matchers.containsString("amount is required")));

        verify(paymentService, never()).transfer(any(TransferRequest.class));
    }

    @Test
    void transfer_blankAccountIds_returnsBadRequestNamingTheFields() throws Exception {
        mockMvc.perform(postTransfer("""
                        {
                            "sourceAccountId":"",
                            "destinationAccountId":"",
                            "amount":10.00
                        }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage", Matchers.containsString("sourceAccountId is required")))
                .andExpect(jsonPath("$.errorMessage", Matchers.containsString("destinationAccountId is required")));

        verify(paymentService, never()).transfer(any(TransferRequest.class));
    }

    private static MockHttpServletRequestBuilder postTransfer(String body) {
        return post(TRANSFERS_API).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
