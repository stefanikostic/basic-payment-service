package com.example.basicpaymentservice.repository;

import com.example.basicpaymentservice.domain.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void beforeEach() {
        accountRepository.saveAll(
                List.of(
                new Account("ACC1", new BigDecimal("30.00")),
                new Account("ACC2", new BigDecimal("10.00")),
                new Account("ACC3", new BigDecimal("20.00"))
        ));
    }

    @Test
    void locksAnExistingAccount() {
        assertThat(accountRepository.lockById("ACC2"))
                .get()
                .satisfies(account -> {
                    assertThat(account.getId()).isEqualTo("ACC2");
                    assertThat(account.getBalance()).isEqualByComparingTo("10.00");
                });
    }

    @Test
    void returnsEmptyForAnUnknownAccount() {
        assertThat(accountRepository.lockById("MISSING")).isEmpty();
    }
}
