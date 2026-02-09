package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_TEST_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SEND_TO_DWP_OFFLINE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler.getJsonCallbackForTest;

import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@RunWith(JUnitParamsRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class DwpUploadResponseSubmittedHandlerTest extends AbstractFunctionalTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Test
    public void givenSubmittedCallback_shouldTriggerReadyToListAndSetDwpState() throws Exception {
        SscsCaseDetails createdCase =
                createCaseWithState(CREATE_TEST_CASE, "UC", "Universal Credit", READY_TO_LIST.getId());

        updateCaseEvent(SEND_TO_DWP_OFFLINE, createdCase);

        String jsonCallbackForTest = getJsonCallbackForTest("callback/dwpUploadResponse.json");
        jsonCallbackForTest = jsonCallbackForTest.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        jsonCallbackForTest = jsonCallbackForTest.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());
        jsonCallbackForTest = jsonCallbackForTest.replace("BENEFIT_CODE_PLACEHOLDER", "UC");
        jsonCallbackForTest = jsonCallbackForTest.replace("BENEFIT_DESCRIPTION_PLACEHOLDER", "Universal Credit");

        simulateCcdCallback(jsonCallbackForTest);

        defaultAwait().untilAsserted(() -> {
            SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
            assertEquals(State.WITH_DWP.toString(), caseDetails.getState());
            assertNotNull(caseDetails.getData());
            assertEquals(DwpState.RESPONSE_SUBMITTED_DWP, caseDetails.getData().getDwpState());
            assertEquals(YesNo.NO, caseDetails.getData().getWorkAllocationFields().getFtaResponseReviewRequired());
            assertEquals("Yes", caseDetails.getData().getDwpFurtherInfo());
            assertNotNull(caseDetails.getData().getSscsDocument());
            assertEquals("appellantEvidence",
                    caseDetails.getData().getSscsDocument().getFirst().getValue().getDocumentType());
        });
    }
}
