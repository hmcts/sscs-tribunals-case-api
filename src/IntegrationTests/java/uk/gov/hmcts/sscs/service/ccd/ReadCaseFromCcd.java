package uk.gov.hmcts.sscs.service.ccd;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReadCaseFromCcd {

    @Autowired
    private ReadCoreCaseDataService readCoreCaseDataService;

    @Test
    public void givenACase_shouldBeSavedAndThenUpdatedIntoCcd() {
        CaseDetails caseDetails = readCoreCaseDataService.getCcdCase("1520116198612015");
        assertNotNull(caseDetails);
    }

}
