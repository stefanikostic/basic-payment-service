package com.example.basicpaymentservice.service.impl;

import com.example.basicpaymentservice.dto.AccountResponse;
import com.example.basicpaymentservice.dto.TransactionResponse;
import com.example.basicpaymentservice.exception.AccountNotFoundException;
import com.example.basicpaymentservice.repository.AccountRepository;
import com.example.basicpaymentservice.repository.TransactionRepository;
import com.example.basicpaymentservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public AccountResponse getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .map(a -> new AccountResponse(a.getId(), a.getBalance()))
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Override
    public List<TransactionResponse> getTransactions(String accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        return transactionRepository.findByAccountId(accountId).stream()
                .map(t -> new TransactionResponse(
                        t.getId(),
                        t.getSourceAccount().getId(),
                        t.getDestinationAccount().getId(),
                        t.getAmount(),
                        t.getCreatedAt()))
                .toList();
    }
}
