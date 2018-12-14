package uk.gov.hmcts.reform.sscs.functional.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.sscs.model.NotificationEventType.CREATE_APPEAL_PDF;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.restassured.RestAssured;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.NotificationEventType;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CreateAppealPdfTest {

    public static final String DEFAULT_TEST_URL = "http://localhost:8082";

    @Autowired
    private CcdService ccdService;

    @Autowired
    private CcdClient ccdClient;

    @Autowired
    private SscsCcdConvertService sscsCcdConvertService;

    @Autowired
    IdamService idamService;

    @Autowired
    ObjectMapper objectMapper;

    IdamTokens idamTokens;

    @Before
    public void setup() {
        idamTokens = idamService.getIdamTokens();
    }

    @Test
    public void shouldHandleCreateAppealPdfEvent() throws IOException, InterruptedException {

        SscsCaseDetails caseDetails = ccdService.createCase(buildCaseData(), idamTokens);
        assertNull(caseDetails.getData().getSscsDocument());

        sendEvent(toEvent(caseDetails, CREATE_APPEAL_PDF));

        SscsCaseDetails updatedCaseDetails = findCaseInCcd(caseDetails.getId());

        assertNotNull(updatedCaseDetails.getData().getSscsDocument());
        assertEquals(1, updatedCaseDetails.getData().getSscsDocument().size());
    }

    private SscsCaseData buildCaseData() {
        SscsCaseData caseData = CaseDataUtils.buildCaseData();
        Appointee appointee = Appointee.builder()
                .name(Name.builder()
                        .firstName("Oscar")
                        .lastName("Giles")
                        .build())
                .address(caseData.getAppeal().getAppellant().getAddress())
                .build();
        caseData.getAppeal().getAppellant().setAppointee(appointee);
        return caseData;
    }

    private String toEvent(SscsCaseDetails caseDetails, NotificationEventType eventType) throws JsonProcessingException {
        return objectMapper.writeValueAsString(
                ImmutableMap.of(
                        "case_details", caseDetails,
                        "event_id", eventType.getId())
        );
    }

    private SscsCaseDetails findCaseInCcd(Long caseId) throws InterruptedException {
        SscsCaseDetails updatedCaseDetails = null;
        for (int i = 0; i < 10; i++) {
            updatedCaseDetails = findById(caseId);
            if (updatedCaseDetails.getData().getSscsDocument() != null) {
                break;
            }
            // Wait for the event to be processed
            Thread.sleep(1000);
        }

        return updatedCaseDetails;
    }

    private SscsCaseDetails findById(Long caseId) {
        return sscsCcdConvertService.getCaseDetails(ccdClient.readForCaseworker(idamTokens, caseId));
    }

    public void sendEvent(String json) {
        final String tribunalApiUrl = baseUrl() + "/send";

        RestAssured.useRelaxedHTTPSValidation();

        RestAssured
                .given()
                .header("ServiceAuthorization", "" + idamTokens.getServiceAuthorization())
                .contentType("application/json")
                .body(json)
                .when()
                .post(tribunalApiUrl)
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    private String baseUrl() {
        String testUrl = System.getenv("TEST_URL");
        return testUrl != null ? testUrl : DEFAULT_TEST_URL;
    }
}
