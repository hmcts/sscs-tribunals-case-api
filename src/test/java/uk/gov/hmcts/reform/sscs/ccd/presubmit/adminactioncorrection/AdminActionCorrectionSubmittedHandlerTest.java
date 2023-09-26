package uk.gov.hmcts.reform.sscs.ccd.presubmit.adminactioncorrection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADMIN_ACTION_CORRECTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingRequestType.CORRECTION;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;

@ExtendWith(MockitoExtension.class)
class AdminActionCorrectionSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    public static final long CASE_ID = 1234L;

    private AdminActionCorrectionSubmittedHandler handler;

    @Mock
    private CcdCallbackMapService ccdCallbackMapService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        handler = new AdminActionCorrectionSubmittedHandler(ccdCallbackMapService, true);

        caseData = SscsCaseData.builder()
                .ccdCaseId(String.valueOf(CASE_ID))
                .postHearing(PostHearing.builder()
                        .requestType(CORRECTION)
                        .build())
                .build();
    }

    @Test
    void givenAValidSubmittedEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        assertThat(handler.canHandle(SUBMITTED, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(READY_TO_LIST);
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenPostHearingsEnabledFalse_thenReturnFalse() {
        handler = new AdminActionCorrectionSubmittedHandler(ccdCallbackMapService, false);
        when(callback.getEvent()).thenReturn(ADMIN_ACTION_CORRECTION);
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = AdminCorrectionType.class)
    void givenAdminCorrectionTypes_shouldReturnCallCorrectCallback(AdminCorrectionType value) {
        caseData.getPostHearing().getCorrection().setAdminCorrectionType(value);

        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(caseDetails.getCaseData()).thenReturn(caseData);

        when(ccdCallbackMapService.handleCcdCallbackMap(value, caseData))
                .thenReturn(SscsCaseData.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        verify(ccdCallbackMapService, times(1))
                .handleCcdCallbackMap(value, caseData);
    }


    @Test
    void givenNoActionTypeSelected_shouldReturnWithTheCorrectErrorMessage() {
        caseData.getPostHearing().getCorrection().setAdminCorrectionType(null);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
                .hasSize(1)
                .containsOnly("Invalid Admin Correction Type Selected or correction "
                        + "selected as callback is null");
    }
}