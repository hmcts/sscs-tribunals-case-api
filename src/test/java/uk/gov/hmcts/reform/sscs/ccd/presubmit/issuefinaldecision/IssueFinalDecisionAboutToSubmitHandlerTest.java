package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.FINAL_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@RunWith(JUnitParamsRunner.class)
public class IssueFinalDecisionAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private IssueFinalDecisionAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private FooterService footerService;

    private SscsCaseData sscsCaseData;

    private SscsDocument document;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService);

        when(callback.getEvent()).thenReturn(EventType.ISSUE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        List<SscsDocument> documentList = new ArrayList<>();
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .sscsDocument(documentList)
            .writeFinalDecisionTypeOfHearing("")
            .writeFinalDecisionPresentingOfficerAttendedQuestion("")
            .writeFinalDecisionAppellantAttendedQuestion("")
            .writeFinalDecisionDisabilityQualifiedPanelMemberName("")
            .writeFinalDecisionMedicallyQualifiedPanelMemberName("")
            .pipWriteFinalDecisionDailyLivingQuestion("")
            .pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("")
            .pipWriteFinalDecisionMobilityQuestion("")
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion("")
            .writeFinalDecisionStartDate("")
            .writeFinalDecisionEndDateType("")
            .writeFinalDecisionEndDate("")
            .writeFinalDecisionDateOfDecision("")
            .pipWriteFinalDecisionDailyLivingActivitiesQuestion(new ArrayList<>())
            .pipWriteFinalDecisionMobilityActivitiesQuestion(new ArrayList<>())
            .pipWriteFinalDecisionPreparingFoodQuestion("")
            .pipWriteFinalDecisionTakingNutritionQuestion("")
            .pipWriteFinalDecisionManagingTherapyQuestion("")
            .pipWriteFinalDecisionWashAndBatheQuestion("")
            .pipWriteFinalDecisionManagingToiletNeedsQuestion("")
            .pipWriteFinalDecisionDressingAndUndressingQuestion("")
            .pipWriteFinalDecisionCommunicatingQuestion("")
            .pipWriteFinalDecisionReadingUnderstandingQuestion("")
            .pipWriteFinalDecisionEngagingWithOthersQuestion("")
            .pipWriteFinalDecisionBudgetingDecisionsQuestion("")
            .pipWriteFinalDecisionPlanningAndFollowingQuestion("")
            .pipWriteFinalDecisionMovingAroundQuestion("")
            .writeFinalDecisionReasons(Arrays.asList(new CollectionItem(null, "")))
            .writeFinalDecisionPageSectionReference("")
            .writeFinalDecisionPreviewDocument(DocumentLink.builder().build())
            .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonIssueFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAnIssueFinalDecisionEvent_thenCreateDecisionWithFooterAndClearTransientFields() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionPreviewDocument(docLink);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(FINAL_DECISION_NOTICE), any(), eq(null), eq(null));
        assertEquals(FINAL_DECISION_ISSUED.getId(), response.getData().getDwpState());
        assertEquals(0, (int) response.getData().getSscsDocument().stream().filter(f -> f.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue())).count());

        assertNull(sscsCaseData.getWriteFinalDecisionTypeOfHearing());
        assertNull(sscsCaseData.getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionAppellantAttendedQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNull(sscsCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionStartDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDate());
        assertNull(sscsCaseData.getWriteFinalDecisionDateOfDecision());
        assertNull(sscsCaseData.getPipWriteFinalDecisionDailyLivingActivitiesQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionMobilityActivitiesQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionPreparingFoodQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionTakingNutritionQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionManagingTherapyQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionWashAndBatheQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionManagingToiletNeedsQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionDressingAndUndressingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionCommunicatingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionReadingUnderstandingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionEngagingWithOthersQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionBudgetingDecisionsQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionPlanningAndFollowingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionMovingAroundQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionReasons());
        assertNull(sscsCaseData.getWriteFinalDecisionPageSectionReference());
        assertNull(sscsCaseData.getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getWriteFinalDecisionGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getWriteFinalDecisionAllowedOrRefused());
    }

    @Test
    public void givenAnIssueFinalDecisionEventWithDocumentNameNotSet_thenCreateDecisionWithFooter() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("bla.pdf").build();
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionPreviewDocument(docLink);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(FINAL_DECISION_NOTICE), any(), eq(null), eq(null));
    }

    @Test
    public void givenAnIssueFinalDecisionEventWithDocumentDateAddedNotSet_thenCreateDecisionWithFooter() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename("test.pdf").build();
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionPreviewDocument(docLink);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(FINAL_DECISION_NOTICE), any(), eq(null), eq(null));
    }

    @Test
    public void givenAnIssueFinalDecisionEventAndNoDraftDecisionOnCase_thenDisplayAnError() {
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionPreviewDocument(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no Preview Draft Decision Notice on the case so decision cannot be issued", error);
    }

    @Test
    public void givenANonPdfDecisionNotice_thenDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("test.doc").build();
        sscsCaseData.setWriteFinalDecisionPreviewDocument(docLink);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You need to upload PDF documents only", error);
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
