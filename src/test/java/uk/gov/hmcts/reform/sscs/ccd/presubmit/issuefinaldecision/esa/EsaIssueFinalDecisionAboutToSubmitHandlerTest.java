package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision.esa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.FINAL_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.validation.Validation;
import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsEsaCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFinalDecisionCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision.IssueFinalDecisionAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.EsaDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.EsaDecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@RunWith(JUnitParamsRunner.class)
public class EsaIssueFinalDecisionAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private IssueFinalDecisionAboutToSubmitHandler handler;

    private EsaDecisionNoticeOutcomeService esaecisionNoticeOutcomeService;

    private DecisionNoticeService decisionNoticeService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private FooterService footerService;

    private SscsCaseData sscsCaseData;

    private SscsDocument document;

    protected static Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        esaecisionNoticeOutcomeService = new EsaDecisionNoticeOutcomeService(new EsaDecisionNoticeQuestionService());

        decisionNoticeService = new DecisionNoticeService(new ArrayList<>(), Arrays.asList(esaecisionNoticeOutcomeService), new ArrayList<>());

        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService, decisionNoticeService, validator);

        when(callback.getEvent()).thenReturn(EventType.ISSUE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        List<SscsDocument> documentList = new ArrayList<>();

        SscsDocumentDetails details = SscsDocumentDetails.builder().documentType(DRAFT_DECISION_NOTICE.getValue()).build();
        documentList.add(new SscsDocument(details));
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("ESA").build()).build())
            .sscsDocument(documentList)
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionTypeOfHearing("")
                .writeFinalDecisionPresentingOfficerAttendedQuestion("")
                .writeFinalDecisionAppellantAttendedQuestion("")
                .writeFinalDecisionDisabilityQualifiedPanelMemberName("")
                .writeFinalDecisionMedicallyQualifiedPanelMemberName("")
                .writeFinalDecisionStartDate("")
                .writeFinalDecisionEndDateType("")
                .writeFinalDecisionEndDate("")
                .writeFinalDecisionDateOfDecision("")
                .writeFinalDecisionReasons(Arrays.asList(new CollectionItem(null, "")))
                .writeFinalDecisionPageSectionReference("")
                .writeFinalDecisionAnythingElse("something else")
                .writeFinalDecisionPreviewDocument(DocumentLink.builder().build())
                .build())
            .dwpReassessTheAward("")
            .sscsEsaCaseData(SscsEsaCaseData.builder()
                .esaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList())
                .esaWriteFinalDecisionSchedule3ActivitiesApply("")
                .esaWriteFinalDecisionAppropriatenessOfBehaviourQuestion("")
                .esaWriteFinalDecisionAwarenessOfHazardsQuestion("")
                .esaWriteFinalDecisionCommunicationQuestion("")
                .esaWriteFinalDecisionConsciousnessQuestion("")
                .esaWriteFinalDecisionCopingWithChangeQuestion("")
                .esaWriteFinalDecisionGettingAboutQuestion("")
                .esaWriteFinalDecisionLearningTasksQuestion("")
                .esaWriteFinalDecisionLossOfControlQuestion("")
                .esaWriteFinalDecisionMakingSelfUnderstoodQuestion("")
                .esaWriteFinalDecisionManualDexterityQuestion("")
                .esaWriteFinalDecisionMentalAssessmentQuestion(Arrays.asList())
                .esaWriteFinalDecisionMobilisingUnaidedQuestion("")
                .esaWriteFinalDecisionNavigationQuestion("")
                .esaWriteFinalDecisionPersonalActionQuestion("")
                .esaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList())
                .esaWriteFinalDecisionPickingUpQuestion("")
                .esaWriteFinalDecisionReachingQuestion("")
                .esaWriteFinalDecisionSocialEngagementQuestion("")
                .esaWriteFinalDecisionStandingAndSittingQuestion("")
                .showRegulation29Page(YES)
                .showSchedule3ActivitiesPage(YES)
                .doesRegulation35Apply(YES)
                .doesRegulation29Apply(YES)
                .build())
            .showFinalDecisionNoticeSummaryOfOutcomePage(YES)
            .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonIssueFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAnIssueFinalDecisionEventForGenerateNoticeFlowWhenAllowedOrRefusedIsNull_ThenDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setWcaAppeal(YES);
        callback.getCaseDetails().getCaseData().setSupportGroupOnlyAppeal("Yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused(null);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice("yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);

        verifyNoInteractions(footerService);
        assertNull(response.getData().getDwpState());
        assertEquals(1, (int) response.getData().getSscsDocument().stream().filter(f -> f.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue())).count());

        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionTypeOfHearing());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppellantAttendedQuestion());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
        assertNotNull(sscsCaseData.getWcaAppeal());
        assertNotNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionAppropriatenessOfBehaviourQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionAwarenessOfHazardsQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionCommunicationQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionConsciousnessQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionCopingWithChangeQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionGettingAboutQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionLearningTasksQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionLossOfControlQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMakingSelfUnderstoodQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionManualDexterityQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMentalAssessmentQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMobilisingUnaidedQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionNavigationQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPersonalActionQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPickingUpQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionReachingQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesApply());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSocialEngagementQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionStandingAndSittingQuestion());
        assertNotNull(sscsCaseData.getDwpReassessTheAward());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate());
        assertNotNull(sscsCaseData.getWcaAppeal());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getShowRegulation29Page());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getShowSchedule3ActivitiesPage());
        assertNotNull(sscsCaseData.getShowFinalDecisionNoticeSummaryOfOutcomePage());
        assertNotNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getDoesRegulation35Apply());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getDoesRegulation29Apply());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
    }

    @Test
    public void givenAnIssueFinalDecisionEventForGenerateNoticeFlowWhenAllowedOrRefusedIsNotNull_ThenDoNotDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setWcaAppeal(YES);
        callback.getCaseDetails().getCaseData().setSupportGroupOnlyAppeal("Yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice("yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(FINAL_DECISION_NOTICE), any(), eq(null), eq(null), eq(null));
        assertEquals(FINAL_DECISION_ISSUED.getId(), response.getData().getDwpState());

        assertEquals("decisionInFavourOfAppellant", response.getData().getOutcome());

        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionTypeOfHearing());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppellantAttendedQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
        assertNull(sscsCaseData.getWcaAppeal());
        assertNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionAppropriatenessOfBehaviourQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionAwarenessOfHazardsQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionCommunicationQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionConsciousnessQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionCopingWithChangeQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionGettingAboutQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionLearningTasksQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionLossOfControlQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMakingSelfUnderstoodQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionManualDexterityQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMentalAssessmentQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMobilisingUnaidedQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionNavigationQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPersonalActionQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPickingUpQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionReachingQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesApply());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSocialEngagementQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionStandingAndSittingQuestion());
        assertNull(sscsCaseData.getDwpReassessTheAward());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getWcaAppeal());
        assertNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNull(sscsCaseData.getSscsEsaCaseData().getDoesRegulation35Apply());
        assertNull(sscsCaseData.getSscsEsaCaseData().getDoesRegulation29Apply());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
    }


    @Test
    @Parameters({
        "allowed, decisionInFavourOfAppellant",
        "refused, decisionUpheld"
    })
    public void givenAnIssueFinalDecisionEvent_thenCreateDecisionWithFooterAndClearTransientFields(String allowedOrRefused, String expectedOutcome) {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused(allowedOrRefused);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(FINAL_DECISION_NOTICE), any(), eq(null), eq(null), eq(null));
        assertEquals(FINAL_DECISION_ISSUED.getId(), response.getData().getDwpState());

        assertEquals(expectedOutcome, response.getData().getOutcome());

        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionTypeOfHearing());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppellantAttendedQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
        assertNull(sscsCaseData.getWcaAppeal());
        assertNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionAppropriatenessOfBehaviourQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionAwarenessOfHazardsQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionCommunicationQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionConsciousnessQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionCopingWithChangeQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionGettingAboutQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionLearningTasksQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionLossOfControlQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMakingSelfUnderstoodQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionManualDexterityQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMentalAssessmentQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMobilisingUnaidedQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionNavigationQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPersonalActionQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPickingUpQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionReachingQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesApply());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSocialEngagementQuestion());
        assertNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionStandingAndSittingQuestion());
        assertNull(sscsCaseData.getDwpReassessTheAward());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getWcaAppeal());
        assertNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNull(sscsCaseData.getSscsEsaCaseData().getDoesRegulation35Apply());
        assertNull(sscsCaseData.getSscsEsaCaseData().getDoesRegulation29Apply());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
    }

    @Test
    public void givenAnIssueFinalDecisionEventAndNoDraftDecisionOnCase_thenDisplayAnError() {
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(null);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice("yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no Preview Draft Decision Notice on the case so decision cannot be issued", error);
    }

    @Test
    public void givenANonPdfDecisionNotice_thenDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("test.doc").build();
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");

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
