package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase.AdjournCasePreviewService;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;

@RunWith(JUnitParamsRunner.class)
public class IssueAdjournmentNoticeAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private IssueAdjournmentNoticeAboutToStartHandler handler;
    private static final String URL = "http://dm-store/documents/123";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private AdjournCasePreviewService previewService;

    @Mock
    private GenerateFile generateFile;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new IssueAdjournmentNoticeAboutToStartHandler(previewService);

        when(callback.getEvent()).thenReturn(EventType.ISSUE_ADJOURNMENT_NOTICE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .adjournCasePreviewDocument(DocumentLink.builder().build())
            .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        when(generateFile.assemble(any())).thenReturn(URL);
    }

    @Test
    public void givenANonIssueAdjournmentEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenAboutToStartRequest_willGeneratePreviewFile() {
        PreSubmitCallbackResponse response = new PreSubmitCallbackResponse(sscsCaseData);

        when(previewService.preview(callback, DocumentType.ADJOURNMENT_NOTICE, USER_AUTHORISATION, true)).thenReturn(response);
        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        verify(previewService).preview(callback, DocumentType.ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);
    }

    @Test
    public void givenNoPreviewDecisionFoundOnCase_thenShowError() {
        sscsCaseData.setAdjournCasePreviewDocument(null);
        PreSubmitCallbackResponse<SscsCaseData> result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String error = result.getErrors().stream().findFirst().orElse("");
        assertEquals("No draft adjournment notice found on case. Please use 'Adjourn case' event before trying to issue.", error);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
    }
}
