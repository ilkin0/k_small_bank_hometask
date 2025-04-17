package com.ilkinmehdiyev.kapitalsmallbankingrest.repository;

import com.ilkinmehdiyev.kapitalsmallbankingrest.exception.TransactionException;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.Customer;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomerRepository {
  private final JdbcClient jdbcClient;

  public Optional<Customer> getCustomerByUidForUpdate(UUID accountUid) {
    return jdbcClient
        .sql(
            """
                        SELECT * FROM customers
                        WHERE uid = :uid
                        FOR UPDATE
                        """)
        .param("uid", accountUid)
        .query(Customer.class)
        .optional();
  }

  public boolean updateCustomerBalance(UUID customerUid, BigDecimal amount) {
    try {
      var updCount =
          jdbcClient
              .sql(
                  """
                              UPDATE customers SET balance = balance + :amount WHERE uid = :uid
                              """)
              .param("amount", amount)
              .param("uid", customerUid)
              .update();

      return updCount == 1;
    } catch (Exception e) {
      log.error("Could not update customer balance.Customer uid {}.", customerUid, e);
      throw new TransactionException(
          "Could not update customer balance, customerUid: [%s]".formatted(customerUid));
    }
  }

  public Optional<Customer> findByPhoneNumber(String phoneNumber) {
    return jdbcClient
        .sql(
            """
                        SELECT * FROM customers
                        WHERE phone_number = :phoneNumber
                        FOR UPDATE
                        """)
        .param("phoneNumber", phoneNumber)
        .query(Customer.class)
        .optional();
  }
}
