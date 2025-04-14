package com.ilkinmehdiyev.kapitalsmallbankingrest.repository;

import com.ilkinmehdiyev.kapitalsmallbankingrest.model.Transaction;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionStatus;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.ilkinmehdiyev.kapitalsmallbankingrest.utils.RepositoryUtils.columnNamesFrom;
import static com.ilkinmehdiyev.kapitalsmallbankingrest.utils.RepositoryUtils.columnValuesFrom;
import static com.ilkinmehdiyev.kapitalsmallbankingrest.utils.RepositoryUtils.nullSafePut;

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
        nullSafePut(params, TRANSACTION_STATUS, transaction.status());
        nullSafePut(params, TRANSACTION_TYPE, transaction.type());
        nullSafePut(params, DESCRIPTION, transaction.description());
        nullSafePut(params, AMOUNT, transaction.amount());
        nullSafePut(params, TRANSACTION_DATE, transaction.transactionDate());

        return params;
    }

    public TransactionResponse insertTransaction(Transaction transaction) {
        var params = sqlParameters(transaction);
        KeyHolder holder = new GeneratedKeyHolder();

        jdbcClient
                .sql("""
                        INSERT INTO transactions (%s) VALUES (%s) RETURNING uid, status, transaction_date
                        """.formatted(columnNamesFrom(params), columnValuesFrom(params)))
                .update(holder);

        Map<String, Object> keys = holder.getKeys();
        return Objects.isNull(keys) ? null : new TransactionResponse((UUID) keys.get(UID), (TransactionStatus) keys.get(TRANSACTION_STATUS), (Instant) keys.get(TRANSACTION_DATE));
    }

    public TransactionResponse topUpByCustomerId(Long customerId, UUID customerUid, BigDecimal amount, UUID idempotencyKey) {
        if (Objects.isNull(customerUid)) {
            return null;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("amount must be greater than zero");
        }


        var tnx = new Transaction(idempotencyKey, customerId, TransactionType.TOP_UP, amount, TransactionType.TOP_UP.toString(), Instant.now(), TransactionStatus.PENDING, "");

        boolean updated = customerRepository.updateCustomerBalance(customerUid, amount);
        if (!updated) {
            var tnxResponse = insertTransaction(tnx);

            log.error("Customer with id {} could no update balance. Transaction uid: [{}]", customerUid, tnxResponse.transactionUid());
        }


        return insertTransaction(tnx);
    }
}
