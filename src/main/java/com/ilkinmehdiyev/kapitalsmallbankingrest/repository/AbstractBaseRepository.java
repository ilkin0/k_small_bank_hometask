package com.ilkinmehdiyev.kapitalsmallbankingrest.repository;

import static com.ilkinmehdiyev.kapitalsmallbankingrest.utils.RepositoryUtils.nullSafePut;

import com.ilkinmehdiyev.kapitalsmallbankingrest.model.Transaction;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AbstractBaseRepository {
  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String CREATED_AT = "created_at";
  public static final String UPDATED_AT = "updated_at";
  public static final String UID = "uid";
  public static final String CREATED_BY = "created_by";
  public static final String CUSTOMER_ID = "customer_id";
  public static final String AMOUNT = "amount";
  public static final String DESCRIPTION = "description";
  public static final String TRANSACTION_DATE = "transaction_date";
  public static final String TRANSACTION_TYPE = "type";
  public static final String TRANSACTION_STATUS = "status";
  public static final String REFERENCE_ID = "reference_uid";

  protected Map<String, Object> tnxSqlParameters(@NotNull Transaction transaction) {
    final Map<String, Object> params = new HashMap<>();

    nullSafePut(params, ID, transaction.id());
    nullSafePut(params, UID, transaction.uid());
    nullSafePut(params, CUSTOMER_ID, transaction.customerId());
    nullSafePut(params, REFERENCE_ID, transaction.referenceUid());
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
}
