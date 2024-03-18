package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.evidenceshare.model.PdfDocument;
import uk.gov.hmcts.reform.sscs.exception.UnsupportedDocumentTypeException;
import uk.gov.hmcts.reform.sscs.helper.PdfHelper;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;

@RunWith(JUnitParamsRunner.class)
public class SscsDocumentServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private PdfStoreService pdfStoreService;

    @Mock
    private PdfHelper pdfHelper;

    @InjectMocks
    private SscsDocumentService sscsDocumentService;

    @Test
    public void filterByDocTypeAndApplyActionHappyPath() {
        List<SscsDocument> sscsDocumentList = createTestData(false);
        Consumer<SscsDocument> action = doc -> doc.getValue().setEvidenceIssued("Yes");

        sscsDocumentService.filterByDocTypeAndApplyAction(sscsDocumentList, APPELLANT_EVIDENCE, action);

        sscsDocumentList.stream()
            .filter(doc -> APPELLANT_EVIDENCE.getValue().equals(doc.getValue().getDocumentType()))
            .forEach(doc -> assertEquals("Yes", doc.getValue().getEvidenceIssued()));
    }

    @Test
    @Parameters({
        "APPELLANT_EVIDENCE,appellantEvidenceDoc, false",
        "REPRESENTATIVE_EVIDENCE,repsEvidenceDoc, false",
        "OTHER_DOCUMENT,otherEvidenceDoc, false",
        "APPELLANT_EVIDENCE,appellantEvidenceDoc, true",
        "REPRESENTATIVE_EVIDENCE,repsEvidenceDoc, true",
        "OTHER_DOCUMENT,otherEvidenceDoc, true"
    })
    public void getPdfsForGivenDocType(DocumentType documentType, String expectedDocName, boolean editedDocument) {

        String expectedDocumentUrl = editedDocument ? "http://editedDocumentUrl" : "http://documentUrl";
        given(pdfStoreService.download(eq(expectedDocumentUrl)))
            .willReturn(new byte[]{'a'});

        List<SscsDocument> testDocs = createTestData(editedDocument);

        List<PdfDocument> actualPdfs = sscsDocumentService.getPdfsForGivenDocTypeNotIssued(testDocs, documentType, true, null);

        assertEquals(1, actualPdfs.size());
        PdfDocument expectedPdfDocument = PdfDocument.builder().pdf(new Pdf(new byte[]{'a'}, expectedDocName)).document(testDocs.get(0)).build();
        assertEquals(expectedPdfDocument, actualPdfs.get(0));
    }

    @Test
    @Parameters({
        "OTHER_PARTY_EVIDENCE, otherPartyDoc, false, 1",
        "OTHER_PARTY_REPRESENTATIVE_EVIDENCE, otherPartyRepDoc, false, 2",
        "OTHER_PARTY_EVIDENCE, otherPartyDoc, true, 1",
        "OTHER_PARTY_REPRESENTATIVE_EVIDENCE, otherPartyRepDoc, true, 2"
    })
    public void givenOtherPartyDocs_getPdfsForGivenDocType(DocumentType documentType, String expectedDocName, boolean editedDocument, String otherPartyOriginalSenderId) {

        String expectedDocumentUrl = editedDocument ? "http://editedDocumentUrl" : "http://documentUrl";
        given(pdfStoreService.download(eq(expectedDocumentUrl)))
            .willReturn(new byte[]{'a'});

        List<SscsDocument> testDocs = createTestData(editedDocument);
        addOtherPartyDocs(editedDocument, testDocs);

        List<PdfDocument> actualPdfs = sscsDocumentService.getPdfsForGivenDocTypeNotIssued(testDocs, documentType, true, otherPartyOriginalSenderId);

        assertEquals(1, actualPdfs.size());
        PdfDocument expectedPdfDocument = PdfDocument.builder().pdf(new Pdf(new byte[]{'a'}, expectedDocName)).document(testDocs.get(0)).build();
        assertEquals(expectedPdfDocument, actualPdfs.get(0));
    }

    @Test
    public void savesAndUpdatesDocumentCorrectly() {

        String resizedHref = "somelink.com";
        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentLink(DocumentLink.builder().documentUrl(resizedHref).build()).build()).build();

        when(pdfStoreService.storeDocument(any(), any(), any())).thenReturn(sscsDocument);
        Pdf pdf = new Pdf("".getBytes(), "file.pdf");
        SscsDocument testDoc = createTestData(false).get(0);
        AbstractDocument result = sscsDocumentService.saveAndUpdateDocument(pdf, testDoc);
        assertEquals(resizedHref, result.getValue().getResizedDocumentLink().getDocumentUrl());
    }

    @Test
    public void savesAndUpdatesDocumentHandlesFailure() {

        when(pdfStoreService.storeDocument((byte[]) any())).thenThrow(new UnsupportedDocumentTypeException(new Exception()));
        Pdf pdf = new Pdf("".getBytes(), "file.pdf");
        SscsDocument testDoc = createTestData(false).get(0);
        AbstractDocument result = sscsDocumentService.saveAndUpdateDocument(pdf, testDoc);
        assertEquals(null, result.getValue().getResizedDocumentLink());
    }

    @Test
    public void resizedPdfHandlesWithinSize() throws Exception {
        when(pdfHelper.scaleToA4(any())).thenReturn(Optional.empty());
        byte[] pdfContent = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("myPdf.pdf"));
        Pdf pdf = new Pdf(pdfContent, "file.pdf");
        Optional<Pdf> result = sscsDocumentService.resizedPdf(pdf);
        assertEquals(true, result.isEmpty());
    }

    @Test
    public void resizedPdfReturnsResizedWhenOutsideSizeLimit() throws Exception {
        byte[] pdfContent = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("myPdf.pdf"));
        when(pdfHelper.scaleToA4(any())).thenReturn(Optional.of(PDDocument.load(pdfContent)));
        Pdf pdf = new Pdf(pdfContent, "file.pdf");
        Optional<Pdf> result = sscsDocumentService.resizedPdf(pdf);
        assertEquals(true, result.isPresent());
        assertEquals(pdf.getName(), result.get().getName());
    }

    @Test(expected = BulkPrintException.class)
    public void resizedPdfPropogatesException() throws Exception {
        when(pdfHelper.scaleToA4(any())).thenThrow(new Exception());
        byte[] pdfContent = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("myPdf.pdf"));
        Pdf pdf = new Pdf(pdfContent, "file.pdf");
        sscsDocumentService.resizedPdf(pdf);
    }

    private List<SscsDocument> createTestData(boolean withEditedDocument) {
        DocumentLink documentLink = DocumentLink.builder()
            .documentUrl("http://documentUrl")
            .build();
        DocumentLink editedDocumentLink = DocumentLink.builder()
            .documentUrl("http://editedDocumentUrl")
            .build();
        SscsDocument sscsDocumentAppellantType = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentFileName("appellantEvidenceDoc")
                .documentType(APPELLANT_EVIDENCE.getValue())
                .documentLink(documentLink)
                .editedDocumentLink(withEditedDocument ? editedDocumentLink : null)
                .evidenceIssued("No")
                .build())
            .build();
        SscsDocument sscsDocumentAppellantTypeIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentFileName("appellantEvidenceDoc")
                .documentType(APPELLANT_EVIDENCE.getValue())
                .documentLink(documentLink)
                .editedDocumentLink(withEditedDocument ? editedDocumentLink : null)
                .evidenceIssued("Yes")
                .build())
            .build();
        SscsDocument sscsDocumentRepsType = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentFileName("repsEvidenceDoc")
                .documentType(REPRESENTATIVE_EVIDENCE.getValue())
                .documentLink(documentLink)
                .editedDocumentLink(withEditedDocument ? editedDocumentLink : null)
                .evidenceIssued("No")
                .build())
            .build();
        SscsDocument sscsDocumentOtherType = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentFileName("otherEvidenceDoc")
                .documentType(OTHER_DOCUMENT.getValue())
                .documentLink(documentLink)
                .editedDocumentLink(withEditedDocument ? editedDocumentLink : null)
                .evidenceIssued("No")
                .build())
            .build();

        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscsDocumentAppellantType);
        sscsDocuments.add(sscsDocumentAppellantTypeIssued);
        sscsDocuments.add(sscsDocumentRepsType);
        sscsDocuments.add(sscsDocumentOtherType);

        return sscsDocuments;
    }

    private void addOtherPartyDocs(boolean withEditedDocument, List<SscsDocument> sscsDocuments) {
        DocumentLink documentLink = DocumentLink.builder()
            .documentUrl("http://documentUrl")
            .build();
        DocumentLink editedDocumentLink = DocumentLink.builder()
            .documentUrl("http://editedDocumentUrl")
            .build();

        SscsDocument sscsDocumentOtherPartyType = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentFileName("otherPartyDoc")
                .documentType(OTHER_PARTY_EVIDENCE.getValue())
                .documentLink(documentLink)
                .editedDocumentLink(withEditedDocument ? editedDocumentLink : null)
                .evidenceIssued("No")
                .originalSenderOtherPartyId("1")
                .build())
            .build();

        SscsDocument sscsDocumentOtherPartyRepType = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentFileName("otherPartyRepDoc")
                .documentType(OTHER_PARTY_REPRESENTATIVE_EVIDENCE.getValue())
                .documentLink(documentLink)
                .editedDocumentLink(withEditedDocument ? editedDocumentLink : null)
                .evidenceIssued("No")
                .originalSenderOtherPartyId("2")
                .build())
            .build();

        sscsDocuments.add(sscsDocumentOtherPartyType);
        sscsDocuments.add(sscsDocumentOtherPartyRepType);
    }

    private UploadResponse createUploadResponse(String linkHref) {

        Document document = new Document();
        Document.Links links = new Document.Links();
        Document.Link link = new Document.Link();
        link.href = linkHref;
        links.self = link;
        document.links = links;

        UploadResponse response = mock(UploadResponse.class);
        UploadResponse.Embedded embedded = mock(UploadResponse.Embedded.class);
        when(response.getEmbedded()).thenReturn(embedded);
        when(embedded.getDocuments()).thenReturn(Collections.singletonList(document));
        return response;
    }
}
