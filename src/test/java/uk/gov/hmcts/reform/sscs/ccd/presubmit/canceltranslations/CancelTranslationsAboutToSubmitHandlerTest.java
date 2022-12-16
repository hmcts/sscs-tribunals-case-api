package uk.gov.hmcts.reform.sscs.ccd.presubmit.canceltranslations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CANCEL_TRANSLATIONS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_TCW;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class CancelTranslationsAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private CancelTranslationsAboutToSubmitHandler handler;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Callback<SscsCaseData> callback;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new CancelTranslationsAboutToSubmitHandler();
        when(callback.getEvent()).thenReturn(CANCEL_TRANSLATIONS);
    }

    @Test
    public void givenCanHandleIsCalledWithValidCallBack_shouldReturnTrue() {
        boolean actualResult = handler.canHandle(ABOUT_TO_SUBMIT, callback);
        assertTrue(actualResult);
    }

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE",
        "ABOUT_TO_START,CANCEL_TRANSLATIONS"
    })
    public void givenCanHandleIsCalledWithInvalidCallBack_shouldReturnCorrectFalse(@Nullable CallbackType callbackType, @Nullable EventType eventType) {
        Callback<SscsCaseData> callback = buildCallback(eventType, State.VALID_APPEAL);
        boolean actualResult = handler.canHandle(callbackType, callback);
        assertFalse(actualResult);
    }


    @Test(expected = NullPointerException.class)
    public void givenCanHandleIsCalled_shouldThrowExceptionWhenCallBackTypeNull() {
        boolean actualResult = handler.canHandle(null, callback);
        assertTrue(actualResult);
    }

    @Test(expected = NullPointerException.class)
    public void givenCanHandleIsCalled_shouldThrowExceptionWhenCallBackNull() {
        boolean actualResult = handler.canHandle(ABOUT_TO_SUBMIT, null);
        assertTrue(actualResult);
    }


    @Test
    public void shouldSetTranslationStatusOfDocumentsAndNextWelshEventCorrectly() {
        Callback<SscsCaseData> callback = buildCallback(CANCEL_TRANSLATIONS, State.VALID_APPEAL);
        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus());
        assertNull(response.getData().getSscsDocument().get(1).getValue().getDocumentTranslationStatus());
        assertNull(response.getData().getSscsDocument().get(2).getValue().getDocumentTranslationStatus());
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE,
                response.getData().getSscsDocument().get(3).getValue().getDocumentTranslationStatus());
        assertNull(response.getData().getSscsDocument().get(4).getValue().getDocumentTranslationStatus());
        assertNull(response.getData().getSscsDocument().get(5).getValue().getDocumentTranslationStatus());
        assertNull(response.getData().getSscsDocument().get(6).getValue().getDocumentTranslationStatus());
        assertEquals("No", response.getData().getTranslationWorkOutstanding());

        assertEquals(EventType.DECISION_ISSUED_WELSH.getCcdType(), response.getData().getSscsWelshPreviewNextEvent());
    }

    @Test
    public void shouldSetInterlocReviewStateIfInterlocReview() {
        Callback<SscsCaseData> callback = buildCallback(CANCEL_TRANSLATIONS, State.INTERLOCUTORY_REVIEW_STATE);
        callback.getCaseDetails().getCaseData().setWelshInterlocNextReviewState("reviewByTcw");
        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus());
        assertNull(response.getData().getSscsDocument().get(1).getValue().getDocumentTranslationStatus());
        assertNull(response.getData().getSscsDocument().get(2).getValue().getDocumentTranslationStatus());
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE,
                response.getData().getSscsDocument().get(3).getValue().getDocumentTranslationStatus());
        assertNull(response.getData().getSscsDocument().get(4).getValue().getDocumentTranslationStatus());
        assertNull(response.getData().getSscsDocument().get(5).getValue().getDocumentTranslationStatus());
        assertNull(response.getData().getSscsDocument().get(6).getValue().getDocumentTranslationStatus());
        assertEquals("No", response.getData().getTranslationWorkOutstanding());

        assertEquals(REVIEW_BY_TCW, response.getData().getInterlocReviewState());
    }

    private Callback<SscsCaseData> buildCallback(EventType eventType, State state) {

        SscsDocument ssc0Doc =
                buildSscsDocument("english.pdf", SscsDocumentTranslationStatus.TRANSLATION_REQUIRED,
                        DocumentType.APPELLANT_EVIDENCE.getValue(), LocalDate.now());

        SscsDocument sscs1Doc =
                buildSscsDocument("english.pdf", SscsDocumentTranslationStatus.TRANSLATION_REQUIRED,
                        DocumentType.SSCS1.getValue(), LocalDate.now().minusDays(1));

        SscsDocument sscs2Doc =
                buildSscsDocument("anything.pdf", SscsDocumentTranslationStatus.TRANSLATION_REQUIRED,
                        DocumentType.DECISION_NOTICE
                                .getValue(), LocalDate.now().minusDays(3));

        SscsDocument sscs3Doc =
                buildSscsDocument("anything.pdf", SscsDocumentTranslationStatus.TRANSLATION_COMPLETE,
                        DocumentType.DIRECTION_NOTICE
                                .getValue(), LocalDate.now().minusDays(2));

        SscsDocument sscs4Doc =
                buildSscsDocument("anything.pdf", null, DocumentType.DECISION_NOTICE
                        .getValue(), LocalDate.now().minusDays(2));

        SscsDocument sscs5Doc =
                buildSscsDocument("anything.pdf", SscsDocumentTranslationStatus.TRANSLATION_REQUESTED,
                        DocumentType.DIRECTION_NOTICE
                                .getValue(), LocalDate.now().minusDays(2));

        SscsDocument sscs6Doc =
                buildSscsDocument("anything.pdf", SscsDocumentTranslationStatus.TRANSLATION_REQUIRED,
                        DocumentType.REINSTATEMENT_REQUEST
                                .getValue(), LocalDate.now().minusDays(2));

        List<SscsDocument> docs = Arrays.asList(ssc0Doc, sscs1Doc, sscs2Doc, sscs3Doc, sscs4Doc, sscs5Doc, sscs6Doc);

        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .sscsDocument(docs)
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
