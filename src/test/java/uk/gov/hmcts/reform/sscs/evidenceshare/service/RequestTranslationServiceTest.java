package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;
import uk.gov.hmcts.reform.sscs.domain.email.RequestTranslationTemplate;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;

@RunWith(JUnitParamsRunner.class)
public class RequestTranslationServiceTest {

    private RequestTranslationService requestTranslationService;
    @Mock
    PdfStoreService pdfStoreService;
    @Mock
    EmailService emailService;
    @Mock
    RequestTranslationTemplate requestTranslationTemplate;
    @Mock
    IdamService idamService;
    @Mock
    private DocmosisPdfGenerationService docmosisPdfGenerationService;
    @Captor
    private ArgumentCaptor<List<EmailAttachment>> captor;
    private SscsCaseData sscsCaseData;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        requestTranslationService = new RequestTranslationService(pdfStoreService,
                emailService,
                requestTranslationTemplate,
                docmosisPdfGenerationService,
                idamService);

        sscsCaseData = buildCaseData("Bloggs");
        sscsCaseData.setCcdCaseId("1");
        sscsCaseData.getAppeal().getAppellant().getIdentity().setNino("789123");
    }

    @Test
    public void givenACaseWithEvidenceToDownloadFromSscsDocuments_thenCreateRequestFromWluWithDownloadedEvidence() {

        byte[] expectedPdf = new byte[]{2, 4, 6, 0, 1};
        byte[] expectedBytes = new byte[]{1, 2, 3};
        given(pdfStoreService.download(any())).willReturn(expectedBytes);

        when(docmosisPdfGenerationService.generatePdf(any())).thenReturn(expectedPdf);

        List<SscsDocument> documents = new ArrayList<>();
        documents.add(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentType("sscs1")
                        .documentFileName("test.jpg")
                        .documentLink(DocumentLink.builder().documentUrl("www.download.com")
                                .documentBinaryUrl("www.download.com").documentFilename("fileName.pdf").build())
                        .documentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED)
                        .build())
                .build());

        sscsCaseData.setSscsDocument(documents);
        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null, "Benefit");

        requestTranslationService.sendCaseToWlu(caseData);

        verify(requestTranslationTemplate).generateEmail(captor.capture(), anyLong());
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.get(0).getFilename(), is("RequestTranslationForm-1.pdf"));
        assertThat(attachmentResult.get(1).getFilename(), is("fileName.pdf"));

        verify(emailService).sendEmail(eq(1L), any());

        assertThat(caseData.getCaseData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus(),
                is(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED));
    }

    @Test
    public void givenACaseWithEvidenceToDownloadFromDwpDocuments_thenCreateRequestFromWluWithDownloadedEvidence() {

        byte[] expectedPdf = new byte[]{2, 4, 6, 0, 1};
        byte[] expectedBytes = new byte[]{1, 2, 3};
        given(pdfStoreService.download(any())).willReturn(expectedBytes);

        when(docmosisPdfGenerationService.generatePdf(any())).thenReturn(expectedPdf);

        List<DwpDocument> documents = new ArrayList<>();
        documents.add(DwpDocument.builder()
            .value(DwpDocumentDetails.builder()
                .documentType(DocumentType.AUDIO_DOCUMENT.getValue())
                .documentFileName("test.jpg")
                .avDocumentLink(DocumentLink.builder().documentUrl("www.download.com")
                    .documentBinaryUrl("www.download.com").documentFilename("audio.mp3").build())
                .documentLink(DocumentLink.builder().documentUrl("www.rip1.com")
                    .documentBinaryUrl("www.rip1.com/binary").documentFilename("rip1.pdf").build())
                .documentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED)
                .build())
            .build());

        sscsCaseData.setDwpDocuments(documents);
        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null, "Benefit");

        requestTranslationService.sendCaseToWlu(caseData);

        verify(requestTranslationTemplate).generateEmail(captor.capture(), anyLong());
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.get(0).getFilename(), is("RequestTranslationForm-1.pdf"));
        assertThat(attachmentResult.get(1).getFilename(), is("rip1.pdf"));

        verify(emailService).sendEmail(eq(1L), any());

        assertThat(caseData.getCaseData().getDwpDocuments().get(0).getValue().getDocumentTranslationStatus(),
            is(SscsDocumentTranslationStatus.TRANSLATION_REQUESTED));

    }

    @Test
    public void givenACaseWithNoEvidenceToDownload_thenDoNotSendRequestEmail() {

        byte[] expectedPdf = new byte[]{2, 4, 6, 0, 1};
        byte[] expectedBytes = new byte[]{1, 2, 3};
        given(pdfStoreService.download(any())).willReturn(expectedBytes);

        when(docmosisPdfGenerationService.generatePdf(any())).thenReturn(expectedPdf);
        sscsCaseData.setSscsDocument(new ArrayList<>());
        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null, "Benefit");

        requestTranslationService.sendCaseToWlu(caseData);

        verifyNoInteractions(emailService);
        verifyNoInteractions(requestTranslationTemplate);

    }

    @Test
    public void givenACaseWithNoEvidenceLinkToDownload_thenDoNotSendRequestEmail() {

        byte[] expectedPdf = new byte[]{2, 4, 6, 0, 1};
        byte[] expectedBytes = new byte[]{0};
        given(pdfStoreService.download(any())).willReturn(expectedBytes);

        when(docmosisPdfGenerationService.generatePdf(any())).thenReturn(expectedPdf);

        List<SscsDocument> documents = new ArrayList<>();
        documents.add(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentType("sscs1")
                        .documentFileName("test.jpg")
                        .documentLink(null)
                        .documentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED)
                        .build())
                .build());

        sscsCaseData.setSscsDocument(documents);
        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null, "Benefit");

        requestTranslationService.sendCaseToWlu(caseData);

        verifyNoInteractions(emailService);
        verifyNoInteractions(requestTranslationTemplate);
    }
}
