package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwprequesttimeextension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpResponseDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

@RunWith(JUnitParamsRunner.class)
public class DwpRequestTimeExtensionAboutToSubmitHandlerTest {

    @Spy
    private Callback<SscsCaseData> callback;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
    private final DwpRequestTimeExtensionAboutToSubmitHandler handler = new DwpRequestTimeExtensionAboutToSubmitHandler();
    private final DocumentLink expectedDocumentLink = DocumentLink.builder()
        .documentBinaryUrl("/BinaryUrl")
        .documentUrl("/url")
        .documentFilename("Tl1Form.pdf")
        .build();

    @Test
    @Parameters({
        "APPEAL_RECEIVED, ABOUT_TO_SUBMIT, false, true",
        "DWP_REQUEST_TIME_EXTENSION, ABOUT_TO_SUBMIT, true, true",
        "DWP_REQUEST_TIME_EXTENSION, ABOUT_TO_START, false, false",
        "DWP_REQUEST_TIME_EXTENSION, null, false, false",
        "null, ABOUT_TO_SUBMIT, false, true",
    })
    public void canHandle(@Nullable EventType eventType, @Nullable CallbackType callbackType, boolean expected,
                          boolean mockNeeded) {
        if (mockNeeded) {
            when(callback.getEvent()).thenReturn(eventType);
        }

        boolean actualResult = handler.canHandle(callbackType, callback);

        assertEquals(expected, actualResult);
    }

    @Test(expected = NullPointerException.class)
    public void givenNullCallback_canHandleThrowException() {
        handler.canHandle(CallbackType.ABOUT_TO_SUBMIT,
            null);
    }

    @Test
    @Parameters(method = "generateSscsCaseDataWithDifferentSscsDocumentLength")
    public void handle(@Nullable List<SscsDocument> sscsDocuments) {
        createTestData(sscsDocuments);

        PreSubmitCallbackResponse<SscsCaseData> actualCallback = handler.handle(CallbackType.ABOUT_TO_SUBMIT,
            callback, "user token");

        assertTl1FromToSscsDocument(actualCallback);
        assertEquals("extensionRequested",actualCallback.getData().getDwpState());

    }

    private void assertTl1FromToSscsDocument(PreSubmitCallbackResponse<SscsCaseData> actualCallback) {
        assertNull(actualCallback.getData().getTl1Form());
        assertNumberOfTl1FormDocsIsOne(actualCallback);
        SscsDocumentDetails tl1FormDoc = getTl1FormFromTheSscsDocuments(actualCallback).getValue();
        assertEquals(expectedDocumentLink, tl1FormDoc.getDocumentLink());
        assertEquals(LocalDate.now().toString(), tl1FormDoc.getDocumentDateAdded());
        assertEquals(expectedDocumentLink.getDocumentFilename(), tl1FormDoc.getDocumentFileName());
    }

    private SscsDocument getTl1FormFromTheSscsDocuments(PreSubmitCallbackResponse<SscsCaseData> actualCallback) {
        return actualCallback.getData().getSscsDocument().stream()
            .filter(doc -> "tl1Form".equals(doc.getValue().getDocumentType()))
            .findFirst().orElse(null);
    }

    private void assertNumberOfTl1FormDocsIsOne(PreSubmitCallbackResponse<SscsCaseData> actualCallback) {
        int tl1FormNumber = (int) actualCallback.getData().getSscsDocument().stream()
            .filter(doc -> "tl1Form".equals(doc.getValue().getDocumentType()))
            .count();
        assertEquals(1, tl1FormNumber);
    }

    private void createTestData(List<SscsDocument> sscsDocuments) {
        SscsCaseData caseData = SscsCaseData.builder()
            .tl1Form(DwpResponseDocument.builder()
                .documentLink(expectedDocumentLink)
                .build())
            .sscsDocument(sscsDocuments)
            .dwpState(null)
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(1L, "sscs", State.WITH_DWP, caseData,
            LocalDateTime.now());
        callback = new Callback<>(caseDetails, Optional.empty(), EventType.DWP_REQUEST_TIME_EXTENSION);
    }

    public Object[] generateSscsCaseDataWithDifferentSscsDocumentLength() {
        SscsDocument sscs1Doc = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("/anotherUrl")
                    .build())
                .documentType("sscs1")
                .build())
            .build();

        SscsDocument appellantEvidenceDoc = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("/anotherUrl")
                    .build())
                .documentType("appellantEvidence")
                .build())
            .build();

        List<SscsDocument> oneDoc = new ArrayList<>();
        oneDoc.add(sscs1Doc);

        List<SscsDocument> twoDocs = new ArrayList<>();
        twoDocs.add(sscs1Doc);
        twoDocs.add(appellantEvidenceDoc);

        return new Object[]{
            new Object[]{null},
            new Object[]{oneDoc},
            new Object[]{twoDocs}
        };
    }

    @Test(expected = IllegalStateException.class)
    public void givenUnHandleCallbackType_shouldThrowException() {
        handler.handle(CallbackType.ABOUT_TO_START, callback, "user token");
    }

    @Test(expected = NullPointerException.class)
    public void givenNullCallback_shouldThrowException() {
        handler.handle(CallbackType.ABOUT_TO_SUBMIT, null, "user token");
    }
}