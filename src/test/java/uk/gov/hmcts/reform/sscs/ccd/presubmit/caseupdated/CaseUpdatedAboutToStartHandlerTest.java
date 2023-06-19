package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static org.junit.Assert.*;
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
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;
import uk.gov.hmcts.reform.sscs.util.DynamicListLanguageUtil;

@RunWith(MockitoJUnitRunner.class)
public class CaseUpdatedAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private SscsCaseData sscsCaseData;

    @Mock
    private DynamicListLanguageUtil dynamicListLanguageUtil;

    @Mock
    private VerbalLanguagesService verbalLanguagesService;

    @InjectMocks
    private CaseUpdatedAboutToStartHandler handler;

    @Before
    public void setUp() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(callback.getEvent()).willReturn(EventType.CASE_UPDATED);
    }

    @Test
    public void givenThatOriginalLanguageFieldIsEmpty_thenSetDynamicListInitialValueToNull() {
        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.getAppeal().getHearingOptions().setLanguages(null);

        DynamicListItem item = new DynamicListItem("abcd", "Abcd Abcd");
        DynamicList list = new DynamicList(null, List.of(item));

        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(dynamicListLanguageUtil.generateInterpreterLanguageFields(any())).willReturn(list);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        HearingOptions hearingOptions = sscsCaseData.getAppeal().getHearingOptions();

        assertEquals(0, response.getErrors().size());
        assertNotNull(hearingOptions.getLanguagesList());
        assertNull(hearingOptions.getLanguagesList().getValue());
    }

    @Test
    public void givenThatOriginalLanguageFieldIsNonEmpty_thenSetDynamicListInitialValue() {
        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.getAppeal().getHearingOptions().setLanguages("Welsh");

        DynamicListItem item = new DynamicListItem("wel", "Welsh");
        DynamicList list = new DynamicList(null, List.of(item));

        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(dynamicListLanguageUtil.generateInterpreterLanguageFields(any())).willReturn(list);
        given(dynamicListLanguageUtil.getLanguageDynamicListItem(any())).willReturn(item);
        given(verbalLanguagesService.getVerbalLanguage(any())).willReturn(new Language("wel", "Welsh"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        HearingOptions hearingOptions = sscsCaseData.getAppeal().getHearingOptions();

        assertEquals(0, response.getErrors().size());
        assertNotNull(hearingOptions.getLanguagesList());
        assertEquals("Welsh", hearingOptions.getLanguagesList().getValue().getLabel());
    }
}