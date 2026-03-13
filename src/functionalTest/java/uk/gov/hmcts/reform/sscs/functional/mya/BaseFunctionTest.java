package uk.gov.hmcts.reform.sscs.functional.mya;

import static java.lang.Long.valueOf;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.TribunalsCaseApiApplication;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TribunalsCaseApiApplication.class, CitizenIdamService.class})
@Slf4j
public abstract class BaseFunctionTest {
    protected final String baseUrl = System.getenv("TEST_URL") != null ? System.getenv("TEST_URL") : "http://localhost:8008";

    private CloseableHttpClient client;
    private HttpClient myaClient;

    protected SscsMyaBackendRequests sscsMyaBackendRequests;

    @Autowired
    protected IdamService idamService;
    @Autowired
    protected CitizenIdamService citizenIdamService;
    @Autowired
    protected CcdService ccdService;
    @Autowired
    protected EvidenceManagementService evidenceManagementService;

    @Value("${idam.url}")
    private String idamApiUrl;
    protected IdamTestApiRequests idamTestApiRequests;

    @BeforeEach
    public void setUp() throws Exception {
        myaClient = buildClient("USE_MYA_PROXY");
        client = buildClient("USE_BACKEND_PROXY");
        sscsMyaBackendRequests = new SscsMyaBackendRequests(idamService, citizenIdamService, baseUrl, client, evidenceManagementService);
        idamTestApiRequests = new IdamTestApiRequests(myaClient, idamApiUrl);
    }

    protected String createRandomEmail() {
        int randomNumber = (int) (Math.random() * 10000000);
        String emailAddress = "test" + randomNumber + "@hmcts.net";
        log.info("emailAddress " + emailAddress);
        return emailAddress;
    }

    protected CreatedCcdCase createCase() throws IOException {
        String emailAddress = createRandomEmail();

        CreatedCcdCase createdCcdCase = sscsMyaBackendRequests.createOralCase(emailAddress);

        return createdCcdCase;
    }

    protected CreatedCcdCase createCcdCase(String emailAddress) throws IOException {
        CreatedCcdCase createdCcdCase = sscsMyaBackendRequests.createOralCase(emailAddress);
        System.out.println("Case id " + createdCcdCase.getCaseId());
        return createdCcdCase;
    }

    private CloseableHttpClient buildClient(String proxySystemProperty) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        SSLConnectionSocketFactory sslsf =
                new SSLConnectionSocketFactory(builder.build(), NoopHostnameVerifier.INSTANCE);

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                .setSSLSocketFactory(sslsf);
        if (System.getenv(proxySystemProperty) != null) {
            httpClientBuilder.setProxy(new HttpHost("proxyout.reform.hmcts.net", 8080));
        }
        return httpClientBuilder.build();
    }

    protected SscsCaseDetails getCaseDetails(String caseId) {
        return ccdService.getByCaseId(valueOf(caseId), idamService.getIdamTokens());
    }

    public static void waitUntil(Supplier<Boolean> condition, long timeoutInSeconds, String timeoutMessage) throws InterruptedException {
        long timeout = timeoutInSeconds * 1000L * 1000000L;
        long startTime = System.nanoTime();
        while (true) {
            if (condition.get()) {
                break;
            } else if (System.nanoTime() - startTime >= timeout) {
                throw new RuntimeException(timeoutMessage);
            }
            Thread.sleep(1000L);
        }
    }
}
