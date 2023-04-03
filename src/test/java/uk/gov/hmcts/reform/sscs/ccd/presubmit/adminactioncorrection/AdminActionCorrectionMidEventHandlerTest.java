package uk.gov.hmcts.reform.sscs.ccd.presubmit.adminactioncorrection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADMIN_ACTION_CORRECTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdminCorrectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
class AdminActionCorrectionMidEventHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    private AdminActionCorrectionMidEventHandler handler;


    @BeforeEach
    void setUp() {
        handler = new AdminActionCorrectionMidEventHandler(true);

        caseData = SscsCaseData.builder()
            .ccdCaseId("1234")
            .build();
    }

    @Test
    void givenAValidMidEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        assertThat(handler.canHandle(MID_EVENT, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(READY_TO_LIST);
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @Test
    void givenPostHearingsEnabledFalse_thenReturnFalse() {
        handler = new AdminActionCorrectionMidEventHandler(false);
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenBodyCorrection_doNothing() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        caseData.getPostHearing().getCorrection().setAdminCorrectionType(AdminCorrectionType.BODY);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenHeaderCorrection_andFinalDecisionWasGenerated_regenerateCorrectedDocument() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        caseData.getPostHearing().getCorrection().setAdminCorrectionType(AdminCorrectionType.HEADER);
        // TODO set finalDecisionNoticeGenerated to YES

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        // TODO expect document generation to be called with current details
    }

    @Test
    void givenHeaderCorrection_andFinalDecisionWasUploaded_expectCorrectedDocumentToBeUploaded() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        caseData.getPostHearing().getCorrection().setAdminCorrectionType(AdminCorrectionType.HEADER);
        // TODO set finalDecisionNoticeGenerated to NO

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        // TODO expect document upload
    }

    @Test
    void givenOtherPageId_doNothing() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        caseData.getPostHearing().getCorrection().setAdminCorrectionType(AdminCorrectionType.HEADER);

        when(callback.getPageId()).thenReturn("test page id");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }


    @ParameterizedTest
    @EnumSource(AdminCorrectionType.class)
    void whenAdminCorrectionTypeExists_shouldReturnWithoutError(AdminCorrectionType adminCorrectionType) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        caseData.getPostHearing().getCorrection().setAdminCorrectionType(adminCorrectionType);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void whenAdminCorrectionTypeIsNull_shouldReturnError() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        caseData.getPostHearing().getCorrection().setAdminCorrectionType(null);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsExactly("adminCorrectionType unexpectedly null for case: 1234");
    }

}