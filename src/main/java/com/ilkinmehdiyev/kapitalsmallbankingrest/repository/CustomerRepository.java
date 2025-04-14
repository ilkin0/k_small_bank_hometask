package com.ilkinmehdiyev.kapitalsmallbankingrest.repository;

import com.ilkinmehdiyev.kapitalsmallbankingrest.model.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CustomerRepository {
    private final JdbcClient jdbcClient;

    public Optional<Customer> getCustomerByUidForUpdate(UUID accountUid) {
        return jdbcClient
                .sql("""
                        SELECT * FROM customers
                        WHERE uid = :uid
                        FOR UPDATE
                        """)
                .param("uid", accountUid)
                .query(Customer.class)
                .optional();
    }

    public boolean updateCustomerBalance(UUID customerUid, BigDecimal amount) {
        var updCount = jdbcClient.sql("""
                        UPDATE customers SET balance = balance + :amount WHERE uid = :uid
                        """)
                .param("amount", amount)
                .param("uid", customerUid)
                .update();

        return updCount == 1;
    }
}
