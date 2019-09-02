package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

public class AdminAppealWithdrawnHandlerTest {

    @Test
    public void canHandle() {
        AdminAppealWithdrawnHandler adminAppealWithdrawnHandler = new AdminAppealWithdrawnHandler();
        boolean actualResult = adminAppealWithdrawnHandler.canHandle(CallbackType.ABOUT_TO_SUBMIT,
            buildTestCallback(EventType.ADMIN_APPEAL_WITHDRAWN));
        assertTrue(actualResult);
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