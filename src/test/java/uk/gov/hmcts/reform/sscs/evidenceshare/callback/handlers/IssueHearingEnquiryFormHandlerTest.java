package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_HEARING_ENQUIRY_FORM;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;

import ch.qos.logback.classic.Level;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentSelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.CoverLetterService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.HearingEnquiryFormPlaceholderService;
import uk.gov.hmcts.reform.sscs.util.LogCaptureExtension;

@ExtendWith(MockitoExtension.class)
class IssueHearingEnquiryFormHandlerTest {

    private static final byte[] COVER_LETTER = new byte[]{1};
    private static final byte[] COVER_SHEET = new byte[]{2};
    private static final byte[] BUNDLED_LETTER = new byte[]{3};
    @RegisterExtension
    private final LogCaptureExtension logCapture =
        new LogCaptureExtension(IssueHearingEnquiryFormHandler.class);
    @Mock
    private HearingEnquiryFormPlaceholderService hearingEnquiryFormPlaceholderService;
    @Mock
    private BulkPrintService bulkPrintService;
    @Mock
    private CoverLetterService coverLetterService;
    @Captor
    private ArgumentCaptor<List<Pdf>> sentPdfsCaptor;
    @Captor
    private ArgumentCaptor<String> templateNameCaptor;
    @Captor
    private ArgumentCaptor<String> letterNameCaptor;
    private IssueHearingEnquiryFormHandler handler;

    @BeforeEach
    void setUp() {
        handler = new IssueHearingEnquiryFormHandler(bulkPrintService, hearingEnquiryFormPlaceholderService, coverLetterService,
            buildTemplateConfig(), true);

    }

    @ParameterizedTest
    @MethodSource("canHandleScenarios")
    void shouldReturnExpectedCanHandleResult(CallbackType callbackType, EventType eventType, boolean featureEnabled,
        boolean expected) {
        final IssueHearingEnquiryFormHandler testHandler = new IssueHearingEnquiryFormHandler(bulkPrintService,
            hearingEnquiryFormPlaceholderService, coverLetterService, buildTemplateConfig(), featureEnabled);
        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(SscsCaseData.builder().build(), READY_TO_LIST,
            eventType);

        assertThat(testHandler.canHandle(callbackType, callback)).isEqualTo(expected);
        logCapture.assertLogContains(
            "IssueHearingEnquiryFormHandler canHandle method called for caseId 1 and callbackType " + callbackType,
            Level.INFO);
    }

    @Test
    void shouldThrowExceptionWhenCanHandleCalledWithNullCallback() {
        assertThatThrownBy(() -> handler.canHandle(SUBMITTED, null)).isInstanceOf(NullPointerException.class)
            .hasMessage("callback must not be null");
    }

    @Test
    void shouldThrowExceptionWhenCanHandleCalledWithNullCallbackType() {
        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(SscsCaseData.builder().build(), READY_TO_LIST,
            ISSUE_HEARING_ENQUIRY_FORM);

        assertThatThrownBy(() -> handler.canHandle(null, callback)).isInstanceOf(NullPointerException.class)
            .hasMessage("callbackType must not be null");
    }

    @Test
    void shouldThrowExceptionWhenHandleCalledForUnsupportedCallback() {
        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(SscsCaseData.builder().build(), READY_TO_LIST,
            APPEAL_RECEIVED);

        assertThatThrownBy(() -> handler.handle(MID_EVENT, callback)).isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot handle callback");
        logCapture.assertLogContains(
            "IssueHearingEnquiryFormHandler canHandle method called for caseId 1 and callbackType MID_EVENT",
            Level.INFO);
        logCapture.assertLogContains("Cannot handle this event for case id: 1", Level.INFO);
    }

    @Test
    void shouldReturnLatestPriority() {
        assertThat(handler.getPriority()).isEqualTo(DispatchPriority.LATEST);
    }

    @Test
    void shouldSendLetterWithoutSelectedDocumentsWhenAddDocumentsIsNo() {
        final SscsCaseData caseData = baseCaseData();
        caseData.setAddDocuments(YesNo.NO);
        final Map<String, Object> placeholders = Map.of("address_name", "Other Party");

        when(hearingEnquiryFormPlaceholderService.populatePlaceholders(any(), any(), anyString())).thenReturn(placeholders);
        when(coverLetterService.generateCoverLetterRetry(any(), anyString(), anyString(), any(), anyInt())).thenReturn(
            COVER_LETTER);
        when(coverLetterService.generateCoverSheet(anyString(), anyString(), any())).thenReturn(COVER_SHEET);
        when(bulkPrintService.buildBundledLetter(anyList())).thenReturn(BUNDLED_LETTER);
        final Callback<SscsCaseData> callback = buildTestCallbackForGivenData(caseData, READY_TO_LIST,
            ISSUE_HEARING_ENQUIRY_FORM);

        handler.handle(SUBMITTED, callback);

        verify(coverLetterService, never()).getSelectedDocuments(caseData);
        verify(coverLetterService, times(2)).generateCoverLetterRetry(any(), templateNameCaptor.capture(),
            letterNameCaptor.capture(), eq(placeholders), eq(1));
        verify(coverLetterService).generateCoverSheet("hearing-enquiry-form-cover.docx", "coversheet", placeholders);
        verify(bulkPrintService).sendToBulkPrint(anyLong(), eq(caseData), sentPdfsCaptor.capture(),
            eq(ISSUE_HEARING_ENQUIRY_FORM), eq("Other Person"));

        assertThat(templateNameCaptor.getAllValues()).containsExactlyInAnyOrder("hearing-enquiry-form-letter.docx",
            "hearing-enquiry-form.docx");
        assertThat(letterNameCaptor.getAllValues()).allSatisfy(name -> {
            assertThat(name).isNotBlank();
            assertThat(name).contains("Other Party");
        });

        final List<Pdf> sentPdfs = sentPdfsCaptor.getValue();
        assertThat(sentPdfs).hasSize(1);
        assertThat(sentPdfs.getFirst().getContent()).isEqualTo(BUNDLED_LETTER);
        assertThat(sentPdfs.getFirst().getName()).isNotBlank().contains("Other Party");
        logCapture.assertLogContains("Sending HEF letter to other parties for case id: 1", Level.INFO);
    }

    @Test
    void shouldSendLetterAndIncludeSelectedDocumentsWhenAddDocumentsIsYes() {
        final SscsCaseData caseData = baseCaseData();
        caseData.setAddDocuments(YesNo.YES);
        final List<Pdf> selectedDocuments = List.of(new Pdf(new byte[]{9}, "selected.pdf"));
        final Map<String, Object> placeholders = Map.of("address_name", "Other Party");

        when(coverLetterService.getSelectedDocuments(caseData)).thenReturn(selectedDocuments);
        when(hearingEnquiryFormPlaceholderService.populatePlaceholders(any(), any(), anyString())).thenReturn(placeholders);
        when(coverLetterService.generateCoverLetterRetry(any(), anyString(), anyString(), any(), anyInt())).thenReturn(
            COVER_LETTER);
        when(coverLetterService.generateCoverSheet(anyString(), anyString(), any())).thenReturn(COVER_SHEET);
        when(bulkPrintService.buildBundledLetter(anyList())).thenReturn(BUNDLED_LETTER);

        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_HEARING_ENQUIRY_FORM);
        handler.handle(SUBMITTED, callback);

        verify(coverLetterService).getSelectedDocuments(caseData);
        verify(bulkPrintService).sendToBulkPrint(anyLong(), eq(caseData), sentPdfsCaptor.capture(),
            eq(ISSUE_HEARING_ENQUIRY_FORM), eq("Other Person"));

        List<Pdf> sentPdfs = sentPdfsCaptor.getValue();
        assertThat(sentPdfs).hasSize(2);
        assertThat(sentPdfs.getFirst().getContent()).isEqualTo(BUNDLED_LETTER);
        assertThat(sentPdfs.getFirst().getName()).isNotBlank().contains("Other Party");
        assertThat(sentPdfs.get(1).getName()).isEqualTo("selected.pdf");
        assertThat(sentPdfs.get(1).getContent()).isEqualTo(new byte[]{9});
        logCapture.assertLogContains("Sending HEF letter to other parties for case id: 1", Level.INFO);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldNotSendAnythingWhenOtherPartySelectionIsNullOrEmpty(
        final List<CcdValue<OtherPartySelectionDetails>> otherPartySelection) {
        SscsCaseData caseData = baseCaseData();
        caseData.setOtherPartySelection(otherPartySelection);
        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_HEARING_ENQUIRY_FORM);

        handler.handle(SUBMITTED, callback);

        verify(bulkPrintService, never()).sendToBulkPrint(anyLong(), any(), any(), any(), anyString());
    }

    @ParameterizedTest
    @MethodSource("incompletePartySelectionScenarios")
    void shouldSkipPartyWithIncompleteSelection(CcdValue<OtherPartySelectionDetails> incompleteEntry) {
        SscsCaseData caseData = baseCaseData();
        caseData.setAddDocuments(YesNo.NO);
        caseData.setOtherPartySelection(List.of(incompleteEntry));
        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_HEARING_ENQUIRY_FORM);

        handler.handle(SUBMITTED, callback);

        verify(bulkPrintService, never()).sendToBulkPrint(anyLong(), any(), any(), any(), anyString());
        logCapture.assertLogContains("Sending HEF letter to other parties for case id: 1", Level.INFO);
        logCapture.assertLogContains("Skipping party with incomplete selection data for case id: 1", Level.WARN);
    }

    @Test
    void shouldThrowExceptionWhenBundlingFails() {
        SscsCaseData caseData = baseCaseData();
        caseData.setAddDocuments(YesNo.NO);
        Map<String, Object> placeholders = Map.of("address_name", "Other Party");

        when(hearingEnquiryFormPlaceholderService.populatePlaceholders(any(), any(), anyString())).thenReturn(placeholders);
        when(coverLetterService.generateCoverLetterRetry(any(), anyString(), anyString(), any(), anyInt())).thenReturn(
            COVER_LETTER);
        when(coverLetterService.generateCoverSheet(anyString(), anyString(), any())).thenReturn(COVER_SHEET);
        when(bulkPrintService.buildBundledLetter(anyList())).thenThrow(new BulkPrintException("Failed to merge documents"));

        Callback<SscsCaseData> callback = buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_HEARING_ENQUIRY_FORM);

        assertThatThrownBy(() -> handler.handle(SUBMITTED, callback)).isInstanceOf(BulkPrintException.class);
    }

    @SuppressWarnings("unchecked")
    private static Stream<Arguments> incompletePartySelectionScenarios() {
        final CcdValue<OtherPartySelectionDetails> nullValueEntry = mock(CcdValue.class);
        when(nullValueEntry.getValue()).thenReturn(null);
        return Stream.of(Arguments.of(Named.named("getValue() returns null", nullValueEntry)),
            Arguments.of(Named.named("getOtherPartiesList() returns null", new CcdValue<>(new OtherPartySelectionDetails(null)))),
            Arguments.of(Named.named("DynamicList has no selection",
                new CcdValue<>(new OtherPartySelectionDetails(new DynamicList(null, List.of()))))));
    }

    private static Stream<Arguments> canHandleScenarios() {
        return Stream.of(Arguments.of(SUBMITTED, ISSUE_HEARING_ENQUIRY_FORM, true, true),
            Arguments.of(MID_EVENT, ISSUE_HEARING_ENQUIRY_FORM, true, false),
            Arguments.of(SUBMITTED, APPEAL_RECEIVED, true, false),
            Arguments.of(SUBMITTED, ISSUE_HEARING_ENQUIRY_FORM, false, false));
    }

    private SscsCaseData baseCaseData() {
        final DynamicListItem selectedOtherParty = new DynamicListItem("otherParty1", "Other Party");
        final OtherPartySelectionDetails otherPartySelectionDetails = new OtherPartySelectionDetails(
            new DynamicList(selectedOtherParty, List.of(selectedOtherParty)));
        final DynamicListItem selectedDocument = new DynamicListItem("doc.pdf", "doc.pdf");
        final DocumentSelectionDetails documentSelectionDetails = new DocumentSelectionDetails(
            new DynamicList(selectedDocument, List.of(selectedDocument)));

        return SscsCaseData.builder().ccdCaseId("1").appeal(
                uk.gov.hmcts.reform.sscs.ccd.domain.Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .otherParties(List.of(new CcdValue<>(
                OtherParty.builder().id("1").name(Name.builder().firstName("Other").lastName("Person").build()).build())))
            .otherPartySelection(List.of(new CcdValue<>(otherPartySelectionDetails)))
            .documentSelection(List.of(new CcdValue<>(documentSelectionDetails))).build();
    }

    private DocmosisTemplateConfig buildTemplateConfig() {
        final Map<String, String> hearingEnquiryFormTemplates = new HashMap<>();
        hearingEnquiryFormTemplates.put("name", "hearing-enquiry-form-letter.docx");
        hearingEnquiryFormTemplates.put("form", "hearing-enquiry-form.docx");
        hearingEnquiryFormTemplates.put("cover", "hearing-enquiry-form-cover.docx");

        final Map<String, Map<String, String>> languageTemplates = new HashMap<>();
        languageTemplates.put("hearing-enquiry-form", hearingEnquiryFormTemplates);

        final Map<LanguagePreference, Map<String, Map<String, String>>> templates = new EnumMap<>(LanguagePreference.class);
        templates.put(LanguagePreference.ENGLISH, languageTemplates);

        final DocmosisTemplateConfig config = new DocmosisTemplateConfig();
        config.setTemplate(templates);
        return config;
    }

}
