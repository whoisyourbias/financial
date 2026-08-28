package dev.whoisyourbias.financial.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

  private static final Currency KRW = Currency.getInstance("KRW");
  private static final Currency USD = Currency.getInstance("USD");

  @Test
  void addsAmountsWithTheSameCurrency() {
    Money result = new Money(1_000, KRW).add(new Money(250, KRW));

    assertEquals(new Money(1_250, KRW), result);
  }

  @Test
  void supportsSignedValues() {
    Money result = new Money(-500, KRW).add(new Money(200, KRW));

    assertEquals(new Money(-300, KRW), result);
  }

  @Test
  void rejectsDifferentCurrencies() {
    Money krw = new Money(1_000, KRW);
    Money usd = new Money(1_000, USD);

    assertThrows(IllegalArgumentException.class, () -> krw.add(usd));
  }

  @Test
  void rejectsAdditionOverflow() {
    Money maximum = new Money(Long.MAX_VALUE, KRW);

    assertThrows(ArithmeticException.class, () -> maximum.add(new Money(1, KRW)));
  }

  @Test
  void rejectsNegationOverflow() {
    Money minimum = new Money(Long.MIN_VALUE, KRW);

    assertThrows(ArithmeticException.class, minimum::negate);
  }
}
