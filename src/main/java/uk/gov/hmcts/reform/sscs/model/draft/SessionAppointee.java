package uk.gov.hmcts.reform.sscs.model.draft;

import lombok.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@Value
public class SessionAppointee {
    private YesNo isAppointee;
}
