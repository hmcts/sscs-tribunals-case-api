package uk.gov.hmcts.reform.sscs.functional.sya;

import static io.restassured.RestAssured.baseURI;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;

import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.RandomStringUtils;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
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
    private CcdService ccdService;

    @Autowired
    private IdamService idamService;

    private IdamTokens idamTokens;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Before
    public void setup() {
        baseURI = testUrl;
        idamTokens = idamService.getIdamTokens();
    }

    private String setLatestMrnDate(String body, LocalDate localDate) {
        return body.replaceAll("01-02-2018", localDate == null ? "" : formatter.format(localDate));
    }

    private String setNino(String body, String nino) {
        return body.replaceAll("AB877533C", nino);
    }

    private SscsCaseDetails findCaseInCcd(Long id) {
        return ccdService.getByCaseId(id, idamTokens);
    }

    private String getRandomNino() {
        return RandomStringUtils.random(9, true, true).toUpperCase();
    }

    @Test
    @Parameters({"ALL_DETAILS, incompleteApplication",
            "ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS, interlocutoryReviewState",
            "ALL_DETAILS, validAppeal",
            "ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS, validAppeal"})
    public void appealShouldBeSavedViaSya(SyaJsonMessageSerializer syaJsonMessageSerializer, String expectedState) {
        String body = syaJsonMessageSerializer.getSerializedMessage();
        String nino = getRandomNino();
        body = setNino(body, nino);
        LocalDate now = LocalDate.now();
        LocalDate interlocutoryReviewDate = now.minusMonths(13).minusDays(1);
        LocalDate mrnDate = expectedState.equals("interlocutoryReviewState") ? interlocutoryReviewDate :
                expectedState.equals("incompleteApplication") ? null : now;
        body = setLatestMrnDate(body, mrnDate);
        SyaCaseWrapper wrapper = syaJsonMessageSerializer.getDeserializeMessage();
        wrapper.getAppellant().setNino(nino);
        wrapper.getMrn().setDate(mrnDate);
        RegionalProcessingCenter rpc = getRegionalProcessingCenter();
        Appeal expected = convertSyaToCcdCaseData(wrapper, rpc.getName(),  rpc).getAppeal();

        RequestSpecification httpRequest = RestAssured.given()
                .body(body)
                .header("Content-Type", "application/json");

        Response response = httpRequest.post("/appeals");

        response.then().statusCode(HttpStatus.SC_CREATED);

        final String location = response.getHeader("Location");
        final Long id = Long.parseLong(location.substring(location.lastIndexOf("/") + 1));
        SscsCaseDetails sscsCaseDetails = findCaseInCcd(id);
        if (expected.getAppellant().getAppointee() == null) {
            sscsCaseDetails.getData().getAppeal().getAppellant().setAppointee(null);
        }
        log.info(String.format("SYA created with CCD ID %s", id));
        assertEquals(expected, sscsCaseDetails.getData().getAppeal());
        assertEquals(expectedState, sscsCaseDetails.getState());
    }

}
