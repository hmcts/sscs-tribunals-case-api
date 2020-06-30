package uk.gov.hmcts.reform.sscs.sya;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.createUploadResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import javax.mail.Session;
import junitparams.JUnitParamsRunner;
import org.apache.commons.io.IOUtils;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.Classification;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.Authorize;
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
    private IdamClient idamClient;

    @MockBean
    @Qualifier("authTokenGenerator")
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private PDFServiceClient pdfServiceClient;

    @MockBean
    private DocumentUploadClientApi documentUploadClientApi;

    @Captor
    private ArgumentCaptor<Map<String, Object>> captor;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubmitAppealService submitAppealService;

    private ObjectMapper mapper;

    private Session session = Session.getInstance(new Properties());

    @Value("${appellant.appeal.html.template.path}")
    private String templateName;

    private SyaCaseWrapper caseWrapper;

    @Before
    public void setup() throws IOException {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        caseWrapper = getCaseWrapper();

        given(pdfServiceClient.generateFromHtml(eq(getTemplate()), captor.capture()))
            .willReturn(PDF.getBytes());

        given(ccdClient.readForCaseworker(any(), any())).willReturn(null);
        given(ccdClient.startEvent(any(), any(), anyString())).willReturn(StartEventResponse.builder().eventId("12345").build());
        given(ccdClient.submitEventForCaseworker(any(), any(), any())).willReturn(CaseDetails.builder()
                .id(123456789876L)
                .data(new HashMap<>()).build());
        given(ccdClient.submitForCaseworker(any(), any())).willReturn(CaseDetails.builder()
                .id(123456789876L)
                .data(new HashMap<>())
                .build());

        Authorize authorize = new Authorize("redirectUrl/", "code", "token");

        given(idamClient.getAccessToken(anyString(), anyString())).willReturn(authorize.getAccessToken());

        given(idamClient.getAccessToken(anyString(), anyString())).willReturn(authorize.getAccessToken());

        given(authTokenGenerator.generate()).willReturn("authToken");

        given(idamClient.getUserDetails(anyString()))
                .willReturn(new uk.gov.hmcts.reform.idam.client.models.UserDetails("userId", "dummy@email.com", "test", "test",
                Arrays.asList("caseworker", "citizen")));

        UploadResponse uploadResponse = createUploadResponse();
        given(documentUploadClientApi.upload(eq(DUMMY_OAUTH_2_TOKEN), eq(AUTH_TOKEN), eq("sscs"),
            eq(Arrays.asList("caseworker", "citizen")), eq(Classification.RESTRICTED), any())).willReturn(uploadResponse);

    }

    @Test
    public void givenAValidAppeal_createAppealCreatedCase() throws Exception {
        given(ccdClient.startCaseForCaseworker(any(), anyString())).willReturn(StartEventResponse.builder().build());

        given(ccdClient.searchForCaseworker(any(), any())).willReturn(Collections.emptyList());

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getCase("json/sya.json")))
            .andExpect(status().isCreated());

        verify(ccdClient).startCaseForCaseworker(any(), eq(VALID_APPEAL_CREATED.getCcdType()));
        verify(ccdClient).submitForCaseworker(any(), any());
    }

    @Test
    public void givenAValidAppealWithNoMrnDate_createIncompleteAppealCase() throws Exception {
        given(ccdClient.startCaseForCaseworker(any(), anyString())).willReturn(StartEventResponse.builder().build());

        given(ccdClient.searchForCaseworker(any(), any())).willReturn(Collections.emptyList());

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getCase("json/syaWithNoMrnDate.json")))
            .andExpect(status().isCreated());

        verify(ccdClient).startCaseForCaseworker(any(), eq(INCOMPLETE_APPLICATION_RECEIVED.getCcdType()));
        verify(ccdClient).submitForCaseworker(any(), any());
        verify(ccdClient, times(0)).startEvent(any(), any(), eq(SEND_TO_DWP.getCcdType()));
    }

    @Test
    public void givenAValidAppealWithMrnDateMoreThan13MonthsAgo_createNonCompliantAppealCase() throws Exception {
        given(ccdClient.startCaseForCaseworker(any(), anyString())).willReturn(StartEventResponse.builder().build());

        given(ccdClient.searchForCaseworker(any(), any())).willReturn(Collections.emptyList());

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getCase("json/syaWithMrnDateMoreThan13Months.json")))
            .andExpect(status().isCreated());

        verify(ccdClient).startCaseForCaseworker(any(), eq(NON_COMPLIANT.getCcdType()));
        verify(ccdClient).submitForCaseworker(any(), any());
        verify(ccdClient, times(0)).startEvent(any(), any(), eq(SEND_TO_DWP.getCcdType()));
    }

    @Test
    public void shouldNotAddDuplicateCaseToCcdAndShouldNotGeneratePdf() throws Exception {
        CaseDetails caseDetails = CaseDetails.builder().id(1L).data(new HashMap<>()).build();

        given(ccdClient.searchForCaseworker(any(), any())).willReturn(Collections.singletonList(caseDetails));

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getCase("json/sya.json")))
            .andExpect(status().isCreated());

        verify(pdfServiceClient, never()).generateFromHtml(eq(getTemplate()), anyMap());
        verify(ccdClient, never()).submitForCaseworker(any(), any());
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

}
