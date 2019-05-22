package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Value;

@Value
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class SessionHearingArrangementsSelection {
    private SessionHearingArrangement interpreterLanguage;
    private SessionHearingArrangement signLanguage;
    private SessionHearingArrangement hearingLoop;
    private SessionHearingArrangement accessibleHearingRoom;
    private SessionHearingArrangement anythingElse;
}
