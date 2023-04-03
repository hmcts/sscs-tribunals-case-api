package uk.gov.hmcts.reform.sscs.ccd.presubmit.adminactioncorrection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADMIN_ACTION_CORRECTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
class AdminActionCorrectionAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private AdminActionCorrectionAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        handler = new AdminActionCorrectionAboutToStartHandler(true);

        caseData = SscsCaseData.builder()
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(LIST_ASSIST).build())
            .ccdCaseId("1234")
            .build();
    }

    @Test
    void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(READY_TO_LIST);
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenPostHearingsEnabledFalse_thenReturnFalse() {
        handler = new AdminActionCorrectionAboutToStartHandler(false);
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @Test
    void givenLaCase_shouldReturnWithoutError() {

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenNonLaCase_shouldReturnErrorWithCorrectMessage() {
        caseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder()
            .hearingRoute(GAPS)
            .build());

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("Cannot process Admin Action Correction on non Scheduling & Listing Case");
    }
}