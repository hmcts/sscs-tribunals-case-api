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
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.UkPortOfEntry;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;
import uk.gov.hmcts.reform.sscs.util.DynamicListLanguageUtil;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

class CaseUpdatedAboutToStartHandlerTest {
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

    @Mock
    private SignLanguagesService signLanguagesService;

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
        assertThat(benefitSelection.getListItems()).hasSize(35);
    }

    @Test
    void givenPortOfEntryValueNotNull_shouldNotSetListUp() {
        DynamicList ukPortOfEntries = SscsUtil.getPortsOfEntry();
        ukPortOfEntries.setValue(new DynamicListItem("GBSTTRT00", "Althorpe"));
        sscsCaseData.setBenefitCode("093");
        sscsCaseData.getAppeal().getAppellant().getAddress().setUkPortOfEntryList(ukPortOfEntries);
        var result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        var portOfEntryList = result.getData().getAppeal().getAppellant().getAddress().getUkPortOfEntryList();
        var portOfEntry = result.getData().getAppeal().getAppellant().getAddress().getUkPortOfEntry();
        var portOfEntryCode = result.getData().getAppeal().getAppellant().getAddress().getPortOfEntry();

        assertThat(portOfEntry).isNull();
        assertThat(portOfEntryCode).isNull();
        assertThat(portOfEntryList).isNotNull();
        assertThat(portOfEntryList.getValue().getCode()).isEqualTo("GBSTTRT00");
        assertThat(portOfEntryList.getValue().getLabel()).isEqualTo("Althorpe");
        assertThat(portOfEntryList.getListItems()).hasSameSizeAs(ukPortOfEntries.getListItems());
    }

    @Test
    void givenPortOfEntryValueNull_shouldSetListUpWithNullValue() {
        DynamicList ukPortOfEntries = SscsUtil.getPortsOfEntry();
        sscsCaseData.setBenefitCode("093");
        sscsCaseData.getAppeal().getAppellant().getAddress().setUkPortOfEntryList(ukPortOfEntries);
        var result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        var portOfEntryList = result.getData().getAppeal().getAppellant().getAddress().getUkPortOfEntryList();
        var portOfEntry = result.getData().getAppeal().getAppellant().getAddress().getUkPortOfEntry();
        var portOfEntryCode = result.getData().getAppeal().getAppellant().getAddress().getPortOfEntry();

        assertThat(portOfEntry).isNull();
        assertThat(portOfEntryCode).isNull();
        assertThat(portOfEntryList).isNotNull();
        assertThat(portOfEntryList.getValue()).isNull();
        assertThat(portOfEntryList.getListItems()).hasSize(UkPortOfEntry.values().length);
    }

    @Test
    void givenPortOfEntryCode_shouldSetListUpWithValueFromCode() {
        sscsCaseData.setBenefitCode("093");
        sscsCaseData.getAppeal().getAppellant().getAddress().setPortOfEntry("GBSTTRT00");
        var result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        var portOfEntryList = result.getData().getAppeal().getAppellant().getAddress().getUkPortOfEntryList();
        var portOfEntry = result.getData().getAppeal().getAppellant().getAddress().getUkPortOfEntry();
        var portOfEntryCode = result.getData().getAppeal().getAppellant().getAddress().getPortOfEntry();

        assertThat(portOfEntry.getLocationCode()).isEqualTo("GBSTTRT00");
        assertThat(portOfEntry.getLabel()).isEqualTo("Althorpe");
        assertThat(portOfEntryCode).isEqualTo("GBSTTRT00");
        assertThat(portOfEntryList).isNotNull();
        assertThat(portOfEntryList.getValue().getCode()).isEqualTo("GBSTTRT00");
        assertThat(portOfEntryList.getValue().getLabel()).isEqualTo("Althorpe");
        assertThat(portOfEntryList.getListItems()).hasSize(UkPortOfEntry.values().length);
    }

    @Test
    void givenNoPortOfEntryCode_shouldSetListUpWithNullValue() {
        sscsCaseData.setBenefitCode("093");
        var result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        var portOfEntryList = result.getData().getAppeal().getAppellant().getAddress().getUkPortOfEntryList();
        var portOfEntry = result.getData().getAppeal().getAppellant().getAddress().getUkPortOfEntry();
        var portOfEntryCode = result.getData().getAppeal().getAppellant().getAddress().getPortOfEntry();

        assertThat(portOfEntry).isNull();
        assertThat(portOfEntryCode).isNull();
        assertThat(portOfEntryList).isNotNull();
        assertThat(portOfEntryList.getValue()).isNull();
        assertThat(portOfEntryList.getListItems()).hasSize(UkPortOfEntry.values().length);
    }

    @Test
    void givenNonIbcCase_shouldNotSetListUp() {
        var result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        var portOfEntryList = result.getData().getAppeal().getAppellant().getAddress().getUkPortOfEntryList();
        var portOfEntry = result.getData().getAppeal().getAppellant().getAddress().getUkPortOfEntry();
        var portOfEntryCode = result.getData().getAppeal().getAppellant().getAddress().getPortOfEntry();

        assertThat(portOfEntry).isNull();
        assertThat(portOfEntryCode).isNull();
        assertThat(portOfEntryList).isNull();
    }

    @Test
    void givenThatOriginalLanguageFieldIsEmpty_thenSetDynamicListInitialValueToNull() {
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
    void givenThatOriginalLanguageFieldIsNonEmpty_thenSetDynamicListInitialValue() {
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
    void givenThatOriginalLanguageFieldIsSignLanguage_thenSetDynamicListInitialValue() {
        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.getAppeal().getHearingOptions().setLanguages("British Sign Language (BSL)");

        DynamicListItem item = new DynamicListItem("bfi", "British Sign Language (BSL)");
        DynamicList list = new DynamicList(null, List.of(item));

        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(dynamicListLanguageUtil.generateInterpreterLanguageFields(any())).willReturn(list);
        given(dynamicListLanguageUtil.getLanguageDynamicListItem(any())).willReturn(item);
        given(verbalLanguagesService.getVerbalLanguage(any())).willReturn(null);
        given(signLanguagesService.getSignLanguage(any())).willReturn(new Language("bfi", "British Sign Language (BSL)"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        HearingOptions hearingOptions = sscsCaseData.getAppeal().getHearingOptions();

        assertThat(response.getErrors()).isEmpty();
        assertThat(hearingOptions.getLanguagesList()).isNotNull();
        assertThat(hearingOptions.getLanguagesList().getValue().getLabel()).isEqualTo("British Sign Language (BSL)");
    }

    @Test
    void givenThatOriginalLanguageFieldIsNonEmptyandInvalid_thenSetDynamicListInitialValue() {
        sscsCaseData = CaseDataUtils.buildCaseData();
        sscsCaseData.getAppeal().getHearingOptions().setLanguages("Wales");

        DynamicListItem item = new DynamicListItem("Wales", "Wales");
        DynamicList list = new DynamicList(null, List.of(item));

        given(caseDetails.getCaseData()).willReturn(sscsCaseData);
        given(dynamicListLanguageUtil.generateInterpreterLanguageFields(any())).willReturn(list);
        given(dynamicListLanguageUtil.getLanguageDynamicListItem(any())).willReturn(item);
        given(verbalLanguagesService.getVerbalLanguage(any())).willReturn(null);
        given(signLanguagesService.getSignLanguage(any())).willReturn(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        HearingOptions hearingOptions = sscsCaseData.getAppeal().getHearingOptions();

        Assertions.assertEquals(0, response.getErrors().size());
        Assertions.assertNotNull(hearingOptions.getLanguagesList());
        Assertions.assertNull(hearingOptions.getLanguagesList().getValue());
    }
}
