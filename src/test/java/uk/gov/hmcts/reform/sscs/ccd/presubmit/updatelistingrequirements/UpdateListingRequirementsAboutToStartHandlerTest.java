package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import java.util.List;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.util.DynamicListLanguageUtil;

@RunWith(MockitoJUnitRunner.class)
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
    @InjectMocks
    private UpdateListingRequirementsAboutToStartHandler handler;

    @Before
    public void setUp() {
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
        ReflectionTestUtils.setField(handler, "isScheduleListingEnabled", true);

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
    public void handleInitiatedUpdateListingRequirementsSandL() {
        ReflectionTestUtils.setField(handler, "isScheduleListingEnabled", true);

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
    }
}
