package com.ilkinmehdiyev.kapitalsmallbankingrest.repository;

import static com.ilkinmehdiyev.kapitalsmallbankingrest.utils.RepositoryUtils.columnNamesFrom;
import static com.ilkinmehdiyev.kapitalsmallbankingrest.utils.RepositoryUtils.columnValuesFrom;

import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionRequest;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.exception.TransactionException;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.Transaction;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionStatus;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
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

  public UUID insertTransaction(Transaction transaction) {
    var params = tnxSqlParameters(transaction);
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

  public TransactionResponse processTransactionByCustomerId(
      Long customerId, TransactionRequest transactionRequest, UUID idempotencyKey) {
    BigDecimal amount = getTransactionAmount(transactionRequest);
    UUID customerUid = transactionRequest.customerUid();
    var tnx = getTransaction(customerId, transactionRequest, idempotencyKey);
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

  private static BigDecimal getTransactionAmount(TransactionRequest transactionRequest) {
    if (TransactionType.TOP_UP.equals(transactionRequest.transactionType())) {
      return transactionRequest.amount();
    } else {
      return transactionRequest.amount().negate();
    }
  }

  private static Transaction getTransaction(
      Long customerId, TransactionRequest request, UUID idempotencyKey) {
    TransactionType type = request.transactionType();
    return new Transaction(
        idempotencyKey,
        customerId,
        type,
        request.amount(),
        type.toString(),
        Instant.now(),
        TransactionStatus.PENDING,
        "");
  }
}
