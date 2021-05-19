package uk.gov.hmcts.reform.sscs.ccd.presubmit.validappeal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;

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
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;

@RunWith(JUnitParamsRunner.class)
public class ValidateAppealAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private ValidateAppealAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private ServiceRequestExecutor serviceRequestExecutor;

    @Mock
    private PreSubmitCallbackResponse<SscsCaseData> response;

    @Mock
    private SscsCaseData bulkScanResponseData;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new ValidateAppealAboutToSubmitHandler(serviceRequestExecutor, "https://sscs-bulk-scan.net", "/validate");

        when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(serviceRequestExecutor.post(eq(callback), eq("https://sscs-bulk-scan.net/validate"))).thenReturn(response);
    }

    @Test
    public void givenANonValidAppealCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
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

    @Test
    public void givenValidateAppealForPreValidCase_thenSetNonDigitalToDigitalCase() {
        when(caseDetails.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
        sscsCaseData.setCreatedInGapsFrom(null);
        when(response.getData()).thenReturn(bulkScanResponseData);
        when(bulkScanResponseData.getCreatedInGapsFrom()).thenReturn(READY_TO_LIST.getId());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getCreatedInGapsFrom(), is(READY_TO_LIST.getId()));
        verify(serviceRequestExecutor).post(callback, "https://sscs-bulk-scan.net/validate");
    }

    @Test
    public void givenValidateAppealForPreValidCase_thenSetNoDigitalToDigitalCase() {
        when(caseDetails.getState()).thenReturn(State.INCOMPLETE_APPLICATION);
        sscsCaseData.setCreatedInGapsFrom(VALID_APPEAL.getId());
        when(response.getData()).thenReturn(bulkScanResponseData);
        when(bulkScanResponseData.getCreatedInGapsFrom()).thenReturn(READY_TO_LIST.getId());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getCreatedInGapsFrom(), is(READY_TO_LIST.getId()));
        verify(serviceRequestExecutor).post(callback, "https://sscs-bulk-scan.net/validate");
    }

    @Test
    public void givenValidateAppealForPreValidCase_thenSetDwpRegionalCenterToCase() {
        when(caseDetails.getState()).thenReturn(State.INCOMPLETE_APPLICATION);
        when(response.getData()).thenReturn(bulkScanResponseData);
        when(bulkScanResponseData.getDwpRegionalCentre()).thenReturn("PIP Newcastle");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("PIP Newcastle", response.getData().getDwpRegionalCentre());
        verify(serviceRequestExecutor).post(callback, "https://sscs-bulk-scan.net/validate");
    }
}
