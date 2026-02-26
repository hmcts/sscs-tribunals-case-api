package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuehearingenquiryform;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_GENERIC_LETTER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_HEARING_ENQUIRY_FORM;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentSelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IssueHearingEnquiryFormMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String DUPLICATE_OTHER_PARTY_ERROR = "Other parties cannot be selected more than once";
    private static final String DUPLICATE_DOCUMENT_ERROR = "The same document cannot be selected more than once";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private IssueHearingEnquiryFormMidEventHandler handler;
    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        handler = new IssueHearingEnquiryFormMidEventHandler(true);
        caseData = SscsCaseData.builder().build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
    }

    @ParameterizedTest
    @MethodSource("canHandleScenarios")
    void shouldReturnExpectedValueFromCanHandle(CallbackType callbackType, EventType eventType, boolean expected) {
        lenient().when(callback.getEvent()).thenReturn(eventType);

        assertThat(handler.canHandle(callbackType, callback)).isEqualTo(expected);
    }

    @Test
    void shouldThrowExceptionWhenCanHandleCalledWithNullCallback() {
        assertThatThrownBy(() -> handler.canHandle(MID_EVENT, null)).isInstanceOf(NullPointerException.class)
            .hasMessage("callback must not be null");
    }

    @Test
    void shouldThrowExceptionWhenCanHandleCalledWithNullCallbackType() {
        assertThatThrownBy(() -> handler.canHandle(null, callback)).isInstanceOf(NullPointerException.class)
            .hasMessage("callbacktype must not be null");
    }

    @Test
    void shouldNotHandleWhenCmOtherPartyConfidentialityIsDisabled() {
        final IssueHearingEnquiryFormMidEventHandler disabledHandler = new IssueHearingEnquiryFormMidEventHandler(false);

        assertThat(disabledHandler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @Test
    void shouldThrowExceptionWhenHandleCalledForUnsupportedCallback() {
        lenient().when(callback.getEvent()).thenReturn(ISSUE_GENERIC_LETTER);

        assertThatThrownBy(() -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION)).isInstanceOf(
            IllegalStateException.class).hasMessage("Cannot handle callback");
    }

    @Test
    void shouldReturnResponseWithoutErrorsWhenNoDuplicateSelectionsPresent() {
        lenient().when(callback.getEvent()).thenReturn(ISSUE_HEARING_ENQUIRY_FORM);
        caseData.setOtherPartySelection(List.of(otherPartySelection("other-party-1"), otherPartySelection("other-party-2")));
        caseData.setDocumentSelection(List.of(documentSelection("doc-1"), documentSelection("doc-2")));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getData()).isSameAs(caseData);
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void shouldAddErrorWhenDuplicateOtherPartiesAreSelected() {
        lenient().when(callback.getEvent()).thenReturn(ISSUE_HEARING_ENQUIRY_FORM);
        caseData.setOtherPartySelection(
            List.of(otherPartySelection("same-other-party"), otherPartySelection("same-other-party")));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).containsExactly(DUPLICATE_OTHER_PARTY_ERROR);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldNotAddOtherPartyDuplicateErrorWhenOtherPartySelectionIsEmptyOrNull(
        List<CcdValue<OtherPartySelectionDetails>> otherPartySelection) {
        lenient().when(callback.getEvent()).thenReturn(ISSUE_HEARING_ENQUIRY_FORM);
        caseData.setOtherPartySelection(otherPartySelection);
        caseData.setOtherPartySelection(emptyList());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void shouldAddErrorWhenDuplicateDocumentsAreSelected() {
        lenient().when(callback.getEvent()).thenReturn(ISSUE_HEARING_ENQUIRY_FORM);
        caseData.setDocumentSelection(List.of(documentSelection("same-doc"), documentSelection("same-doc")));
        caseData.setOtherPartySelection(emptyList());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isNotEmpty().containsExactly(DUPLICATE_DOCUMENT_ERROR);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldNotAddDocumentDuplicateErrorWhenDocumentSelectionIsEmptyOrNull(
        List<CcdValue<DocumentSelectionDetails>> documentSelection) {
        lenient().when(callback.getEvent()).thenReturn(ISSUE_HEARING_ENQUIRY_FORM);
        caseData.setDocumentSelection(documentSelection);
        caseData.setOtherPartySelection(emptyList());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void shouldAddBothErrorsWhenBothOtherPartiesAndDocumentsContainDuplicates() {
        lenient().when(callback.getEvent()).thenReturn(ISSUE_HEARING_ENQUIRY_FORM);
        caseData.setOtherPartySelection(
            List.of(otherPartySelection("same-other-party"), otherPartySelection("same-other-party")));
        caseData.setDocumentSelection(List.of(documentSelection("same-doc"), documentSelection("same-doc")));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).containsExactlyInAnyOrder(DUPLICATE_OTHER_PARTY_ERROR, DUPLICATE_DOCUMENT_ERROR);
    }

    private static Stream<Arguments> canHandleScenarios() {
        return Stream.of(Arguments.of(MID_EVENT, ISSUE_HEARING_ENQUIRY_FORM, true),
            Arguments.of(ABOUT_TO_START, ISSUE_HEARING_ENQUIRY_FORM, false),
            Arguments.of(SUBMITTED, ISSUE_HEARING_ENQUIRY_FORM, false), Arguments.of(MID_EVENT, ISSUE_GENERIC_LETTER, false));
    }

    private CcdValue<OtherPartySelectionDetails> otherPartySelection(String code) {
        DynamicListItem selectedItem = new DynamicListItem(code, code);
        return new CcdValue<>(new OtherPartySelectionDetails(new DynamicList(selectedItem, List.of(selectedItem))));
    }

    private CcdValue<DocumentSelectionDetails> documentSelection(String code) {
        DynamicListItem selectedItem = new DynamicListItem(code, code);
        return new CcdValue<>(new DocumentSelectionDetails(new DynamicList(selectedItem, List.of(selectedItem))));
    }

}