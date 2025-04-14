package com.ilkinmehdiyev.kapitalsmallbankingrest.repository;

import static com.ilkinmehdiyev.kapitalsmallbankingrest.utils.RepositoryUtils.columnNamesFrom;
import static com.ilkinmehdiyev.kapitalsmallbankingrest.utils.RepositoryUtils.columnValuesFrom;
import static com.ilkinmehdiyev.kapitalsmallbankingrest.utils.RepositoryUtils.nullSafePut;

import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TopUpRequest;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.exception.TransactionException;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.Transaction;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionStatus;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TransactionRepository extends AbstractBaseRepository {
  private final JdbcClient jdbcClient;
  private final CustomerRepository customerRepository;

  private static Map<String, Object> sqlParameters(@NotNull Transaction transaction) {
    final Map<String, Object> params = new HashMap<>();

    nullSafePut(params, ID, transaction.id());
    nullSafePut(params, UID, transaction.uid());
    nullSafePut(params, CUSTOMER_ID, transaction.customerId());
    nullSafePut(params, REFERENCE_ID, transaction.referenceId());
    nullSafePut(
        params, TRANSACTION_STATUS, Optional.ofNullable(transaction.status()).map(Enum::toString));
    nullSafePut(
        params, TRANSACTION_TYPE, Optional.ofNullable(transaction.type()).map(Enum::toString));
    nullSafePut(params, DESCRIPTION, transaction.description());
    nullSafePut(params, AMOUNT, transaction.amount());
    nullSafePut(
        params,
        TRANSACTION_DATE,
        Optional.ofNullable(transaction.transactionDate()).map(Timestamp::from));

    return params;
  }

  public UUID insertTransaction(Transaction transaction) {
    var params = sqlParameters(transaction);
    KeyHolder holder = new GeneratedKeyHolder();

    String formatted =
        """
            INSERT INTO transactions (%s) VALUES (%s) RETURNING uid, status, transaction_date
            """
            .formatted(columnNamesFrom(params), columnValuesFrom(params));

    jdbcClient.sql(formatted).params(params).update(holder);

    Map<String, Object> keys = holder.getKeys();
    return (UUID) Objects.requireNonNull(keys).get(UID);
  }

  public boolean updateTransactionStatusBy(UUID transactionUid, TransactionStatus status) {
    int updateCount =
        jdbcClient
            .sql(
                """
                        UPDATE transactions SET status = :status WHERE uid = :uid
                        """)
            .param("status", status.toString())
            .param("uid", transactionUid)
            .update();

    return updateCount == 1;
  }

  public Optional<Transaction> findByUid(@NotNull UUID uid) {
    return jdbcClient
        .sql(
            """
                        SELECT * FROM transactions WHERE uid = :uid
                        """)
        .param("uid", uid)
        .query(Transaction.class)
        .optional();
  }

  public TransactionResponse topUpByCustomerId(
      Long customerId, TopUpRequest topUpRequest, UUID idempotencyKey) {
    BigDecimal amount = topUpRequest.amount();
    UUID customerUid = topUpRequest.customerUid();
    var tnx = getTransaction(customerId, amount, idempotencyKey);
    var tnxUid = insertTransaction(tnx);

    boolean balanceUpdated = customerRepository.updateCustomerBalance(customerUid, amount);
    if (!balanceUpdated) {
      updateTransactionStatusBy(tnxUid, TransactionStatus.FAILED);
      log.error(
          "Could not update customer balance.Customer uid {}. Transaction uid: [{}]",
          customerUid,
          tnxUid);
      throw new TransactionException("Could not update Transaction: [%s]".formatted(tnxUid));
    }

    boolean tnxUpdated = updateTransactionStatusBy(tnxUid, TransactionStatus.COMPLETED);
    if (!tnxUpdated) {
      log.error("Could not update Transaction: [{}]", tnxUid);
      throw new TransactionException("Could not update Transaction: [%s]".formatted(tnxUid));
    }

    return new TransactionResponse(tnxUid, TransactionStatus.COMPLETED, tnx.transactionDate());
  }

  private static Transaction getTransaction(
      Long customerId, BigDecimal amount, UUID idempotencyKey) {
    return new Transaction(
        idempotencyKey,
        customerId,
        TransactionType.TOP_UP,
        amount,
        TransactionType.TOP_UP.toString(),
        Instant.now(),
        TransactionStatus.PENDING,
        "");
  }
}
