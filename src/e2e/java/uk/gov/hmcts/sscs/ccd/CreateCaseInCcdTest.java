package uk.gov.hmcts.sscs.ccd;

import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.service.CcdService;
import uk.gov.hmcts.sscs.service.ccd.CaseDataUtils;
import uk.gov.hmcts.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CreateCaseInCcdTest {

    @Autowired
    private CcdService ccdService;

    @Test
    public void givenACaseShouldBeSavedIntoCcd() {
        CaseDetails caseDetails = ccdService.createCase(CaseDataUtils.buildCaseData());
        assertNotNull(caseDetails);
    }

    @Test
    public void givenASyaCaseShouldBeSavedIntoCcd() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        CaseData caseData = new SubmitYourAppealToCcdCaseDataDeserializer().convertSyaToCcdCaseData(syaCaseWrapper);
        CaseDetails caseDetails = ccdService.createCase(caseData);
        assertNotNull(caseDetails);
    }
}
