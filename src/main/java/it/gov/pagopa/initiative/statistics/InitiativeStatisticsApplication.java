package it.gov.pagopa.initiative.statistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
public class InitiativeStatisticsApplication {

  public static void main(String[] args) {
    SpringApplication.run(InitiativeStatisticsApplication.class, args);
  }

}

