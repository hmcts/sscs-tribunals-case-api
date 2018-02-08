package uk.gov.hmcts.sscs.sya;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.io.IOUtils;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.pdf.service.client.exception.PDFServiceClientException;
import uk.gov.hmcts.sscs.controller.SyaController;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.service.SubmitAppealService;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Properties;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations="classpath:config/application_it.properties")
@AutoConfigureMockMvc
public class SyaEndpointsIT {

    public static final String PDF = "abc";
    @MockBean
    PDFServiceClient pdfServiceClient;

    @MockBean
    JavaMailSender mailSender;

    @Autowired
    private MockMvc mockMvc;

    ObjectMapper mapper;

    private Session session = Session.getInstance(new Properties());

    @Autowired
    SyaController controller;

    @Value("${appellant.appeal.html.template.path}")
    private String templateName;

    @Value("${appeal.email.from}")
    private String emailFrom;

    @Value("${appeal.email.to}")
    private String emailTo;

    @Value("${appeal.email.subject}")
    private String emailSubject;

    private SyaCaseWrapper caseWrapper;
    private MimeMessage message;

    @Before
    public void setup() throws IOException {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        caseWrapper = getCaseWrapper();

        message = new MimeMessage(session);
        when(mailSender.createMimeMessage()).thenReturn(message);
    }

    @Test
    public void shouldGeneratePdfAndSend() throws Exception {
        HashMap<String, Object> placeHolder = new HashMap<>();
        placeHolder.put(SubmitAppealService.SYA_CASE_WRAPPER, caseWrapper);
        when(pdfServiceClient.generateFromHtml(eq(getTemplate()), any())).thenReturn(PDF.getBytes());

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getCase()))
            .andExpect(status().isCreated());

        assertThat(message.getFrom()[0].toString(), containsString(emailFrom));
        assertThat(message.getAllRecipients()[0].toString(), containsString(emailTo));
        assertThat(message.getSubject(), is(emailSubject));
        assertThat(getPdf(), is(PDF));

        verify(mailSender).send(message);
    }

    @Test
    public void shouldReturnErrorStatusGivenPdfFails() throws Exception {
        doThrow(new PDFServiceClientException(null))
            .when(pdfServiceClient).generateFromHtml(any(), any());

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getCase()))
            .andExpect(status().isInternalServerError());

        verify(mailSender, never()).send(message);
    }

    private String getPdf() throws IOException, MessagingException {
        MimeMultipart content = (MimeMultipart) new MimeMessageHelper(message).getMimeMessage().getContent();
        InputStream input = (InputStream) content.getBodyPart(1).getContent();

        return IOUtils.toString(input, Charset.defaultCharset());
    }

    private byte[] getTemplate() throws IOException {
        URL resource = getClass().getResource(templateName);
        return IOUtils.toByteArray(resource);
    }

    private String getCase() {
        String syaCaseJson = "json/sya.json";
        URL resource = getClass().getClassLoader().getResource(syaCaseJson);
        try {
            return IOUtils.toString(resource, Charset.defaultCharset());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private SyaCaseWrapper getCaseWrapper() throws IOException {
        String syaCaseJson = "json/sya.json";
        URL resource = getClass().getClassLoader().getResource(syaCaseJson);
        return mapper.readValue(resource, SyaCaseWrapper.class);
    }
}
