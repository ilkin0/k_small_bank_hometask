package com.ilkinmehdiyev.kapitalsmallbankingrest.service;

import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TopUpRequest;
import com.ilkinmehdiyev.kapitalsmallbankingrest.dto.TransactionResponse;
import com.ilkinmehdiyev.kapitalsmallbankingrest.exception.CustomerNotFoundException;
import com.ilkinmehdiyev.kapitalsmallbankingrest.repository.CustomerRepository;
import com.ilkinmehdiyev.kapitalsmallbankingrest.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;


    @Transactional
    public TransactionResponse topUpCustomer(TopUpRequest request, UUID idempotencyKey) {
        UUID customerUid = request.customerUid();
        BigDecimal amount = request.amount();
        log.info("Top up customer with uid: {}", customerUid);


        var customer = customerRepository.getCustomerByUidForUpdate(customerUid)
                .orElseThrow(() -> {
                    log.error("Customer with id {} not found", customerUid);
                    return new CustomerNotFoundException("Customer with id: [%s] not found".formatted(customerUid));
                });

        return transactionRepository.topUpByCustomerId(customer.id(), customerUid, amount, idempotencyKey);
    }
}
