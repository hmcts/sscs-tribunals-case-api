package uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Destination {
    public final String email;
    public final String sms;
}
