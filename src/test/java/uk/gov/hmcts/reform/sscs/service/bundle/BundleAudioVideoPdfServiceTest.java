package uk.gov.hmcts.reform.sscs.service.bundle;

import static java.util.Collections.singletonList;
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
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.docassembly.PdfTemplateContent;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.DocmosisPdfService;

@RunWith(JUnitParamsRunner.class)
public class BundleAudioVideoPdfServiceTest {

    private BundleAudioVideoPdfService service;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private DocmosisPdfService docmosisPdfService;
    @Mock
    private EvidenceManagementService evidenceManagementService;

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
        service = new BundleAudioVideoPdfService(docmosisPdfService, evidenceManagementService, "gateway-link", "dm-store-url");
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        byte[] expectedPdf = new byte[]{2, 4, 6, 0, 1};

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(), anyString())).thenReturn(uploadResponse);
        when(docmosisPdfService.createPdf(capture.capture(),any())).thenReturn(expectedPdf);

        now = LocalDate.now();
        nowFormatted = DATEFORMATTER.format(LocalDate.parse(now.toString()));
        yesterdayFormatted = DATEFORMATTER.format(LocalDate.parse(now.minusDays(1).toString()));
    }

    @Test
    public void givenAudioVideoEvidenceThatIsIncluded_thenCreateAudioVideoPdfAndWriteToCase() {
        List<SscsDocument> audioVideoDocuments = new ArrayList<>();
        audioVideoDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentDateAdded(now.toString())
                .documentLink(DocumentLink.builder().documentFilename("Myfilename.mp3").documentUrl("dm-store-url/123").documentBinaryUrl("dm-store-url/123/binary").build()).build())
                .build());
        caseDetails.getCaseData().setSscsDocument(audioVideoDocuments);

        service.createAudioVideoPdf(caseDetails.getCaseData());

        assertEquals(1, capture.getValue().getContent().size());
        assertEquals("Appellant evidence", capture.getValue().getContent().get(0).getDocumentType());
        assertEquals("PLACEHOLDER", capture.getValue().getContent().get(0).getUploadParty());
        assertEquals("PLACEHOLDER", capture.getValue().getContent().get(0).getDateApproved());
        assertEquals(nowFormatted, capture.getValue().getContent().get(0).getDateAdded());
        assertEquals("Myfilename.mp3|gateway-link/123/binary", capture.getValue().getContent().get(0).getDocumentUrl());

        assertEquals("Audio-video-bundle-document.pdf", caseDetails.getCaseData().getAudioVideoEvidenceBundleDocument().getDocumentLink().getDocumentFilename());
    }

    @Test
    public void givenNoAudioVideoDocuments_thenDoNotCreateAudioVideoPdf() {
        List<SscsDocument> audioVideoDocuments = new ArrayList<>();
        audioVideoDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentDateAdded(now.toString())
                .documentLink(DocumentLink.builder().documentFilename("Myfilename.pdf").documentUrl("test.com").documentBinaryUrl("test.com/binary").build()).build())
                .build());
        caseDetails.getCaseData().setSscsDocument(audioVideoDocuments);

        service.createAudioVideoPdf(caseDetails.getCaseData());

        assertNull(caseDetails.getCaseData().getAudioVideoEvidenceBundleDocument());
        verifyNoInteractions(docmosisPdfService);
    }

    @Test
    public void givenMultipleAudioVideoEvidence_thenOnlyCreateAudioVideoPdfFromMp3OrMp4() {
        List<SscsDocument> audioVideoDocuments = new ArrayList<>();
        audioVideoDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentDateAdded(now.minusDays(1).toString())
                .documentLink(DocumentLink.builder().documentFilename("Myfilename1.mp4").documentUrl("dm-store-url/123").documentBinaryUrl("dm-store-url/123/binary").build()).build())
                .build());

        audioVideoDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType("dwpEvidence")
                .documentDateAdded(now.toString())
                .documentLink(DocumentLink.builder().documentFilename("Myfilename2.mp3").documentUrl("dm-store-url/456").documentBinaryUrl("dm-store-url/456/binary").build()).build())
                .build());

        audioVideoDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentDateAdded(now.toString())
                .documentLink(DocumentLink.builder().documentFilename("Myfilename3.pdf").documentUrl("dm-store-url/356").documentBinaryUrl("dm-store-url/356/binary").build()).build())
                .build());

        audioVideoDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType("representativeEvidence")
                .documentDateAdded(now.toString())
                .documentLink(DocumentLink.builder().documentFilename("Myfilename4.mp3").documentUrl("dm-store-url/789").documentBinaryUrl("dm-store-url/789/binary").build()).build())
                .build());

        caseDetails.getCaseData().setSscsDocument(audioVideoDocuments);

        service.createAudioVideoPdf(caseDetails.getCaseData());

        assertEquals(3, capture.getValue().getContent().size());
        assertEquals("Appellant evidence", capture.getValue().getContent().get(0).getDocumentType());
        assertEquals("PLACEHOLDER", capture.getValue().getContent().get(0).getUploadParty());
        assertEquals("PLACEHOLDER", capture.getValue().getContent().get(0).getDateApproved());
        assertEquals(yesterdayFormatted, capture.getValue().getContent().get(0).getDateAdded());
        assertEquals("Myfilename1.mp4|gateway-link/123/binary", capture.getValue().getContent().get(0).getDocumentUrl());

        assertEquals("DWP evidence", capture.getValue().getContent().get(1).getDocumentType());
        assertEquals("PLACEHOLDER", capture.getValue().getContent().get(1).getUploadParty());
        assertEquals("PLACEHOLDER", capture.getValue().getContent().get(1).getDateApproved());
        assertEquals(nowFormatted, capture.getValue().getContent().get(1).getDateAdded());
        assertEquals("Myfilename2.mp3|gateway-link/456/binary", capture.getValue().getContent().get(1).getDocumentUrl());

        assertEquals("Representative evidence", capture.getValue().getContent().get(2).getDocumentType());
        assertEquals("PLACEHOLDER", capture.getValue().getContent().get(2).getUploadParty());
        assertEquals("PLACEHOLDER", capture.getValue().getContent().get(2).getDateApproved());
        assertEquals(nowFormatted, capture.getValue().getContent().get(2).getDateAdded());
        assertEquals("Myfilename4.mp3|gateway-link/789/binary", capture.getValue().getContent().get(2).getDocumentUrl());

        assertEquals("Audio-video-bundle-document.pdf", caseDetails.getCaseData().getAudioVideoEvidenceBundleDocument().getDocumentLink().getDocumentFilename());

    }

    private UploadResponse createUploadResponse() {
        UploadResponse response = mock(UploadResponse.class);
        UploadResponse.Embedded embedded = mock(UploadResponse.Embedded.class);
        when(response.getEmbedded()).thenReturn(embedded);
        Document document = createDocument();
        when(embedded.getDocuments()).thenReturn(singletonList(document));
        return response;
    }

    private static Document createDocument() {
        Document document = new Document();
        Document.Links links = new Document.Links();
        Document.Link link = new Document.Link();
        link.href = "some location";
        links.self = link;
        document.links = links;
        return document;
    }
}