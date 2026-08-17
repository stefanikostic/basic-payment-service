package com.example.basicpaymentservice.repository;

import com.example.basicpaymentservice.domain.Account;
import com.example.basicpaymentservice.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TransactionRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Account account1;
    private Account account2;
    private Account account3;

    @BeforeEach
    void createAccounts() {
        account1 = accountRepository.save(new Account("ACC1", new BigDecimal("100.00")));
        account2 = accountRepository.save(new Account("ACC2", new BigDecimal("100.00")));
        account3 = accountRepository.save(new Account("ACC3", new BigDecimal("100.00")));
    }

    @Test
    void returnsIdOnSaveSuccessfully() {
        Transaction saved = transactionRepository.save(
                new Transaction(account1, account2, new BigDecimal("25.00"), NOW));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void returnsEmptyListForAccountWithNoTransactions() {
        assertThat(transactionRepository.findByAccountId("notFoundId")).isEmpty();
    }

    @Test
    void findsTransactionsWhereAccountIsSourceOrDestination() {
        transactionRepository.saveAll(List.of(
                new Transaction(account1, account2, new BigDecimal("10.00"), NOW),
                new Transaction(account3, account1, new BigDecimal("20.00"), NOW),
                new Transaction(account2, account3, new BigDecimal("30.00"), NOW)
        ));

        List<Transaction> found = transactionRepository.findByAccountId("ACC1");

        assertThat(found).extracting(Transaction::getAmount)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactlyInAnyOrder(new BigDecimal("10.00"), new BigDecimal("20.00"));
    }

    @Test
    void returnsNewestFirst() {
        transactionRepository.saveAll(List.of(
                new Transaction(account1, account2, new BigDecimal("10.00"), NOW.minusSeconds(60)),
                new Transaction(account1, account2, new BigDecimal("20.00"), NOW),
                new Transaction(account1, account2, new BigDecimal("30.00"), NOW.minusSeconds(30))
        ));

        List<Transaction> found = transactionRepository.findByAccountId("ACC1");

        assertThat(found).extracting(Transaction::getCreatedAt)
                .containsExactly(NOW, NOW.minusSeconds(30), NOW.minusSeconds(60));
    }
}
