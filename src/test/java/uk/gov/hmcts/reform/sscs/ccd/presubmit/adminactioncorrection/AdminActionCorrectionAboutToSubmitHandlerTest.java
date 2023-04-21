package uk.gov.hmcts.reform.sscs.ccd.presubmit.adminactioncorrection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADMIN_ACTION_CORRECTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;

@ExtendWith(MockitoExtension.class)
class AdminActionCorrectionAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private AdminActionCorrectionAboutToSubmitHandler handler;
    @Mock
    private DecisionNoticeService decisionNoticeService;
    @Mock
    private PreviewDocumentService previewDocumentService;
    @Mock
    private DecisionNoticeOutcomeService decisionNoticeOutcomeService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        handler = new AdminActionCorrectionAboutToSubmitHandler(decisionNoticeService,  previewDocumentService, true);

        caseData = SscsCaseData.builder()
            .ccdCaseId("1234")
            .build();
        caseData.setAppeal(Appeal.builder().hearingOptions(HearingOptions.builder().build()).build());
        caseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
    }

    @Test
    void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(READY_TO_LIST);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenPostHearingsEnabledFalse_thenReturnFalse() {
        handler = new AdminActionCorrectionAboutToSubmitHandler(decisionNoticeService,  previewDocumentService, false);
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void shouldReturnWithoutError() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenHeaderCorrection_shouldUpdatePreviousStateWhenCurrentStateIsNotReadyToListOrWithFta() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(decisionNoticeService.getOutcomeService(any(String.class))).thenReturn(decisionNoticeOutcomeService);

        caseData.setPreviousState(State.VOID_STATE);
        caseData.setState(State.APPEAL_CREATED);
        caseData.getPostHearing().getCorrection().setAdminCorrectionType(AdminCorrectionType.HEADER);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(decisionNoticeOutcomeService, times(1)).validate(response, caseData);
        verify(previewDocumentService, times(1)).writePreviewDocumentToSscsDocument(
            caseData,
            DRAFT_DECISION_NOTICE,
            caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertThat(response.getData().getPreviousState()).isEqualTo(State.APPEAL_CREATED);
    }

    @Test
    void givenHeaderCorrection_shouldNotUpdatePreviousStateWhenCurrentStateIsReadyToList() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(decisionNoticeService.getOutcomeService(any(String.class))).thenReturn(decisionNoticeOutcomeService);

        caseData.setPreviousState(State.VOID_STATE);
        caseData.setState(State.READY_TO_LIST);
        caseData.getPostHearing().getCorrection().setAdminCorrectionType(AdminCorrectionType.HEADER);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(decisionNoticeOutcomeService, times(1)).validate(response, caseData);
        verify(previewDocumentService, times(1)).writePreviewDocumentToSscsDocument(
            caseData,
            DRAFT_DECISION_NOTICE,
            caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertThat(response.getData().getPreviousState()).isEqualTo(State.VOID_STATE);
    }

    @Test
    void givenHeaderCorrection_shouldNotUpdatePreviousStateWhenCurrentStateIsWithFta() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(decisionNoticeService.getOutcomeService(any(String.class))).thenReturn(decisionNoticeOutcomeService);

        caseData.setPreviousState(State.VOID_STATE);
        caseData.setState(State.WITH_DWP);
        caseData.getPostHearing().getCorrection().setAdminCorrectionType(AdminCorrectionType.HEADER);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(decisionNoticeOutcomeService, times(1)).validate(response, caseData);
        verify(previewDocumentService, times(1)).writePreviewDocumentToSscsDocument(
            caseData,
            DRAFT_DECISION_NOTICE,
            caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertThat(response.getData().getPreviousState()).isEqualTo(State.VOID_STATE);
    }
}