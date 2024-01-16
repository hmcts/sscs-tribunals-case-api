package uk.gov.hmcts.reform.sscs.ccd.presubmit.markdocsfortranslation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.MARK_DOCS_FOR_TRANSATION;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

@RunWith(JUnitParamsRunner.class)
public class MarkDocumentsForTranslationAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private MarkDocumentsForTranslationAboutToSubmitHandler handler;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Callback<SscsCaseData> callback;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new MarkDocumentsForTranslationAboutToSubmitHandler();
        when(callback.getEvent()).thenReturn(MARK_DOCS_FOR_TRANSATION);
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
        Callback<SscsCaseData> callback = buildCallback(eventType);
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
    public void shouldSetTranslationStatusOfDocumentsCorrectly() {
        Callback<SscsCaseData> callback = buildCallback(MARK_DOCS_FOR_TRANSATION);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED,
                response.getData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus());
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED,
                response.getData().getSscsDocument().get(1).getValue().getDocumentTranslationStatus());
        assertEquals("Yes",
                response.getData().getTranslationWorkOutstanding());
    }

    private Callback<SscsCaseData> buildCallback(EventType eventType) {

        SscsDocument ssc0Doc =
                buildSscsDocument("english.pdf",
                        DocumentType.APPELLANT_EVIDENCE.getValue(), LocalDate.now());

        SscsDocument sscs1Doc =
                buildSscsDocument("worldmap.pdf",
                        DocumentType.SSCS1.getValue(), LocalDate.now().minusDays(3));


        List<SscsDocument> docs = Arrays.asList(ssc0Doc, sscs1Doc);

        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .sscsDocument(docs)
                .languagePreferenceWelsh("Yes")
                .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
                State.VALID_APPEAL, sscsCaseData, LocalDateTime.now(), "Benefit");
        return new Callback<>(caseDetails, Optional.empty(), eventType, false);
    }

    private SscsDocument buildSscsDocument(String document, String docType,
                                           LocalDate dateAdded) {
        return SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentLink(DocumentLink.builder()
                                .documentUrl("/anotherUrl")
                                .documentFilename(document)
                                .build())
                        .documentType(docType)
                        .documentDateAdded(dateAdded.toString())
                        .build())
                .build();
    }

}
