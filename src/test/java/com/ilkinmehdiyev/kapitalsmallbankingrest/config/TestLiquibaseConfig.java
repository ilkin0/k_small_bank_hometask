package com.ilkinmehdiyev.kapitalsmallbankingrest.config;

import com.ilkinmehdiyev.kapitalsmallbankingrest.initalizer.PostgresSQLEmbeddedContainer;
import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DriverManager;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@TestConfiguration
public class TestLiquibaseConfig {

  @Bean
  public DataSource dataSource() {
    try {
      String jdbcUrl = PostgresSQLEmbeddedContainer.INSTANCE.getJdbcUrl();
      String username = PostgresSQLEmbeddedContainer.INSTANCE.getUsername();
      String password = PostgresSQLEmbeddedContainer.INSTANCE.getPassword();

      return new SingleConnectionDataSource(jdbcUrl, username, password, true);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create test data source", e);
    }
  }

  @Bean
  public JdbcTemplate jdbcTemplate() {
    return new JdbcTemplate(dataSource());
  }

  @PostConstruct
  public void initLiquibase() {
    try (Connection connection =
        DriverManager.getConnection(
            PostgresSQLEmbeddedContainer.INSTANCE.getJdbcUrl(),
            PostgresSQLEmbeddedContainer.INSTANCE.getUsername(),
            PostgresSQLEmbeddedContainer.INSTANCE.getPassword())) {

      Database database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));

      try (Liquibase liquibase =
          new Liquibase(
              "db/changelog/db.changelog-master.yaml",
              new ClassLoaderResourceAccessor(),
              database)) {

        liquibase.update(new Contexts(), new LabelExpression());
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to execute Liquibase migrations for test", e);
    }
  }
}
