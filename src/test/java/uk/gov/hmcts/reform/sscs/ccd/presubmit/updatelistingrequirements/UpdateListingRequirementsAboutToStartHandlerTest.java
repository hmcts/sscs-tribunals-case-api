package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.HmcHearingType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.util.DynamicListLanguageUtil;

public class UpdateListingRequirementsAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private SscsCaseData sscsCaseData;
    @Mock
    DynamicListLanguageUtil utils;

    private UpdateListingRequirementsAboutToStartHandler handler;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        handler = new UpdateListingRequirementsAboutToStartHandler(utils);
        ReflectionTestUtils.setField(handler, "isScheduleListingEnabled", true);
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
    }

    @Test
    public void givenValidCallback_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isTrue();
    }

    @Test
    public void handleUpdateListingRequirementsNonSandL() {
        ReflectionTestUtils.setField(handler, "isScheduleListingEnabled", false);
        sscsCaseData = CaseDataUtils.buildCaseData();
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    public void handleNonInitiatedUpdateListingRequirementsSandL() {
        sscsCaseData = CaseDataUtils.buildCaseData();

        DynamicListItem item = new DynamicListItem("abcd", "Abcd Abcd");
        DynamicList list = new DynamicList(item, List.of(item));

        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(utils.generateInterpreterLanguageFields(any())).willReturn(list);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        HearingInterpreter interpreter = response.getData().getSchedulingAndListingFields().getOverrideFields().getAppellantInterpreter();

        assertThat(response.getErrors()).isEmpty();
        assertThat(interpreter).isNotNull();
        assertThat(interpreter.getInterpreterLanguage()).isNotNull();
        assertThat(interpreter.getInterpreterLanguage().getListItems().size()).isEqualTo(1);
    }

    @Test
    public void handleNonInitiatedInterpreterUpdateListingRequirementsSandL() {
        sscsCaseData = CaseDataUtils.buildCaseData();

        DynamicListItem item = new DynamicListItem("abcd", "Abcd Abcd");
        DynamicList list = new DynamicList(item, List.of(item));
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(utils.generateInterpreterLanguageFields(any())).willReturn(list);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        HearingInterpreter interpreter = response.getData().getSchedulingAndListingFields().getOverrideFields().getAppellantInterpreter();

        assertThat(response.getErrors()).isEmpty();
        assertThat(interpreter).isNotNull();
        assertThat(interpreter.getInterpreterLanguage()).isNotNull();
        assertThat(interpreter.getInterpreterLanguage().getListItems().size()).isEqualTo(1);
    }

    @Test
    public void handleInitiatedUpdateListingRequirementsWithInterpreterLanguageSandL() {
        sscsCaseData = CaseDataUtils.buildCaseData();

        DynamicListItem item = new DynamicListItem("abcd", "Abcd Abcd");
        DynamicList list = new DynamicList(item, List.of(item));
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(utils.generateInterpreterLanguageFields(any())).willReturn(list);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        HearingInterpreter interpreter = response.getData().getSchedulingAndListingFields().getOverrideFields().getAppellantInterpreter();

        assertThat(response.getErrors()).isEmpty();
        assertThat(interpreter).isNotNull();
        assertThat(interpreter.getInterpreterLanguage()).isNotNull();
        assertThat(interpreter.getInterpreterLanguage().getListItems().size()).isEqualTo(1);
    }

    @Test
    public void handleInitiatedUpdateListingRequirementsWithoutInterpreterLanguageSandL() {
        sscsCaseData = CaseDataUtils.buildCaseData();
        DynamicList interpreterLanguage = new DynamicList(null, List.of());
        OverrideFields overrideFields = OverrideFields.builder()
            .appellantInterpreter(HearingInterpreter.builder()
                .interpreterLanguage(interpreterLanguage)
                .build())
            .build();
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(overrideFields);

        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(utils.generateInterpreterLanguageFields(any())).willReturn(interpreterLanguage);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        HearingInterpreter interpreter = response.getData().getSchedulingAndListingFields().getOverrideFields().getAppellantInterpreter();

        assertThat(response.getErrors()).isEmpty();
        assertThat(interpreter).isNotNull();
        assertThat(interpreter.getInterpreterLanguage()).isNotNull();
        assertThat(interpreter.getInterpreterLanguage().getListItems().size()).isEqualTo(0);
        assertThat(response.getData().getSchedulingAndListingFields().getOverrideFields().getHmcHearingType()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = HmcHearingType.class, names = {"SUBSTANTIVE", "DIRECTION_HEARINGS"})
    public void setOverrideHmcHearingTypeIfNonNull(HmcHearingType expectedHearingType) {
        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.setHmcHearingType(expectedHearingType);
        DynamicList interpreterLanguage = new DynamicList(null, List.of());
        given(utils.generateInterpreterLanguageFields(any())).willReturn(interpreterLanguage);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(expectedHearingType).isEqualTo(response.getData().getSchedulingAndListingFields().getOverrideFields().getHmcHearingType());
    }

    @Test
    public void setOverrideHmcHearingTypeIfNull() {
        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.setHmcHearingType(null);
        DynamicList interpreterLanguage = new DynamicList(null, List.of());
        given(utils.generateInterpreterLanguageFields(any())).willReturn(interpreterLanguage);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getSchedulingAndListingFields().getOverrideFields().getHmcHearingType()).isNull();
    }

    @Test
    public void setReserveToJudgeToNoIfPanelCompositionJudgeSelected() {
        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder().panelCompositionJudge("84").build());
        DynamicList interpreterLanguage = new DynamicList(null, List.of());
        given(utils.generateInterpreterLanguageFields(any())).willReturn(interpreterLanguage);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getSchedulingAndListingFields().getReserveTo().getReservedDistrictTribunalJudge()).isEqualTo(YesNo.NO);
    }
}
