package com.example.basicpaymentservice.controller;

import com.example.basicpaymentservice.dto.AccountResponse;
import com.example.basicpaymentservice.dto.TransactionResponse;
import com.example.basicpaymentservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountAPI {

    private final AccountService accountService;

    @GetMapping("/{accountId}")
    public AccountResponse getAccount(@PathVariable String accountId) {
        return accountService.getAccount(accountId);
    }

    @GetMapping("/{accountId}/transactions")
    public List<TransactionResponse> getTransactions(@PathVariable String accountId) {
        return accountService.getTransactions(accountId);
    }
}
