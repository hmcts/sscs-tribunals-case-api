package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_LISTING_REQUIREMENTS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;

@ExtendWith(MockitoExtension.class)
class UpdateListingRequirementsAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private HearingDurationsService hearingDurationsService;

    @InjectMocks
    private UpdateListingRequirementsAboutToSubmitHandler handler;

    private SscsCaseData sscsCaseData;
    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "isDefaultPanelCompEnabled", true);
        sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().build())
            .dwpIsOfficerAttending("Yes")
                .schedulingAndListingFields(SchedulingAndListingFields.builder().build())
            .build();

        caseDetails =
                new CaseDetails<>(1234L, "SSCS", State.READY_TO_LIST, sscsCaseData, now(), "Benefit");

        callback = new Callback<>(caseDetails, empty(), UPDATE_LISTING_REQUIREMENTS, false);
    }

    @Test
    void givenValidCallback_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT,callback)).isTrue();
    }

    @Test
    void givenInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @Test
    void givenInvalidEventType_thenReturnFalse() {
        callback = new Callback<>(caseDetails, empty(), EventType.ADD_HEARING, false);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void handleUpdateListingRequirementsNoOverrides() {
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
    void givenReservedDistrictTribunalJudgeIsYesAndReservedJudgeIsNotNull_responseReservedJudgeAndPanelCompositionJudgeAreNull() {
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder().panelCompositionJudge("84").panelCompositionMemberMedical1("NoMedicalMemberRequired").build());
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
    void givenNoMedicalMemberRequiredSelected_thenClearMedicalMemberFields() {
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder().panelCompositionJudge("84")
                .panelCompositionMemberMedical1("NoMedicalMemberRequired").panelCompositionMemberMedical2("58").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                ABOUT_TO_SUBMIT,
                callback,
                USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getPanelMemberComposition().getPanelCompositionMemberMedical1()).isNull();
        assertThat(response.getData().getPanelMemberComposition().getPanelCompositionMemberMedical2()).isNull();
    }

    @Test
    void givenHearingChannelIsNotNull_thenReturnHearingSubtype() {
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

    @Test
    void givenAppellantInterpreterHasChanged_thenUpdateCaseDataDefaultDuration() {
        sscsCaseData.getSchedulingAndListingFields().setDefaultListingValues(OverrideFields.builder()
                .duration(60)
                .build());
        sscsCaseData.getAppeal().setHearingOptions(HearingOptions.builder()
            .languageInterpreter("No")
            .build());
        DynamicListItem interpreterLanguageItem = new DynamicListItem("arabic", "Arabic");
        DynamicList interpreterLanguage = new DynamicList(interpreterLanguageItem, List.of());
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder()
                .appellantInterpreter(HearingInterpreter.builder().isInterpreterWanted(YES).interpreterLanguage(interpreterLanguage).build())
                .build());

        when(hearingDurationsService.getHearingDurationBenefitIssueCodes(eq(sscsCaseData))).thenReturn(90);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat("Yes").isEqualTo(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter());
        assertThat(response.getData().getSchedulingAndListingFields().getDefaultListingValues().getDuration()).isEqualTo(90);
    }

    @Test
    void givenAppellantInterpreterHasNotChanged_thenDoNotUpdateCaseDataDefaultDuration() {
        sscsCaseData.getAppeal().setHearingOptions(HearingOptions.builder()
            .languageInterpreter("No")
            .build());
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder()
                .appellantInterpreter(HearingInterpreter.builder().isInterpreterWanted(NO).build())
                .build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT,
            callback,
            USER_AUTHORISATION);
        verifyNoInteractions(hearingDurationsService);
        assertThat(response.getErrors()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = HmcHearingType.class, names = {"SUBSTANTIVE", "DIRECTION_HEARINGS"})
    void givenHmcHearingTypeIsNotNull_thenUpdateToNonNullHearingOptions(HmcHearingType hmcHearingType) {
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
