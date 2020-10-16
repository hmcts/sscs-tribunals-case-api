package uk.gov.hmcts.reform.sscs.ccd.presubmit.makecaseurgent;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.io.IOException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class MakeCaseUrgentAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private MakeCaseUrgentAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        handler = new MakeCaseUrgentAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.MAKE_CASE_URGENT);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonMakeCaseUrgentEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenMakeCaseUrgentEvent_thenSetUrgentCaseParameters() {

        callback.getCaseDetails().getCaseData().setUrgentCase(null);
        callback.getCaseDetails().getCaseData().setUrgentHearingRegistered(null);
        callback.getCaseDetails().getCaseData().setUrgentHearingOutcome(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Yes", response.getData().getUrgentCase());
        assertNotNull(response.getData().getUrgentHearingRegistered());
        assertEquals(RequestOutcome.IN_PROGRESS.getValue(), response.getData().getUrgentHearingOutcome());

    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
