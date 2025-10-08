package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_LISTING_REQUIREMENTS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.reference.data.model.DefaultPanelComposition;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;
import uk.gov.hmcts.reform.sscs.util.DynamicListLanguageUtil;

@ExtendWith(MockitoExtension.class)
public class UpdateListingRequirementsAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private PanelCompositionService panelCompositionService;
    @Mock
    private DynamicListLanguageUtil utils;

    private SscsCaseData sscsCaseData;
    private Callback<SscsCaseData> callback;
    private UpdateListingRequirementsAboutToStartHandler handler;

    @BeforeEach
    public void setUp() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        CaseDetails<SscsCaseData> caseDetails =
                new CaseDetails<>(1234L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, empty(), UPDATE_LISTING_REQUIREMENTS, false);
        handler = new UpdateListingRequirementsAboutToStartHandler(panelCompositionService, utils);
    }

    @Test
    public void givenValidCallback_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void handleNonInitiatedUpdateListingRequirementsSandL() {
        DynamicListItem item = new DynamicListItem("abcd", "Abcd Abcd");
        DynamicList list = new DynamicList(item, List.of(item));
        given(utils.generateInterpreterLanguageFields(any())).willReturn(list);
        given(panelCompositionService.getDefaultPanelComposition(eq(sscsCaseData)))
                .willReturn(new DefaultPanelComposition());

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
        given(panelCompositionService.getDefaultPanelComposition(eq(sscsCaseData)))
                .willReturn(new DefaultPanelComposition());

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
        given(panelCompositionService.getDefaultPanelComposition(eq(sscsCaseData)))
                .willReturn(new DefaultPanelComposition());

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
        given(panelCompositionService.getDefaultPanelComposition(eq(sscsCaseData)))
                .willReturn(new DefaultPanelComposition());

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
        given(panelCompositionService.getDefaultPanelComposition(eq(sscsCaseData)))
                .willReturn(new DefaultPanelComposition());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(expectedHearingType, response.getData().getSchedulingAndListingFields().getOverrideFields().getHmcHearingType());
    }

    @Test
    public void setOverrideHmcHearingTypeIfNull() {
        sscsCaseData.setHmcHearingType(null);
        DynamicList interpreterLanguage = new DynamicList(null, List.of());
        given(utils.generateInterpreterLanguageFields(any())).willReturn(interpreterLanguage);
        given(panelCompositionService.getDefaultPanelComposition(eq(sscsCaseData)))
                .willReturn(new DefaultPanelComposition());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertNull(response.getData().getSchedulingAndListingFields().getOverrideFields().getHmcHearingType());
    }

    @Test
    public void setReserveToJudgeToNoIfPanelCompositionJudgeSelected() {
        handler = new UpdateListingRequirementsAboutToStartHandler(panelCompositionService, utils);
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder().panelCompositionJudge("84").build());
        DynamicList interpreterLanguage = new DynamicList(null, List.of());
        given(utils.generateInterpreterLanguageFields(any())).willReturn(interpreterLanguage);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getSchedulingAndListingFields().getReserveTo().getReservedDistrictTribunalJudge()).isEqualTo(YesNo.NO);
    }

    @Test
    public void whenPanelCompositionIsEmptyOrNull_thenPopulatePanelCompositionWithDefaultValues() {
        handler = new UpdateListingRequirementsAboutToStartHandler(panelCompositionService, utils);
        DynamicList interpreterLanguage = new DynamicList(null, List.of());
        given(utils.generateInterpreterLanguageFields(any())).willReturn(interpreterLanguage);
        var johTiers = List.of("50", "84");
        var defaultPanelComposition = new DefaultPanelComposition(sscsCaseData.getIssueCode(), sscsCaseData);
        defaultPanelComposition.setJohTiers(johTiers);
        given(panelCompositionService.getDefaultPanelComposition(any())).willReturn(defaultPanelComposition);


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        verify(panelCompositionService, times(1)).getDefaultPanelComposition(any());
        PanelMemberComposition panelMemberComposition = new PanelMemberComposition(johTiers);
        assertThat(response.getData().getPanelMemberComposition()).isEqualTo(panelMemberComposition);
    }

    @Test
    public void whenPanelCompositionExists_thenUseExistingValues() {
        DynamicList interpreterLanguage = new DynamicList(null, List.of());
        given(utils.generateInterpreterLanguageFields(any())).willReturn(interpreterLanguage);
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder().panelCompositionJudge("22")
                .panelCompositionMemberMedical1("58").panelCompositionDisabilityAndFqMember(null).build());

        var response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        verifyNoInteractions(panelCompositionService);
        assertEquals("22", response.getData().getPanelMemberComposition().getPanelCompositionJudge());
        assertEquals("58", response.getData().getPanelMemberComposition().getPanelCompositionMemberMedical1());
        assertNull(response.getData().getPanelMemberComposition().getPanelCompositionDisabilityAndFqMember());

    }
}
