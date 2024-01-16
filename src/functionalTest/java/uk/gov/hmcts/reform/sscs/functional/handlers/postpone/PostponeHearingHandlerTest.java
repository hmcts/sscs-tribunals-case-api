package uk.gov.hmcts.reform.sscs.functional.handlers.postpone;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.POSTPONED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import java.util.Optional;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class PostponeHearingHandlerTest extends BaseHandler {

    @Autowired
    private ObjectMapper mapper;

    @DisplayName("When a hearing postpone callback is made, the correct fields are changed")
    @Test
    public void hitCallback() throws JsonProcessingException {
        SscsCaseDetails caseDetails = createCase();

        caseDetails.getData().getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        caseDetails.getData().getPostponement().setUnprocessedPostponement(YES);
        caseDetails.getData().getPostponement().setPostponementEvent(READY_TO_LIST);
        caseDetails.getData().setDwpState(DwpState.IN_PROGRESS);

        SscsCaseDetails updatedCaseDetails = runEvent(caseDetails.getData(), EventType.UPDATE_CASE_ONLY);

        CaseDetails<SscsCaseData> callbackCaseDetails = new CaseDetails<>(
            updatedCaseDetails.getId(),
            updatedCaseDetails.getJurisdiction(),
            State.getById(updatedCaseDetails.getState()),
            updatedCaseDetails.getData(),
            updatedCaseDetails.getCreatedDate(),
            updatedCaseDetails.getCaseTypeId());

        Callback<SscsCaseData> body = new Callback<>(
            callbackCaseDetails,
            Optional.of(callbackCaseDetails),
            POSTPONED,
            false);

        String response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", idamTokens.getIdamOauth2Token())
            .header("ServiceAuthorization", idamTokens.getServiceAuthorization())
            .body(body)
            .expect()
            .statusCode(200)
            .when()
            .post("/ccdSubmittedEvent/")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .log().all(true)
            .extract().body().asString();

        JsonNode root = mapper.readTree(response);
        SscsCaseData result = mapper.readValue(root.path("data").toPrettyString(), new TypeReference<SscsCaseData>(){});

        assertThat(result.getPostponement().getUnprocessedPostponement()).isEqualTo(NO);
        assertThat(result.getPostponement().getPostponementEvent()).isNull();
        assertThat(result.getDwpState()).isEqualTo(DwpState.HEARING_POSTPONED);
    }
}
