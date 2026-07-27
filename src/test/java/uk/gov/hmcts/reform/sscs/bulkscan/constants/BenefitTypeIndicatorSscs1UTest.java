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
public class BenefitTypeIndicatorSscs1UTest {

    @Test
    public void testAllBenefitTypesIndicatorsAreConfiguredCorrectly() {
        for (BenefitTypeIndicatorSscs1U benefitTypeIndicatorSscs1U : BenefitTypeIndicatorSscs1U.values()) {
            // Assert that each BenefitTypeIndicatorSscs1U enum has both an indicator string and a benefit except other
            assertNotNull(benefitTypeIndicatorSscs1U.getIndicatorString());
            if (!benefitTypeIndicatorSscs1U.getIndicatorString().equals("is_benefit_type_other")) {
                assertNotNull(benefitTypeIndicatorSscs1U.getBenefit());
                // Assert that if we lookup the benefit type indicator via it's indicator string, we
                // find the same benefit as has been configured.
                Optional<Benefit> benefitTypeLookup = BenefitTypeIndicatorSscs1U.findByIndicatorString(benefitTypeIndicatorSscs1U.getIndicatorString());
                assertTrue(benefitTypeLookup.isPresent());
                assertEquals(benefitTypeLookup.get(), benefitTypeIndicatorSscs1U.getBenefit());
            }
        }
    }

    @Test
    public void testLookupByInvalidBenefitTypeIndicatorReturnsEmptyOptional() {
        Optional<Benefit> optional = BenefitTypeIndicatorSscs1U.findByIndicatorString("something");
        assertTrue(optional.isEmpty());
    }

    @Test
    public void testGetAllIndicatorStrings() {
        assertEquals(BenefitTypeIndicatorSscs1U.values().length, BenefitTypeIndicatorSscs1U.getAllIndicatorStrings().size());
    }
}
