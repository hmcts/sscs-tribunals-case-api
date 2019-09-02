package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

@RunWith(JUnitParamsRunner.class)
public class AdminAppealWithdrawnHandlerTest {

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,ADMIN_APPEAL_WITHDRAWN,true",
        "ABOUT_TO_START,ADMIN_APPEAL_WITHDRAWN,false",
        "SUBMITTED,ADMIN_APPEAL_WITHDRAWN,false",
        "MID_EVENT,ADMIN_APPEAL_WITHDRAWN,false"
    })
    public void canHandle(CallbackType callbackType, EventType eventType, boolean expectedResult) {
        AdminAppealWithdrawnHandler adminAppealWithdrawnHandler = new AdminAppealWithdrawnHandler();
        boolean actualResult = adminAppealWithdrawnHandler.canHandle(callbackType,
            buildTestCallback(eventType));
        assertEquals(expectedResult, actualResult);
    }

    private Callback<SscsCaseData> buildTestCallback(EventType eventType) {
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(1L, "SSCS", State.DORMANT_APPEAL_STATE,
            SscsCaseData.builder().build(), LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), eventType);
    }

    @Test
    public void handle() {
    }
}