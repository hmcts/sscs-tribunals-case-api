package uk.gov.hmcts.reform.sscs.functional.ccd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.reform.sscs.functional.TestHelper.buildSscsCaseDataForTesting;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UpdateCaseInCcdTest {

    @Autowired
    private CcdService ccdService;

    @Autowired
    private IdamService idamService;

    private IdamTokens idamTokens;

    @Before
    public void setup() {
        idamTokens = idamService.getIdamTokens();
    }

    @Test
    public void givenACase_shouldBeUpdatedInCcd() {
        SscsCaseData testCaseData = buildSscsCaseDataForTesting();
        SscsCaseDetails caseDetails = ccdService.createCase(testCaseData, "appealCreated",
            "Appeal created summary", "Appeal created description",
            idamTokens);

        assertNotNull(caseDetails);
        testCaseData.setCaseReference("SC123/12/78765");
        SscsCaseDetails updatedCaseDetails = ccdService.updateCase(testCaseData, caseDetails.getId(),
            "appealReceived", "", "", idamTokens);
        assertEquals("SC123/12/78765", updatedCaseDetails.getData().getCaseReference());
    }
}
