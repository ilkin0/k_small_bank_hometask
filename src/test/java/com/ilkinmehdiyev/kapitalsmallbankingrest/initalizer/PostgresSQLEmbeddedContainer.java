package com.ilkinmehdiyev.kapitalsmallbankingrest.initalizer;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresSQLEmbeddedContainer
    extends PostgreSQLContainer<PostgresSQLEmbeddedContainer> {
  public static final PostgresSQLEmbeddedContainer INSTANCE =
      new PostgresSQLEmbeddedContainer().withReuse(true);

  private PostgresSQLEmbeddedContainer() {
    super("postgres:17");
  }

  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      INSTANCE.start();

      TestPropertyValues.of(
              "spring.datasource.url=".concat(INSTANCE.getJdbcUrl()),
              "spring.datasource.username=".concat(INSTANCE.getUsername()),
              "spring.datasource.password=".concat(INSTANCE.getPassword()))
          .applyTo(applicationContext.getEnvironment());
    }
  }
}
