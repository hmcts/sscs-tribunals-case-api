package uk.gov.hmcts.reform.sscs.functional.sya;

import static io.restassured.RestAssured.baseURI;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN_CCD_CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN_CCD_SSCS5;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN_CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN_NO_MRN_DATE_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN_SSCS5;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN_WITH_INTERLOC_CCD;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer;

@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Slf4j
public class SubmitAppealTest {

    @Value("${test-url}")
    private String testUrl;

    @Autowired
    private SubmitHelper submitHelper;

    @Autowired
    private IdamService idamService;

    private IdamTokens idamTokens;

    public static long getCcdIdFromLocationHeader(String location) {
        return Long.parseLong(location.substring(location.lastIndexOf("/") + 1));
    }

    @BeforeEach
    public void setup() {
        baseURI = testUrl;
        idamTokens = idamService.getIdamTokens();
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    public void givenValidAppealIsSubmittedFromNonSaveAndReturnRoute_thenCreateValidAppeal() {
        assertSscsCaseIsExpectedResult("validAppeal", ALL_DETAILS_NON_SAVE_AND_RETURN_CCD.getSerializedMessage(),
            ALL_DETAILS_NON_SAVE_AND_RETURN);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
    public void givenValidChildSupportAppealIsSubmitted_thenCreateValidAppeal() {
        assertSscsCaseIsExpectedResult("validAppeal", ALL_DETAILS_NON_SAVE_AND_RETURN_CCD_CHILD_SUPPORT.getSerializedMessage(),
            ALL_DETAILS_NON_SAVE_AND_RETURN_CHILD_SUPPORT);
    }

    @Test
    public void givenValidSscs5AppealIsSubmitted_thenCreateValidAppeal() {
        assertSscsCaseIsExpectedResult("validAppeal", ALL_DETAILS_NON_SAVE_AND_RETURN_CCD_SSCS5.getSerializedMessage(),
            ALL_DETAILS_NON_SAVE_AND_RETURN_SSCS5);
    }

    @Test
    public void givenIncompleteAppealIsSubmittedFromNonSaveAndReturnRoute_thenCreateIncompleteAppeal() {
        assertSscsCaseIsExpectedResult("incompleteApplication",
            ALL_DETAILS_NON_SAVE_AND_RETURN_NO_MRN_DATE_CCD.getSerializedMessage(), ALL_DETAILS_NON_SAVE_AND_RETURN);
    }

    @Test
    public void givenNonCompliantAppealIsSubmittedFromNonSaveAndReturnRoute_thenCreateNonCompliantAppeal() {
        assertSscsCaseIsExpectedResult("interlocutoryReviewState",
            ALL_DETAILS_NON_SAVE_AND_RETURN_WITH_INTERLOC_CCD.getSerializedMessage(), ALL_DETAILS_NON_SAVE_AND_RETURN);
    }

    @Test
    public void appealShouldCreateDuplicateAndLinked() {
        String nino = submitHelper.getRandomNino();
        LocalDate mrnDate = LocalDate.now();
        log.info("Generated NINO: {} and MRN date: {}", nino, mrnDate);

        Response response = submitHelper.submitAppeal(nino, mrnDate);
        response.then().statusCode(HttpStatus.SC_CREATED);

        final Long firstCaseId = getCcdIdFromLocationHeader(response.getHeader("Location"));
        log.info("First SYA case created with CCD ID {}", firstCaseId);
        submitHelper.defaultAwait().untilAsserted(() -> {
            SscsCaseDetails firstCaseDetails = submitHelper.findCaseInCcd(firstCaseId, idamTokens);
            assertThat(firstCaseDetails.getState()).isEqualTo(State.WITH_DWP.getId());
        });

        // create a case with different mrn date
        mrnDate = LocalDate.now().minusMonths(12);

        response = submitHelper.submitAppeal(nino, mrnDate);
        response.then().statusCode(HttpStatus.SC_CREATED);

        final Long secondCaseId = getCcdIdFromLocationHeader(response.getHeader("Location"));
        log.info("Duplicate SYA case created with CCD ID {} and MRN date {}", secondCaseId, mrnDate);

        submitHelper.defaultAwait().untilAsserted(() -> {
            SscsCaseDetails secondCaseDetails = submitHelper.findCaseInCcd(secondCaseId, idamTokens);
            assertThat(secondCaseDetails.getData().getAssociatedCase())
                .as("AssociatedCase was not created or size doesn't match!")
                .isNotNull()
                .hasSize(1);
            assertThat(YesNo.YES.toString()).isEqualTo(secondCaseDetails.getData().getLinkedCasesBoolean());
        });

        log.info("Resubmitting case with nino {} and mrn date {} for second time", nino, mrnDate);
        // check duplicate returns 409
        response = submitHelper.submitAppeal(nino, mrnDate);
        response.then().statusCode(HttpStatus.SC_CONFLICT);

        log.info("True duplicate was rejected");
    }

    private void assertSscsCaseIsExpectedResult(String expectedState, String expectedResponse,
        SyaJsonMessageSerializer jsonMessage) {
        assertCaseIsExpectedResult(jsonMessage.getSerializedMessage(), expectedState,
            expectedResponse);
    }

    private void assertCaseIsExpectedResult(String expectedBody, String expectedState, String expectedResponse) {
        LocalDate now = LocalDate.now();
        LocalDate interlocutoryReviewDate = now.minusMonths(13).minusDays(1);
        LocalDate mrnDate = switch (expectedState) {
            case "interlocutoryReviewState" -> interlocutoryReviewDate;
            case "incompleteApplication" -> null;
            default -> now;
        };
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

        log.info("SYA created with CCD ID {}", id);

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

        assertThat(expectedState).isEqualTo(sscsCaseDetails.getState());

        assertThat("Joe Bloggs").isEqualTo(sscsCaseDetails.getData().getCaseAccessManagementFields().getCaseNameHmctsInternal());
        assertThat("Joe Bloggs").isEqualTo(
            sscsCaseDetails.getData().getCaseAccessManagementFields().getCaseNameHmctsRestricted());
        assertThat("Joe Bloggs").isEqualTo(sscsCaseDetails.getData().getCaseAccessManagementFields().getCaseNamePublic());
    }

    private String changeExpectedFields(String serializedMessage, String nino, LocalDate mrnDate) {
        serializedMessage = serializedMessage
            .replace("AB877533C", nino)
            .replace("2021-04-13", LocalDate.now().toString());

        if (mrnDate != null) {
            serializedMessage = serializedMessage.replace("2018-02-01", mrnDate.toString());
        }

        return serializedMessage;
    }
}
