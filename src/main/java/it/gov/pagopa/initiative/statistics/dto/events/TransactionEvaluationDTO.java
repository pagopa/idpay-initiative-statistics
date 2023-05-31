package it.gov.pagopa.initiative.statistics.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvaluationDTO {

    private String id;
    private String userId;
    private String merchantId;
    private String operationTypeTranscoded;

    private String status;

    private Map<String, Reward> rewards;
}
