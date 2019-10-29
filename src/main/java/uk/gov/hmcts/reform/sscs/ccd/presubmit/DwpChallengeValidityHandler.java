package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import java.util.Collections;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
public class DwpChallengeValidityHandler extends EventToFieldPreSubmitCallbackHandler {

    DwpChallengeValidityHandler() {
        super(Collections.singletonMap(EventType.DWP_CHALLENGE_VALIDITY, "password1"));
    }

    @Override
    protected SscsCaseData setField(SscsCaseData sscsCaseData, String newValue, EventType eventType) {
        return sscsCaseData.toBuilder().interlocReviewState("reviewByJudge").build();
    }
}
