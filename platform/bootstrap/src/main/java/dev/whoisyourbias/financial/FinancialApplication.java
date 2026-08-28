package dev.whoisyourbias.financial;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FinancialApplication {

  public static void main(String[] args) {
    SpringApplication.run(FinancialApplication.class, args);
  }

  @Bean
  Clock applicationClock() {
    return Clock.systemUTC();
  }
}
