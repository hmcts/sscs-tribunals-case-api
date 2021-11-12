package uk.gov.hmcts.reform.sscs.functional.sya;

import static io.restassured.RestAssured.baseURI;
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static net.javacrumbs.jsonunit.JsonAssert.whenIgnoringPaths;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.*;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
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

    public static long getCcdIdFromLocationHeader(String location) {
        return Long.parseLong(location.substring(location.lastIndexOf("/") + 1));
    }

    @Test
    public void givenValidAppealIsSubmittedFromNonSaveAndReturnRoute_thenCreateValidAppeal() throws InterruptedException {
        assertSscsCaseIsExpectedResult("validAppeal", ALL_DETAILS_NON_SAVE_AND_RETURN_CCD.getSerializedMessage());
    }

    @Test
    public void givenValidChildSupportAppealIsSubmittedFromNonSaveAndReturnRoute_thenCreateValidAppeal() throws InterruptedException {
        assertSscs2CaseIsExpectedResult("validAppeal", ALL_DETAILS_NON_SAVE_AND_RETURN_CCD_CHILD_SUPPORT.getSerializedMessage());
    }

    @Test
    public void givenIncompleteAppealIsSubmittedFromNonSaveAndReturnRoute_thenCreateIncompleteAppeal() throws InterruptedException {
        assertSscsCaseIsExpectedResult("incompleteApplication", ALL_DETAILS_NON_SAVE_AND_RETURN_NO_MRN_DATE_CCD.getSerializedMessage());
    }

    @Test
    public void givenNonCompliantAppealIsSubmittedFromNonSaveAndReturnRoute_thenCreateNonCompliantAppeal() throws InterruptedException {
        assertSscsCaseIsExpectedResult("interlocutoryReviewState", ALL_DETAILS_NON_SAVE_AND_RETURN_WITH_INTERLOC_CCD.getSerializedMessage());
    }

    private void assertSscs2CaseIsExpectedResult(String expectedState, String expectedResponse) {
        assertCaseIsExpectedResult(ALL_DETAILS_NON_SAVE_AND_RETURN_CHILD_SUPPORT.getSerializedMessage(),  expectedState,
                expectedResponse);
    }

    private void assertSscsCaseIsExpectedResult(String expectedState, String expectedResponse) {
        assertCaseIsExpectedResult(ALL_DETAILS_NON_SAVE_AND_RETURN.getSerializedMessage(),  expectedState,
                expectedResponse);
    }

    private void assertCaseIsExpectedResult(String expectedBody, String expectedState, String expectedResponse) {
        LocalDate now = LocalDate.now();
        LocalDate interlocutoryReviewDate = now.minusMonths(13).minusDays(1);
        LocalDate mrnDate = expectedState.equals("interlocutoryReviewState") ? interlocutoryReviewDate :
                expectedState.equals("incompleteApplication") ? null : now;
        String nino = submitHelper.getRandomNino();

        expectedBody = submitHelper.setNino(expectedBody, nino);
        expectedBody = submitHelper.setLatestMrnDate(expectedBody, mrnDate);

        Response response = RestAssured.given()
                .body(expectedBody)
                .header("Content-Type", "application/json")
                .post("/appeals");

        response.then().statusCode(HttpStatus.SC_CREATED);

        final Long id = getCcdIdFromLocationHeader(response.getHeader("Location"));

        SscsCaseDetails sscsCaseDetails = submitHelper.findCaseInCcd(id, idamTokens);

        log.info(String.format("SYA created with CCD ID %s", id));

        assertJsonEquals(changeExpectedFields(expectedResponse, nino, mrnDate), sscsCaseDetails.getData(), whenIgnoringPaths("sscsDocument"));

        assertEquals(expectedState, sscsCaseDetails.getState());
    }

    private String changeExpectedFields(String serializedMessage, String nino, LocalDate mrnDate) {
        serializedMessage = serializedMessage.replace("AB877533C", nino);
        serializedMessage = serializedMessage.replace("2021-04-13", LocalDate.now().toString());

        if (mrnDate != null) {
            serializedMessage = serializedMessage.replace("2018-02-01", mrnDate.toString());
        }


        return serializedMessage;
    }

    @Test
    public void appealShouldCreateDuplicateAndLinked() throws InterruptedException {
        SyaJsonMessageSerializer syaJsonMessageSerializer = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS;
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
        assertEquals("validAppeal", sscsCaseDetails.getState());

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
