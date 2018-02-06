package uk.gov.hmcts.sscs.sya;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.io.IOUtils;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.sscs.controller.SyaController;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.exception.CcdException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations="classpath:config/application_it.properties")
public class SyaEndpointsIT {

    @MockBean
    PDFServiceClient pdfServiceClient;

    @MockBean
    JavaMailSender mailSender;

    ObjectMapper mapper;

    private Session session = Session.getInstance(new Properties());

    @Autowired
    SyaController controller;

    @Value("${appellant.appeal.html.template.path}")
    private String templateName;

    private SyaCaseWrapper caseWrapper;
    private MimeMessage message;

    @Before
    public void setup() throws IOException {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        caseWrapper = getCaseWrapper();
        when(pdfServiceClient.generateFromHtml(getTemplate(), getPlaceholder(caseWrapper))).thenReturn("abc".getBytes());

        message = new MimeMessage(session);
        when(mailSender.createMimeMessage()).thenReturn(message);
    }

    private Map<String, Object> getPlaceholder(SyaCaseWrapper caseWrapper) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(SyaController.SYA_CASE_WRAPPER, caseWrapper);
        return map;
    }

    @Test
    public void shouldGeneratePdfAndSend() throws CcdException {
        assertThat(controller.createAppeals(caseWrapper).getStatusCode(), is(HttpStatus.OK));
        verify(mailSender, times(2)).send(message);
    }

    private byte[] getTemplate() throws IOException {
        URL resource = getClass().getResource(templateName);
        return IOUtils.toByteArray(resource);
    }

    private SyaCaseWrapper getCaseWrapper() throws IOException {
        String syaCaseJson = "json/sya.json";
        URL resource = getClass().getClassLoader().getResource(syaCaseJson);
        return mapper.readValue(resource, SyaCaseWrapper.class);
    }
}
