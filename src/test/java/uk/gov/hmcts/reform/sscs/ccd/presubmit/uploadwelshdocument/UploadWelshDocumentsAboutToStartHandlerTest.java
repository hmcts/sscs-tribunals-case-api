package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadwelshdocument;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import java.util.Arrays;
import java.util.List;
import junitparams.converters.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;

public class UploadWelshDocumentsAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private UploadWelshDocumentsAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new UploadWelshDocumentsAboutToStartHandler();
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_WELSH_DOCUMENT);
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @ParameterizedTest
    public void givenAUploadWelshDocumentEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @ParameterizedTest
    public void givenANonUploadWelshDocument_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @ParameterizedTest
    public void givenNullCallback_shouldThrowException() {
        assertThrows(NullPointerException.class, () ->
            handler.handle(CallbackType.ABOUT_TO_SUBMIT, null, "user token"));
    }

    @ParameterizedTest
    public void givenNoDocumentWithSscsDocumentTranslationStatus_thenDisplayError() {
        sscsCaseData = CaseDataUtils.buildMinimalCaseData();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Event cannot be triggered - no documents awaiting translation on this case", error);
    }

    @ParameterizedTest
    @MethodSource("generateSscsDocuments")
    public void originalDocumentDropDownWhenSscsDocumentTranslationStatusIsSet(@Nullable List<SscsDocument> sscsDocuments) {
        sscsCaseData = SscsCaseData.builder()
                .sscsDocument(sscsDocuments)
                .appeal(Appeal.builder().build())
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(1, response.getData().getOriginalDocuments().getListItems().size());
        assertEquals("test.pdf", response.getData().getOriginalDocuments().getValue().getCode());
        assertEquals("test.pdf", response.getData().getOriginalDocuments().getListItems().get(0).getCode());
    }

    public static Object[] generateSscsDocuments() {
        SscsDocument sscs1Doc = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("/anotherUrl")
                    .documentFilename("test.pdf")
                    .build())
                .documentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED)
                .documentType(DocumentType.SSCS1.getValue())
                .build())
            .build();

        SscsDocument sscs2Doc = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("/anotherUrl")
                    .documentFilename("directionNotice.pdf")
                    .build())
                .documentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED)
                .documentType(DocumentType.DIRECTION_NOTICE.getValue())
                .build())
            .build();

        List<SscsDocument> docs = Arrays.asList(sscs1Doc, sscs2Doc);

        return new Object[]{
            new Object[]{docs}
        };
    }

}