package dev.whoisyourbias.financial.shared;

import java.util.Currency;
import java.util.Objects;

public record Money(long amountMinor, Currency currency) {

  public Money {
    Objects.requireNonNull(currency, "currency must not be null");
  }

  public Money add(Money other) {
    requireSameCurrency(other);
    return new Money(Math.addExact(amountMinor, other.amountMinor), currency);
  }

  public Money subtract(Money other) {
    requireSameCurrency(other);
    return new Money(Math.subtractExact(amountMinor, other.amountMinor), currency);
  }

  public Money negate() {
    return new Money(Math.negateExact(amountMinor), currency);
  }

  private void requireSameCurrency(Money other) {
    Objects.requireNonNull(other, "other money must not be null");
    if (!currency.equals(other.currency)) {
      throw new IllegalArgumentException(
          "Currency mismatch: "
              + currency.getCurrencyCode()
              + " != "
              + other.currency.getCurrencyCode());
    }
  }
}
