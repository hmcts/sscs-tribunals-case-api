package uk.gov.hmcts.sscs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.net.URL;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class SubmitAppealServiceTest {
    public static final String TEMPLATE_PATH = "/templates/appellant_appeal_template.html";

    private ObjectMapper mapper;

    @Mock
    private PDFServiceClient pdfServiceClient;

    @Mock
    private EmailService emailService;

    @Mock
    private SubmitYourAppealEmail submitYourAppealEmail;

    private SubmitAppealService service;

    @Before
    public void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        service = new SubmitAppealService(TEMPLATE_PATH,
                pdfServiceClient, emailService, submitYourAppealEmail);
    }

    @Test
    public void shouldCreatePdfWithAppealDetails() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class))).willReturn(expected);

        service.submitAppeal(appealData);

        verify(emailService).sendEmail(any(SubmitYourAppealEmail.class));
    }

    @Test(expected = PdfGenerationException.class)
    public void shouldHandlePdfServiceClientException() {
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class)))
                .willThrow(
                        new PDFServiceClientException(
                                new RuntimeException("Malformed html error")));

        service.submitAppeal(getSyaCaseWrapper());
    }

    @Test(expected = EmailSendFailedException.class)
    public void shouldHandleEmailSendFailedException() {
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class))).willReturn(new byte[]{});
        doThrow(new EmailSendFailedException(new Exception("Error Sending email")))
                .when(emailService).sendEmail(any(SubmitYourAppealEmail.class));

        service.submitAppeal(getSyaCaseWrapper());
    }

    private SyaCaseWrapper getSyaCaseWrapper() {
        URL resource = getClass().getClassLoader().getResource("json/sya.json");
        try {
            return mapper.readValue(resource, SyaCaseWrapper.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}