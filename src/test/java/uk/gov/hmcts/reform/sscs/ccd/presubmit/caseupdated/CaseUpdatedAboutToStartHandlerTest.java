package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;
import uk.gov.hmcts.reform.sscs.util.DynamicListLanguageUtil;

public class CaseUpdatedAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private DynamicListLanguageUtil dynamicListLanguageUtil;

    @Mock
    private VerbalLanguagesService verbalLanguagesService;

    @InjectMocks
    private CaseUpdatedAboutToStartHandler handler;

    @BeforeEach
    public void setUp() {
        openMocks(this);

        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("ccdId")
                .appeal(Appeal.builder()
                        .benefitType(BenefitType.builder()
                                .code("PIP")
                                .build())
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("First").lastName("Last").build())
                                .address(Address.builder().line1("Line1").line2("Line2").postcode("CM120NS").build())
                                .identity(Identity.builder().nino("AB223344B").dob("1995-12-20").build())
                                .build())
                        .build())
                .benefitCode("002")
                .issueCode("DD")
                .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenBenefitType_shouldHaveCorrectBenefitSelection() {
        var result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        var benefitSelection = result.getData().getAppeal().getBenefitType().getDescriptionSelection();

        assertThat(benefitSelection).isNotNull();
        assertThat(benefitSelection.getValue()).isNotNull();
        assertThat(benefitSelection.getValue().getCode()).isEqualTo("002");
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

        Assertions.assertEquals(0, response.getErrors().size());
        Assertions.assertNotNull(hearingOptions.getLanguagesList());
        Assertions.assertNull(hearingOptions.getLanguagesList().getValue());
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

        Assertions.assertEquals(0, response.getErrors().size());
        Assertions.assertNotNull(hearingOptions.getLanguagesList());
        Assertions.assertEquals("Welsh", hearingOptions.getLanguagesList().getValue().getLabel());
    }

    @Test
    public void givenThatOriginalLanguageFieldIsNonEmptyandInvalid_thenSetDynamicListInitialValue() {
        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.getAppeal().getHearingOptions().setLanguages("Wales");

        DynamicListItem item = new DynamicListItem("Wales", "Wales");
        DynamicList list = new DynamicList(null, List.of(item));

        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(dynamicListLanguageUtil.generateInterpreterLanguageFields(any())).willReturn(list);
        given(dynamicListLanguageUtil.getLanguageDynamicListItem(any())).willReturn(item);
        given(verbalLanguagesService.getVerbalLanguage(any())).willReturn(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        HearingOptions hearingOptions = sscsCaseData.getAppeal().getHearingOptions();

        Assertions.assertEquals(0, response.getErrors().size());
        Assertions.assertNotNull(hearingOptions.getLanguagesList());
        Assertions.assertNull(hearingOptions.getLanguagesList().getValue());
    }
}
