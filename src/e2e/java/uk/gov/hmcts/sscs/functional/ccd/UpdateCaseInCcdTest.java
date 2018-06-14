package uk.gov.hmcts.sscs.functional.ccd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.service.CcdService;
import uk.gov.hmcts.sscs.service.ccd.CaseDataUtils;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UpdateCaseInCcdTest {

    @Autowired
    private CcdService ccdService;

    @Test
    public void givenACase_shouldBeUpdatedInCcd() {
        CaseDetails caseDetails = ccdService.createCase(CaseDataUtils.buildCaseData());

        assertNotNull(caseDetails);
        CaseData updatedCaseRefData = CaseDataUtils.buildCaseData().toBuilder().caseReference("SC123/12/78765").build();
        CaseDetails updatedCaseDetails = ccdService.updateCase(updatedCaseRefData, caseDetails.getId(),
                "appealReceived");
        assertEquals("SC123/12/78765", updatedCaseDetails.getData().get("caseReference"));
    }

    @Test
    public void givenACase_shouldBeUpdatedInCcdWithNewReminderEvent() {
        CaseDetails caseDetails = ccdService.createCase(CaseDataUtils.buildCaseData());

        CaseDetails updatedCaseDetails = ccdService.updateCase(null, caseDetails.getId(), "evidenceReminder");

        //FIXME: At the moment there is no way to verify that a case event has successfully been added to CCD. Therefore, for now,
        // just check that the updatedCaseDetails object is not null. New endpoint being written in CCD to fix this (RDM-1999).
        assertNotNull(updatedCaseDetails);
    }

}
