package uk.gov.hmcts.reform.sscs.service.bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.service.bundle.BundleAudioVideoPdfService.DATEFORMATTER;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.docassembly.PdfTemplateContent;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.DocmosisPdfService;

@RunWith(JUnitParamsRunner.class)
public class BundleAudioVideoPdfServiceTest {

    private BundleAudioVideoPdfService service;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private DocmosisPdfService docmosisPdfService;
    @Mock
    private PdfStoreService pdfStoreService;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Captor
    private ArgumentCaptor<PdfTemplateContent> capture;

    LocalDate now;
    String nowFormatted;
    String yesterdayFormatted;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new BundleAudioVideoPdfService(docmosisPdfService, pdfStoreService, "gateway-link", "dm-store-url");
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        byte[] expectedPdf = new byte[]{2, 4, 6, 0, 1};

        SscsDocument sscsDocument = createSscsDocument();
        when(pdfStoreService.storeDocument(any(), anyString(), any())).thenReturn(sscsDocument);
        when(docmosisPdfService.createPdf(capture.capture(),any())).thenReturn(expectedPdf);

        now = LocalDate.now();
        nowFormatted = DATEFORMATTER.format(LocalDate.parse(now.toString()));
        yesterdayFormatted = DATEFORMATTER.format(LocalDate.parse(now.minusDays(1).toString()));
    }

    @Test
    public void givenAudioVideoEvidenceThatIsIncludedFromAppellant_thenCreateAudioVideoPdfAndWriteToSscsDocuments() {
        List<SscsDocument> audioVideoDocuments = new ArrayList<>();
        audioVideoDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType("audioDocument")
                .documentDateAdded(now.toString())
                .dateApproved(now.toString())
                .partyUploaded(UploadParty.APPELLANT)
                .documentLink(DocumentLink.builder().documentFilename("statement.pdf").documentUrl("dm-store-url/123").documentBinaryUrl("dm-store-url/123/binary").build())
                .avDocumentLink(DocumentLink.builder().documentFilename("Myfilename.mp3").documentUrl("dm-store-url/123").documentBinaryUrl("dm-store-url/123/binary").build()).build())
                .build());
        caseDetails.getCaseData().setSscsDocument(audioVideoDocuments);

        service.createAudioVideoPdf(caseDetails.getCaseData());

        assertEquals(1, capture.getValue().getContent().size());
        assertEquals("Audio document", capture.getValue().getContent().get(0).getDocumentType());
        assertEquals("Appellant", capture.getValue().getContent().get(0).getUploadParty());
        assertEquals(nowFormatted, capture.getValue().getContent().get(0).getDateApproved());
        assertEquals(nowFormatted, capture.getValue().getContent().get(0).getDateAdded());
        assertEquals("Myfilename.mp3|gateway-link/123/binary", capture.getValue().getContent().get(0).getDocumentUrl());

        assertEquals("Audio-video-bundle-document.pdf", caseDetails.getCaseData().getAudioVideoEvidenceBundleDocument().getDocumentLink().getDocumentFilename());
    }

    @Test
    public void givenAudioVideoEvidenceThatIsIncludedFromDwp_thenCreateAudioVideoPdfAndWriteToDwpDocuments() {
        List<DwpDocument> audioVideoDocuments = new ArrayList<>();
        audioVideoDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentType("audioDocument")
                .documentDateAdded(now.toString())
                .dateApproved(now.toString())
                .partyUploaded(UploadParty.DWP)
                .avDocumentLink(DocumentLink.builder().documentFilename("Myfilename.mp3").documentUrl("dm-store-url/123").documentBinaryUrl("dm-store-url/123/binary").build()).build())
                .build());
        caseDetails.getCaseData().setDwpDocuments(audioVideoDocuments);

        service.createAudioVideoPdf(caseDetails.getCaseData());

        assertEquals(1, capture.getValue().getContent().size());
        assertEquals("Audio document", capture.getValue().getContent().get(0).getDocumentType());
        assertEquals("DWP", capture.getValue().getContent().get(0).getUploadParty());
        assertEquals(nowFormatted, capture.getValue().getContent().get(0).getDateApproved());
        assertEquals(nowFormatted, capture.getValue().getContent().get(0).getDateAdded());
        assertEquals("Myfilename.mp3|gateway-link/123/binary", capture.getValue().getContent().get(0).getDocumentUrl());

        assertEquals("Audio-video-bundle-document.pdf", caseDetails.getCaseData().getAudioVideoEvidenceBundleDocument().getDocumentLink().getDocumentFilename());
    }

    @Test
    public void givenEmptySscsDocumentsAndDwpDocuments_thenDoNotCreateAudioVideoPdf() {
        caseDetails.getCaseData().setSscsDocument(null);
        caseDetails.getCaseData().setDwpDocuments(null);

        service.createAudioVideoPdf(caseDetails.getCaseData());

        assertNull(caseDetails.getCaseData().getAudioVideoEvidenceBundleDocument());
        verifyNoInteractions(docmosisPdfService);
    }

    @Test
    public void givenNoAudioVideoDocuments_thenDoNotCreateAudioVideoPdf() {
        List<SscsDocument> sscsAudioVideoDocuments = new ArrayList<>();
        sscsAudioVideoDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentDateAdded(now.toString())
                .dateApproved(now.toString())
                .partyUploaded(UploadParty.APPELLANT)
                .documentLink(DocumentLink.builder().documentFilename("Myfilename.pdf").documentUrl("test.com").documentBinaryUrl("test.com/binary").build()).build())
                .build());
        caseDetails.getCaseData().setSscsDocument(sscsAudioVideoDocuments);

        List<DwpDocument> dwpAudioVideoDocuments = new ArrayList<>();
        dwpAudioVideoDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentType("dwpEvidence")
                .documentDateAdded(now.toString())
                .dateApproved(now.toString())
                .partyUploaded(UploadParty.DWP)
                .documentLink(DocumentLink.builder().documentFilename("Myfilename.pdf").documentUrl("test.com").documentBinaryUrl("test.com/binary").build()).build())
                .build());
        caseDetails.getCaseData().setDwpDocuments(dwpAudioVideoDocuments);

        service.createAudioVideoPdf(caseDetails.getCaseData());

        assertNull(caseDetails.getCaseData().getAudioVideoEvidenceBundleDocument());
        verifyNoInteractions(docmosisPdfService);
    }

    @Test
    public void givenMultipleAudioVideoEvidence_thenOnlyCreateAudioVideoPdfFromMp3OrMp4() {
        List<SscsDocument> audioVideoSscsDocuments = new ArrayList<>();
        List<DwpDocument> audioVideoDwpDocuments = new ArrayList<>();

        audioVideoSscsDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType("videoDocument")
                .documentDateAdded(now.minusDays(1).toString())
                .documentLink(DocumentLink.builder().documentFilename("statement.pdf").documentUrl("dm-store-url/123").documentBinaryUrl("dm-store-url/123/binary").build())
                .avDocumentLink(DocumentLink.builder().documentFilename("Myfilename1.mp4").documentUrl("dm-store-url/123").documentBinaryUrl("dm-store-url/123/binary").build())
                .dateApproved(now.toString())
                .partyUploaded(UploadParty.APPELLANT).build())
            .build());

        audioVideoDwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentType("audioDocument")
                .documentDateAdded(now.toString())
                .avDocumentLink(DocumentLink.builder().documentFilename("Myfilename2.mp3").documentUrl("dm-store-url/456").documentBinaryUrl("dm-store-url/456/binary").build())
                .dateApproved(now.toString())
                .partyUploaded(UploadParty.DWP).build())
            .build());

        audioVideoSscsDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentDateAdded(now.toString())
                .documentLink(DocumentLink.builder().documentFilename("I-am-pdf.pdf").documentUrl("dm-store-url/356").documentBinaryUrl("dm-store-url/356/binary").build())
                .dateApproved(now.toString())
                .partyUploaded(UploadParty.APPELLANT).build())
            .build());

        audioVideoSscsDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType("audioDocument")
                .documentDateAdded(now.toString())
                .documentLink(DocumentLink.builder().documentFilename("statement.pdf").documentUrl("dm-store-url/123").documentBinaryUrl("dm-store-url/123/binary").build())
                .avDocumentLink(DocumentLink.builder().documentFilename("Myfilename4.mp3").documentUrl("dm-store-url/789").documentBinaryUrl("dm-store-url/789/binary").build())
                .dateApproved(now.toString())
                .partyUploaded(UploadParty.REP).build())
            .build());

        caseDetails.getCaseData().setSscsDocument(audioVideoSscsDocuments);
        caseDetails.getCaseData().setDwpDocuments(audioVideoDwpDocuments);

        service.createAudioVideoPdf(caseDetails.getCaseData());

        assertEquals(3, capture.getValue().getContent().size());
        assertEquals("Video document", capture.getValue().getContent().get(0).getDocumentType());
        assertEquals("Appellant", capture.getValue().getContent().get(0).getUploadParty());
        assertEquals(nowFormatted, capture.getValue().getContent().get(0).getDateApproved());
        assertEquals(yesterdayFormatted, capture.getValue().getContent().get(0).getDateAdded());
        assertEquals("Myfilename1.mp4|gateway-link/123/binary", capture.getValue().getContent().get(0).getDocumentUrl());

        assertEquals("Audio document", capture.getValue().getContent().get(1).getDocumentType());
        assertEquals("Representative", capture.getValue().getContent().get(1).getUploadParty());
        assertEquals(nowFormatted, capture.getValue().getContent().get(1).getDateApproved());
        assertEquals(nowFormatted, capture.getValue().getContent().get(1).getDateAdded());
        assertEquals("Myfilename4.mp3|gateway-link/789/binary", capture.getValue().getContent().get(1).getDocumentUrl());

        assertEquals("Audio document", capture.getValue().getContent().get(2).getDocumentType());
        assertEquals("DWP", capture.getValue().getContent().get(2).getUploadParty());
        assertEquals(nowFormatted, capture.getValue().getContent().get(2).getDateApproved());
        assertEquals(nowFormatted, capture.getValue().getContent().get(2).getDateAdded());
        assertEquals("Myfilename2.mp3|gateway-link/456/binary", capture.getValue().getContent().get(2).getDocumentUrl());

        assertEquals("Audio-video-bundle-document.pdf", caseDetails.getCaseData().getAudioVideoEvidenceBundleDocument().getDocumentLink().getDocumentFilename());

    }

    private SscsDocument createSscsDocument() {
        DocumentLink documentLink = DocumentLink.builder().documentUrl("some location").build();
        return SscsDocument.builder().value(SscsDocumentDetails.builder().documentLink(documentLink).build()).build();
    }
}