package it.gov.pagopa.initiative.statistics.dto.events;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvaluationDTO {

    @JsonAlias("_id")
    private String id;
    private String userId;
    private String merchantId;
    private String operationTypeTranscoded;

    private String status;

    private Map<String, Reward> rewards;
}
