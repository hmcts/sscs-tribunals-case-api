package uk.gov.hmcts.reform.sscs.sya;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INCOMPLETE_APPLICATION_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SEND_TO_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SYA_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.Classification;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.domain.pdf.PdfWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.Authorize;
import uk.gov.hmcts.reform.sscs.idam.IdamApiClient;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.json.RoboticsJsonMapper;
import uk.gov.hmcts.reform.sscs.json.RoboticsJsonValidator;
import uk.gov.hmcts.reform.sscs.service.SubmitAppealService;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_it.properties")
@AutoConfigureMockMvc
public class SyaEndpointsIt {

    // being: it needed to run springRunner and junitParamsRunner
    @ClassRule
    public static final SpringClassRule SCR = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    // end

    private static final String PDF = "abc";
    private static final String AUTH_TOKEN = "authToken";
    private static final String DUMMY_OAUTH_2_TOKEN = "oauth2Token";

    @MockBean
    private CcdClient ccdClient;

    @MockBean
    private IdamApiClient idamApiClient;

    @MockBean
    @Qualifier("authTokenGenerator")
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private PDFServiceClient pdfServiceClient;

    @MockBean
    private DocumentUploadClientApi documentUploadClientApi;

    @MockBean
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<Map<String, Object>> captor;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoboticsJsonMapper roboticsJsonMapper;
    @SpyBean
    private RoboticsJsonValidator jsonValidator;

    @Autowired
    private SubmitAppealService submitAppealService;

    private ObjectMapper mapper;

    private Session session = Session.getInstance(new Properties());

    @Value("${appellant.appeal.html.template.path}")
    private String templateName;

    @Value("${appeal.email.from}")
    private String emailFrom;

    @Value("${appeal.email.to}")
    private String emailTo;

    private SyaCaseWrapper caseWrapper;

    private MimeMessage message;

    @Before
    public void setup() throws IOException {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        caseWrapper = getCaseWrapper();

        message = new MimeMessage(session);
        when(mailSender.createMimeMessage()).thenReturn(message);

        given(pdfServiceClient.generateFromHtml(eq(getTemplate()), captor.capture()))
            .willReturn(PDF.getBytes());

        given(ccdClient.readForCaseworker(any(), any())).willReturn(null);
        given(ccdClient.startEvent(any(), any(), anyString())).willReturn(StartEventResponse.builder().eventId("12345").build());
        given(ccdClient.submitEventForCaseworker(any(), any(), any())).willReturn(CaseDetails.builder().id(123456789876L).build());

        Authorize authorize = new Authorize("redirectUrl/", "code", "token");
        given(idamApiClient.authorizeCodeType(anyString(), anyString(), anyString(), anyString(), anyString()))
            .willReturn(authorize);
        given(idamApiClient.authorizeToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .willReturn(authorize);

        given(authTokenGenerator.generate()).willReturn("authToken");
        given(idamApiClient.getUserDetails(anyString())).willReturn(new UserDetails("userId"));

        UploadResponse uploadResponse = createUploadResponse();
        given(documentUploadClientApi.upload(eq(DUMMY_OAUTH_2_TOKEN), eq(AUTH_TOKEN), eq("sscs"),
            eq(Arrays.asList("caseworker", "citizen")), eq(Classification.RESTRICTED), any())).willReturn(uploadResponse);

    }

    @Test
    @Parameters({"true,Birmingham-SYA-Receipts@justice.gov.uk", "false,null", "null,null"})
    public void givenAValidAppealWithDwpFeatureFlagOff_createAppealCreatedCaseAndGeneratePdfAndSendAndWithRpcEmailPresent(
        @Nullable Boolean rpcEmailRoboticsFeatureValue, String expectedRpcEmail) throws Exception {

        ReflectionTestUtils.setField(submitAppealService, "sendToDwpFeature", false);
        ReflectionTestUtils.setField(roboticsJsonMapper, "rpcEmailRoboticsFeature", rpcEmailRoboticsFeatureValue);

        given(ccdClient.startCaseForCaseworker(any(), anyString())).willReturn(StartEventResponse.builder().build());

        given(ccdClient.submitForCaseworker(any(), any())).willReturn(CaseDetails.builder().id(123456789876L).build());

        given(ccdClient.searchForCaseworker(any(), any())).willReturn(Collections.emptyList());

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getCase("json/sya.json")))
            .andExpect(status().isCreated());

        verify(pdfServiceClient).generateFromHtml(eq(getTemplate()), captor.capture());

        assertThat(message.getFrom()[0].toString(), containsString(emailFrom));
        assertThat(message.getAllRecipients()[0].toString(), containsString(emailTo));
        assertThat(message.getSubject(), is("Bloggs_33C"));

        verify(ccdClient).startCaseForCaseworker(any(), eq(SYA_APPEAL_CREATED.getCcdType()));
        verify(ccdClient).submitForCaseworker(any(), any());
        verify(mailSender, times(2)).send(message);

        assertNotNull(getPdfWrapper().getCcdCaseId());

        assertRpcEmail(rpcEmailRoboticsFeatureValue, expectedRpcEmail);
    }

    private void assertRpcEmail(@Nullable Boolean rpcEmailRoboticsFeatureValue, String expectedRpcEmail) {
        ArgumentCaptor<JSONObject> roboticsAppealCaptor = ArgumentCaptor.forClass(JSONObject.class);
        then(jsonValidator).should().validate(roboticsAppealCaptor.capture());

        if (rpcEmailRoboticsFeatureValue != null && rpcEmailRoboticsFeatureValue) {
            assertTrue(roboticsAppealCaptor.getValue().has("rpcEmail"));
            assertThat(roboticsAppealCaptor.getValue().get("rpcEmail"), is(expectedRpcEmail));
        } else {
            assertFalse(roboticsAppealCaptor.getValue().has("rpcEmail"));
        }
    }

    @Test
    public void givenAValidAppealWithDwpFeatureFlagOn_createAppealCreatedCaseAndGeneratePdfAndSend() throws Exception {
        ReflectionTestUtils.setField(submitAppealService, "sendToDwpFeature", true);

        given(ccdClient.startCaseForCaseworker(any(), anyString())).willReturn(StartEventResponse.builder().build());

        given(ccdClient.submitForCaseworker(any(), any())).willReturn(CaseDetails.builder().id(123456789876L).build());

        given(ccdClient.searchForCaseworker(any(), any())).willReturn(Collections.emptyList());

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getCase("json/sya.json")))
            .andExpect(status().isCreated());

        verify(pdfServiceClient).generateFromHtml(eq(getTemplate()), captor.capture());

        assertThat(message.getFrom()[0].toString(), containsString(emailFrom));
        assertThat(message.getAllRecipients()[0].toString(), containsString(emailTo));
        assertThat(message.getSubject(), is("Bloggs_33C"));

        verify(ccdClient).startCaseForCaseworker(any(), eq(VALID_APPEAL_CREATED.getCcdType()));
        verify(ccdClient).submitForCaseworker(any(), any());
        verify(mailSender, times(1)).send(message);
        verify(ccdClient).startEvent(any(), any(), eq("uploadDocument"));
        verify(ccdClient).startEvent(any(), any(), eq(SEND_TO_DWP.getCcdType()));
        verify(ccdClient, times(2)).submitEventForCaseworker(any(), any(), any());

        assertNotNull(getPdfWrapper().getCcdCaseId());
    }

    @Test
    public void givenAValidAppealWithNoMrnDate_createIncompleteAppealCaseAndSendPdfAndDoNotSendRobotics() throws Exception {
        given(ccdClient.startCaseForCaseworker(any(), anyString())).willReturn(StartEventResponse.builder().build());

        given(ccdClient.submitForCaseworker(any(), any())).willReturn(CaseDetails.builder().id(123456789876L).build());

        given(ccdClient.searchForCaseworker(any(), any())).willReturn(Collections.emptyList());

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getCase("json/syaWithNoMrnDate.json")))
            .andExpect(status().isCreated());

        verify(pdfServiceClient).generateFromHtml(eq(getTemplate()), captor.capture());

        assertThat(message.getFrom()[0].toString(), containsString(emailFrom));
        assertThat(message.getAllRecipients()[0].toString(), containsString(emailTo));
        assertThat(message.getSubject(), is("Bloggs_33C"));

        verify(ccdClient).startCaseForCaseworker(any(), eq(INCOMPLETE_APPLICATION_RECEIVED.getCcdType()));
        verify(ccdClient).submitForCaseworker(any(), any());
        verify(mailSender).send(message);
        verify(ccdClient, times(0)).startEvent(any(), any(), eq(SEND_TO_DWP.getCcdType()));

        assertNotNull(getPdfWrapper().getCcdCaseId());
    }

    @Test
    public void givenAValidAppealWithMrnDateMoreThan13MonthsAgo_createNonCompliantAppealCaseAndSendPdfAndDoNotSendRobotics() throws Exception {
        given(ccdClient.startCaseForCaseworker(any(), anyString())).willReturn(StartEventResponse.builder().build());

        given(ccdClient.submitForCaseworker(any(), any())).willReturn(CaseDetails.builder().id(123456789876L).build());

        given(ccdClient.searchForCaseworker(any(), any())).willReturn(Collections.emptyList());

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getCase("json/syaWithMrnDateMoreThan13Months.json")))
            .andExpect(status().isCreated());

        verify(pdfServiceClient).generateFromHtml(eq(getTemplate()), captor.capture());

        assertThat(message.getFrom()[0].toString(), containsString(emailFrom));
        assertThat(message.getAllRecipients()[0].toString(), containsString(emailTo));
        assertThat(message.getSubject(), is("Bloggs_33C"));

        verify(ccdClient).startCaseForCaseworker(any(), eq(NON_COMPLIANT.getCcdType()));
        verify(ccdClient).submitForCaseworker(any(), any());
        verify(mailSender).send(message);
        verify(ccdClient, times(0)).startEvent(any(), any(), eq(SEND_TO_DWP.getCcdType()));

        assertNotNull(getPdfWrapper().getCcdCaseId());
    }

    @Test
    public void shouldNotAddDuplicateCaseToCcdAndShouldNotGeneratePdf() throws Exception {
        CaseDetails caseDetails = CaseDetails.builder().id(1L).build();

        given(ccdClient.searchForCaseworker(any(), any())).willReturn(Collections.singletonList(caseDetails));

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getCase("json/sya.json")))
            .andExpect(status().isCreated());

        verify(pdfServiceClient, never()).generateFromHtml(eq(getTemplate()), anyMap());
        verify(ccdClient, never()).submitForCaseworker(any(), any());
    }

    private PdfWrapper getPdfWrapper() {
        Map<String, Object> placeHolders = captor.getAllValues().get(0);
        return (PdfWrapper) placeHolders.get("PdfWrapper");
    }

    @Test
    public void shouldSendEmailWithPdfWhenDocumentStoreIsDown() throws Exception {
        ReflectionTestUtils.setField(submitAppealService, "sendToDwpFeature", false);

        given(ccdClient.startCaseForCaseworker(any(), anyString())).willReturn(StartEventResponse.builder().build());
        given(ccdClient.submitForCaseworker(any(), any())).willReturn(CaseDetails.builder().id(123456789876L).build());

        given(documentUploadClientApi.upload(eq(DUMMY_OAUTH_2_TOKEN), eq(AUTH_TOKEN), anyString(), any()))
            .willThrow(new RestClientException("Document store is down"));

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getCase("json/sya.json")))
            .andExpect(status().isCreated());

        then(mailSender).should(times(2)).send(message);
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

    private String getCase(String path) {
        String syaCaseJson = path;
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

    private UploadResponse createUploadResponse() {
        UploadResponse response = mock(UploadResponse.class);
        UploadResponse.Embedded embedded = mock(UploadResponse.Embedded.class);
        when(response.getEmbedded()).thenReturn(embedded);
        Document document = createDocument();
        when(embedded.getDocuments()).thenReturn(Collections.singletonList(document));
        return response;
    }

    private Document createDocument() {
        Document document = new Document();
        Document.Links links = new Document.Links();
        Document.Link link = new Document.Link();
        link.href = "some location";
        links.self = link;
        document.links = links;
        return document;
    }

}
