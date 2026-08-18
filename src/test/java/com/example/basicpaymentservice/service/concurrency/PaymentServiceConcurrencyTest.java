package com.example.basicpaymentservice.service.concurrency;

import com.example.basicpaymentservice.domain.Account;
import com.example.basicpaymentservice.dto.TransferRequest;
import com.example.basicpaymentservice.exception.InsufficientFundsException;
import com.example.basicpaymentservice.repository.AccountRepository;
import com.example.basicpaymentservice.repository.TransactionRepository;
import com.example.basicpaymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PaymentServiceConcurrencyTest {

    private static final String SOURCE = "ACC1";
    private static final String DESTINATION = "ACC2";

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void resetAccounts() {
        transactionRepository.deleteAll();
        accountRepository.saveAll(List.of(
                new Account(SOURCE, new BigDecimal("100.00")),
                new Account(DESTINATION, new BigDecimal("0.00"))
        ));
    }

    @Test
    void concurrentTransfers_BalanceIsCorrect() throws Exception {
        int transfers = 50;
        BigDecimal amount = new BigDecimal("1.00");

        runConcurrently(transfers, () -> paymentService.transfer(
                new TransferRequest(SOURCE, DESTINATION, amount)));

        assertThat(balanceOf(SOURCE)).isEqualByComparingTo("50.00");
        assertThat(balanceOf(DESTINATION)).isEqualByComparingTo("50.00");
        assertThat(transactionRepository.findByAccountId(SOURCE)).hasSize(transfers);
    }

    @Test
    void concurrentTransfers_CannotOverdrawSourceAccount() throws Exception {
        int attempts = 30;
        BigDecimal amount = new BigDecimal("10.00");
        AtomicInteger rejected = new AtomicInteger();

        runConcurrently(attempts, () -> {
            try {
                return paymentService.transfer(new TransferRequest(SOURCE, DESTINATION, amount));
            } catch (InsufficientFundsException e) {
                rejected.incrementAndGet();
                return null;
            }
        });

        assertThat(balanceOf(SOURCE)).isEqualByComparingTo("0.00");
        assertThat(balanceOf(DESTINATION)).isEqualByComparingTo("100.00");
        assertThat(rejected.get()).isEqualTo(attempts - 10);
        assertThat(transactionRepository.findByAccountId(SOURCE)).hasSize(10);
    }

    @Test
    void concurrentTransfers_OpposingTransfersDoNotDeadlock() throws Exception {
        int pairs = 20;
        BigDecimal amount = new BigDecimal("1.00");
        accountRepository.save(new Account(DESTINATION, new BigDecimal("100.00")));

        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < pairs; i++) {
                futures.add(executor.submit(() -> paymentService.transfer(
                        new TransferRequest(SOURCE, DESTINATION, amount))));
                futures.add(executor.submit(() -> paymentService.transfer(
                        new TransferRequest(DESTINATION, SOURCE, amount))));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        }

        assertThat(balanceOf(SOURCE)).isEqualByComparingTo("100.00");
        assertThat(balanceOf(DESTINATION)).isEqualByComparingTo("100.00");
    }

    private void runConcurrently(int times, Callable<Object> task) throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            for (Future<Object> future : executor.invokeAll(Collections.nCopies(times, task))) {
                future.get();
            }
        }
    }

    private BigDecimal balanceOf(String accountId) {
        return accountRepository.findById(accountId).orElseThrow().getBalance();
    }
}
