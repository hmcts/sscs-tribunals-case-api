package uk.gov.hmcts.reform.sscs.functional.workallocation;


import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADMIN_APPEAL_WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_TEST_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SYA_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_CASE_ONLY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.*;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseDataV2;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS;

@RunWith(SpringRunner.class)
//@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
@Slf4j
public class WACreateTaskTest {

    //WIP - Rough outline

    // This class will contain functional tests for Work Allocation Task creation
    // It will:
    // - Create a case in CCD
    // - Update the Case and trigger an Event
    // - We will verify the Case is created and has the correct state

    //References to CcdService, IdamService, and WorkAllocationService will be needed
    //CreateAndUpdateCaseInCcdTestV2 is a good reference for CCD interactions

    private static final String TM_SEARCH_TASKS_PATH = "task";
    private static final String TM_SEARCH_PARAMS = "?first_result=0&max_results=10";

    @Autowired
    private CcdService ccdService;

    @Autowired
    private IdamService idamService;

    private IdamTokens idamTokens;

    @Before
    public void setup() {
        //Using the system user
        idamTokens = idamService.getIdamTokens();
    }

    @Test
    public void shouldCreateCaseInCcdThenChangeTheCaseStateAndSomeAttributes() {
        //Get and update test data
        SscsCaseData caseData =
            convertSyaToCcdCaseDataV2(ALL_DETAILS.getDeserializeMessage(),
                false,
                new SscsCaseData());

        // Create case in CCD and verify
        SscsCaseDetails caseDetails =
            ccdService.createCase(caseData,
                CREATE_TEST_CASE.getCcdType(),
                "SSCS:WA Creating a Test Case from FT",
                "Test case created description", idamTokens);

        assertNotNull(caseDetails);
        assertNotNull(caseDetails.getState());
        assertEquals(State.VALID_APPEAL.getId(), caseDetails.getState());
        assertNull(caseDetails.getData().getUrgentCase());

        //Change some data
        caseData.setUrgentCase("Yes");

        // Run another event to update the Case data and change the State
        SscsCaseData updatedCaseData =
            ccdService.updateCase(caseData,
                caseDetails.getId(),
                ADMIN_APPEAL_WITHDRAWN.getCcdType(), //The user triggering the event needs to have permissions
                "Update Case from functional test",
                "Test case",
                idamTokens).getData();

        assertEquals(caseData.getUrgentCase(), updatedCaseData.getUrgentCase());
        //TODO common code doesn't return it for some reason
        //assertEquals(State.DORMANT_APPEAL_STATE, updatedCaseData.getState());

        SscsCaseDetails fullUpdatedCase =
            ccdService.getByCaseId(Long.valueOf(updatedCaseData.getCcdCaseId()), idamTokens);

        assertEquals(State.DORMANT_APPEAL_STATE.getId(), fullUpdatedCase.getState());

    }

    @Test
    public void shouldCreateUCCaseWithCorrectData() {
        //Get and update test data
        SscsCaseData caseData =
            convertSyaToCcdCaseDataV2(ALL_DETAILS.getDeserializeMessage(),
                false,
                new SscsCaseData());

        // Create case in CCD and verify
        SscsCaseDetails caseDetails =
            ccdService.createCase(caseData,
                CREATE_TEST_CASE.getCcdType(),
                "SSCS: Creating a Test Case from FT",
                "UC Test Case",
                idamTokens
            );
        assertNotNull(caseDetails);
        assertNotNull(caseDetails.getState());
        assertEquals(State.VALID_APPEAL.getId(), caseDetails.getState());

        BenefitType ucBenefitType = BenefitType.builder()
            .code(UC.getShortName())
            .description(UC.getDescription())
            .build();

        caseData.setWcaAppeal(YES);
        caseData.setBenefitCode(UC.getBenefitCode());
        caseData.getAppeal().setBenefitType(ucBenefitType);

        // Run another event to update the Case data and change the State
        SscsCaseData updatedCaseData =
            ccdService.updateCase(caseData,
                caseDetails.getId(),
                UPDATE_CASE_ONLY.getCcdType(), //The user triggering the event needs to have permissions
                "Update Case from functional test",
                "Test case",
                idamTokens).getData();

        assertEquals(YES, updatedCaseData.getWcaAppeal());
        assertEquals(ucBenefitType, updatedCaseData.getAppeal().getBenefitType());
        assertEquals(UC.getBenefitCode(), updatedCaseData.getBenefitCode());

        SscsCaseDetails fullUpdatedCase =
            ccdService.getByCaseId(Long.valueOf(updatedCaseData.getCcdCaseId()), idamTokens);

        assertEquals(State.VALID_APPEAL.getId(), fullUpdatedCase.getState());

        //Run Write Final Appeal Event with populated data
        //OR trigger the callbacks with populated fields


    }
    @Test
    public void shouldCreateESACaseWithCorrectData() {
        //Get and update test data
        SscsCaseData caseData =
            convertSyaToCcdCaseDataV2(ALL_DETAILS.getDeserializeMessage(),
                false,
                new SscsCaseData());

        // Create case in CCD and verify
        SscsCaseDetails caseDetails =
            ccdService.createCase(caseData,
                CREATE_TEST_CASE.getCcdType(),
                "SSCS: Creating a Test Case from FT",
                "UC Test Case",
                idamTokens
            );

        assertNotNull(caseDetails);
        assertNotNull(caseDetails.getState());
        assertEquals(State.VALID_APPEAL.getId(), caseDetails.getState());

        BenefitType ucBenefitType = BenefitType.builder()
            .code(ESA.getShortName())
            .description(ESA.getDescription())
            .build();

        caseData.setWcaAppeal(YES);
        caseData.setBenefitCode(ESA.getBenefitCode());
        caseData.getAppeal().setBenefitType(ucBenefitType);

        // Run another event to update the Case data and change the State
        SscsCaseData updatedCaseData =
            ccdService.updateCase(caseData,
                caseDetails.getId(),
                UPDATE_CASE_ONLY.getCcdType(), //The user triggering the event needs to have permissions
                "Update Case from functional test",
                "Test case",
                idamTokens).getData();

        assertEquals(YES, updatedCaseData.getWcaAppeal());
        assertEquals(ucBenefitType, updatedCaseData.getAppeal().getBenefitType());
        assertEquals(ESA.getBenefitCode(), updatedCaseData.getBenefitCode());

        SscsCaseDetails fullUpdatedCase =
            ccdService.getByCaseId(Long.valueOf(updatedCaseData.getCcdCaseId()), idamTokens);

        assertEquals(State.VALID_APPEAL.getId(), fullUpdatedCase.getState());

        //Run Write Final Appeal Event with populated data
        //OR trigger the callbacks with populated fields

    }

    @Test
    public void shouldCreateCaseInCcdThenChangeTheCaseStateAndSomeAttributes_Alternative() {
        //Get and update test data
        SscsCaseData caseData =
            convertSyaToCcdCaseDataV2(ALL_DETAILS.getDeserializeMessage(),
                false,
                new SscsCaseData());

        // Create case in CCD and verify
        SscsCaseDetails caseDetails =
            ccdService.createCase(caseData,
                SYA_APPEAL_CREATED.getCcdType(),
                "SSCS:WA Creating a Test Case from FT",
                "Test case created description", idamTokens);

        assertNotNull(caseDetails);
        assertNotNull(caseDetails.getState());
        assertEquals(State.APPEAL_CREATED.getId(), caseDetails.getState());
        assertNull(caseDetails.getData().getUrgentCase());

        //Change some data
        caseData.setUrgentCase("Yes");
        caseData.setCreatedInGapsFrom(READY_TO_LIST.getCcdType());

        // Run another event to update the Case data and change the State
        SscsCaseData updatedCaseData =
            ccdService.updateCase(
                    caseData,
                    caseDetails.getId(),
                    READY_TO_LIST.getCcdType(), //The user triggering the event needs to have permissions
                    "Update Case from functional test",
                    "Test case",
                    idamTokens)
                .getData();

        assertEquals(caseData.getUrgentCase(), updatedCaseData.getUrgentCase());
        //TODO common code doesn't return it for some reason
        //assertEquals(State.DORMANT_APPEAL_STATE, updatedCaseData.getState());

        SscsCaseDetails fullUpdatedCase =
            ccdService.getByCaseId(Long.valueOf(updatedCaseData.getCcdCaseId()), idamTokens);

        assertEquals(State.READY_TO_LIST.getId(), fullUpdatedCase.getState());

    }


    @Test
    public void shouldCreateAnOrgRoleWithinAM() {
        //
    }


    @Test
    public void shouldTriggerAnEventToCreateTaskThenFetchTaskRecordToVerify() {
        //to be implemented
        //Trigger the event like other tests above
        //Wait for Task Management to do its thing
        //Fetch the Task record and verify it is correct

        //String taskId = "12345"; //placeholder
        //idamTokens = idamService.getIdamTokens();
        //log.info("idamTokens.getUserId()" + idamTokens.getUserId());
        //log.info("idamTokens.getServiceAuthorization()" + idamTokens.getServiceAuthorization());
        //log.info("idamTokens.getIdamOauth2Token()" + idamTokens.getIdamOauth2Token());
        //
        //Headers headers = new Headers(
        //    new Header("ServiceAuthorization", "" + idamTokens.getServiceAuthorization()),
        //    new Header("Authorization", idamTokens.getIdamOauth2Token()),
        //    new Header("Content-Type", "application/json")
        //);
        //
        //Response result = tmClient.get(
        //    TM_SEARCH_TASKS_PATH + TM_SEARCH_PARAMS,
        //    ,
        //    headers
        //);
        //
        //result.then().assertThat()
        //    .statusCode(HttpStatus.OK.value())
        //    .body("tasks.size()", lessThanOrEqualTo(10))
        //    .body("tasks.id", everyItem(notNullValue()));
        //

    }


}
