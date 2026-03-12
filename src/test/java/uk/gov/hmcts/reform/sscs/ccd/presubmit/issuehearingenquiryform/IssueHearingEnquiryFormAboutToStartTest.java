package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuehearingenquiryform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_HEARING_ENQUIRY_FORM;

import ch.qos.logback.classic.Level;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.util.LogCaptureExtension;

@ExtendWith(MockitoExtension.class)
class IssueHearingEnquiryFormAboutToStartTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String OTHER_PARTY_HEARING_PREFERENCES = "otherPartyHearingPreferences";

    @RegisterExtension
    private final LogCaptureExtension logCapture =
        new LogCaptureExtension(IssueHearingEnquiryFormAboutToStart.class);

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private IssueHearingEnquiryFormAboutToStart handler;
    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        handler = new IssueHearingEnquiryFormAboutToStart(true);
        caseData = SscsCaseData.builder().build();

        lenient().when(callback.getCaseDetails()).thenReturn(caseDetails);
        lenient().when(caseDetails.getCaseData()).thenReturn(caseData);
    }

    @ParameterizedTest
    @MethodSource("canHandleScenarios")
    void shouldReturnExpectedValueFromCanHandle(CallbackType callbackType, EventType eventType, boolean expected) {
        lenient().when(callback.getEvent()).thenReturn(eventType);

        assertThat(handler.canHandle(callbackType, callback)).isEqualTo(expected);
    }

    @Test
    void shouldThrowExceptionWhenCanHandleCalledWithNullCallback() {
        assertThatThrownBy(() -> handler.canHandle(ABOUT_TO_START, null)).isInstanceOf(NullPointerException.class)
            .hasMessage("callback must not be null");
    }

    @Test
    void shouldThrowExceptionWhenCanHandleCalledWithNullCallbackType() {
        assertThatThrownBy(() -> handler.canHandle(null, callback)).isInstanceOf(NullPointerException.class)
            .hasMessage("callbacktype must not be null");
    }

    @Test
    void shouldThrowExceptionWhenHandleCalledForUnsupportedCallback() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);

        assertThatThrownBy(() -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION)).isInstanceOf(
            IllegalStateException.class).hasMessage("Cannot handle callback.");
    }

    @Test
    void shouldSetNoOtherPartiesAndEmptyDocumentListWhenCaseHasNoRelevantData() {
        when(callback.getEvent()).thenReturn(ISSUE_HEARING_ENQUIRY_FORM);
        caseData.setOtherParties(null);
        caseData.setDwpDocuments(null);
        caseData.setSscsDocument(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData()).isSameAs(caseData);
        assertThat(response.getData().getOtherPartySelection()).isNull();
        assertThat(response.getData().getDocumentSelection()).hasSize(1);
        assertThat(response.getData().getDocumentSelection().getFirst().getValue().getDocumentsList().getListItems()).isEmpty();
    }

    @Test
    void shouldNotHandleWhenCmOtherPartyConfidentialityIsDisabled() {
        final IssueHearingEnquiryFormAboutToStart disabledHandler = new IssueHearingEnquiryFormAboutToStart(false);

        assertThat(disabledHandler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @Test
    void shouldSetOtherPartyOptionsAndOnlyIncludeHearingPreferenceDocumentsWithEditedVersions() {
        when(callback.getEvent()).thenReturn(ISSUE_HEARING_ENQUIRY_FORM);
        caseData.setOtherParties(List.of(otherParty()));
        caseData.setDwpDocuments(List.of(
            dwpDocument(OTHER_PARTY_HEARING_PREFERENCES, "dwp-hearing-preferences.pdf", "dwp-hearing-preferences-edited.pdf"),
            dwpDocument("dwpEvidenceBundle", "dwp-evidence-bundle.pdf", null)));
        caseData.setSscsDocument(List.of(
            sscsDocument(OTHER_PARTY_HEARING_PREFERENCES, "sscs-hearing-preferences.pdf", "sscs-hearing-preferences-edited.pdf"),
            sscsDocument("sscsCorrespondence", "sscs-correspondence.pdf", null)));
        caseData.setGenericLetterText("should be removed");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getData().getOtherPartySelection()).hasSize(1);
        final List<DynamicListItem> otherPartyItems = response.getData().getOtherPartySelection().getFirst().getValue()
            .getOtherPartiesList().getListItems();
        assertThat(otherPartyItems).hasSize(1);
        assertThat(otherPartyItems.getFirst().getCode()).contains("op-1");

        final List<DynamicListItem> documentItems = response.getData().getDocumentSelection().getFirst().getValue()
            .getDocumentsList().getListItems();
        assertThat(documentItems).extracting(DynamicListItem::getCode)
            .containsExactlyInAnyOrder("sscs-hearing-preferences.pdf", "sscs-hearing-preferences-edited.pdf",
                "sscs-correspondence.pdf");
    }

    @Test
    void shouldLogAWarningWhenOtherPartiesAreNotSetForHearingEnquiryForm() {
        when(callback.getEvent()).thenReturn(ISSUE_HEARING_ENQUIRY_FORM);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        logCapture.assertLogContains(
            "Other parties not set for hearing enquiry form for case id " + response.getData().getCcdCaseId(),
            Level.WARN);
    }

    private static Stream<Arguments> canHandleScenarios() {
        return Stream.of(Arguments.of(ABOUT_TO_START, ISSUE_HEARING_ENQUIRY_FORM, true),
            Arguments.of(ABOUT_TO_SUBMIT, ISSUE_HEARING_ENQUIRY_FORM, false),
            Arguments.of(MID_EVENT, ISSUE_HEARING_ENQUIRY_FORM, false),
            Arguments.of(SUBMITTED, ISSUE_HEARING_ENQUIRY_FORM, false), Arguments.of(ABOUT_TO_START, APPEAL_RECEIVED, false));
    }

    private CcdValue<OtherParty> otherParty() {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder().id("op-1").name(Name.builder().firstName("John").lastName("Smith").build()).build())
            .build();
    }

    private DwpDocument dwpDocument(String documentType, String filename, String editedFilename) {
        return DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(documentType).documentFileName(filename)
            .editedDocumentLink(editedFilename == null ? null : DocumentLink.builder().documentFilename(editedFilename).build())
            .build()).build();
    }

    private SscsDocument sscsDocument(String documentType, String filename, String editedFilename) {
        return new SscsDocument(SscsDocumentDetails.builder().documentType(documentType).documentFileName(filename)
            .editedDocumentLink(editedFilename == null ? null : DocumentLink.builder().documentFilename(editedFilename).build())
            .build());
    }
}
