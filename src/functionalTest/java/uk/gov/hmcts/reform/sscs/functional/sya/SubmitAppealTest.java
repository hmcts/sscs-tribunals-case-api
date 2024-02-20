package uk.gov.hmcts.reform.sscs.functional.sya;

import static io.restassured.RestAssured.baseURI;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN_CCD_CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN_CCD_SSCS5;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN_CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN_NO_MRN_DATE_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN_SSCS5;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN_WITH_INTERLOC_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer;

@TestPropertySource(locations = "classpath:config/application_functional.properties")
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
    public void givenValidAppealIsSubmittedFromNonSaveAndReturnRoute_thenCreateValidAppeal() {
        assertSscsCaseIsExpectedResult("validAppeal", ALL_DETAILS_NON_SAVE_AND_RETURN_CCD.getSerializedMessage(), ALL_DETAILS_NON_SAVE_AND_RETURN);
    }

    @Test
    public void givenValidChildSupportAppealIsSubmitted_thenCreateValidAppeal() {
        assertSscsCaseIsExpectedResult("validAppeal", ALL_DETAILS_NON_SAVE_AND_RETURN_CCD_CHILD_SUPPORT.getSerializedMessage(), ALL_DETAILS_NON_SAVE_AND_RETURN_CHILD_SUPPORT);
    }

    @Test
    public void givenValidSscs5AppealIsSubmitted_thenCreateValidAppeal() {
        assertSscsCaseIsExpectedResult("validAppeal", ALL_DETAILS_NON_SAVE_AND_RETURN_CCD_SSCS5.getSerializedMessage(), ALL_DETAILS_NON_SAVE_AND_RETURN_SSCS5);
    }

    @Test
    public void givenIncompleteAppealIsSubmittedFromNonSaveAndReturnRoute_thenCreateIncompleteAppeal() {
        assertSscsCaseIsExpectedResult("incompleteApplication", ALL_DETAILS_NON_SAVE_AND_RETURN_NO_MRN_DATE_CCD.getSerializedMessage(), ALL_DETAILS_NON_SAVE_AND_RETURN);
    }

    @Test
    public void givenNonCompliantAppealIsSubmittedFromNonSaveAndReturnRoute_thenCreateNonCompliantAppeal() {
        assertSscsCaseIsExpectedResult("interlocutoryReviewState", ALL_DETAILS_NON_SAVE_AND_RETURN_WITH_INTERLOC_CCD.getSerializedMessage(), ALL_DETAILS_NON_SAVE_AND_RETURN);
    }


    private void assertSscsCaseIsExpectedResult(String expectedState, String expectedResponse, SyaJsonMessageSerializer jsonMessage) {
        assertCaseIsExpectedResult(jsonMessage.getSerializedMessage(),  expectedState,
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

        assertThatJson(sscsCaseDetails.getData())
            .whenIgnoringPaths(
                "sscsDocument",
                "regionalProcessingCenter.hearingRoute",
                "caseManagementLocation.region",
                "regionalProcessingCenter.epimsId",
                "appeal.appellant.id",
                "appeal.appellant.appointee.id",
                "appeal.rep.id",
                "jointPartyId",
                "correction",
                "correctionBodyContent",
                "bodyContent",
                "correctionGenerateNotice",
                "generateNotice",
                "dateAdded",
                "directionNoticeContent",
                "libertyToApply",
                "libertyToApplyBodyContent",
                "libertyToApplyGenerateNotice",
                "permissionToAppeal",
                "postHearingRequestType",
                "postHearingReviewType",
                "previewDocument",
                "setAside",
                "signedBy",
                "signedRole",
                "statementOfReasons",
                "statementOfReasonsBodyContent",
                "statementOfReasonsGenerateNotice")
            .isEqualTo(changeExpectedFields(expectedResponse, nino, mrnDate));

        assertEquals(expectedState, sscsCaseDetails.getState());

        assertEquals("Joe Bloggs", sscsCaseDetails.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal());
        assertEquals("Joe Bloggs", sscsCaseDetails.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted());
        assertEquals("Joe Bloggs", sscsCaseDetails.getData().getCaseAccessManagementFields().getCaseNamePublic());
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
        Thread.sleep(5000L);

        response = httpRequest.post("/appeals");

        response.then().statusCode(HttpStatus.SC_CONFLICT);
    }
}
