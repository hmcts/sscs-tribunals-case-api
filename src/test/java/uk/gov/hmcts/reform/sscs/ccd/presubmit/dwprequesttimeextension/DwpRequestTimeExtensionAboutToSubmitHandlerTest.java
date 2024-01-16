package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwprequesttimeextension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.EXTENSION_REQUESTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason.TIME_EXTENSION;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;

@RunWith(JUnitParamsRunner.class)
public class DwpRequestTimeExtensionAboutToSubmitHandlerTest {

    @Spy
    private Callback<SscsCaseData> callback;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
    private DwpRequestTimeExtensionAboutToSubmitHandler handler;
    private final DocumentLink expectedDocumentLink = DocumentLink.builder()
        .documentBinaryUrl("/BinaryUrl")
        .documentUrl("/url")
        .documentFilename("Tl1Form.pdf")
        .build();

    private DwpDocumentService dwpDocumentService;

    @Before
    public void setUp() {
        dwpDocumentService = new DwpDocumentService();
        handler = new DwpRequestTimeExtensionAboutToSubmitHandler(dwpDocumentService);
    }


    @Test
    @Parameters({
        "APPEAL_RECEIVED, ABOUT_TO_SUBMIT, false, true",
        "DWP_REQUEST_TIME_EXTENSION, ABOUT_TO_SUBMIT, true, true",
        "DWP_REQUEST_TIME_EXTENSION, ABOUT_TO_SUBMIT, false, false",
        "DWP_REQUEST_TIME_EXTENSION, ABOUT_TO_START, false, true",
        "DWP_REQUEST_TIME_EXTENSION, null, false, true"
    })
    public void canHandle(@Nullable EventType eventType, @Nullable CallbackType callbackType, boolean expected,
                          boolean tl1Set) {

        SscsDocument sscs1Doc = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentLink(DocumentLink.builder()
                                .documentUrl("/anotherUrl")
                                .build())
                        .documentType("sscs1")
                        .build())
                .build();

        List<SscsDocument> docs = new ArrayList<SscsDocument>();
        docs.add(sscs1Doc);

        createTestData(docs, eventType, tl1Set);

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
        createTestData(sscsDocuments, EventType.DWP_REQUEST_TIME_EXTENSION, true);

        PreSubmitCallbackResponse<SscsCaseData> actualCallback = handler.handle(CallbackType.ABOUT_TO_SUBMIT,
            callback, "user token");


        List<DwpDocument> dwpDocs = actualCallback.getData().getDwpDocuments();
        assertEquals(1, dwpDocs.size());

        DwpDocumentDetails dwpDoc = dwpDocs.get(0).getValue();
        assertNull(actualCallback.getData().getTl1Form());
        assertEquals(DwpDocumentType.TL1_FORM.getValue(), dwpDoc.getDocumentType());
        assertEquals(expectedDocumentLink, dwpDoc.getDocumentLink());
        assertEquals(expectedDocumentLink.getDocumentFilename(), dwpDoc.getDocumentLink().getDocumentFilename());

        assertNull(actualCallback.getData().getTl1Form());

        assertEquals(EXTENSION_REQUESTED, actualCallback.getData().getDwpState());
        assertEquals(TIME_EXTENSION, actualCallback.getData().getInterlocReferralReason());
    }

    private void createTestData(List<SscsDocument> sscsDocuments, EventType eventType, boolean tl1Form) {
        SscsCaseData caseData = SscsCaseData.builder()
            .sscsDocument(sscsDocuments)
            .dwpState(null)
            .build();

        if (tl1Form) {
            caseData.setTl1Form(DwpResponseDocument.builder()
                    .documentLink(expectedDocumentLink)
                    .build());
        }

        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(1L, "sscs", State.WITH_DWP, caseData,
            LocalDateTime.now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.empty(), eventType, false);
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
