package it.gov.pagopa.initiative.statistics.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ErrorDTO
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDTO {

  private String code;
  private String message;

}

