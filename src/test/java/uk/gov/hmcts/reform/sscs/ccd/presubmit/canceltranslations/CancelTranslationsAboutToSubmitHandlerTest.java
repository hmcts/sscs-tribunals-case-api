package uk.gov.hmcts.reform.sscs.ccd.presubmit.canceltranslations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CANCEL_TRANSLATIONS;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
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
    public void givenCanHandleIsCalled_shouldReturnCorrectResult() {
        boolean actualResult = handler.canHandle(ABOUT_TO_SUBMIT, callback);
        assertTrue(actualResult);
    }

    @Test
    public void shouldSetTranslationStatusOfDocumentsAndNextWelshEventCorrectly() {
        Callback<SscsCaseData> callback = buildCallback();
        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus());
        assertNull(response.getData().getSscsDocument().get(1).getValue().getDocumentTranslationStatus());
        assertNull(response.getData().getSscsDocument().get(2).getValue().getDocumentTranslationStatus());
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE,
            response.getData().getSscsDocument().get(3).getValue().getDocumentTranslationStatus());
        assertNull(response.getData().getSscsDocument().get(4).getValue().getDocumentTranslationStatus());
        assertNull(response.getData().getSscsDocument().get(5).getValue().getDocumentTranslationStatus());
        assertEquals("No", response.getData().getTranslationWorkOutstanding());

        assertEquals(EventType.DECISION_ISSUED_WELSH.getCcdType(), response.getData().getSscsWelshPreviewNextEvent());
    }

    private Callback<SscsCaseData> buildCallback() {

        SscsDocument ssc0Doc =
            buildSscsDocument("english.pdf", SscsDocumentTranslationStatus.TRANSLATION_REQUIRED,
                DocumentType.APPELLANT_EVIDENCE.getValue(), LocalDate.now());

        SscsDocument sscs1Doc =
            buildSscsDocument("english.pdf", SscsDocumentTranslationStatus.TRANSLATION_REQUIRED,
                DocumentType.SSCS1.getValue(), LocalDate.now().minusDays(3));

        SscsDocument sscs2Doc =
            buildSscsDocument("anything.pdf", SscsDocumentTranslationStatus.TRANSLATION_REQUIRED,
                DocumentType.DECISION_NOTICE
                    .getValue(), LocalDate.now().minusDays(1));

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

        List<SscsDocument> docs = Arrays.asList(ssc0Doc, sscs1Doc, sscs2Doc, sscs3Doc, sscs4Doc, sscs5Doc);

        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .sscsDocument(docs)
            .languagePreferenceWelsh("Yes")
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            State.VALID_APPEAL, sscsCaseData, LocalDateTime.now());
        return new Callback<>(caseDetails, Optional.empty(), CANCEL_TRANSLATIONS, false);
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