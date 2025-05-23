package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_LISTING_REQUIREMENTS;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.HmcHearingType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReserveTo;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;
import uk.gov.hmcts.reform.sscs.util.DynamicListLanguageUtil;

public class UpdateListingRequirementsAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private DynamicListLanguageUtil utils;
    @Mock
    private PanelCompositionService panelCompositionService;

    private UpdateListingRequirementsAboutToStartHandler handler;
    private SscsCaseData sscsCaseData;
    private Callback<SscsCaseData> callback;

    @BeforeEach
    public void setUp() {
        handler = new UpdateListingRequirementsAboutToStartHandler(panelCompositionService, utils);
        ReflectionTestUtils.setField(handler, "isScheduleListingEnabled", true);
        ReflectionTestUtils.setField(handler, "isDefaultPanelCompEnabled", true);

        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();

        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(1234L, "SSCS", State.READY_TO_LIST, sscsCaseData, now(), "Benefit");

        callback = new Callback<>(caseDetails, empty(), UPDATE_LISTING_REQUIREMENTS, false);
    }

    @Test
    public void givenValidCallback_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void handleUpdateListingRequirementsNonSandL() {
        ReflectionTestUtils.setField(handler, "isScheduleListingEnabled", false);
        sscsCaseData = CaseDataUtils.buildCaseData();
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void handleNonInitiatedUpdateListingRequirementsSandL() {

        DynamicListItem item = new DynamicListItem("abcd", "Abcd Abcd");
        DynamicList list = new DynamicList(item, List.of(item));

        given(utils.generateInterpreterLanguageFields(any())).willReturn(list);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        HearingInterpreter interpreter = response.getData().getSchedulingAndListingFields().getOverrideFields().getAppellantInterpreter();

        assertEquals(0, response.getErrors().size());
        assertNotNull(interpreter);
        assertNotNull(interpreter.getInterpreterLanguage());
        assertEquals(1, interpreter.getInterpreterLanguage().getListItems().size());
    }

    @Test
    public void handleNonInitiatedInterpreterUpdateListingRequirementsSandL() {
        DynamicListItem item = new DynamicListItem("abcd", "Abcd Abcd");
        DynamicList list = new DynamicList(item, List.of(item));
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());
        given(utils.generateInterpreterLanguageFields(any())).willReturn(list);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        HearingInterpreter interpreter = response.getData().getSchedulingAndListingFields().getOverrideFields().getAppellantInterpreter();

        assertEquals(0, response.getErrors().size());
        assertNotNull(interpreter);
        assertNotNull(interpreter.getInterpreterLanguage());
        assertEquals(1, interpreter.getInterpreterLanguage().getListItems().size());
    }

    @Test
    public void handleInitiatedUpdateListingRequirementsWithInterpreterLanguageSandL() {
        DynamicListItem item = new DynamicListItem("abcd", "Abcd Abcd");
        DynamicList list = new DynamicList(item, List.of(item));
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());
        given(utils.generateInterpreterLanguageFields(any())).willReturn(list);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        HearingInterpreter interpreter = response.getData().getSchedulingAndListingFields().getOverrideFields().getAppellantInterpreter();

        assertEquals(0, response.getErrors().size());
        assertNotNull(interpreter);
        assertNotNull(interpreter.getInterpreterLanguage());
        assertEquals(1, interpreter.getInterpreterLanguage().getListItems().size());
    }

    @Test
    public void handleInitiatedUpdateListingRequirementsWithoutInterpreterLanguageSandL() {
        DynamicList interpreterLanguage = new DynamicList(null, List.of());
        OverrideFields overrideFields = OverrideFields.builder()
            .appellantInterpreter(HearingInterpreter.builder()
                .interpreterLanguage(interpreterLanguage)
                .build())
            .build();
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(overrideFields);

        given(utils.generateInterpreterLanguageFields(any())).willReturn(interpreterLanguage);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        HearingInterpreter interpreter = response.getData().getSchedulingAndListingFields().getOverrideFields().getAppellantInterpreter();

        assertEquals(0, response.getErrors().size());
        assertNotNull(interpreter);
        assertNotNull(interpreter.getInterpreterLanguage());
        assertEquals(0, interpreter.getInterpreterLanguage().getListItems().size());
        assertNull(response.getData().getSchedulingAndListingFields().getOverrideFields().getHmcHearingType());
    }

    @ParameterizedTest
    @EnumSource(value = HmcHearingType.class, names = {"SUBSTANTIVE", "DIRECTION_HEARINGS"})
    public void setOverrideHmcHearingTypeIfNonNull(HmcHearingType expectedHearingType) {
        sscsCaseData.setHmcHearingType(expectedHearingType);
        DynamicList interpreterLanguage = new DynamicList(null, List.of());
        given(utils.generateInterpreterLanguageFields(any())).willReturn(interpreterLanguage);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(expectedHearingType, response.getData().getSchedulingAndListingFields().getOverrideFields().getHmcHearingType());
    }

    @Test
    public void setOverrideHmcHearingTypeIfNull() {
        sscsCaseData.setHmcHearingType(null);
        DynamicList interpreterLanguage = new DynamicList(null, List.of());
        given(utils.generateInterpreterLanguageFields(any())).willReturn(interpreterLanguage);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertNull(response.getData().getSchedulingAndListingFields().getOverrideFields().getHmcHearingType());
    }

    @Test
    public void setReserveToJudgeToNoIfPanelCompositionJudgeSelected() {
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder().panelCompositionJudge("84").build());
        DynamicList interpreterLanguage = new DynamicList(null, List.of());
        given(utils.generateInterpreterLanguageFields(any())).willReturn(interpreterLanguage);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getSchedulingAndListingFields().getReserveTo().getReservedDistrictTribunalJudge()).isEqualTo(YesNo.NO);
    }

    @Test
    public void leaveReserveToJudgeIfDefaultPanelCompDisabled() {
        ReflectionTestUtils.setField(handler, "isDefaultPanelCompEnabled", false);
        sscsCaseData.getSchedulingAndListingFields().setReserveTo(ReserveTo.builder()
                .reservedDistrictTribunalJudge(YesNo.YES).build());
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder().panelCompositionJudge("84").build());
        DynamicList interpreterLanguage = new DynamicList(null, List.of());
        given(utils.generateInterpreterLanguageFields(any())).willReturn(interpreterLanguage);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getSchedulingAndListingFields().getReserveTo().getReservedDistrictTribunalJudge()).isEqualTo(YesNo.YES);
    }
}
