package com.example.basicpaymentservice.service.impl;

import com.example.basicpaymentservice.domain.Account;
import com.example.basicpaymentservice.domain.Transaction;
import com.example.basicpaymentservice.dto.TransferRequest;
import com.example.basicpaymentservice.dto.TransferResponse;
import com.example.basicpaymentservice.exception.AccountNotFoundException;
import com.example.basicpaymentservice.exception.InsufficientFundsException;
import com.example.basicpaymentservice.exception.InvalidPaymentException;
import com.example.basicpaymentservice.repository.AccountRepository;
import com.example.basicpaymentservice.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void transferBetweenAccounts_checkBalances() {
        Account source = givenAccount("ACC1", "100.00");
        Account destination = givenAccount("ACC2", "10.00");
        givenSavedTransactionIsReturned();

        paymentService.transfer(request("ACC1", "ACC2", "30.00"));

        assertThat(source.getBalance()).isEqualByComparingTo("70.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("40.00");
    }

    @Test
    void transferBetweenAccounts_checkTransactionDetails() {
        givenAccount("ACC1", "100.00");
        givenAccount("ACC2", "10.00");
        givenSavedTransactionIsReturned();

        TransferResponse response = paymentService.transfer(request("ACC1", "ACC2", "30.00"));

        assertThat(response.sourceAccountId()).isEqualTo("ACC1");
        assertThat(response.destinationAccountId()).isEqualTo("ACC2");
        assertThat(response.amount()).isEqualByComparingTo("30.00");
        assertThat(response.sourceBalance()).isEqualByComparingTo("70.00");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void transferBetweenAccounts_locksLowerAccountIdFirst() {
        givenAccount("ACC1", "100.00");
        givenAccount("ACC2", "100.00");
        givenSavedTransactionIsReturned();

        paymentService.transfer(request("ACC2", "ACC1", "30.00"));

        InOrder inOrder = Mockito.inOrder(accountRepository);
        inOrder.verify(accountRepository).lockById("ACC1");
        inOrder.verify(accountRepository).lockById("ACC2");
    }

    @Test
    void transferBetweenAccounts_rejectsTransferIfSourceHasInsufficientFunds() {
        givenAccount("ACC1", "10.00");
        givenAccount("ACC2", "0.00");

        assertThatThrownBy(() -> paymentService.transfer(request("ACC1", "ACC2", "30.00")))
                .isInstanceOf(InsufficientFundsException.class);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferBetweenAccounts_rejectsTransferWhenSourceAccountIsUnknown() {
        givenMissingAccount("ACC1");

        assertThatThrownBy(() -> paymentService.transfer(request("ACC1", "ACC2", "30.00")))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("ACC1");

        verify(accountRepository, never()).lockById("ACC2");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferBetweenAccounts_rejectsTransferWhenDestinationAccountIsUnknown() {
        givenAccount("ACC1", "100.00");
        givenMissingAccount("ACC2");

        assertThatThrownBy(() -> paymentService.transfer(request("ACC1", "ACC2", "30.00")))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("ACC2");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferBetweenAccounts_rejectsTransferToTheSameAccount() {
        assertThatThrownBy(() -> paymentService.transfer(request("ACC1", "ACC1", "30.00")))
                .isInstanceOf(InvalidPaymentException.class);

        verify(accountRepository, never()).lockById(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-5.00"})
    void transferBetweenAccounts_rejectsNonPositiveAmounts(String inputAmount) {
        assertThatThrownBy(() -> paymentService.transfer(request("ACC1", "ACC2", inputAmount)))
                .isInstanceOf(InvalidPaymentException.class);

        verify(accountRepository, never()).lockById(anyString());
    }

    @Test
    void transferBetweenAccounts_rejectsMissingAmount() {
        assertThatThrownBy(() -> paymentService.transfer(new TransferRequest("ACC1", "ACC2", null)))
                .isInstanceOf(InvalidPaymentException.class);
    }

    private Account givenAccount(String accountId, String balance) {
        Account account = new Account(accountId, new BigDecimal(balance));
        when(accountRepository.lockById(accountId)).thenReturn(Optional.of(account));
        return account;
    }

    private void givenMissingAccount(String accountId) {
        when(accountRepository.lockById(accountId)).thenReturn(Optional.empty());
    }

    private void givenSavedTransactionIsReturned() {
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static TransferRequest request(String source, String destination, String amount) {
        return new TransferRequest(source, destination, new BigDecimal(amount));
    }
}
