package uk.gov.hmcts.sscs.ccd;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.service.CcdService;
import uk.gov.hmcts.sscs.service.ccd.ReadCoreCaseDataService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReadCaseFromCcdTest {

    private static final String APPEAL_NUMBER = "abcde12345";

    @Autowired
    private ReadCoreCaseDataService readCoreCaseDataService;

    @Autowired
    private CcdService ccdService;

    @Test
    public void givenACaseIdShouldRetrieveCaseDetails() {
        CaseDetails caseDetails = readCoreCaseDataService.getCcdCaseDetailsByCaseId("1520966754095462");
        assertNotNull(caseDetails);
    }

    @Test
    public void givenAnAppealNumberShouldRetrieveCaseDetails() {
        CaseDetails caseDetails = readCoreCaseDataService.getCcdCaseDetailsByAppealNumber(APPEAL_NUMBER);
        assertNotNull(caseDetails);
    }

    @Test
    public void givenAnAppealNumberandSurnameShouldRetrieveCaseDetailsUsingCcdService() throws CcdException {
        CaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(APPEAL_NUMBER, "test");
        assertNotNull(caseData);
    }

    @Test
    public void givenAnAppealNumberShouldRetrieveCaseDetailsUsingCcdService() throws CcdException {
        CaseData caseData = ccdService.findCcdCaseByAppealNumber(APPEAL_NUMBER);
        assertNotNull(caseData);
    }

}
