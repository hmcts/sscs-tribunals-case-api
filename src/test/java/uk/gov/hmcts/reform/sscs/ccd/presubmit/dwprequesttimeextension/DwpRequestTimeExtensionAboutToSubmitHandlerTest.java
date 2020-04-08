package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwprequesttimeextension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

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

        assertNotNull(actualCallback.getData().getTl1Form());
        DwpResponseDocument tl1FormDoc = actualCallback.getData().getTl1Form();
        assertEquals(expectedDocumentLink, tl1FormDoc.getDocumentLink());
        assertEquals(expectedDocumentLink.getDocumentFilename(), tl1FormDoc.getDocumentLink().getDocumentFilename());
        assertEquals("extensionRequested", actualCallback.getData().getDwpState());
        assertEquals("timeExtension", actualCallback.getData().getInterlocReferralReason());
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
        callback = new Callback<>(caseDetails, Optional.empty(), EventType.DWP_REQUEST_TIME_EXTENSION, false);
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