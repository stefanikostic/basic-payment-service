package com.example.basicpaymentservice.service;

import com.example.basicpaymentservice.dto.AccountResponse;
import com.example.basicpaymentservice.dto.TransactionResponse;

import java.util.List;

public interface AccountService {

    AccountResponse getAccount(String accountId);

    List<TransactionResponse> getTransactions(String accountId);
}
