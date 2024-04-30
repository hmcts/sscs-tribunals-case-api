package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_TO_PROCEED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT;

import java.io.IOException;
import java.time.LocalDate;
import org.junit.Test;
import org.slf4j.Logger;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

public class AppealToProceedFunctionalTest extends AbstractFunctionalTest {

    private static final Logger log = getLogger(AppealToProceedFunctionalTest.class);


    public AppealToProceedFunctionalTest() {
        super();
    }

    // Need tribunals running to pass this functional test
    @Test
    public void processAnAppealToProceedEvent_shouldUpdateHmctsDwpState() throws IOException {

        createDigitalCaseWithEvent(NON_COMPLIANT);

        String json = getJson(APPEAL_TO_PROCEED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());

        simulateCcdCallback(json);

        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);

        log.info("CseDetails {}", caseDetails);
        log.info("CseDetails.getData {}", caseDetails.getData());
        log.info("CseDetails.getState {}", caseDetails.getState());

        assertEquals("sentToDwp", caseDetails.getData().getHmctsDwpState());
        assertEquals("withDwp", caseDetails.getState());
    }
}
