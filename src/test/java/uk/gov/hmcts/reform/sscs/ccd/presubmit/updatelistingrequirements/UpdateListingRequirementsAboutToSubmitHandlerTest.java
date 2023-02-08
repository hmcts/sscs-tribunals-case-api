package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.UPDATE_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;

@RunWith(MockitoJUnitRunner.class)
public class UpdateListingRequirementsAboutToSubmitHandlerTest {

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

    @Before
    public void setUp() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).dwpIsOfficerAttending("Yes").build();
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(caseDetails.getState()).willReturn(State.READY_TO_LIST);
    }

    @Test
    public void givenValidCallback_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    public void givenInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @Test
    public void givenInvalidEventType_thenReturnFalse() {
        given(callback.getEvent()).willReturn(EventType.ADD_HEARING);

        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    public void handleUpdateListingRequirementsNonGapsSwitchOverFeature() {
        ReflectionTestUtils.setField(handler, "gapsSwitchOverFeature", false);
        sscsCaseData = CaseDataUtils.buildCaseData();
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void handleUpdateListingRequirementsGapsSwitchOverFeatureSendSuccessful() {
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

        assertEquals(0, response.getErrors().size());

        assertThat(response.getData()).isNotNull();
        SscsCaseData caseData = response.getData();
        assertThat(caseData.getSchedulingAndListingFields().getHearingState()).isEqualTo(UPDATE_HEARING);
    }

    @Test
    public void handleUpdateListingRequirementsGapsSwitchOverFeatureSendUnsuccessful() {
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

        assertEquals(1, response.getErrors().size());

        assertThat(response.getData()).isNotNull();
    }

    @Test
    public void handleUpdateListingRequirementsGapsSwitchOverFeatureNoOverrides() {
        ReflectionTestUtils.setField(handler, "gapsSwitchOverFeature", true);
        sscsCaseData = CaseDataUtils.buildCaseData();
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void handleUpdateListingRequirementsGapsSwitchOverFeatureWrongState() {
        ReflectionTestUtils.setField(handler, "gapsSwitchOverFeature", true);
        sscsCaseData = CaseDataUtils.buildCaseData();

        given(caseDetails.getState()).willReturn(State.UNKNOWN);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void reservedDistrictTribunalJudge_Yes() {
        ReserveTo reserveTo = new ReserveTo();
        reserveTo.setReservedDistrictTribunalJudge(YES);
        reserveTo.setReservedJudge(new JudicialUserBase("", ""));
        sscsCaseData.getSchedulingAndListingFields().setReserveTo(reserveTo);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getData().getSchedulingAndListingFields().getReserveTo().getReservedDistrictTribunalJudge()).isEqualTo(YES);
    }

    @Test
    public void reservedDistrictTribunalJudge_No() {
        ReserveTo reserveTo = new ReserveTo();
        reserveTo.setReservedDistrictTribunalJudge(NO);
        reserveTo.setReservedJudge(new JudicialUserBase("", ""));
        sscsCaseData.getSchedulingAndListingFields().setReserveTo(reserveTo);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getData().getSchedulingAndListingFields().getReserveTo().getReservedDistrictTribunalJudge()).isEqualTo(NO);
    }

    @Test
    public void reservedDistrictTribunalJudge_null() {
        ReserveTo reserveTo = new ReserveTo();
        reserveTo.setReservedDistrictTribunalJudge(null);
        reserveTo.setReservedJudge(new JudicialUserBase("", ""));
        sscsCaseData.getSchedulingAndListingFields().setReserveTo(reserveTo);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getData().getSchedulingAndListingFields().getReserveTo().getReservedDistrictTribunalJudge()).isNull();
    }

    @Test
    public void reservedDistrictTribunalJudge_throwsException() {
        ReserveTo reserveTo = new ReserveTo();
        reserveTo.setReservedDistrictTribunalJudge(YES);
        reserveTo.setReservedJudge(new JudicialUserBase("123", "456"));
        sscsCaseData.getSchedulingAndListingFields().setReserveTo(reserveTo);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("Reserved Judge field is not applicable as case is reserved to a District Tribunal Judge");
    }
}
