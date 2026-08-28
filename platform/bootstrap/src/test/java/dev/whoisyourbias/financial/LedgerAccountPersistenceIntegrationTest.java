package dev.whoisyourbias.financial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.whoisyourbias.financial.ledger.AccountType;
import dev.whoisyourbias.financial.ledger.AccountView;
import dev.whoisyourbias.financial.ledger.LedgerAccountService;
import java.time.Duration;
import java.util.Currency;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@SpringBootTest
@Testcontainers
class LedgerAccountPersistenceIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE =
      DockerImageName.parse(
              "postgres:18.6@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280")
          .asCompatibleSubstituteFor("postgres");

  @Container
  static final PostgreSQLContainer postgres =
      new PostgreSQLContainer(POSTGRES_IMAGE)
          .withDatabaseName("financial")
          .withUsername("financial")
          .withPassword("financial_test")
          .withEnv("POSTGRES_INITDB_ARGS", "--encoding=UTF8 --locale=C.UTF-8")
          .withEnv("TZ", "UTC")
          .waitingFor(Wait.forSuccessfulCommand("pg_isready -U financial -d financial"))
          .withStartupTimeout(Duration.ofSeconds(120));

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private ApplicationContext applicationContext;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private LedgerAccountService accountService;

  @Test
  void startsBootContextAndAppliesFreshMigration() {
    assertNotNull(applicationContext);
    Integer flywayVersion =
        jdbcTemplate.queryForObject(
            "SELECT MAX(CAST(version AS INTEGER)) FROM flyway_schema_history WHERE success",
            Integer.class);
    assertEquals(1, flywayVersion);

    Integer balanceColumns =
        jdbcTemplate.queryForObject(
            """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'ledger_account'
                          AND column_name = 'balance'
                        """,
            Integer.class);
    assertEquals(0, balanceColumns);
  }

  @Test
  void savesAndReadsAnAccountThroughTheLedgerBoundary() {
    AccountView created = accountService.create(AccountType.ASSET, Currency.getInstance("KRW"));

    AccountView loaded = accountService.find(created.id()).orElseThrow();

    assertEquals(created, loaded);
    assertTrue(loaded.createdAt().toEpochMilli() > 0);
  }
}
