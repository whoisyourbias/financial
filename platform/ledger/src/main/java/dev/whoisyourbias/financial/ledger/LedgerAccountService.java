package dev.whoisyourbias.financial.ledger;

import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LedgerAccountService {

  private final LedgerAccountRepository repository;
  private final Clock clock;

  LedgerAccountService(LedgerAccountRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  public AccountView create(AccountType accountType, Currency currency) {
    Objects.requireNonNull(accountType, "accountType must not be null");
    Objects.requireNonNull(currency, "currency must not be null");
    LedgerAccountEntity entity =
        new LedgerAccountEntity(
            UUID.randomUUID(), accountType, currency.getCurrencyCode(), Instant.now(clock));
    return toView(repository.saveAndFlush(entity));
  }

  @Transactional(readOnly = true)
  public Optional<AccountView> find(UUID accountId) {
    Objects.requireNonNull(accountId, "accountId must not be null");
    return repository.findById(accountId).map(LedgerAccountService::toView);
  }

  private static AccountView toView(LedgerAccountEntity entity) {
    return new AccountView(
        entity.id(),
        entity.accountType(),
        Currency.getInstance(entity.currency()),
        entity.createdAt());
  }
}
