package com.ilkinmehdiyev.kapitalsmallbankingrest.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ilkinmehdiyev.kapitalsmallbankingrest.config.TestLiquibaseConfig;
import com.ilkinmehdiyev.kapitalsmallbankingrest.initalizer.PostgresSQLEmbeddedContainer;
import com.ilkinmehdiyev.kapitalsmallbankingrest.model.Customer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = {PostgresSQLEmbeddedContainer.Initializer.class})
@Import(TestLiquibaseConfig.class)
class CustomerRepositoryTest {

  @Autowired private CustomerRepository customerRepository;
  @Autowired private JdbcClient jdbcClient;

  private UUID existingCustomerUid;
  private UUID nonExistingCustomerUid;

  @BeforeEach
  void setUp() {
    existingCustomerUid = UUID.randomUUID();
    nonExistingCustomerUid = UUID.randomUUID();

    populateDb();
  }

  @Test
  @DisplayName("Should find customer by UID when customer exists")
  void shouldFindCustomerByUidWhenCustomerExists() {
    Optional<Customer> customerOptional =
        customerRepository.getCustomerByUidForUpdate(existingCustomerUid);

    assertThat(customerOptional).isPresent();
    Customer customer = customerOptional.get();
    assertThat(customer.uid()).isEqualTo(existingCustomerUid);
    assertThat(customer.name()).isEqualTo("Eldar");
    assertThat(customer.surname()).isEqualTo("Mammadov");
    assertThat(customer.balance()).isEqualByComparingTo(new BigDecimal("100.00"));
  }

  @Test
  @DisplayName("Should return empty when customer does not exist")
  void shouldReturnEmptyWhenCustomerDoesNotExist() {
    Optional<Customer> customerOptional =
        customerRepository.getCustomerByUidForUpdate(nonExistingCustomerUid);

    assertThat(customerOptional).isEmpty();
  }

  @Test
  @DisplayName("Should update customer balance")
  void shouldUpdateCustomerBalance() {
    BigDecimal amountToAdd = new BigDecimal("50.00");

    boolean updated = customerRepository.updateCustomerBalance(existingCustomerUid, amountToAdd);
    assertThat(updated).isTrue();

    Optional<Customer> updatedCustomer =
        customerRepository.getCustomerByUidForUpdate(existingCustomerUid);

    assertThat(updatedCustomer)
        .isPresent()
        .get()
        .extracting("balance")
        .isEqualTo(new BigDecimal("150.00"));
  }

  @Test
  @DisplayName("Should return false when updating non-existing customer")
  void shouldReturnFalseWhenUpdatingNonExistingCustomer() {
    BigDecimal amountToAdd = new BigDecimal("50.00");
    boolean updated = customerRepository.updateCustomerBalance(nonExistingCustomerUid, amountToAdd);

    assertThat(updated).isFalse();
  }

  private void populateDb() {
    jdbcClient
        .sql(
            """
                    INSERT INTO customers (uid, name, surname, balance, phone_number, birth_date, password)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)
        .params(
            existingCustomerUid,
            "Eldar",
            "Mammadov",
            new BigDecimal("100.00"),
            "+994501234567",
            LocalDate.now().minusYears(25),
                "$2a$10$f0zqjKXV4MEwHinjHcWUpeeVpeGH55k4FsHqDQhuAxCUmMrV.CmD.")
        .update();
  }
}
