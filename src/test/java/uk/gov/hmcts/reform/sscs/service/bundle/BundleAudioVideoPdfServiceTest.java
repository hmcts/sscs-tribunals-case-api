package uk.gov.hmcts.reform.sscs.service.bundle;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

    }

    @Test
    public void givenAudioVideoEvidenceThatIsIncluded_thenCreateAudioVideoPdfAndWriteToCase() {
        List<AudioVideoEvidence> audioVideoEvidences = new ArrayList<>();
        audioVideoEvidences.add(AudioVideoEvidence.builder().value(AudioVideoEvidenceDetails.builder()
                .status(AudioVideoStatus.INCLUDED)
                .documentType("appellantEvidence")
                .partyUploaded(AudioVideoUploadParty.APPELLANT)
                .dateApproved(LocalDate.now())
                .dateAdded(LocalDate.now())
                .documentLink(DocumentLink.builder().documentFilename("Myfilename.mp3").documentUrl("dm-store-url/123").documentBinaryUrl("dm-store-url/123/binary").build()).build())
                .build());
        caseDetails.getCaseData().setAudioVideoEvidence(audioVideoEvidences);

        service.createAudioVideoPdf(caseDetails.getCaseData());

        assertEquals(1, capture.getValue().getContent().size());
        assertEquals("Appellant evidence", capture.getValue().getContent().get(0).getDocumentType());
        assertEquals("Appellant", capture.getValue().getContent().get(0).getUploadParty());
        assertEquals(LocalDate.now().toString(), capture.getValue().getContent().get(0).getDateApproved());
        assertEquals(LocalDate.now().toString(), capture.getValue().getContent().get(0).getDateAdded());
        assertEquals("Myfilename.mp3|gateway-link/123/binary", capture.getValue().getContent().get(0).getDocumentUrl());

        assertEquals("Audio-video-bundle-document.pdf", caseDetails.getCaseData().getAudioVideoEvidenceBundleDocument().getDocumentFilename());
    }

    @Test
    public void givenAudioVideoEvidenceThatIsExcluded_thenDoNotCreateAudioVideoPdf() {
        List<AudioVideoEvidence> audioVideoEvidences = new ArrayList<>();
        audioVideoEvidences.add(AudioVideoEvidence.builder().value(AudioVideoEvidenceDetails.builder()
                .status(AudioVideoStatus.EXCLUDED)
                .documentType("appellantEvidence")
                .partyUploaded(AudioVideoUploadParty.APPELLANT)
                .dateApproved(LocalDate.now())
                .dateAdded(LocalDate.now())
                .documentLink(DocumentLink.builder().documentFilename("Myfilename.mp3").documentUrl("test.com").documentBinaryUrl("test.com/binary").build()).build())
                .build());
        caseDetails.getCaseData().setAudioVideoEvidence(audioVideoEvidences);

        service.createAudioVideoPdf(caseDetails.getCaseData());

        assertNull(caseDetails.getCaseData().getAudioVideoEvidenceBundleDocument());
        verifyNoInteractions(docmosisPdfService);
    }

    @Test
    public void givenMultipleAudioVideoEvidence_thenDoNotCreateAudioVideoPdf() {
        List<AudioVideoEvidence> audioVideoEvidences = new ArrayList<>();
        audioVideoEvidences.add(AudioVideoEvidence.builder().value(AudioVideoEvidenceDetails.builder()
                .status(AudioVideoStatus.INCLUDED)
                .documentType("appellantEvidence")
                .partyUploaded(AudioVideoUploadParty.APPELLANT)
                .dateApproved(LocalDate.now().minusDays(1))
                .dateAdded(LocalDate.now().minusDays(1))
                .documentLink(DocumentLink.builder().documentFilename("Myfilename1.mp3").documentUrl("dm-store-url/123").documentBinaryUrl("dm-store-url/123/binary").build()).build())
                .build());

        audioVideoEvidences.add(AudioVideoEvidence.builder().value(AudioVideoEvidenceDetails.builder()
                .status(AudioVideoStatus.INCLUDED)
                .documentType("dwpEvidence")
                .partyUploaded(AudioVideoUploadParty.DWP)
                .dateApproved(LocalDate.now())
                .dateAdded(LocalDate.now())
                .documentLink(DocumentLink.builder().documentFilename("Myfilename2.mp3").documentUrl("dm-store-url/456").documentBinaryUrl("dm-store-url/456/binary").build()).build())
                .build());

        audioVideoEvidences.add(AudioVideoEvidence.builder().value(AudioVideoEvidenceDetails.builder()
                .status(AudioVideoStatus.INCLUDED)
                .documentType("appellantEvidence")
                .partyUploaded(AudioVideoUploadParty.CTSC)
                .dateApproved(LocalDate.now())
                .dateAdded(LocalDate.now())
                .documentLink(DocumentLink.builder().documentFilename("Myfilename3.mp3").documentUrl("dm-store-url/356").documentBinaryUrl("dm-store-url/356/binary").build()).build())
                .build());

        audioVideoEvidences.add(AudioVideoEvidence.builder().value(AudioVideoEvidenceDetails.builder()
                .status(AudioVideoStatus.EXCLUDED)
                .documentType("representativeEvidence")
                .partyUploaded(AudioVideoUploadParty.REP)
                .dateApproved(LocalDate.now())
                .dateAdded(LocalDate.now())
                .documentLink(DocumentLink.builder().documentFilename("Myfilename4.mp3").documentUrl("dm-store-url/789").documentBinaryUrl("dm-store-url/789/binary").build()).build())
                .build());

        caseDetails.getCaseData().setAudioVideoEvidence(audioVideoEvidences);

        service.createAudioVideoPdf(caseDetails.getCaseData());

        assertEquals(3, capture.getValue().getContent().size());
        assertEquals("Appellant evidence", capture.getValue().getContent().get(0).getDocumentType());
        assertEquals("Appellant", capture.getValue().getContent().get(0).getUploadParty());
        assertEquals(LocalDate.now().minusDays(1).toString(), capture.getValue().getContent().get(0).getDateApproved());
        assertEquals(LocalDate.now().minusDays(1).toString(), capture.getValue().getContent().get(0).getDateAdded());
        assertEquals("Myfilename1.mp3|gateway-link/123/binary", capture.getValue().getContent().get(0).getDocumentUrl());

        assertEquals("DWP evidence", capture.getValue().getContent().get(1).getDocumentType());
        assertEquals("DWP", capture.getValue().getContent().get(1).getUploadParty());
        assertEquals(LocalDate.now().toString(), capture.getValue().getContent().get(1).getDateApproved());
        assertEquals(LocalDate.now().toString(), capture.getValue().getContent().get(1).getDateAdded());
        assertEquals("Myfilename2.mp3|gateway-link/456/binary", capture.getValue().getContent().get(1).getDocumentUrl());

        assertEquals("Appellant evidence", capture.getValue().getContent().get(2).getDocumentType());
        assertEquals("CTSC clerk", capture.getValue().getContent().get(2).getUploadParty());
        assertEquals(LocalDate.now().toString(), capture.getValue().getContent().get(2).getDateApproved());
        assertEquals(LocalDate.now().toString(), capture.getValue().getContent().get(2).getDateAdded());
        assertEquals("Myfilename3.mp3|gateway-link/356/binary", capture.getValue().getContent().get(2).getDocumentUrl());

        assertEquals("Audio-video-bundle-document.pdf", caseDetails.getCaseData().getAudioVideoEvidenceBundleDocument().getDocumentFilename());

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