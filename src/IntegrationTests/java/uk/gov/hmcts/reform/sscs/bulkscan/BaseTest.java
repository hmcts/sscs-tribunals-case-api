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
import static uk.gov.hmcts.reform.sscs.bulkscan.helper.TestConstants.*;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.TestSocketUtils;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsQueryBuilder;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.RefDataService;
import uk.gov.hmcts.reform.sscs.service.VenueService;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock
@TestPropertySource(locations = "classpath:application_it.yaml")
public abstract class BaseTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @LocalServerPort
    protected int randomServerPort;

    @MockitoBean
    protected AuthTokenValidator authTokenValidator;

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
        DateTimeUtils.setCurrentMillisFixed(1542820369000L);
        idamTokens = IdamTokens.builder().idamOauth2Token(USER_AUTH_TOKEN).serviceAuthorization(SERVICE_AUTH_TOKEN).userId(USER_ID).build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @After
    public void tearDown() {
        ccdServer.stop();
    }

    protected void findCaseByForCaseworker(String eventUrl, String mrnDate, String benefitType) {
        SearchSourceBuilder query = SscsQueryBuilder.findCcdCaseByNinoAndBenefitTypeAndMrnDateQuery("BB000000B", benefitType, mrnDate);

        ccdServer.stubFor(post(concat(eventUrl)).atPriority(1)
            .withHeader(AUTHORIZATION, equalTo(USER_AUTH_TOKEN))
            .withHeader(SERVICE_AUTHORIZATION_HEADER_KEY, equalTo(SERVICE_AUTH_TOKEN))
            .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
            .withRequestBody(containing(query.toString()))
            .willReturn(aResponse()
                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .withStatus(200)
                .withBody("{\"total\":0,\"cases\":[]}")));
    }

    protected void checkForLinkedCases(String eventUrl) {
        SearchSourceBuilder query = SscsQueryBuilder.findCaseBySingleField("data.appeal.appellant.identity.nino", "BB000000B");

        ccdServer.stubFor(post(concat(eventUrl)).atPriority(1)
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
