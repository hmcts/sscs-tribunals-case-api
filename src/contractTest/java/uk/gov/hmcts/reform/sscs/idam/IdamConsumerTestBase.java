package uk.gov.hmcts.reform.sscs.idam;

import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import org.apache.http.client.fluent.Executor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.idam.client.IdamApi;

@ExtendWith(PactConsumerTestExt.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@PactTestFor(providerName = "idamApi_oidc", port = "8891")
@PactDirectory("pacts")
@SpringBootTest({
    "idam.api.url : localhost:8891"
})
@TestPropertySource(locations = {"/config/application_contract.properties"})
public abstract class IdamConsumerTestBase {

    public static final int SLEEP_TIME = 2000;
    protected static final String SOME_AUTHORIZATION_TOKEN = "Bearer UserAuthToken";
    @Autowired
    protected IdamApi idamApi;
    @Value("${idam.oauth2.user.email}")
    protected String caseworkerUsername;
    @Value("${idam.oauth2.user.password}")
    protected String caseworkerPwd;
    @Value("${idam.client.secret}")
    protected String clientSecret;

    @BeforeEach
    public void prepareTest() throws Exception {
        Thread.sleep(SLEEP_TIME);
    }

    @AfterEach
    void teardown() {
        Executor.closeIdleConnections();
    }

}
