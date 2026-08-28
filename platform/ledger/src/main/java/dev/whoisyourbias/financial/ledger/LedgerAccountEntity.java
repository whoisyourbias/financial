package dev.whoisyourbias.financial.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_account")
class LedgerAccountEntity {

  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_type", nullable = false, length = 32)
  private AccountType accountType;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected LedgerAccountEntity() {}

  LedgerAccountEntity(UUID id, AccountType accountType, String currency, Instant createdAt) {
    this.id = id;
    this.accountType = accountType;
    this.currency = currency;
    this.createdAt = createdAt;
  }

  UUID id() {
    return id;
  }

  AccountType accountType() {
    return accountType;
  }

  String currency() {
    return currency;
  }

  Instant createdAt() {
    return createdAt;
  }
}
