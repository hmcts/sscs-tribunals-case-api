package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_TCW;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class IssueDirectionFunctionalTest extends AbstractFunctionalTest {

    public IssueDirectionFunctionalTest() {
        super();
    }

    // Need tribunals running to pass this functional test
    @Test
    public void processAnIssueDirectionEvent_shouldUpdateInterlocReviewState() throws IOException {

        createDigitalCaseWithEvent(NON_COMPLIANT);

        String json = getJson(DIRECTION_ISSUED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());

        simulateCcdCallback(json);

        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();

        assertNull(caseData.getDirectionTypeDl());
    }

    @Test
    public void processAnIssueDirectionEvent_ifPastHearingExcludedDatesAreOnCaseDetails() throws IOException {
        //set case to have past hearing excluded dates
        //set case to be in appeal to proceed
        //set case to be in interloc review state

        //assert that issue directions event is created.
        SscsCaseDetails caseDetails = createDigitalCaseWithEvent(NON_COMPLIANT);
        SscsCaseData caseData = caseDetails.getData();

        caseData.getAppeal().setHearingOptions(HearingOptions.builder()
                        .excludeDates(List.of(
                                ExcludeDate.builder().value(new DateRange("2024-04-03", "2024-05-04")).build()
                        )).build());

        caseData.setInterlocReviewState(REVIEW_BY_TCW);

        caseData.setDirectionTypeDl(new DynamicList(EventType.APPEAL_TO_PROCEED.toString()));

        updateCaseEvent(UPDATE_CASE_ONLY, caseDetails);

        String json = getJson(DIRECTION_ISSUED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());

        simulateCcdCallback(json);

        caseDetails = findCaseById(ccdCaseId); //get the case again to validate
        caseData = caseDetails.getData();

        assertTrue(caseData.getInterlocReviewState() == AWAITING_ADMIN_ACTION);

    }
}
