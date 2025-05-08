package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.VIDEO;

import java.util.List;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingSubtype;
import uk.gov.hmcts.reform.sscs.ccd.domain.HmcHearingType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReserveTo;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;

@ExtendWith(MockitoExtension.class)
class UpdateListingRequirementsAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @InjectMocks
    private UpdateListingRequirementsAboutToSubmitHandler handler;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "isDefaultPanelCompEnabled", true);
        sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().build())
            .dwpIsOfficerAttending("Yes")
            .build();
    }

    @Test
    void givenValidCallback_thenReturnTrue() {
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT,callback)).isTrue();
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
        ReflectionTestUtils.setField(handler, "gapsSwitchOverFeature", false);
        sscsCaseData = CaseDataUtils.buildCaseData();
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void handleUpdateListingRequirementsGapsSwitchOverFeatureNoOverrides() {
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        ReflectionTestUtils.setField(handler, "gapsSwitchOverFeature", true);
        sscsCaseData = CaseDataUtils.buildCaseData();
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
        ReserveTo reserveTo = new ReserveTo();
        reserveTo.setReservedDistrictTribunalJudge(reservedDtj);
        sscsCaseData.getSchedulingAndListingFields().setReserveTo(reserveTo);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        YesNo result = response.getData().getSchedulingAndListingFields().getReserveTo().getReservedDistrictTribunalJudge();
        assertThat(reservedDtj).isEqualTo(result);
    }

    @Test
    void givenReservedDistrictTribunalJudgeIsYesAndReservedJudgeIsNotNull_responseReservedJudgeIsNull() {
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder().panelCompositionJudge("84").build());
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
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
        assertThat(response.getData().getPanelMemberComposition().getPanelCompositionJudge()).isNull();
    }

    @Test
    void givenHearingChannelIsNotNull_thenReturnHearingSubtype() {
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().appellantHearingChannel(VIDEO).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getAppeal().getHearingSubtype()).isNotNull();
        assertThat(response.getData().getAppeal().getHearingSubtype().isWantsHearingTypeVideo()).isTrue();
    }

    @Test
    void givenHearingChannelIsNull_thenReturnCaseDataHearingSubtype() {
        HearingSubtype hearingSubType = HearingSubtype.builder()
            .hearingTelephoneNumber("09038920")
            .wantsHearingTypeTelephone(YES.getValue())
            .build();

        sscsCaseData.getAppeal().setHearingSubtype(hearingSubType);

        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().appellantHearingChannel(null).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getAppeal().getHearingSubtype()).isNotNull();
        assertThat(response.getData().getAppeal().getHearingSubtype().isWantsHearingTypeTelephone()).isTrue();
    }

    @Test
    void givenAppellantInterpreterIsNotNull_thenReturnLanguageInterpreterOnCaseData() {
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        DynamicListItem interpreterLanguageItem = new DynamicListItem("test", "Arabic");
        DynamicList interpreterLanguage = new DynamicList(interpreterLanguageItem, List.of());

        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder()
            .appellantInterpreter(HearingInterpreter.builder()
                .isInterpreterWanted(YES)
                .interpreterLanguage(interpreterLanguage)
                .build())
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat("Yes").isEqualTo(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter());
        assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isNotNull();
        assertThat("Arabic").isEqualTo(response.getData().getAppeal().getHearingOptions().getLanguages());

    }

    @Test
    void givenAppellantInterpreterIsNull_thenDoNotUpdateCaseDataInterpreter() {
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder()
            .appellantInterpreter(null)
            .build());

        sscsCaseData.getAppeal().setHearingOptions(HearingOptions.builder()
            .languageInterpreter("Yes")
            .languages("Welsh")
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat("Yes").isEqualTo(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter());
        assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = HmcHearingType.class, names = {"SUBSTANTIVE", "DIRECTION_HEARINGS"})
    void givenHmcHearingTypeIsNotNull_thenUpdateToNonNullHearingOptions(HmcHearingType hmcHearingType) {
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder()
            .appellantInterpreter(null)
            .hmcHearingType(hmcHearingType)
            .build());

        sscsCaseData.getAppeal().setHearingOptions(HearingOptions.builder()
            .languageInterpreter("Yes")
            .languages("Welsh")
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat("Yes").isEqualTo(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter());
        assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isNotNull();
        assertThat(hmcHearingType).isEqualTo(response.getData().getAppeal().getHearingOptions().getHmcHearingType());
    }

    @ParameterizedTest
    @EnumSource(value = HmcHearingType.class, names = {"SUBSTANTIVE", "DIRECTION_HEARINGS"})
    void givenHmcHearingTypeIsNotNull_thenUpdateToNullHearingOptions(HmcHearingType hmcHearingType) {
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder()
            .hmcHearingType(hmcHearingType)
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(hmcHearingType).isEqualTo(response.getData().getAppeal().getHearingOptions().getHmcHearingType());
    }
}
