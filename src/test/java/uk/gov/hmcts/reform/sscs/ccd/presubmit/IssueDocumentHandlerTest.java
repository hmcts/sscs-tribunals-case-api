package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;

import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;

@RunWith(JUnitParamsRunner.class)
public class IssueDocumentHandlerTest {

    private IssueDocumentHandler handler;

    @Before
    public void setUp() {
        handler = new IssueDocumentHandler();
    }

    @Test
    @Parameters({"CHILD_SUPPORT", "TAX_CREDIT"})
    public void givenAnSscs2OrSscs5BenefitType_thenHideNino(Benefit benefit) {
        assertTrue(handler.isBenefitTypeValidToHideNino(Optional.ofNullable(benefit)));
    }

    @Test
    public void givenAnSscs1BenefitType_thenDoNotHideNino() {
        assertFalse(handler.isBenefitTypeValidToHideNino(Optional.ofNullable(PIP)));
    }

}