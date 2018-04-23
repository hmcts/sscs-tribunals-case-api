package uk.gov.hmcts.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.pdf.service.client.exception.PDFServiceClientException;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmail;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.exception.EmailSendFailedException;
import uk.gov.hmcts.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer;

@RunWith(MockitoJUnitRunner.class)
public class SubmitAppealServiceTest {
    private static final String TEMPLATE_PATH = "/templates/appellant_appeal_template.html";

    private ObjectMapper mapper;

    @Mock
    private CcdService ccdService;

    @Mock
    private PDFServiceClient pdfServiceClient;

    @Mock
    private EmailService emailService;

    private SubmitYourAppealEmail submitYourAppealEmail;

    private SubmitAppealService service;

    @Before
    public void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        SubmitYourAppealToCcdCaseDataDeserializer submitYourAppealToCcdCaseDataDeserializer = new
                SubmitYourAppealToCcdCaseDataDeserializer();

        submitYourAppealEmail = new SubmitYourAppealEmail("from", "to", "dummy", "message");

        service = new SubmitAppealService(TEMPLATE_PATH, submitYourAppealToCcdCaseDataDeserializer, ccdService,
                pdfServiceClient, emailService, submitYourAppealEmail);
    }

    @Test
    public void shouldCreateCaseWithAppealDetails() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class))).willReturn(expected);

        service.submitAppeal(appealData);

        verify(ccdService).createCase(any(CaseData.class));
    }

    @Test
    public void shouldCreatePdfWithAppealDetails() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class))).willReturn(expected);

        service.submitAppeal(appealData);

        assertThat(submitYourAppealEmail.getSubject(), is("Bloggs_33C"));
        verify(emailService).sendEmail(any(SubmitYourAppealEmail.class));
    }

    @Test(expected = CcdException.class)
    public void shouldHandleCcdServiceException() {
        given(ccdService.createCase(any(CaseData.class)))
                .willThrow(new CcdException(
                        new RuntimeException("Error while creating case in CCD")));

        service.submitAppeal(getSyaCaseWrapper());
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