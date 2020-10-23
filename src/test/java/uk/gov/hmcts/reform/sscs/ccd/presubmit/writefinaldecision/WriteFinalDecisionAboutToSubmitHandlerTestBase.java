package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;


import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.EsaDecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;

public abstract class WriteFinalDecisionAboutToSubmitHandlerTestBase {

    protected static final String USER_AUTHORISATION = "Bearer token";
    protected WriteFinalDecisionAboutToSubmitHandler handler;

    @Mock
    protected Callback<SscsCaseData> callback;

    @Mock
    protected CaseDetails<SscsCaseData> caseDetails;

    protected DecisionNoticeQuestionService decisionNoticeQuestionService;
    protected DecisionNoticeService decisionNoticeService;
    protected PreviewDocumentService previewDocumentService;
    protected SscsCaseData sscsCaseData;

    public WriteFinalDecisionAboutToSubmitHandlerTestBase(DecisionNoticeQuestionService decisionNoticeQuestionService) {
        this.decisionNoticeQuestionService = decisionNoticeQuestionService;
    }

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        decisionNoticeService = new DecisionNoticeService(Arrays.asList(decisionNoticeQuestionService), new ArrayList<>());
        previewDocumentService = new PreviewDocumentService();
        handler = new WriteFinalDecisionAboutToSubmitHandler(decisionNoticeService, previewDocumentService);

        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(decisionNoticeQuestionService.getBenefitType()).build()).build())
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonWriteFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }


    /**
     * Due to a CCD bug ( https://tools.hmcts.net/jira/browse/RDM-8200 ) we have had
     * to implement a workaround in WriteFinalDecisionAboutToSubmitHandler to set
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

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals(LocalDate.now().toString(), sscsCaseData.getWriteFinalDecisionGeneratedDate());

    }

    /**
     * This test asserts that whatever the value of the existing generated date from CCD
     * submitted as part of the payload to the WriterFinalSubmissionAboutToSubmitHandler,
     * then that date is updated to now() after the WriterFinalSubmissionAboutToSubmitHandler is called.
     * This is due to a workaround we have implemented in the WriterFinalSubmissionAboutToSubmitHandler
     *
     */
    @Test
    public void givenValidSubmissionWithGeneratedDateSet_thenSetUpdateGeneratedDateAndDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setWriteFinalDecisionGeneratedDate("2018-01-01");

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals(LocalDate.now().toString(), sscsCaseData.getWriteFinalDecisionGeneratedDate());
    }

    @Test
    public void givenEndDateTypeOfIndefinite_thenDoNotSetEndDateTypeToNull() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("indefinite");
        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals("indefinite", sscsCaseData.getWriteFinalDecisionEndDateType());
    }

    @Test
    public void givenEndDateTypeOfSetEndDate_thenDoNotSetEndDateTypeToNull() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("setEndDate");

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals("setEndDate", sscsCaseData.getWriteFinalDecisionEndDateType());

    }

    protected abstract void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue);

    @Test
    public void givenEndDateTypeOfNA_thenSetEndDateTypeToNull() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionEndDateType("na");

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());

    }

    @Test
    public void givenDraftFinalDecisionAlreadyExistsOnCase_thenOverwriteExistingDraft() {
        SscsDocument doc = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("oldDraft.doc").documentType(DRAFT_DECISION_NOTICE.getValue()).build()).build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(doc);
        callback.getCaseDetails().getCaseData().setSscsDocument(docs);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        // Why do we not need to set valid scenario ?

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getSscsDocument().size());
        assertEquals((String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))), response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
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
