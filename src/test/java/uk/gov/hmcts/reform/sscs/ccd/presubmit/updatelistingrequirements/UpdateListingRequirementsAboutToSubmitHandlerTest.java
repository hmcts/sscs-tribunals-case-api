package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.UPDATE_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReserveTo;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;

@ExtendWith(MockitoExtension.class)
class UpdateListingRequirementsAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private ListAssistHearingMessageHelper listAssistHearingMessageHelper;
    @InjectMocks
    private UpdateListingRequirementsAboutToSubmitHandler handler;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);
        sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().build())
            .dwpIsOfficerAttending("Yes")
            .build();
    }

    @Test
    void givenValidCallback_thenReturnTrue() {
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    void givenInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @Test
    void givenInvalidEventType_thenReturnFalse() {
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getEvent()).willReturn(EventType.ADD_HEARING);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void handleUpdateListingRequirementsNonGapsSwitchOverFeature() {
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(caseDetails.getState()).willReturn(State.READY_TO_LIST);
        ReflectionTestUtils.setField(handler, "gapsSwitchOverFeature", false);
        sscsCaseData = CaseDataUtils.buildCaseData();
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void handleUpdateListingRequirementsGapsSwitchOverFeatureSendSuccessful() {
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(caseDetails.getState()).willReturn(State.READY_TO_LIST);
        ReflectionTestUtils.setField(handler, "gapsSwitchOverFeature", true);
        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());
        sscsCaseData.setCcdCaseId("1234");

        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        given(listAssistHearingMessageHelper.sendHearingMessage(
            anyString(),any(HearingRoute.class),any(HearingState.class),eq(null)))
            .willReturn(true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        assertThat(response.getData()).isNotNull();
        SscsCaseData caseData = response.getData();
        assertThat(caseData.getSchedulingAndListingFields().getHearingState()).isEqualTo(UPDATE_HEARING);
    }

    @Test
    void handleUpdateListingRequirementsGapsSwitchOverFeatureSendUnsuccessful() {
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(caseDetails.getState()).willReturn(State.READY_TO_LIST);
        ReflectionTestUtils.setField(handler, "gapsSwitchOverFeature", true);
        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());
        sscsCaseData.setCcdCaseId("1234");

        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        given(listAssistHearingMessageHelper.sendHearingMessage(
            anyString(),any(HearingRoute.class),any(HearingState.class),eq(null)))
            .willReturn(false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .contains("An error occurred during message publish. Please try again.");
        assertThat(response.getData()).isNotNull();
    }

    @Test
    void handleUpdateListingRequirementsGapsSwitchOverFeatureNoOverrides() {
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(caseDetails.getState()).willReturn(State.READY_TO_LIST);
        ReflectionTestUtils.setField(handler, "gapsSwitchOverFeature", true);
        sscsCaseData = CaseDataUtils.buildCaseData();
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void handleUpdateListingRequirementsGapsSwitchOverFeatureWrongState() {
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(caseDetails.getState()).willReturn(State.READY_TO_LIST);
        ReflectionTestUtils.setField(handler, "gapsSwitchOverFeature", true);
        sscsCaseData = CaseDataUtils.buildCaseData();

        given(caseDetails.getState()).willReturn(State.UNKNOWN);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = YesNo.class)
    @NullSource
    void reservedDistrictTribunalJudge_savesSelectionToCaseData(YesNo reservedDtj) {
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(caseDetails.getState()).willReturn(State.READY_TO_LIST);
        ReserveTo reserveTo = new ReserveTo();
        reserveTo.setReservedDistrictTribunalJudge(reservedDtj);
        sscsCaseData.getSchedulingAndListingFields().setReserveTo(reserveTo);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        YesNo result = response.getData().getSchedulingAndListingFields().getReserveTo().getReservedDistrictTribunalJudge();
        assertThat(result).isEqualTo(reservedDtj);
    }

    @Test
    void givenReservedDistrictTribunalJudgeIsYesAndReservedJudgeIsNotNull_responseReservedJudgeIsNull() {
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(caseDetails.getState()).willReturn(State.READY_TO_LIST);
        ReserveTo reserveTo = new ReserveTo();
        reserveTo.setReservedDistrictTribunalJudge(YES);
        reserveTo.setReservedJudge(new JudicialUserBase("1", "2"));
        sscsCaseData.getSchedulingAndListingFields().setReserveTo(reserveTo);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        JudicialUserBase result = response.getData().getSchedulingAndListingFields().getReserveTo().getReservedJudge();
        assertThat(result).isNull();
    }

    @Test
    void givenAdjournmentFlagIsOff_thenDontUpdateHearingChannel() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", false);

        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(caseDetails.getState()).willReturn(State.READY_TO_LIST);
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().appellantHearingChannel(HearingChannel.FACE_TO_FACE).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                ABOUT_TO_SUBMIT,
                callback,
                USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getAppeal().getHearingSubtype()).isNull();
    }
}
