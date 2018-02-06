package uk.gov.hmcts.sscs.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.pdf.service.client.exception.PDFServiceClientException;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmail;
import uk.gov.hmcts.sscs.exception.EmailSendFailedException;
import uk.gov.hmcts.sscs.exception.PdfGenerationException;
import java.io.IOException;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class SubmitAppealServiceTest {
    public static final String TEMPLATE_PATH = "/templates/appellant_appeal_template.html";
    public static final String UNIQUE_ID = "unique-id";
    @Mock
    private PDFServiceClient pdfServiceClient;

    @Mock
    private EmailService emailService;

    @Mock
    private SubmitYourAppealEmail submitYourAppealEmail;

    private SubmitAppealService service;

    @Before
    public void setUp() {
        service = new SubmitAppealService(TEMPLATE_PATH,
                pdfServiceClient, emailService, submitYourAppealEmail);
    }

    @Test
    public void shouldCreatePdfWithAppealDetails() throws IOException {
        SyaCaseWrapper appealData = new SyaCaseWrapper();
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class))).willReturn(expected);

        service.submitAppeal(appealData, UNIQUE_ID);

        verify(emailService).sendEmail(any(SubmitYourAppealEmail.class));
    }

    @Test(expected = PdfGenerationException.class)
    public void shouldHandlePdfServiceClientException() {
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class)))
                .willThrow(
                        new PDFServiceClientException(
                                new RuntimeException("Malformed html error")));

        service.submitAppeal(new SyaCaseWrapper(), UNIQUE_ID);
    }

    @Test(expected = EmailSendFailedException.class)
    public void shouldHandleEmailSendFailedException() throws Exception {
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class))).willReturn(new byte[]{});
        doThrow(new EmailSendFailedException(new Exception("Error Sending email")))
                .when(emailService).sendEmail(any(SubmitYourAppealEmail.class));

        service.submitAppeal(new SyaCaseWrapper(), UNIQUE_ID);
    }

}