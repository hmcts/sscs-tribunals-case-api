package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.UpdateDocParams;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class CcdPdfServiceTest {

    private static final String UPLOADED_DOCUMENT_INTO_SSCS = "Uploaded document into SSCS";

    @InjectMocks
    private CcdPdfService service;

    @Mock
    PdfStoreService pdfStoreService;

    @Mock
    CcdService ccdService;

    private final SscsCaseData caseData = buildCaseData();

    @Before
    public void setup() {
        openMocks(this);
    }

    @Test
    public void mergeValidPdfAndStoreInDocumentStore() {
        SscsDocument sscsDocument = SscsDocument.builder()
                .value(SscsDocumentDetails.builder().documentFileName("Test.jpg").build()).build();

        byte[] pdf = {};
        UpdateDocParams params = UpdateDocParams.builder().fileName("Myfile.pdf").pdf(pdf).caseId(1L).caseData(caseData).documentType("dl6").build();

        when(pdfStoreService.storeDocument(params)).thenReturn(sscsDocument);
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any()))
            .thenReturn(SscsCaseDetails.builder().data(caseData).build());

        service.mergeDocIntoCcd("Myfile.pdf", pdf, 1L, caseData, IdamTokens.builder().build(),
            "dl6");

        verify(pdfStoreService).storeDocument(params);
        verify(ccdService).updateCase(any(), any(), any(), eq("SSCS - upload document event"),
            eq("Uploaded document into SSCS"), any());
        assertEquals("Test.jpg", caseData.getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    @Test
    @Parameters(method = "generateScenariosForSscsDocuments")
    public void givenStatement_shouldMergeDocIntoCcd(
        String fileName,
        List<SscsDocument> newStoredSscsDocuments,
        List<SscsDocument> existingSscsDocuments,
        List<ScannedDocument> existingScannedDocuments,
        int expectedNumberOfScannedDocs) {

        UpdateDocParams expectedParams = UpdateDocParams.builder()
                .pdf(new byte[0])
                .caseId(1L)
                .caseData(caseData)
                .fileName(fileName)
                .documentType("Other evidence")
                .build();

        when(pdfStoreService.storeDocument(expectedParams)).thenReturn(newStoredSscsDocuments.size() == 1 ? newStoredSscsDocuments.get(0) : null);
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any()))
            .thenReturn(SscsCaseDetails.builder().data(caseData).build());

        caseData.setSscsDocument(existingSscsDocuments);
        caseData.setScannedDocuments(existingScannedDocuments);

        service.mergeDocIntoCcd(fileName, new byte[0], 1L, caseData, IdamTokens.builder().build(),
            "Other evidence");


        verify(pdfStoreService, times(1)).storeDocument(expectedParams);

        ArgumentCaptor<SscsCaseData> caseDataCaptor = ArgumentCaptor.forClass(SscsCaseData.class);
        verify(ccdService, times(1))
            .updateCase(caseDataCaptor.capture(), eq(1L), eq(UPLOAD_DOCUMENT.getCcdType()), anyString(),
                anyString(), any(IdamTokens.class));

        assertThat(caseDataCaptor.getValue().getScannedDocuments().size(), is(expectedNumberOfScannedDocs));
        assertThat(caseDataCaptor.getValue().getEvidenceHandled(), is(NO.getValue()));
        if (!newStoredSscsDocuments.isEmpty()) {
            String expectedFilename = newStoredSscsDocuments.get(0).getValue().getDocumentFileName();
            Optional<ScannedDocument> scannedDocument = caseDataCaptor.getValue().getScannedDocuments().stream()
                .filter(scannedDoc -> expectedFilename.equals(scannedDoc.getValue().getFileName()))
                .findFirst();
            assertThat(scannedDocument.isPresent(), is(true));
            assertThat(LocalDateTime.parse(scannedDocument.get().getValue().getScannedDate()), greaterThan(LocalDateTime.now().minusSeconds(2)));
            assertThat(LocalDateTime.parse(scannedDocument.get().getValue().getScannedDate()), lessThan(LocalDateTime.now().plusSeconds(2)));
            assertThat(scannedDocument.get().getValue(), samePropertyValuesAs(buildExpectedScannedDocument(newStoredSscsDocuments).getValue(), "scannedDate"));
        }
    }

    private ScannedDocument buildExpectedScannedDocument(List<SscsDocument> newStoredSscsDocuments) {
        SscsDocumentDetails expectedDocValues = newStoredSscsDocuments.get(0).getValue();
        return ScannedDocument.builder()
            .value(ScannedDocumentDetails.builder()
                .fileName(expectedDocValues.getDocumentFileName())
                .url(expectedDocValues.getDocumentLink())
                .scannedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .type("other")
                .build())
            .build();
    }

    private Object[] generateScenariosForSscsDocuments() {
        String doc1FileName = "Appellant statement 1 - SC0011111.pdf";
        SscsDocumentDetails sscsDocumentDetails1 = SscsDocumentDetails.builder()
            .documentFileName(doc1FileName)
            .documentDateAdded(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
            .documentLink(DocumentLink.builder().documentUrl("http://dm-store").build())
            .documentType("Other evidence")
            .build();

        String doc1RepFileName = "Representative statement 1 - SC0011111.pdf";
        SscsDocumentDetails sscsDocumentDetails1WithRepStatement = SscsDocumentDetails.builder()
            .documentFileName(doc1RepFileName)
            .documentDateAdded(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
            .documentLink(DocumentLink.builder().documentUrl("http://dm-store").build())
            .documentType("Other evidence")
            .build();
        List<SscsDocument> newStoredSscsDocumentsWithDocWithRepsStatement = singletonList(SscsDocument.builder()
            .value(sscsDocumentDetails1WithRepStatement)
            .build());


        String doc2FileName = "Appellant statement 2 - SC0022222.pdf";
        SscsDocumentDetails sscsDocumentDetails2 = SscsDocumentDetails.builder()
            .documentFileName(doc2FileName)
            .documentDateAdded(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
            .documentLink(DocumentLink.builder().documentUrl("http://dm-store2").build())
            .documentType("Other evidence")
            .build();

        List<SscsDocument> newStoredSscsDocumentsWithDoc1 = singletonList(SscsDocument.builder()
            .value(sscsDocumentDetails1)
            .build());

        List<SscsDocument> newStoredSscsDocumentsWithDoc2 = singletonList(SscsDocument.builder()
            .value(sscsDocumentDetails2)
            .build());

        int expectedNumberOfScannedDocsIsOne = 1;
        int expectedNumberOfScannedDocsIsZero = 0;
        int expectedNumberOfScannedDocsIsTwo = 2;

        ScannedDocumentDetails existingScannedDoc1 = ScannedDocumentDetails.builder()
            .fileName(doc1FileName)
            .url(DocumentLink.builder().documentUrl("http://dm-store").build())
            .type("other")
            .build();
        List<ScannedDocument> existingScannedDocsWithScannedDoc1 = singletonList(ScannedDocument.builder()
            .value(existingScannedDoc1)
            .build());


        return new Object[]{
            new Object[]{doc1FileName, newStoredSscsDocumentsWithDoc1, null, null, expectedNumberOfScannedDocsIsOne},
            new Object[]{doc1RepFileName, newStoredSscsDocumentsWithDocWithRepsStatement, null, null, expectedNumberOfScannedDocsIsOne},
            new Object[]{doc1FileName, Collections.emptyList(), null, null, expectedNumberOfScannedDocsIsZero},
            new Object[]{doc2FileName, newStoredSscsDocumentsWithDoc2, null, existingScannedDocsWithScannedDoc1, expectedNumberOfScannedDocsIsTwo},
            new Object[]{doc2FileName, newStoredSscsDocumentsWithDoc2, newStoredSscsDocumentsWithDoc1, existingScannedDocsWithScannedDoc1, expectedNumberOfScannedDocsIsTwo}
        };
    }

    @Test
    public void mergeValidPdfAndStoreInDocumentStoreWithDescription() {
        byte[] pdf = {};
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().data(caseData).build());

        service.mergeDocIntoCcd("Myfile.pdf", pdf, 1L, caseData, IdamTokens.builder().build(), "My description", "dl6");

        UpdateDocParams params = UpdateDocParams.builder().fileName("Myfile.pdf").pdf(pdf).caseId(1L).caseData(caseData).documentType("dl6").build();
        verify(pdfStoreService).storeDocument(params);
        verify(ccdService).updateCase(any(), any(), any(), eq("SSCS - upload document event"), eq("My description"), any());
    }

    @Test
    public void mergeValidPdfAndStoreInDocumentStoreWithoutDescription() {
        byte[] pdf = {};
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().data(caseData).build());

        service.mergeDocIntoCcd("Myfile.pdf", pdf, 1L, caseData, IdamTokens.builder().build());

        UpdateDocParams params = UpdateDocParams.builder().fileName("Myfile.pdf").pdf(pdf).caseId(1L).caseData(caseData).build();
        verify(pdfStoreService).storeDocument(params);
        verify(ccdService).updateCase(any(), any(), any(), eq("SSCS - upload document event"), eq(UPLOADED_DOCUMENT_INTO_SSCS), any());
    }

    @Test
    public void mergeValidPdfAndStoreInDocumentStoreWithoutDescriptionAndUsingUpdateDocParams() {
        byte[] pdf = {};
        UpdateDocParams params = UpdateDocParams.builder().fileName("Myfile.pdf").pdf(pdf).caseId(1L).caseData(caseData).build();
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().data(caseData).build());

        service.mergeDocIntoCcd(params, IdamTokens.builder().build());

        verify(pdfStoreService).storeDocument(params);
        verify(ccdService).updateCase(any(), any(), any(), eq("SSCS - upload document event"), eq(UPLOADED_DOCUMENT_INTO_SSCS), any());
    }

    @Test
    public void mergeValidPdfAndNotStoreWhenException() {
        byte[] pdf = {};
        UpdateDocParams params = UpdateDocParams.builder().fileName("Myfile.pdf").pdf(pdf).caseId(1L).caseData(caseData).build();
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenThrow(CcdException.class);

        SscsCaseData expectedCaseData = service.mergeDocIntoCcd(params, IdamTokens.builder().build());

        assertNull(expectedCaseData);
        verify(pdfStoreService).storeDocument(params);
        verify(ccdService).updateCase(any(), any(), any(), eq("SSCS - upload document event"), eq(UPLOADED_DOCUMENT_INTO_SSCS), any());
    }

    @Test
    public void updateDocWhenUpdateDocParamsCaseIdIsNull() {
        byte[] pdf = {};
        UpdateDocParams params = UpdateDocParams.builder().fileName("Myfile.pdf").pdf(pdf).caseData(caseData).build();

        SscsCaseData expectedCaseData = service.updateDoc(params);

        assertThat(expectedCaseData, is(caseData));
        verify(pdfStoreService).storeDocument(params);
    }

    @Test
    public void updateDocWhenUpdateDocParamsTranslationStatusIsRequired() {
        byte[] pdf = {};
        UpdateDocParams params = UpdateDocParams.builder().fileName("Myfile.pdf").caseId(1L).documentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED).pdf(pdf).caseData(caseData).build();

        SscsCaseData expectedCaseData = service.updateDoc(params);

        assertThat(expectedCaseData.getTranslationWorkOutstanding(), is(YES));
        assertThat(expectedCaseData, is(caseData));
        verify(pdfStoreService).storeDocument(params);
    }
}
