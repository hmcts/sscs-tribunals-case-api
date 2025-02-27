package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_TO_PROCEED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT;

import java.io.IOException;
import java.time.LocalDate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

public class AppealToProceedFunctionalTest extends AbstractFunctionalTest {

    public AppealToProceedFunctionalTest() {
        super();
    }

    @Autowired
    private Environment environment;

    // Need tribunals running to pass this functional test
    @Test
    public void processAnAppealToProceedEvent_shouldUpdateHmctsDwpState() throws IOException {

        System.out.println("processAnAppealToProceedEvent_shouldUpdateHmctsDwpState started");

        String jobStoreClass = environment.getProperty("job.scheduler.quartzProperties.org.quartz.jobStore.class");
        System.out.println("jobStoreClass: " + jobStoreClass);
        String testfunctionalmarker = environment.getProperty("test.functional.marker");
        System.out.println("test.functional.marker: " + testfunctionalmarker);
        assertEquals("org.quartz.simpl.RAMJobStore", jobStoreClass);
        createDigitalCaseWithEvent(NON_COMPLIANT);

        String json = getJson(APPEAL_TO_PROCEED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());

        simulateCcdCallback(json);

        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);

        assertEquals("sentToDwp", caseDetails.getData().getHmctsDwpState());
        assertEquals("withDwp", caseDetails.getState());
    }
}
