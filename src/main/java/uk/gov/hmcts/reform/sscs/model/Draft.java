package uk.gov.hmcts.reform.sscs.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Draft {
    long id;
}
