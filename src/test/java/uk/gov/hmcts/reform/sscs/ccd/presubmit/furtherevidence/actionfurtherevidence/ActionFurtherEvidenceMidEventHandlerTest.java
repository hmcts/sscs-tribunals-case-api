package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@RunWith(JUnitParamsRunner.class)
public class ActionFurtherEvidenceMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private final List<ScannedDocument> scannedDocumentList = new ArrayList<>();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    private ActionFurtherEvidenceMidEventHandler handler;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private FooterService footerService;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        handler = new ActionFurtherEvidenceMidEventHandler(footerService);

        when(callback.getEvent()).thenReturn(EventType.ACTION_FURTHER_EVIDENCE);
        when(footerService.isReadablePdf(any())).thenReturn(PdfState.OK);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                .fileName("bla.pdf")
                .type("type")
                .subtype("sscs1")
                .url(DocumentLink.builder().documentUrl("www.test.com").build())
                .scannedDate("2019-06-13T00:00:00.000")
                .controlNumber("123")
                .build()).build();

        scannedDocumentList.add(scannedDocument);
        DynamicList furtherEvidenceActionList = buildFurtherEvidenceActionItemListForGivenOption("otherDocumentManual",
            "Other document type - action manually");

        DynamicListItem value = new DynamicListItem("appellant", "Appellant (or Appointee)");
        DynamicList originalSender = new DynamicList(value, Collections.singletonList(value));

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1234")
            .scannedDocuments(scannedDocumentList)
            .furtherEvidenceAction(furtherEvidenceActionList)
            .originalSender(originalSender)
            .appeal(Appeal.builder().appellant(
                Appellant.builder().address(Address.builder().line1("My Road").postcode("TS1 2BA").build()).build())
                .build())
            .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    private DynamicList buildFurtherEvidenceActionItemListForGivenOption(String code, String label) {
        DynamicListItem selectedOption = new DynamicListItem(code, label);
        return new DynamicList(selectedOption,
            Collections.singletonList(selectedOption));
    }

    @Test
    public void givenAHandleEvidenceEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(MID_EVENT, callback));
    }


    @Test
    public void givenACaseWithScannedDocumentsAndSscsCaseDocuments_thenAppendNewDocumentsToSscsDocumentsList() {
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        SscsDocument doc = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentFileName("exist.pdf")
                .build())
            .build();
        sscsDocuments.add(doc);

        sscsCaseData.setScannedDocuments(scannedDocumentList);
        sscsCaseData.setSscsDocument(sscsDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);


        assertTrue(response.getErrors().isEmpty());
        assertTrue(response.getWarnings().isEmpty());
    }

    @Test
    public void givenACaseWithScannedDocumentWithNoFileName_showAnError() {
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                .type("type")
                .url(DocumentLink.builder().documentUrl("www.test.com").build())
                .build()).build();

        scannedDocumentList.add(scannedDocument);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getWarnings().isEmpty());
        assertEquals(1, response.getErrors().size());
        assertEquals("No document file name so could not process", response.getErrors().iterator().next());
    }

    @Test
    public void givenACaseWithScannedDocumentWithNoDocumentType_showAWarning() {
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                .fileName("type.pdf")
                .url(DocumentLink.builder().documentUrl("www.test.com").build())
                .build()).build();

        scannedDocumentList.add(scannedDocument);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
        assertEquals(1, response.getWarnings().size());
        assertEquals("Document type is empty, are you happy to proceed?", response.getWarnings().iterator().next());
    }

    @Test
    public void givenACaseWithNoScannedDocuments_thenAddAnErrorToResponse() {
        sscsCaseData.setScannedDocuments(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("Please add a scanned document", response.getErrors().iterator().next());
    }

    @Test
    public void givenADocumentWithNoUrl_thenAddAnErrorToResponse() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("Testing.jpg").build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("No document URL so could not process", response.getErrors().iterator().next());
    }

    @Test
    public void givenANonConfidentialCaseAndEditedDocumentPopulated_thenAddAnErrorToResponse() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("Testing.jpg")
                .url(DocumentLink.builder().documentUrl("test.com").build())
                .editedUrl(DocumentLink.builder().documentUrl("test").build()).build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);
        sscsCaseData.setIsConfidentialCase(YesNo.NO);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("Case is not marked as confidential so cannot upload an edited document",
            response.getErrors().iterator().next());
    }

    @Test
    @Parameters({"null", " ", "    "})
    public void givenADocumentWithNoDocFileName_thenAddAnErrorToResponse(@Nullable String filename) {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName(filename)
                .url(DocumentLink.builder().documentUrl("test.com").build()).build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("No document file name so could not process", response.getErrors().iterator().next());
    }

    @Test
    public void givenACaseWithUnreadableScannedDocument_showAnError() {
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                .fileName("Testing.jpg")
                .type("type")
                .url(DocumentLink.builder().documentUrl("www.test.com").build())
                .build()).build();

        scannedDocumentList.add(scannedDocument);
        when(footerService.isReadablePdf(any())).thenReturn(PdfState.UNREADABLE);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getWarnings().isEmpty());
        assertEquals(3, response.getErrors().size());
        Iterator<String> iterator = response.getErrors().iterator();
        assertEquals("The below PDF document(s) are not readable, please correct this", iterator.next());
        assertEquals("bla.pdf", iterator.next());
        assertEquals("Testing.jpg", iterator.next());
    }

    @Test
    public void givenACaseWithPasswordEncryptedScannedDocument_showAnError() {
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                .fileName("Testing.jpg")
                .type("type")
                .url(DocumentLink.builder().documentUrl("www.test.com").build())
                .build()).build();

        scannedDocumentList.add(scannedDocument);
        when(footerService.isReadablePdf(any())).thenReturn(PdfState.PASSWORD_ENCRYPTED);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getWarnings().isEmpty());
        assertEquals(3, response.getErrors().size());
        Iterator<String> iterator = response.getErrors().iterator();
        assertEquals("The below PDF document(s) cannot be password protected, please correct this", iterator.next());
        assertEquals("bla.pdf", iterator.next());
        assertEquals("Testing.jpg", iterator.next());
    }


    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }

}
