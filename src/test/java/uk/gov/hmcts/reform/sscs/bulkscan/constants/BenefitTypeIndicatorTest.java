package uk.gov.hmcts.reform.sscs.bulkscan.constants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;

@RunWith(JUnitParamsRunner.class)
public class BenefitTypeIndicatorTest {

    @Test
    public void testAllBenefitTypesIndicatorsAreConfiguredCorrectly() {
        for (BenefitTypeIndicator benefitTypeIndicator : BenefitTypeIndicator.values()) {
            // Assert that each BenefitTypeIndicator enum has both an indicator string and a benefit
            assertNotNull(benefitTypeIndicator.getIndicatorString());
            assertNotNull(benefitTypeIndicator.getBenefit());
            // Assert that if we lookup the benefit type indicator via it's indicator string, we
            // find the same benefit as has been configured.
            Optional<Benefit> benefitTypeLookup = BenefitTypeIndicator.findByIndicatorString(benefitTypeIndicator.getIndicatorString());
            assertTrue(benefitTypeLookup.isPresent());
            assertEquals(benefitTypeLookup.get(), benefitTypeIndicator.getBenefit());

        }
    }

    @Test
    public void testLookupByInvalidBenefitTypeIndicatorReturnsEmptyOptional() {
        Optional<Benefit> optional = BenefitTypeIndicator.findByIndicatorString("something");
        assertTrue(optional.isEmpty());
    }

    @Test
    public void testGetAllIndicatorStrings() {
        assertEquals(BenefitTypeIndicator.values().length, BenefitTypeIndicator.getAllIndicatorStrings().size());
    }
}
