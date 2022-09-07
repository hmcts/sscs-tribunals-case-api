package uk.gov.hmcts.reform.sscs.functional.handlers.writefinaldecision;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class WriteFinalDecisionAboutToSubmitHandlerTest extends BaseHandler {

    private static final String USER_AUTHORISATION = "Bearer token";
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    WriteFinalDecisionAboutToSubmitHandler handler;
    private SscsCaseData sscsCaseData;
    @Mock
    private DecisionNoticeService decisionNoticeService;

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    PreviewDocumentService previewDocumentService;

    @Mock
    DecisionNoticeOutcomeService decisionNoticeOutcomeService;

    @Before
    public void setUp() {
        openMocks(this);

        handler = new WriteFinalDecisionAboutToSubmitHandler(decisionNoticeService, previewDocumentService);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();
        sscsCaseData.setAppeal(Appeal.builder().hearingOptions(HearingOptions.builder().build()).build());
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(decisionNoticeService.getOutcomeService(any(String.class))).thenReturn(decisionNoticeOutcomeService);
        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

    }

    @Test
    public void givenAboutToSubmitCallbackForEvent_shouldUpdatePreviousStateWhenStateIsNotReadyToListOrWithFta() {
        sscsCaseData.setPreviousState(State.VOID_STATE);
        sscsCaseData.setState(State.APPEAL_CREATED);
        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getPreviousState(), is(State.APPEAL_CREATED));
    }

    @Test
    public void givenAboutToSubmitCallbackForEvent_shouldNotUpdatePreviousStateWhenStateIsReadyToList() {
        sscsCaseData.setPreviousState(State.VOID_STATE);
        sscsCaseData.setState(State.READY_TO_LIST);
        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getPreviousState(), is(State.VOID_STATE));
    }

    @Test
    public void givenAboutToSubmitCallbackForEvent_shouldNotUpdatePreviousStateWhenStateIsWithFta() {
        sscsCaseData.setPreviousState(State.VOID_STATE);
        sscsCaseData.setState(State.WITH_DWP);
        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getPreviousState(), is(State.VOID_STATE));
    }

    @Test
    public void givenAboutToSubmitCallbackForEvent_shouldSetFields() throws Exception {

        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(getJsonCallbackForTest("handlers/writefinaldecision/writeFinalDecisionCallback.json"))
            .post("/ccdAboutToSubmit")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .rootPath("data")
            .assertThat().body("pipWriteFinalDecisionComparedToDWPMobilityQuestion", equalTo("same"))
            .assertThat().body("pipWriteFinalDecisionDailyLivingActivitiesQuestion[0]", equalTo("preparingFood"))
            .assertThat().body("pipWriteFinalDecisionDailyLivingActivitiesQuestion[1]", equalTo("takingNutrition"))
            .assertThat().body("pipWriteFinalDecisionDailyLivingQuestion", equalTo("standardRate"))
            .assertThat().body("pipWriteFinalDecisionMobilityActivitiesQuestion[0]", equalTo("planningAndFollowing"))
            .assertThat().body("pipWriteFinalDecisionMobilityQuestion", equalTo("standardRate"))
            .assertThat().body("pipWriteFinalDecisionPlanningAndFollowingQuestion", equalTo("planningAndFollowing11d"))
            .assertThat().body("pipWriteFinalDecisionPreparingFoodQuestion", equalTo("preparingFood1f"))
            .assertThat().body("pipWriteFinalDecisionTakingNutritionQuestion", equalTo("takingNutrition2b"))
            .assertThat().body("writeFinalDecisionDateOfDecision", equalTo("2020-06-01"))
            .assertThat().body("writeFinalDecisionDisabilityQualifiedPanelMemberName", equalTo("Fred"))
            .assertThat().body("writeFinalDecisionEndDate", equalTo("2020-10-10"))
            .assertThat().body("writeFinalDecisionEndDateType", equalTo("setEndDate"))
            .assertThat().body("writeFinalDecisionMedicallyQualifiedPanelMemberName", equalTo("Ted"))
            .assertThat().body("writeFinalDecisionPageSectionReference", equalTo("B2"))
            .assertThat().body("writeFinalDecisionAppellantAttendedQuestion", equalTo("Yes"))
            .assertThat().body("writeFinalDecisionPresentingOfficerAttendedQuestion", equalTo("Yes"))
            .assertThat().body("writeFinalDecisionReasons[0].value", equalTo("Because appellant has trouble walking"))
            .assertThat().body("writeFinalDecisionDetailsOfDecision", equalTo("The details of the decision."))
            .assertThat().body("writeFinalDecisionAnythingElse", equalTo("Something else."))
            .assertThat().body("writeFinalDecisionStartDate", equalTo("2019-10-10"))
            .assertThat().body("writeFinalDecisionTypeOfHearing", equalTo("telephone"))
            .assertThat().body("writeFinalDecisionPreviewDocument.document_url", notNullValue());
    }
}
