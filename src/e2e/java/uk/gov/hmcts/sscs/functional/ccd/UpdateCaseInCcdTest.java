package uk.gov.hmcts.reform.sscs.functional.ccd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
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
        SscsCaseDetails caseDetails = ccdService.createCase(CaseDataUtils.buildCaseData(), idamTokens);

        assertNotNull(caseDetails);
        SscsCaseData updatedCaseRefData = CaseDataUtils.buildCaseData().toBuilder().caseReference("SC123/12/78765").build();
        SscsCaseDetails updatedCaseDetails = ccdService.updateCase(updatedCaseRefData, caseDetails.getId(),
                "appealReceived", "", "", idamTokens);
        assertEquals("SC123/12/78765", updatedCaseDetails.getData().getCaseReference());
    }
}
