package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;

@RunWith(JUnitParamsRunner.class)
public class AdjournCaseAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private AdjournCaseAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private PreviewDocumentService previewDocumentService;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        previewDocumentService = new PreviewDocumentService();
        handler = new AdjournCaseAboutToSubmitHandler(previewDocumentService);

        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonAdjournCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenDraftAdjournmentNoticeAlreadyExistsOnCase_thenOverwriteExistingDraft() {
        SscsDocument doc = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("oldDraft.doc").documentType(DRAFT_ADJOURNMENT_NOTICE.getValue()).build()).build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(doc);
        callback.getCaseDetails().getCaseData().setSscsDocument(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getSscsDocument().size());
        assertEquals((String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))), response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    @Test
    public void givenAnAdjournmentEventWithLanguageInterpreterRequiredAndCaseHasExistingInterpreter_thenOverwriteExistingInterpreterInHearingOptions() {
        callback.getCaseDetails().getCaseData().setAdjournCaseInterpreterRequired("Yes");
        callback.getCaseDetails().getCaseData().setAdjournCaseInterpreterLanguage(new DynamicList("Spanish"));
        callback.getCaseDetails().getCaseData().getAppeal().setHearingOptions(HearingOptions.builder().languageInterpreter("No").languages("French").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Yes", response.getData().getAppeal().getHearingOptions().getLanguageInterpreter());
        assertEquals("Spanish", response.getData().getAppeal().getHearingOptions().getLanguages());
    }

    @Test
    public void givenAnAdjournmentEventWithLanguageInterpreterRequiredAndIntepreterLanguageSet_thenDoNotDisplayError() {
        callback.getCaseDetails().getCaseData().setAdjournCaseInterpreterRequired("Yes");
        callback.getCaseDetails().getCaseData().setAdjournCaseInterpreterLanguage(new DynamicList("Spanish"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Yes", response.getData().getAppeal().getHearingOptions().getLanguageInterpreter());
        assertEquals("Spanish", response.getData().getAppeal().getHearingOptions().getLanguages());
    }

    /**
     * Due to a CCD bug ( https://tools.hmcts.net/jira/browse/RDM-8200 ) we have had
     * to implement a workaround in AdjournCaseAboutToSubmitHandler to set
     * the generated date to now, even though it is already being determined by the
     * preview document handler.  This is because on submission, the correct generated date
     * (the one referenced in the preview document) is being overwritten to a null value.
     * Once RDM-8200 is fixed and we remove the workaround, this test should be changed
     * to assert that a "something has gone wrong" error is displayed, as a null generated
     * date would indicate that the date in the preview document hasn't been set.
     *
     */
    @Test
    public void givenValidSubmissionWithGeneratedDateNotSet_thenSetGeneratedDateAsNowAndDoNotDisplayAnError() {

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(LocalDate.now().toString(), callback.getCaseDetails().getCaseData().getAdjournCaseGeneratedDate());
    }

    /**
     * This test asserts that whatever the value of the existing generated date from CCD
     * submitted as part of the payload to the AdjournCaseAboutToSubmitHandler,
     * then that date is updated to now() after the AdjournCaseAboutToSubmitHandler is called.
     * This is due to a workaround we have implemented in the AdjournCaseAboutToSubmitHandler
     *
     */
    @Test
    public void givenValidSubmissionWithGeneratedDateSet_thenSetGeneratedDateToNowAndDoNotDisplayAnError() {

        callback.getCaseDetails().getCaseData().setAdjournCaseGeneratedDate("2018-01-01");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(LocalDate.now().toString(), callback.getCaseDetails().getCaseData().getAdjournCaseGeneratedDate());
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
