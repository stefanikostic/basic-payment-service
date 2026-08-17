package com.example.basicpaymentservice.repository;

import com.example.basicpaymentservice.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("""
            select t from Transaction t
            join fetch t.sourceAccount join fetch t.destinationAccount
            where t.sourceAccount.id = :accountId or t.destinationAccount.id = :accountId
            order by t.createdAt desc
            """)
    List<Transaction> findByAccountId(@Param("accountId") String accountId);
}
