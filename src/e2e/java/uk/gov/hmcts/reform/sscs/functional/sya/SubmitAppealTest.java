package uk.gov.hmcts.reform.sscs.functional.sya;

import static io.restassured.RestAssured.baseURI;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_DWP_REGIONAL_CENTRE;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer;

@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
@RunWith(JUnitParamsRunner.class)
@Slf4j
public class SubmitAppealTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Value("${test-url}")
    private String testUrl;

    @Autowired
    private SubmitHelper submitHelper;

    @Autowired
    private IdamService idamService;

    private IdamTokens idamTokens;

    @Before
    public void setup() {
        baseURI = testUrl;
        idamTokens = idamService.getIdamTokens();
        RestAssured.useRelaxedHTTPSValidation();
    }


    @Test
    @Parameters({
        "PIP,DWP PIP (1),Newcastle",
        "PIP,DWP PIP (2),Glasgow",
        "ESA,Inverness DRT,Inverness DRT",
        "ESA,Coatbridge Benefit Centre,Coatbridge Benefit Centre",
        "UC,,Universal Credit",
        "carersAllowance,,Tyneview Park DRT"
    })
    public void givenAppealIsSubmitted_shouldSetDwpRegionalCentre(String benefitCode, String dwpIssuingOffice,
                                                                  String expectedDwpRegionalCentre) {
        String body = ALL_DETAILS_DWP_REGIONAL_CENTRE.getSerializedMessage();
        String nino = submitHelper.getRandomNino();
        body = submitHelper.setNino(body, nino);
        body = submitHelper.setLatestMrnDate(body, LocalDate.now());
        body = submitHelper.setDwpIssuingOffice(body, dwpIssuingOffice);
        body = submitHelper.setBenefitCode(body, benefitCode);

        Response response = RestAssured.given()
            .body(body)
            .header("Content-Type", "application/json")
            .post("/appeals");

        response.then().statusCode(HttpStatus.SC_CREATED);
        long ccdId = getCcdIdFromLocationHeader(response.getHeader("Location"));
        SscsCaseDetails sscsCaseDetails = submitHelper.findCaseInCcd(ccdId, idamTokens);

        assertEquals(expectedDwpRegionalCentre, sscsCaseDetails.getData().getDwpRegionalCentre());
    }

    public static long getCcdIdFromLocationHeader(String location) {
        return Long.parseLong(location.substring(location.lastIndexOf("/") + 1));
    }

    @Test
    @Parameters({
        "ALL_DETAILS, incompleteApplication",
        "ALL_DETAILS, interlocutoryReviewState",
        "ALL_DETAILS, validAppeal"
    })
    public void appealShouldBeSavedViaSya(SyaJsonMessageSerializer syaJsonMessageSerializer, String expectedState) {

        String body = syaJsonMessageSerializer.getSerializedMessage();
        String nino = submitHelper.getRandomNino();
        body = submitHelper.setNino(body, nino);

        LocalDate now = LocalDate.now();
        LocalDate interlocutoryReviewDate = now.minusMonths(13).minusDays(1);
        LocalDate mrnDate = expectedState.equals("interlocutoryReviewState") ? interlocutoryReviewDate :
            expectedState.equals("incompleteApplication") ? null : now;

        body = submitHelper.setLatestMrnDate(body, mrnDate);

        SyaCaseWrapper wrapper = syaJsonMessageSerializer.getDeserializeMessage();

        wrapper.getAppellant().setNino(nino);
        wrapper.getMrn().setDate(mrnDate);

        RegionalProcessingCenter rpc = getRegionalProcessingCenter();

        Appeal expected = convertSyaToCcdCaseData(wrapper, rpc.getName(), rpc).getAppeal();

        Response response = RestAssured.given()
            .body(body)
            .header("Content-Type", "application/json")
            .post("/appeals");

        response.then().statusCode(HttpStatus.SC_CREATED);

        final Long id = getCcdIdFromLocationHeader(response.getHeader("Location"));
        SscsCaseDetails sscsCaseDetails = submitHelper.findCaseInCcd(id, idamTokens);

        if (expected.getAppellant().getAppointee() == null) {
            sscsCaseDetails.getData().getAppeal().getAppellant().setAppointee(null);
        }

        log.info(String.format("SYA created with CCD ID %s", id));
        assertEquals(expected, sscsCaseDetails.getData().getAppeal());
        assertEquals(expectedState, sscsCaseDetails.getState());
    }

    @Test
    @Parameters({"ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS, validAppeal"})
    public void appealShouldCreateDuplicateAndLinked(SyaJsonMessageSerializer syaJsonMessageSerializer, String expectedState) throws InterruptedException {
        String body = syaJsonMessageSerializer.getSerializedMessage();
        String nino = submitHelper.getRandomNino();

        body = submitHelper.setNino(body, nino);

        LocalDate mrnDate = LocalDate.now();
        body = submitHelper.setLatestMrnDate(body, mrnDate);

        SyaCaseWrapper wrapper = syaJsonMessageSerializer.getDeserializeMessage();
        wrapper.getAppellant().setNino(nino);

        RegionalProcessingCenter rpc = getRegionalProcessingCenter();

        RequestSpecification httpRequest = RestAssured.given()
            .body(body)
            .header("Content-Type", "application/json");

        Response response = httpRequest.post("/appeals");

        response.then().statusCode(HttpStatus.SC_CREATED);

        final Long id = getCcdIdFromLocationHeader(response.getHeader("Location"));
        SscsCaseDetails sscsCaseDetails = submitHelper.findCaseInCcd(id, idamTokens);

        log.info(String.format("SYA created with CCD ID %s", id));
        assertEquals(expectedState, sscsCaseDetails.getState());

        //create a case with different mrn date
        body = syaJsonMessageSerializer.getSerializedMessage();
        body = submitHelper.setNino(body, nino);

        mrnDate = LocalDate.now().minusMonths(12);
        body = submitHelper.setLatestMrnDate(body, mrnDate);

        httpRequest = RestAssured.given()
            .body(body)
            .header("Content-Type", "application/json");

        // Give ES time to index
        Thread.sleep(3000L);

        response = httpRequest.post("/appeals");

        response.then().statusCode(HttpStatus.SC_CREATED);

        final Long secondCaseId = getCcdIdFromLocationHeader(response.getHeader("Location"));
        log.info("Duplicate case " + secondCaseId);
        SscsCaseDetails secondCaseSscsCaseDetails = submitHelper.findCaseInCcd(secondCaseId, idamTokens);

        if (secondCaseSscsCaseDetails.getData().getAssociatedCase() == null) {
            //Give time for evidence share to create associated case link
            Thread.sleep(5000L);
            secondCaseSscsCaseDetails = submitHelper.findCaseInCcd(secondCaseId, idamTokens);
        }

        log.info("Duplicate case " + secondCaseSscsCaseDetails.getId() + " has been found");

        assertEquals(1, secondCaseSscsCaseDetails.getData().getAssociatedCase().size());
        assertEquals("Yes", secondCaseSscsCaseDetails.getData().getLinkedCasesBoolean());
        log.info(secondCaseSscsCaseDetails.toString());

        // check duplicate returns 409
        httpRequest = RestAssured.given()
                .body(body)
                .header("Content-Type", "application/json");

        // Give ES time to index
        Thread.sleep(2000L);

        response = httpRequest.post("/appeals");

        response.then().statusCode(HttpStatus.SC_CONFLICT);

    }
}
