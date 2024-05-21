package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.DORMANT_APPEAL_STATE;

import java.time.LocalDate;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

public class DormantCaseFunctionalTest extends AbstractFunctionalTest {

    @Test
    public void otherDetailsChangedTest() throws Exception {
        SscsCaseDetails createdCase = createCaseWithState(CREATE_TEST_CASE, "Child Support",
            "Child Support", State.VALID_APPEAL.getId());
        updateCaseEvent(DORMANT, createdCase);

        String json = getJson(UPDATE_OTHER_PARTY_DATA.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());

        simulateCcdCallback(json);

        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);

        assertEquals(DORMANT_APPEAL_STATE.toString(), caseDetails.getState());
    }
}
