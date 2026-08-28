package dev.whoisyourbias.financial.ledger;

import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

public record AccountView(UUID id, AccountType accountType, Currency currency, Instant createdAt) {}
