package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_LISTING_REQUIREMENTS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.UPDATE_HEARING;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;

@ExtendWith(MockitoExtension.class)
public class UpdateListingRequirementsRequestSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private ListAssistHearingMessageHelper listAssistHearingMessageHelper;
    @InjectMocks
    private UpdateListingRequirementsRequestSubmittedHandler handler;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setUp() {
        sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().build())
                .dwpIsOfficerAttending("Yes")
                .build();
        ReflectionTestUtils.setField(handler, "isDefaultPanelCompEnabled", true);
    }

    @Test
    void givenValidCallback_thenReturnTrue() {
        given(callback.getEvent()).willReturn(UPDATE_LISTING_REQUIREMENTS);
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
    void handleUpdateListingRequirementsSendSuccessful() {
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(caseDetails.getState()).willReturn(State.READY_TO_LIST);
        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());
        sscsCaseData.setCcdCaseId("1234");

        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        given(listAssistHearingMessageHelper.sendHearingMessage(
                anyString(), any(HearingRoute.class), any(HearingState.class), eq(null)))
                .willReturn(true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                SUBMITTED,
                callback,
                USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        assertThat(response.getData()).isNotNull();
        SscsCaseData caseData = response.getData();
        assertThat(UPDATE_HEARING).isEqualTo(caseData.getSchedulingAndListingFields().getHearingState());
    }

    @Test
    void handleUpdateListingRequirementsSendUnsuccessful() {
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(caseDetails.getState()).willReturn(State.READY_TO_LIST);
        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());
        sscsCaseData.setCcdCaseId("1234");

        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        given(listAssistHearingMessageHelper.sendHearingMessage(
                anyString(), any(HearingRoute.class), any(HearingState.class), eq(null)))
                .willReturn(false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                SUBMITTED,
                callback,
                USER_AUTHORISATION);

        assertThat(1).isEqualTo(response.getErrors().size());
        assertThat(response.getErrors()).contains("An error occurred during message publish. Please try again.");
        assertThat(response.getData()).isNotNull();
    }

    @Test
    void handleUpdateListingRequirementsWrongState() {
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(caseDetails.getState()).willReturn(State.READY_TO_LIST);
        sscsCaseData = CaseDataUtils.buildCaseData();

        given(caseDetails.getState()).willReturn(State.UNKNOWN);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                SUBMITTED,
                callback,
                USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void handleUpdateListingRequirementsShouldNotSendMessageWhenNoPanelCompositionOrOverrideFields() {
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(caseDetails.getState()).willReturn(State.READY_TO_LIST);
        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.setCcdCaseId("1234");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                SUBMITTED,
                callback,
                USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verify(listAssistHearingMessageHelper, times(0)).sendHearingMessage(
                anyString(), any(HearingRoute.class), any(HearingState.class), eq(null));
    }

    @Test
    void isDefaultPanelCompNotEnabledAndPopulatedOverrideFieldsThenReturnTrue() {
        ReflectionTestUtils.setField(handler, "isDefaultPanelCompEnabled", false);

        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());
        sscsCaseData.setCcdCaseId("1234");

        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(caseDetails.getState()).willReturn(State.READY_TO_LIST);
        given(listAssistHearingMessageHelper.sendHearingMessage(
                anyString(), any(HearingRoute.class), any(HearingState.class), eq(null)))
                .willReturn(true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                SUBMITTED,
                callback,
                USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verify(listAssistHearingMessageHelper, times(1)).sendHearingMessage(
                anyString(), any(HearingRoute.class), any(HearingState.class), eq(null));
        assertThat(response.getData()).isNotNull();
        assertThat(UPDATE_HEARING).isEqualTo(response.getData().getSchedulingAndListingFields().getHearingState());
    }

    @Test
    void isDefaultPanelCompNotEnabledAndNoOverRideFieldsThenReturnFalse() {
        ReflectionTestUtils.setField(handler, "isDefaultPanelCompEnabled", false);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(caseDetails.getState()).willReturn(State.READY_TO_LIST);
        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.setCcdCaseId("1234");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                SUBMITTED,
                callback,
                USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verify(listAssistHearingMessageHelper, times(0)).sendHearingMessage(
                anyString(), any(HearingRoute.class), any(HearingState.class), eq(null));
    }
}
