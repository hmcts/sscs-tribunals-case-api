package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

@RunWith(JUnitParamsRunner.class)
public class AdminAppealWithdrawnHandlerTest {

    private final AdminAppealWithdrawnHandler adminAppealWithdrawnHandler = new AdminAppealWithdrawnHandler();

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,ADMIN_APPEAL_WITHDRAWN,true",
        "ABOUT_TO_START,ADMIN_APPEAL_WITHDRAWN,false",
        "SUBMITTED,ADMIN_APPEAL_WITHDRAWN,false",
        "MID_EVENT,ADMIN_APPEAL_WITHDRAWN,false",
        "ABOUT_TO_SUBMIT,ISSUE_FURTHER_EVIDENCE,false",
        "null,ADMIN_APPEAL_WITHDRAWN,false",
        "ABOUT_TO_SUBMIT,null,false",
    })
    public void canHandle(@Nullable CallbackType callbackType, @Nullable EventType eventType, boolean expectedResult) {
        boolean actualResult = adminAppealWithdrawnHandler.canHandle(callbackType, buildTestCallback(eventType));
        assertEquals(expectedResult, actualResult);
    }

    private Callback<SscsCaseData> buildTestCallback(EventType eventType) {
        if (eventType == null) return null;
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(1L, "SSCS", State.DORMANT_APPEAL_STATE,
            SscsCaseData.builder().build(), LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), eventType);
    }

    @Test
    public void handle() {
        PreSubmitCallbackResponse<SscsCaseData> actualResult = adminAppealWithdrawnHandler.handle(
            CallbackType.ABOUT_TO_SUBMIT, buildTestCallback(EventType.ADMIN_APPEAL_WITHDRAWN));
        assertEquals("withdrawalReceived", actualResult.getData().getDwpState());
    }
}