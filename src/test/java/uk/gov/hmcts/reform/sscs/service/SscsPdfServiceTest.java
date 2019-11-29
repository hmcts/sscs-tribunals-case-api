package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.email.SubmitYourAppealEmailTemplate;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class SscsPdfServiceTest {

    private SscsPdfService service;

    private static final String TEMPLATE_PATH = "/templates/appellant_appeal_template.html";

    @Mock
    private PDFServiceClient pdfServiceClient;

    @Mock
    EmailService emailService;

    @Mock
    SubmitYourAppealEmailTemplate submitYourAppealEmailTemplate;

    @Mock
    CcdPdfService ccdPdfService;

    SscsCaseData caseData = buildCaseData();

    @Before
    public void setup() {
        initMocks(this);
        service = new SscsPdfService(TEMPLATE_PATH, pdfServiceClient, emailService, submitYourAppealEmailTemplate, ccdPdfService);
    }

    @Test
    public void createValidPdfAndSendEmailAndStoreInDocumentStore() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        service.generateAndSendPdf(caseData, 1L, IdamTokens.builder().build(), "appellantEvidence");

        verify(pdfServiceClient).generateFromHtml(any(), any());
        verify(emailService).sendEmail(any());
        verify(ccdPdfService).mergeDocIntoCcd(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void givenUserDoesNotWantToAttendHearing_createValidPdfAndSendEmailAndStoreInDocumentStore() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        caseData.getAppeal().setHearingOptions(HearingOptions.builder().wantsToAttend("No").build());

        service.generateAndSendPdf(caseData, 1L, IdamTokens.builder().build(), "appellantEvidence");

        verify(pdfServiceClient).generateFromHtml(any(), any());
        verify(emailService).sendEmail(any());
        verify(ccdPdfService).mergeDocIntoCcd(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void givenNoRepresentative_createValidPdfAndSendEmailAndStoreInDocumentStore() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        caseData.getAppeal().setRep(Representative.builder().hasRepresentative("No").build());

        service.generateAndSendPdf(caseData, 1L, IdamTokens.builder().build(), "appellantEvidence");

        verify(pdfServiceClient).generateFromHtml(any(), any());
        verify(emailService).sendEmail(any());
        verify(ccdPdfService).mergeDocIntoCcd(any(), any(), any(), any(), any(), any());
    }
}
