package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpResponseDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;

@RunWith(JUnitParamsRunner.class)
public class CreateBundleSubmittedHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private CreateBundleSubmittedHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private ServiceRequestExecutor serviceRequestExecutor;

    private final SscsCaseData sscsCaseData = SscsCaseData.builder().createdInGapsFrom("readyToList").appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();


    @Before
    public void setUp() {
        openMocks(this);
        handler = new CreateBundleSubmittedHandler(serviceRequestExecutor, "bundleUrl.com");

        when(callback.getEvent()).thenReturn(EventType.CREATE_BUNDLE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(serviceRequestExecutor.post(any(), any())).thenReturn(new PreSubmitCallbackResponse<>(sscsCaseData));
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenANonCreateBundleEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenANonCaseUpdatedCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
    }

    @Test
    @Parameters({"warning", "error", ""})
    public void givenCreateBundleEvent_thenThereAreNoErrors(String errorOrWarning) {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        PreSubmitCallbackResponse<SscsCaseData> serviceResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (errorOrWarning.equals("error")) {
            serviceResponse.addError(errorOrWarning);
        } else if (errorOrWarning.equals("warning")) {
            serviceResponse.addWarning(errorOrWarning);
        }
        when(serviceRequestExecutor.post(any(), any())).thenReturn(serviceResponse);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(serviceRequestExecutor).post(callback, "bundleUrl.com/api/new-bundle");
        if (errorOrWarning.equals("error")) {
            assertThat(0, is(response.getWarnings().size()));
            assertThat(1, is(response.getErrors().size()));
            assertThat(errorOrWarning, is(response.getErrors().iterator().next()));
        } else if (errorOrWarning.equals("warning")) {
            assertThat(0, is(response.getErrors().size()));
            assertThat(1, is(response.getWarnings().size()));
            assertThat(errorOrWarning, is(response.getWarnings().iterator().next()));
        } else {
            assertThat(0, is(response.getErrors().size()));
            assertThat(0, is(response.getWarnings().size()));
        }
    }

}