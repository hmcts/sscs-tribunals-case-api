package uk.gov.hmcts.reform.sscs.bulkscan;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.assertj.core.util.Strings.concat;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.TestConstants.SERVICE_AUTHORIZATION_HEADER_KEY;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.TestConstants.SERVICE_AUTH_TOKEN;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.TestConstants.USER_AUTH_TOKEN;
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.TestConstants.USER_ID;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.TestSocketUtils;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsQueryBuilder;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.RefDataService;
import uk.gov.hmcts.reform.sscs.service.VenueService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@TestPropertySource(locations = "classpath:config/application_it.properties")
public abstract class BaseTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @LocalServerPort
    protected int randomServerPort;

    @MockitoBean
    protected ServiceAuthorisationApi serviceAuthorisationApi;

    @MockitoBean
    protected IdamService idamService;

    @MockitoBean
    protected RefDataService refDataService;

    @MockitoBean
    protected VenueService venueService;

    @Rule
    public WireMockRule ccdServer;

    protected static int wiremockPort = 0;

    protected String baseUrl;

    protected IdamTokens idamTokens;

    static {
        wiremockPort = TestSocketUtils.findAvailableTcpPort();
        System.setProperty("core_case_data.api.url", "http://localhost:" + wiremockPort);
    }

    @Before
    public void setUp() {
        ccdServer = new WireMockRule(wiremockPort);
        ccdServer.start();
        idamTokens = IdamTokens.builder().idamOauth2Token(USER_AUTH_TOKEN).serviceAuthorization(SERVICE_AUTH_TOKEN).userId(USER_ID).build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @After
    public void tearDown() {
        ccdServer.stop();
    }

    protected void findCaseByForCaseworker(String mrnDate, String benefitType) {
        SearchSourceBuilder query = SscsQueryBuilder.findCcdCaseByNinoAndBenefitTypeAndMrnDateQuery("BB000000B", benefitType, mrnDate);

        ccdServer.stubFor(post(concat(uk.gov.hmcts.reform.sscs.bulkscan.helper.TestConstants.FIND_CASE_EVENT_URL)).atPriority(1)
            .withHeader(AUTHORIZATION, equalTo(USER_AUTH_TOKEN))
            .withHeader(SERVICE_AUTHORIZATION_HEADER_KEY, equalTo(SERVICE_AUTH_TOKEN))
            .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
            .withRequestBody(containing(query.toString()))
            .willReturn(aResponse()
                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .withStatus(200)
                .withBody("{\"total\":0,\"cases\":[]}")));
    }

    protected void checkForLinkedCases() {
        SearchSourceBuilder query = SscsQueryBuilder.findCaseBySingleField("data.appeal.appellant.identity.nino", "BB000000B");

        ccdServer.stubFor(post(concat(uk.gov.hmcts.reform.sscs.bulkscan.helper.TestConstants.FIND_CASE_EVENT_URL)).atPriority(1)
            .withHeader(AUTHORIZATION, equalTo(idamTokens.getIdamOauth2Token()))
            .withHeader(SERVICE_AUTHORIZATION_HEADER_KEY, equalTo(idamTokens.getServiceAuthorization()))
            .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
            .withRequestBody(containing(query.toString()))
            .willReturn(aResponse()
                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .withStatus(200)
                .withBody("{\"total\":0,\"cases\":[]}")));
    }

}
