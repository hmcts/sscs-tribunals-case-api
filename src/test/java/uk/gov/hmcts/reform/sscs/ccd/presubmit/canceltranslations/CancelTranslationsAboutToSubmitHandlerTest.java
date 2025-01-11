package uk.gov.hmcts.reform.sscs.ccd.presubmit.canceltranslations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CANCEL_TRANSLATIONS;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
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
    public void givenCanHandleIsCalledWithInvalidCallBack_shouldReturnCorrectFalse(CallbackType callbackType,
                                                                                   EventType eventType) {
        Callback<SscsCaseData> callback = buildCallback(eventType, State.VALID_APPEAL,
            SscsDocumentTranslationStatus.TRANSLATION_REQUIRED, DocumentType.SSCS1, 1);
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
    public void givenHandleIsCalledWithInvalidCallBack_shouldThrowIllegalStateException(CallbackType callbackType,
                                                                                        EventType eventType) {
        Callback<SscsCaseData> callback = buildCallback(eventType, State.VALID_APPEAL,
            SscsDocumentTranslationStatus.TRANSLATION_REQUIRED, DocumentType.SSCS1, 1);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> handler.handle(callbackType, callback, USER_AUTHORISATION));
        assertEquals("Cannot handle callback", exception.getMessage());
    }

    @Test
    @Parameters({
        "null,TRANSLATION_REQUIRED,APPELLANT_EVIDENCE,0",
        "SEND_TO_DWP,TRANSLATION_REQUIRED,SSCS1,1",
        "DECISION_ISSUED_WELSH,TRANSLATION_REQUIRED,DECISION_NOTICE,3",
        "null,TRANSLATION_COMPLETE,DIRECTION_NOTICE,2",
        "null,null,DECISION_NOTICE,2",
        "DIRECTION_ISSUED_WELSH,TRANSLATION_REQUESTED,DIRECTION_NOTICE,2",
        "UPDATE_CASE_ONLY,TRANSLATION_REQUIRED,REINSTATEMENT_REQUEST,2",
        "SEND_TO_DWP,TRANSLATION_REQUIRED,SSCS8,4",
        "UPDATE_CASE_ONLY,TRANSLATION_REQUESTED,URGENT_HEARING_REQUEST,1"
    })
    public void shouldSetTranslationStatusOfDocumentsAndNextWelshEventCorrectly(@Nullable EventType eventType,
                                                                                @Nullable SscsDocumentTranslationStatus translationStatus,
                                                                                DocumentType documentType,
                                                                                int daysToSubtract) {
        Callback<SscsCaseData> callback = buildCallback(CANCEL_TRANSLATIONS, State.VALID_APPEAL,
            translationStatus, documentType, daysToSubtract);
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        if (SscsDocumentTranslationStatus.TRANSLATION_COMPLETE.equals(translationStatus)) {
            assertEquals(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE,
                response.getData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus());
        } else {
            assertNull(response.getData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus());
        }
        assertEquals("No", response.getData().getTranslationWorkOutstanding());
        assertEquals(eventType != null ? eventType.getCcdType() : null, response.getData().getSscsWelshPreviewNextEvent());
    }

    @Test
    @Parameters({
        "TRANSLATION_REQUIRED,APPELLANT_EVIDENCE,0",
        "TRANSLATION_REQUIRED,SSCS1,1",
        "TRANSLATION_REQUIRED,DECISION_NOTICE,3",
        "TRANSLATION_COMPLETE,DIRECTION_NOTICE,2",
        "null,DECISION_NOTICE,2",
        "TRANSLATION_REQUESTED,DIRECTION_NOTICE,2",
        "TRANSLATION_REQUIRED,REINSTATEMENT_REQUEST,2",
        "TRANSLATION_REQUIRED,SSCS8,4",
        "TRANSLATION_REQUESTED,URGENT_HEARING_REQUEST,1"
    })
    public void shouldSetInterlocReviewStateIfInterlocReview(@Nullable SscsDocumentTranslationStatus translationStatus,
                                                             DocumentType documentType,
                                                             int daysToSubtract) {
        Callback<SscsCaseData> callback = buildCallback(CANCEL_TRANSLATIONS, State.INTERLOCUTORY_REVIEW_STATE,
            translationStatus, documentType, daysToSubtract);
        callback.getCaseDetails().getCaseData().setWelshInterlocNextReviewState("reviewByTcw");
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        if (SscsDocumentTranslationStatus.TRANSLATION_COMPLETE.equals(translationStatus)) {
            assertEquals(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE,
                response.getData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus());
        } else {
            assertNull(response.getData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus());
        }
        assertEquals("No", response.getData().getTranslationWorkOutstanding());

        assertEquals(InterlocReviewState.REVIEW_BY_TCW, response.getData().getInterlocReviewState());
    }

    private Callback<SscsCaseData> buildCallback(EventType eventType, State state,
                                                 SscsDocumentTranslationStatus translationStatus,
                                                 DocumentType documentType,
                                                 int daysToSubtract) {
        SscsDocument sscsDocument = buildSscsDocument(translationStatus, documentType.getValue(),
            LocalDate.now().minusDays(daysToSubtract));
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .sscsDocument(Collections.singletonList(sscsDocument))
            .state(State.VALID_APPEAL)
            .languagePreferenceWelsh("Yes")
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            state, sscsCaseData, LocalDateTime.now(), "Benefit");
        return new Callback<>(caseDetails, Optional.empty(), eventType, false);
    }

    private SscsDocument buildSscsDocument(SscsDocumentTranslationStatus translationRequired, String docType,
                                           LocalDate dateAdded) {
        return SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("/anotherUrl")
                    .documentFilename("document.pdf")
                    .build())
                .documentTranslationStatus(translationRequired)
                .documentType(docType)
                .documentDateAdded(dateAdded.toString())
                .build())
            .build();
    }
}
