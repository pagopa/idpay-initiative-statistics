package it.gov.pagopa.initiative.statistics.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@AllArgsConstructor
@FieldNameConstants
public class CommittedOffset {
    private int partition;
    private long offset;
}
