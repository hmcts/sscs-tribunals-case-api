package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.service.SubmitAppealService.DM_STORE_USER_ID;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.pdf.PdfWatermarker;

@RunWith(JUnitParamsRunner.class)
public class FooterServiceTest {

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @Mock
    private PdfWatermarker pdfWatermarker;

    private FooterService footerService;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Mock private UploadResponse uploadResponse;
    @Mock private UploadResponse.Embedded uploadResponseEmbedded;
    @Mock private List<uk.gov.hmcts.reform.document.domain.Document> uploadedDocuments;
    private uk.gov.hmcts.reform.document.domain.Document uploadedDocument = new uk.gov.hmcts.reform.document.domain.Document();

    private SscsCaseData sscsCaseData;

    private String fileName = "some-file.pdf";
    private String expectedDocumentUrl = "document-self-href";
    private String expectedBinaryUrl = "document-binary-href";

    @Before
    public void setup() {
        footerService = new FooterService(evidenceManagementService, pdfWatermarker);

        uploadedDocument.originalDocumentName = fileName;
        uploadedDocument.links = new uk.gov.hmcts.reform.document.domain.Document.Links();
        uploadedDocument.links.self = new uk.gov.hmcts.reform.document.domain.Document.Link();
        uploadedDocument.links.self.href = expectedDocumentUrl;
        uploadedDocument.links.binary = new uk.gov.hmcts.reform.document.domain.Document.Link();
        uploadedDocument.links.binary.href = expectedBinaryUrl;

        when(uploadResponse.getEmbedded()).thenReturn(uploadResponseEmbedded);
        when(uploadResponseEmbedded.getDocuments()).thenReturn(uploadedDocuments);
        when(uploadedDocuments.get(0)).thenReturn(uploadedDocument);

        SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("myTest.doc").build()).build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(document);

        sscsCaseData = SscsCaseData.builder()
                .generateNotice("Yes")
                .signedBy("User")
                .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
                .signedRole("Judge")
                .dateAdded(LocalDate.now().minusDays(1))
                .sscsDocument(docs)
                .previewDocument(DocumentLink.builder()
                        .documentUrl("dm-store/documents/123")
                        .documentBinaryUrl("dm-store/documents/123/binary")
                        .documentFilename("directionIssued.pdf")
                        .build())
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .name(Name.builder().build())
                                .identity(Identity.builder().build())
                                .build())
                        .build()).build();
    }

    @Test
    public void givenADocument_thenAddAFooter() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(evidenceManagementService.download(any(), anyString())).thenReturn(pdfBytes);

        when(evidenceManagementService.upload(any(), eq(DM_STORE_USER_ID))).thenReturn(uploadResponse);

        when(pdfWatermarker.shrinkAndWatermarkPdf(any(), stringCaptor.capture(), stringCaptor.capture())).thenReturn(new byte[]{});

        String now = LocalDate.now().toString();

        footerService.createFooterAndAddDocToCase(DocumentLink.builder().documentUrl("MyUrl").build(),
                sscsCaseData, DocumentType.DIRECTION_NOTICE, now, null, null);

        assertEquals(2, sscsCaseData.getSscsDocument().size());
        SscsDocumentDetails footerDoc = sscsCaseData.getSscsDocument().get(0).getValue();
        assertEquals(DocumentType.DIRECTION_NOTICE.getValue(), footerDoc.getDocumentType());
        assertEquals("Addition A - Directions Notice issued on " + now + ".pdf", footerDoc.getDocumentFileName());
        assertEquals(now, footerDoc.getDocumentDateAdded());
        assertEquals(expectedDocumentUrl, footerDoc.getDocumentLink().getDocumentUrl());
        verify(evidenceManagementService).upload(any(), eq(DM_STORE_USER_ID));
        assertEquals("Directions Notice", stringCaptor.getAllValues().get(0));
        assertEquals("Addition A", stringCaptor.getAllValues().get(1));
    }

    @Test
    public void givenADocumentWithOverridenDateAndFileName_thenAddAFooterWithOverriddenValues() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(evidenceManagementService.download(any(), anyString())).thenReturn(pdfBytes);

        when(evidenceManagementService.upload(any(), eq(DM_STORE_USER_ID))).thenReturn(uploadResponse);

        when(pdfWatermarker.shrinkAndWatermarkPdf(any(), stringCaptor.capture(), stringCaptor.capture())).thenReturn(new byte[]{});

        footerService.createFooterAndAddDocToCase(DocumentLink.builder().documentUrl("MyUrl").build(),
                sscsCaseData, DocumentType.DIRECTION_NOTICE, LocalDate.now().toString(), LocalDate.now().minusDays(1), "overriden.pdf");

        assertEquals(2, sscsCaseData.getSscsDocument().size());
        SscsDocumentDetails footerDoc = sscsCaseData.getSscsDocument().get(0).getValue();
        assertEquals(DocumentType.DIRECTION_NOTICE.getValue(), footerDoc.getDocumentType());
        assertEquals("overriden.pdf", footerDoc.getDocumentFileName());
        assertEquals(LocalDate.now().minusDays(1).toString(), footerDoc.getDocumentDateAdded());
        assertEquals(expectedDocumentUrl, footerDoc.getDocumentLink().getDocumentUrl());
        verify(evidenceManagementService).upload(any(), eq(DM_STORE_USER_ID));
        assertEquals("Directions Notice", stringCaptor.getAllValues().get(0));
        assertEquals("Addition A", stringCaptor.getAllValues().get(1));
    }

    @Test
    public void buildFooterLinkFromLeftAndRightText() throws IOException {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(evidenceManagementService.download(any(), anyString())).thenReturn(pdfBytes);

        when(evidenceManagementService.upload(any(), eq(DM_STORE_USER_ID))).thenReturn(uploadResponse);

        DocumentLink result = footerService.addFooter(DocumentLink.builder().documentUrl("oldLink").build(), "leftText", "rightText");

        assertEquals(expectedDocumentUrl, result.getDocumentUrl());
    }

    @Test
    @Parameters({"", "A", "B", "C", "D", "X", "Y"})
    public void canWorkOutTheNextAppendixValue(String currentAppendix) {
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        if (!currentAppendix.equals("")) {
            SscsDocument theDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition(currentAppendix).build()).build();
            sscsDocuments.add(theDocument);

            if (currentAppendix.toCharArray()[0] > 'A') {
                SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("A").build()).build();
                sscsDocuments.add(document);
            }
            if (currentAppendix.toCharArray()[0] > 'B') {
                SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("B").build()).build();
                sscsDocuments.add(document);
            }
            if (currentAppendix.toCharArray()[0] > 'C') {
                SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("C").build()).build();
                sscsDocuments.add(document);
            }
        }

        String actual = footerService.getNextBundleAddition(sscsDocuments);

        String expected = currentAppendix.equals("") ? "A" : String.valueOf((char)(currentAppendix.charAt(0) +  1));
        assertEquals(expected, actual);
    }

    @Test
    @Parameters({"Z", "Z1", "Z9", "Z85", "Z100"})
    public void canWorkOutTheNextAppendixValueAfterZ(String currentAppendix) {
        SscsDocument theDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition(currentAppendix).build()).build();
        SscsDocument documentA = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("A").build()).build();
        SscsDocument documentB = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("B").build()).build();
        SscsDocument documentC = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("Y").build()).build();
        List<SscsDocument> sscsDocuments = new ArrayList<>(Arrays.asList(theDocument, documentA, documentB, documentC));

        int index = currentAppendix.length() == 1 ? 0 : (Integer.valueOf(currentAppendix.substring(1)));

        if (index > 0) {
            SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("Z").build()).build();
            sscsDocuments.add(document);
        }
        if (index > 8) {
            SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("Z7").build()).build();
            sscsDocuments.add(document);
        }
        if (index > 30) {
            SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("Z28").build()).build();
            sscsDocuments.add(document);
        }
        if (index > 80) {
            SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition("Z79").build()).build();
            sscsDocuments.add(document);
        }

        String expected = index == 0 ? "Z1" : "Z" + (index + 1);
        String actual = footerService.getNextBundleAddition(sscsDocuments);
        assertEquals(expected, actual);
    }

    @Test
    @Parameters({"Z!", "Z3$", "ZN"})
    public void nextAppendixCanHandleInvalidDataThatAreNotNumbersAfterZ(String currentAppendix) {
        SscsDocument theDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().bundleAddition(currentAppendix).build()).build();
        String actual = footerService.getNextBundleAddition(Collections.singletonList(theDocument));
        assertEquals("[", actual);
    }

    @Test
    public void buildBundleAdditionFileNameText() {
        String result = footerService.buildBundleAdditionFileName("A", "I am the right text");

        assertEquals("Addition A - I am the right text.pdf", result);
    }
}
