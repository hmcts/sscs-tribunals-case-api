package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
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
        ReflectionTestUtils.setField(handler, "isDirectionHearingsEnabled", false);
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(callback.getEvent()).willReturn(EventType.UPDATE_LISTING_REQUIREMENTS);
    }

    @Test
    public void givenValidCallback_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void handleUpdateListingRequirementsNonSandL() {
        ReflectionTestUtils.setField(handler, "isScheduleListingEnabled", false);
        sscsCaseData = CaseDataUtils.buildCaseData();
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
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

        assertEquals(0, response.getErrors().size());
        assertNotNull(interpreter);
        assertNotNull(interpreter.getInterpreterLanguage());
        assertEquals(1, interpreter.getInterpreterLanguage().getListItems().size());
    }

    @Test
    public void handleNonInitiatedInterpreterUpdateListingRequirementsSandL() {
        ReflectionTestUtils.setField(handler, "isScheduleListingEnabled", true);

        sscsCaseData = CaseDataUtils.buildCaseData();

        DynamicListItem item = new DynamicListItem("abcd", "Abcd Abcd");
        DynamicList list = new DynamicList(item, List.of(item));
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(utils.generateInterpreterLanguageFields(any())).willReturn(list);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        HearingInterpreter interpreter = response.getData().getSchedulingAndListingFields().getOverrideFields().getAppellantInterpreter();

        assertEquals(0, response.getErrors().size());
        assertNotNull(interpreter);
        assertNotNull(interpreter.getInterpreterLanguage());
        assertEquals(1, interpreter.getInterpreterLanguage().getListItems().size());
    }

    @Test
    public void handleInitiatedUpdateListingRequirementsSandL() {
        ReflectionTestUtils.setField(handler, "isScheduleListingEnabled", true);

        sscsCaseData = CaseDataUtils.buildCaseData();

        DynamicListItem item = new DynamicListItem("abcd", "Abcd Abcd");
        DynamicList list = new DynamicList(item, List.of(item));
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(utils.generateInterpreterLanguageFields(any())).willReturn(list);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        HearingInterpreter interpreter = response.getData().getSchedulingAndListingFields().getOverrideFields().getAppellantInterpreter();

        assertEquals(0, response.getErrors().size());
        assertNotNull(interpreter);
        assertNotNull(interpreter.getInterpreterLanguage());
        assertEquals(1, interpreter.getInterpreterLanguage().getListItems().size());
    }

    @Test
    public void handleInitiatedUpdateListingRequirementsSandL() {
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

        assertEquals(0, response.getErrors().size());
        assertNotNull(interpreter);
        assertNotNull(interpreter.getInterpreterLanguage());
        assertEquals(0, interpreter.getInterpreterLanguage().getListItems().size());
        assertNull(response.getData().getSchedulingAndListingFields().getOverrideFields().getHmcHearingType());
    }

    @ParameterizedTest
    @EnumSource(value = HmcHearingType.class, names = {"SUBSTANTIVE", "DIRECTION_HEARINGS"})
    public void setOverrideHmcHearingTypeIfNonNull(HmcHearingType expectedHearingType) {
        ReflectionTestUtils.setField(handler, "isDirectionHearingsEnabled", true);
        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.setHmcHearingType(expectedHearingType);
        DynamicList interpreterLanguage = new DynamicList(null, List.of());
        given(utils.generateInterpreterLanguageFields(any())).willReturn(interpreterLanguage);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(expectedHearingType, response.getData().getSchedulingAndListingFields().getOverrideFields().getHmcHearingType());
    }

    @Test
    public void setOverrideHmcHearingTypeIfNull() {
        ReflectionTestUtils.setField(handler, "isDirectionHearingsEnabled", true);
        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.setHmcHearingType(null);
        DynamicList interpreterLanguage = new DynamicList(null, List.of());
        given(utils.generateInterpreterLanguageFields(any())).willReturn(interpreterLanguage);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertNull(response.getData().getSchedulingAndListingFields().getOverrideFields().getHmcHearingType());
    }
}
