package uk.gov.hmcts.reform.sscs.sya;

import static org.junit.Assert.assertEquals;
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
import java.time.LocalDate;
import java.util.*;
import javax.mail.Session;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.IOUtils;
import org.junit.*;
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
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.Classification;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.callback.AbstractEventIt;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.client.RefDataApi;
import uk.gov.hmcts.reform.sscs.idam.Authorize;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;
import uk.gov.hmcts.reform.sscs.service.SubmitAppealService;

@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_it.properties")
@AutoConfigureMockMvc(addFilters = false)
@RunWith(JUnitParamsRunner.class)
public class SyaEndpointsIt extends AbstractEventIt {

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

    @MockBean
    private RefDataApi refDataApi;

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

    @Captor
    private ArgumentCaptor<CaseDataContent> caseDataCaptor;

    @Before
    public void setup() throws IOException {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());

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

        given(idamClient.getUserInfo(anyString()))
                .willReturn(new UserInfo("16","userId", "dummy@email.com", "test", "test",
                Arrays.asList("caseworker", "citizen")));

        given(refDataApi.courtVenueByEpimsId("token", "authToken", "239985"))
            .willReturn(List.of(CourtVenue.builder()
                .regionId("2")
                .courtTypeId("31")
                .courtStatus("Open")
                .venueName("Ashford")
                .build()));

        UploadResponse uploadResponse = createUploadResponse();
        given(documentUploadClientApi.upload(eq(DUMMY_OAUTH_2_TOKEN), eq(AUTH_TOKEN), eq("sscs"),
            eq(Arrays.asList("caseworker", "citizen")), eq(Classification.RESTRICTED), any())).willReturn(uploadResponse);

    }

    @Test
    @Ignore
    public void givenAValidAppeal_createValidAppealCreatedCase() throws Exception {
        given(ccdClient.startCaseForCaseworker(any(), anyString())).willReturn(StartEventResponse.builder().build());

        given(ccdClient.searchCases(any(), any())).willReturn(SearchResult.builder().cases(Collections.emptyList()).build());

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getCase("json/sya.json")))
            .andExpect(status().isCreated());

        verify(ccdClient).startCaseForCaseworker(any(), eq(VALID_APPEAL_CREATED.getCcdType()));
        verify(ccdClient).submitForCaseworker(any(), caseDataCaptor.capture());

        assertEquals("Ashford", ((SscsCaseData) caseDataCaptor.getValue().getData()).getProcessingVenue());
        assertEquals("698118", ((SscsCaseData) caseDataCaptor.getValue().getData()).getCaseManagementLocation().getBaseLocation());
        assertEquals("2", ((SscsCaseData) caseDataCaptor.getValue().getData()).getCaseManagementLocation().getRegion());
    }

    @Test
    public void givenAValidAppealFromDraft_updateDraftToValidAppealCreatedCase() throws Exception {
        Map<String, Object> appeal = new HashMap<>();
        appeal.put("mrnDetails", Collections.singletonMap("mrnDate", LocalDate.now().toString()));
        appeal.put("appellant", Collections.singletonMap("identity",  Collections.singletonMap("nino", "BB000000B")));
        appeal.put("benefitType", Collections.singletonMap("code", "PIP"));

        Map<String, Object> data = new HashMap<>();
        data.put("appeal", appeal);

        given(ccdClient.readForCaseworker(any(), any())).willReturn(CaseDetails.builder().id(1234567890L).data(data).build());

        given(ccdClient.searchCases(any(), any())).willReturn(SearchResult.builder().cases(Collections.emptyList()).build());

        mockMvc.perform(post("/appeals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getCase("json/sya_with_ccdId.json")))
                .andExpect(status().isCreated());

        verify(ccdClient).startEvent(any(), eq(1234567890L), eq(DRAFT_TO_VALID_APPEAL_CREATED.getCcdType()));
        verify(ccdClient).submitEventForCaseworker(any(), eq(1234567890L), any());
    }

    @Test
    public void givenAValidAppealWithNoMrnDate_createIncompleteCase() throws Exception {
        given(ccdClient.startCaseForCaseworker(any(), anyString())).willReturn(StartEventResponse.builder().build());

        given(ccdClient.searchCases(any(), any())).willReturn(SearchResult.builder().cases(Collections.emptyList()).build());

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getCase("json/syaWithNoMrnDate.json")))
            .andExpect(status().isCreated());

        verify(ccdClient).startCaseForCaseworker(any(), eq(INCOMPLETE_APPLICATION_RECEIVED.getCcdType()));
        verify(ccdClient).submitForCaseworker(any(), any());
        verify(ccdClient, times(0)).startEvent(any(), any(), eq(SEND_TO_DWP.getCcdType()));
    }

    @Test
    public void givenAValidAppealWithNoMrnDateFromDraft_updateDraftToIncompleteCase() throws Exception {
        Map<String, Object> appeal = new HashMap<>();
        appeal.put("mrnDetails", Collections.singletonMap("mrnDate", null));
        appeal.put("appellant", Collections.singletonMap("identity",  Collections.singletonMap("nino", "BB000000B")));
        appeal.put("benefitType", Collections.singletonMap("code", "PIP"));

        Map<String, Object> data = new HashMap<>();
        data.put("appeal", appeal);

        given(ccdClient.readForCaseworker(any(), any())).willReturn(CaseDetails.builder().id(1234567890L).data(data).build());

        given(ccdClient.searchCases(any(), any())).willReturn(SearchResult.builder().cases(Collections.emptyList()).build());

        mockMvc.perform(post("/appeals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getCase("json/sya_with_ccdId_noMrnDate.json")))
                .andExpect(status().isCreated());

        verify(ccdClient).startEvent(any(), eq(1234567890L), eq(DRAFT_TO_INCOMPLETE_APPLICATION.getCcdType()));
        verify(ccdClient).submitEventForCaseworker(any(), eq(1234567890L), any());
    }

    @Test
    public void givenAValidAppealWithMrnDateMoreThan13MonthsAgo_createNonCompliantCase() throws Exception {
        given(ccdClient.startCaseForCaseworker(any(), anyString())).willReturn(StartEventResponse.builder().build());

        given(ccdClient.searchCases(any(), any())).willReturn(SearchResult.builder().cases(Collections.emptyList()).build());

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getCase("json/syaWithMrnDateMoreThan13Months.json")))
            .andExpect(status().isCreated());

        verify(ccdClient).startCaseForCaseworker(any(), eq(NON_COMPLIANT.getCcdType()));
        verify(ccdClient).submitForCaseworker(any(), any());
        verify(ccdClient, times(0)).startEvent(any(), any(), eq(SEND_TO_DWP.getCcdType()));
    }

    @Test
    public void givenAValidAppealWithMrnDateMoreThan13MonthsAgoFromDraft_updateDraftToNonCompliantCase() throws Exception {
        Map<String, Object> appeal = new HashMap<>();
        appeal.put("mrnDetails", Collections.singletonMap("mrnDate", LocalDate.now().minusMonths(13).minusDays(1).toString()));
        appeal.put("appellant", Collections.singletonMap("identity",  Collections.singletonMap("nino", "BB000000B")));
        appeal.put("benefitType", Collections.singletonMap("code", "PIP"));

        Map<String, Object> data = new HashMap<>();
        data.put("appeal", appeal);

        given(ccdClient.readForCaseworker(any(), any())).willReturn(CaseDetails.builder().id(1234567890L).data(data).build());

        given(ccdClient.searchCases(any(), any())).willReturn(SearchResult.builder().cases(Collections.emptyList()).build());

        mockMvc.perform(post("/appeals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getCase("json/sya_with_ccdId_mrnDateMoreThan13Months.json")))
                .andExpect(status().isCreated());

        verify(ccdClient).startEvent(any(), eq(1234567890L), eq(DRAFT_TO_NON_COMPLIANT.getCcdType()));
        verify(ccdClient).submitEventForCaseworker(any(), eq(1234567890L), any());
    }

    @Test
    public void shouldNotAddDuplicateCaseToCcdAndShouldNotGeneratePdf() throws Exception {
        CaseDetails caseDetails = CaseDetails.builder().id(1L).data(new HashMap<>()).build();

        given(ccdClient.searchCases(any(), any())).willReturn(SearchResult.builder().cases(Collections.singletonList(caseDetails)).build());

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is4xxClientError());

        verify(pdfServiceClient, never()).generateFromHtml(eq(getTemplate()), anyMap());
        verify(ccdClient, never()).submitForCaseworker(any(), any());
    }

    @Test
    @Ignore
    @Parameters({"PIP, DWP PIP (1), Newcastle",
        "ESA, Inverness DRT, Inverness DRT",
        "UC,, Universal Credit",
        "ESA, Coatbridge Benefit Centre,Coatbridge Benefit Centre",
        "DLA, Disability Benefit Centre 4, DLA Child/Adult",
        "carersAllowance,, Carers Allowance",
        "attendanceAllowance, The Pension Service 11, Attendance Allowance",
        "bereavementBenefit,, Bereavement Benefit"})
    public void givenAValidAppealForBenefitType_createValidAppealCreatedCaseWithDwpRegionalCentre(String benefitTypeCode, String dwpIssuingOffice, String expectedDwpRegionalCentre) throws Exception {
        given(ccdClient.startCaseForCaseworker(any(), anyString())).willReturn(StartEventResponse.builder().build());

        given(ccdClient.searchCases(any(), any())).willReturn(SearchResult.builder().cases(Collections.emptyList()).build());

        setJsonAndReplace("json/sya_with_benefit_type.json", Arrays.asList("BENEFIT_TYPE", "ISSUING_OFFICE"), Arrays.asList(benefitTypeCode, dwpIssuingOffice));

        mockMvc.perform(post("/appeals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        verify(ccdClient).startCaseForCaseworker(any(), eq(VALID_APPEAL_CREATED.getCcdType()));
        verify(ccdClient).submitForCaseworker(any(), caseDataCaptor.capture());

        assertEquals(benefitTypeCode, ((SscsCaseData) caseDataCaptor.getValue().getData()).getAppeal().getBenefitType().getCode());
        assertEquals(expectedDwpRegionalCentre, ((SscsCaseData) caseDataCaptor.getValue().getData()).getDwpRegionalCentre());
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

}
