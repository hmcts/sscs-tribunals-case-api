package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class DwpUploadResponseAboutToSubmitHandlerTest {

    private DwpUploadResponseAboutToSubmitHandler dwpUploadResponseAboutToSubmitHandler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        initMocks(this);
        dwpUploadResponseAboutToSubmitHandler = new DwpUploadResponseAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.DWP_UPLOAD_RESPONSE);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1234")
            .benefitCode("002")
            .issueCode("DD")
            .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenADwpUploadResponseEvent_thenReturnTrue() {
        assertTrue(dwpUploadResponseAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(dwpUploadResponseAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenADwpUploadResponseEvent_thenSetCaseCode() {
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals("002DD", response.getData().getCaseCode());
    }

    @Test
    public void givenADwpUploadResponseEventWithEmptyBenefitCode_displayAnError() {
        callback.getCaseDetails().getCaseData().setBenefitCode(null);
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Benefit code cannot be empty", error);
        }
    }

    @Test
    public void givenADwpUploadResponseEventWithEmptyIssueCode_displayAnError() {
        callback.getCaseDetails().getCaseData().setIssueCode(null);
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Issue code cannot be empty", error);
        }
    }

}