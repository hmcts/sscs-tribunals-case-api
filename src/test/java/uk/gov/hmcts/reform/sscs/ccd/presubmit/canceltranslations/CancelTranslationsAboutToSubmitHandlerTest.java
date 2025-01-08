package uk.gov.hmcts.reform.sscs.ccd.presubmit.canceltranslations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CANCEL_TRANSLATIONS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_TCW;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

@RunWith(JUnitParamsRunner.class)
public class CancelTranslationsAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private final CancelTranslationsAboutToSubmitHandler handler = new CancelTranslationsAboutToSubmitHandler();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Callback<SscsCaseData> callback;

    @Test
    public void givenCanHandleIsCalledWithValidCallBack_shouldReturnTrue() {
        when(callback.getEvent()).thenReturn(CANCEL_TRANSLATIONS);
        boolean actualResult = handler.canHandle(ABOUT_TO_SUBMIT, callback);
        assertTrue(actualResult);
    }

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE",
        "ABOUT_TO_START,CANCEL_TRANSLATIONS"
    })
    public void givenCanHandleIsCalledWithInvalidCallBack_shouldReturnCorrectFalse(@Nullable CallbackType callbackType, @Nullable EventType eventType) {
        Callback<SscsCaseData> callback = buildCallback(eventType, State.VALID_APPEAL, 1);
        boolean actualResult = handler.canHandle(callbackType, callback);
        assertFalse(actualResult);
    }

    @Test
    public void givenCanHandleIsCalled_shouldThrowExceptionWhenCallBackTypeNull() {
        when(callback.getEvent()).thenReturn(CANCEL_TRANSLATIONS);
        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> handler.canHandle(null, callback));
        assertEquals("callbackType must not be null", exception.getMessage());
    }

    @Test
    public void givenCanHandleIsCalled_shouldThrowExceptionWhenCallBackNull() {
        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> handler.canHandle(ABOUT_TO_SUBMIT, null));
        assertEquals("callback must not be null", exception.getMessage());
    }

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE",
        "ABOUT_TO_START,CANCEL_TRANSLATIONS"
    })
    public void givenHandleIsCalledWithInvalidCallBack_shouldThrowIllegalStateException(CallbackType callbackType, EventType eventType) {
        Callback<SscsCaseData> callback = buildCallback(eventType, State.VALID_APPEAL, 1);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> handler.handle(callbackType, callback, USER_AUTHORISATION));
        assertEquals("Cannot handle callback", exception.getMessage());
    }

    @Test
    @Parameters({
        "0,null",
        "1,SEND_TO_DWP",
        "2,DECISION_ISSUED_WELSH",
        "3,null",
        "4,null",
        "5,DIRECTION_ISSUED_WELSH",
        "6,UPDATE_CASE_ONLY",
        "8,SEND_TO_DWP"
    })
    public void shouldSetTranslationStatusOfDocumentsAndNextWelshEventCorrectly(int sscsDocumentNumber, @Nullable EventType eventType) {
        Callback<SscsCaseData> callback = buildCallback(CANCEL_TRANSLATIONS, State.VALID_APPEAL, sscsDocumentNumber);
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        if (sscsDocumentNumber == 3) {
            assertEquals(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE,
                response.getData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus());
        } else {
            assertNull(response.getData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus());
        }
        assertEquals("No", response.getData().getTranslationWorkOutstanding());
        assertEquals(eventType != null ? eventType.getCcdType() : null, response.getData().getSscsWelshPreviewNextEvent());
    }

    @Test
    @Parameters({"0", "1", "2", "3", "4", "5", "6", "8"})
    public void shouldSetInterlocReviewStateIfInterlocReview(int sscsDocumentNumber) {
        Callback<SscsCaseData> callback = buildCallback(CANCEL_TRANSLATIONS, State.INTERLOCUTORY_REVIEW_STATE, sscsDocumentNumber);
        callback.getCaseDetails().getCaseData().setWelshInterlocNextReviewState("reviewByTcw");
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        if (sscsDocumentNumber == 3) {
            assertEquals(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE,
                response.getData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus());
        } else {
            assertNull(response.getData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus());
        }
        assertEquals("No", response.getData().getTranslationWorkOutstanding());

        assertEquals(REVIEW_BY_TCW, response.getData().getInterlocReviewState());
    }

    private Callback<SscsCaseData> buildCallback(EventType eventType, State state, int sscsDocumentNumber) {
        SscsDocument sscsDocument = switch (sscsDocumentNumber) {
            case 0 -> buildSscsDocument("english.pdf", SscsDocumentTranslationStatus.TRANSLATION_REQUIRED,
                DocumentType.APPELLANT_EVIDENCE.getValue(), LocalDate.now());
            case 1 -> buildSscsDocument("english.pdf", SscsDocumentTranslationStatus.TRANSLATION_REQUIRED,
                DocumentType.SSCS1.getValue(), LocalDate.now().minusDays(1));
            case 2 -> buildSscsDocument("anything.pdf", SscsDocumentTranslationStatus.TRANSLATION_REQUIRED,
                DocumentType.DECISION_NOTICE
                    .getValue(), LocalDate.now().minusDays(3));
            case 3 -> buildSscsDocument("anything.pdf", SscsDocumentTranslationStatus.TRANSLATION_COMPLETE,
                DocumentType.DIRECTION_NOTICE
                    .getValue(), LocalDate.now().minusDays(2));
            case 4 -> buildSscsDocument("anything.pdf", null, DocumentType.DECISION_NOTICE
                .getValue(), LocalDate.now().minusDays(2));
            case 5 -> buildSscsDocument("anything.pdf", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED,
                DocumentType.DIRECTION_NOTICE
                    .getValue(), LocalDate.now().minusDays(2));
            case 6 -> buildSscsDocument("anything.pdf", SscsDocumentTranslationStatus.TRANSLATION_REQUIRED,
                DocumentType.REINSTATEMENT_REQUEST
                    .getValue(), LocalDate.now().minusDays(2));
            case 8 -> buildSscsDocument("ibc.pdf", SscsDocumentTranslationStatus.TRANSLATION_REQUIRED,
                DocumentType.SSCS8.getValue(), LocalDate.now().minusDays(4));
            default -> null;
        };

        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .sscsDocument(Collections.singletonList(sscsDocument))
            .state(State.VALID_APPEAL)
            .languagePreferenceWelsh("Yes")
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            state, sscsCaseData, LocalDateTime.now(), "Benefit");
        return new Callback<>(caseDetails, Optional.empty(), eventType, false);
    }

    private SscsDocument buildSscsDocument(String s, SscsDocumentTranslationStatus translationRequired, String docType,
                                           LocalDate dateAdded) {
        return SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("/anotherUrl")
                    .documentFilename(s)
                    .build())
                .documentTranslationStatus(translationRequired)
                .documentType(docType)
                .documentDateAdded(dateAdded.toString())
                .build())
            .build();
    }
}
