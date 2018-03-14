package uk.gov.hmcts.sscs.ccd;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.service.ccd.ReadCoreCaseDataService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReadCaseFromCcd {

    @Autowired
    private ReadCoreCaseDataService readCoreCaseDataService;

    @Test
    public void givenACaseId_shouldRetrieveCaseDetails() {
        CaseDetails caseDetails = readCoreCaseDataService.getCcdCaseDetailsByCaseId("1520966754095462");
        assertNotNull(caseDetails);
    }

    @Test
    public void givenAnAppealNumber_shouldRetrieveCaseDetails() {
        CaseDetails caseDetails = readCoreCaseDataService.getCcdCaseDetailsByAppealNumber("abcde12345");
        assertNotNull(caseDetails);
    }

}
