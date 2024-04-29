package uk.gov.hmcts.reform.sscs.controllers;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.sscs.TribunalsCaseApiApplication;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.servicebus.TopicPublisher;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {TribunalsCaseApiApplication.class})
@EnableFeignClients(basePackageClasses = {IdamApi.class})
public class CcdCallbackOrchestratorControllerTest {

    @MockBean
    private TopicPublisher topicPublisher;

    @MockBean
    private CcdRequestDetails ccdRequestDetails;

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @Autowired
    private IdamService idamService;

    @Autowired
    private TestRestTemplate restTemplate;

    private IdamTokens idamTokens;

    @Before
    public void setUp() {
        idamTokens = idamService.getIdamTokens();
    }

    @Test
    public void canAuthenticateAndPlaceJsonBodyInAMessageQueue() throws Exception {

        HttpHeaders headers = new HttpHeaders();
        headers.set("ServiceAuthorization", idamTokens.getServiceAuthorization());
        String jsonCallbackForTest = getJsonCallbackForTest("validAppealCreatedCallback.json");
        HttpEntity<String> request = new HttpEntity<String>(jsonCallbackForTest, headers);
        ResponseEntity<String> response = restTemplate.exchange("/send", HttpMethod.POST, request, String.class);

        assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    }

    public String getJsonCallbackForTest(String path) throws IOException {
        return FileUtils.readFileToString(new ClassPathResource(path).getFile(), StandardCharsets.UTF_8.name());
    }
}
