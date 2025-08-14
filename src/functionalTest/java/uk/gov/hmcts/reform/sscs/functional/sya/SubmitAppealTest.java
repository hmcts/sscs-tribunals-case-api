package uk.gov.hmcts.reform.sscs.functional.sya;

import static io.restassured.RestAssured.baseURI;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseDataV2;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
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

    @Autowired
    private CcdService ccdService;

    //@Autowired
    //private SubmitAppealService submitAppealService;

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
                "dateSentToDwp",
                "directionNoticeContent",
                "dwpDueDate",
                "hmctsDwpState",
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
                "statementOfReasonsGenerateNotice",
                "preWorkAllocation")
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
        //String nino = submitHelper.getRandomNino();
        String nino = "Z2ZZNCIWW";
        log.info("Random NINO: {}", nino);
        LocalDate mrnDate = LocalDate.now();
        log.info("MRN date: {}",mrnDate);

        SyaCaseWrapper wrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS.getDeserializeMessage();
        wrapper.getAppellant().setNino(nino);
        wrapper.getMrn().setDate(mrnDate);

        SscsCaseData caseData = convertSyaToCcdCaseDataV2(wrapper, true, SscsCaseData.builder().build());
        log.info("Appeal created: {}", caseData.getAppeal());
        SscsCaseDetails firstCaseDetails = ccdService.createCase(
            caseData,
            VALID_APPEAL_CREATED.getCcdType(),
            "Appeal created summary",
            "Appeal created description",
            idamTokens);

        log.info("First SYA case created with CCD ID {}", firstCaseDetails.getId());
        assertEquals("validAppeal", firstCaseDetails.getState());

        mrnDate = LocalDate.now().minusMonths(12);
        wrapper.getMrn().setDate(mrnDate);
        log.info("New MRN date: {}", mrnDate);

        SscsCaseData caseData2 = convertSyaToCcdCaseDataV2(wrapper, true, SscsCaseData.builder().build());
        log.info("Appeal created: {}", caseData2.getAppeal());
        SscsCaseDetails secondCaseDetails = ccdService.createCase(
            caseData2,
            VALID_APPEAL_CREATED.getCcdType(),
            "Appeal created summary",
            "Appeal created description",
            idamTokens);
        log.info("Duplicate case {}", secondCaseDetails.getId());
        SscsCaseDetails secondCaseSscsCaseDetails = ccdService.getByCaseId(secondCaseDetails.getId(), idamTokens);

        if (secondCaseSscsCaseDetails.getData().getAssociatedCase() == null) {
            log.info("Give time for evidence share to create associated case link");
            //Give time for evidence share to create associated case link
            Thread.sleep(5000L);
            secondCaseSscsCaseDetails = ccdService.getByCaseId(secondCaseDetails.getId(), idamTokens);
        }

        log.info("Duplicate case {} has been found", secondCaseSscsCaseDetails.getId());

        assertEquals("Number of associated cases doesn't match", 1, secondCaseSscsCaseDetails.getData().getAssociatedCase().size());
        assertEquals("Yes", secondCaseSscsCaseDetails.getData().getLinkedCasesBoolean());
        log.info(secondCaseSscsCaseDetails.toString());

        // check duplicate returns 409
        //httpRequest = RestAssured.given()
        //        .body(body)
        //        .header("Content-Type", "application/json");

        // Give ES time to index
        //Thread.sleep(5000L);

        //response = httpRequest.post("/appeals");

        //response.then().statusCode(HttpStatus.SC_CONFLICT);
        SscsCaseDetails thirdCaseDetails = ccdService.createCase(
            caseData2,
            VALID_APPEAL_CREATED.getCcdType(),
            "Appeal created summary",
            "Appeal created description",
            idamTokens);
        log.info("Second duplicate case {}", thirdCaseDetails.getId());
        //the second duplicate with the same details shouldn't be created
        assertNull("Appeal shouldn't be created", thirdCaseDetails.getId());
    }
}
