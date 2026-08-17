package com.example.basicpaymentservice.service.impl;

import com.example.basicpaymentservice.domain.Account;
import com.example.basicpaymentservice.domain.Transaction;
import com.example.basicpaymentservice.dto.TransferRequest;
import com.example.basicpaymentservice.dto.TransferResponse;
import com.example.basicpaymentservice.exception.AccountNotFoundException;
import com.example.basicpaymentservice.exception.InvalidPaymentException;
import com.example.basicpaymentservice.repository.AccountRepository;
import com.example.basicpaymentservice.repository.TransactionRepository;
import com.example.basicpaymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        validate(request);

        String sourceId = request.sourceAccountId();
        String destinationId = request.destinationAccountId();

        boolean sourceLocksFirst = sourceId.compareTo(destinationId) < 0;
        Account firstLocked = lock(sourceLocksFirst ? sourceId : destinationId);
        Account secondLocked = lock(sourceLocksFirst ? destinationId : sourceId);

        Account source = sourceLocksFirst ? firstLocked : secondLocked;
        Account destination = sourceLocksFirst ? secondLocked : firstLocked;

        source.withdraw(request.amount());
        destination.deposit(request.amount());

        Transaction transaction = transactionRepository.save(
                new Transaction(source, destination, request.amount(), Instant.now()));

        return new TransferResponse(transaction.getId(), sourceId, destinationId, request.amount(), source.getBalance(),
                transaction.getCreatedAt());
    }

    private void validate(TransferRequest request) {
        if (StringUtils.isBlank(request.sourceAccountId())
                || StringUtils.isBlank(request.destinationAccountId())) {
            throw new InvalidPaymentException("Source or destination account is blank");
        }
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new InvalidPaymentException("Source and destination accounts must differ");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Transfer amount must be positive");
        }
    }

    private Account lock(String accountId) {
        return accountRepository.lockById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
