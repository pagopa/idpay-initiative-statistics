package it.gov.pagopa.initiative.statistics.config;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.initiative.statistics.exception.StatisticsNotFoundException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ServiceExceptionConfig {

  @Bean
  public Map<Class<? extends ServiceException>, HttpStatus> serviceExceptionMapper() {
    Map<Class<? extends ServiceException>, HttpStatus> exceptionMap = new HashMap<>();

    // NotFound
    exceptionMap.put(StatisticsNotFoundException.class, HttpStatus.NOT_FOUND);

    return exceptionMap;
  }

}
