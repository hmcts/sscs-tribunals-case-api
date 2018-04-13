package uk.gov.hmcts.sscs.ccd;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.service.CcdService;
import uk.gov.hmcts.sscs.service.ccd.CaseDataUtils;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CreateCaseInCcdTest {

    @Autowired
    private CcdService ccdService;

    @Test
    public void givenACase_shouldBeSavedIntoCcd() {
        CaseDetails caseDetails = ccdService.createCase(CaseDataUtils.buildCaseData());
        assertNotNull(caseDetails);
    }
}
