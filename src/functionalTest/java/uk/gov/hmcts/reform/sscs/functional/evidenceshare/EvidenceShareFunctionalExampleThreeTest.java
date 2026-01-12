package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_TEST_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.functional.utilities.idam.LocalIdamService;
import uk.gov.hmcts.reform.sscs.functional.utilities.idam.model.User;

/**
 * Uses the current mechanism for creating users. Least effort but users created at server start-up and not on the fly
 * Still uses the new Idam Service to retrieve/cache it - but could prob add that logic to existing idam service
 */
class EvidenceShareFunctionalExampleThreeTest extends AbstractFunctionalTest {

    @Autowired
    private LocalIdamService localIdamService;

    EvidenceShareFunctionalExampleThreeTest() {
        super();
    }

    @BeforeEach
    void beforeEach() {
        ccdCaseId = null;
    }

    // Toggle off scenario
    @Test
    void processANonDigitalAppealWithValidMrn_shouldGenerateADl6AndAddToCcdAndUpdateStateToggleOn() throws Exception {

        User user = localIdamService.getUser("example.three@hmcts.net");

        createNonDigitalCaseWithEvent(CREATE_TEST_CASE, Benefit.CHILD_SUPPORT, State.VALID_APPEAL, user.tokens());

        String json = getJson(VALID_APPEAL_CREATED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());
        json = json.replace("CREATED_IN_GAPS_FROM", State.VALID_APPEAL.getId());
        json = json.replaceAll("NINO_TO_BE_REPLACED", getRandomNino());

        simulateCcdCallback(json, user.tokens());

        defaultAwait().untilAsserted(() -> {
            SscsCaseDetails caseDetails = findCaseById(ccdCaseId);

            SscsCaseData caseData = caseDetails.getData();

            List<SscsDocument> docs = caseData.getSscsDocument();
            assertNotNull(docs);
            assertEquals(1, docs.size());
            assertEquals("dl6-" + ccdCaseId + ".pdf", docs.getFirst().getValue().getDocumentFileName());
            assertEquals("withDwp", caseDetails.getState());
            assertEquals(LocalDate.now().toString(), caseData.getDateSentToDwp());
            //since the SUBMITTED callback no longer contains the updated caseData, the dateCaseSentToGaps will not be present
            //better to test that in the UTs
        });
    }
}