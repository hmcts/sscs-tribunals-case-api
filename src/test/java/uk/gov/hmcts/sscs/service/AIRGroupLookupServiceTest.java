package uk.gov.hmcts.sscs.service;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AIRGroupLookupServiceTest {
    AIRLookupService AIRLookupService;

    @Before
    public void setUp() {
        AIRLookupService = new AIRLookupService();
        AIRLookupService.init();
    }

    @Test
    public void lookupPostcode() {
        String adminGroup = AIRLookupService.lookupRegionalCentre("BR3");
        assertEquals("Sutton", adminGroup);
    }

    @Test
    public void lookupPostcodeLowerCase() {
        String adminGroup = AIRLookupService.lookupRegionalCentre("br3");
        assertEquals("Sutton", adminGroup);
    }

    @Test
    public void lookupPostcodeNotThere() {
        String adminGroup = AIRLookupService.lookupRegionalCentre("aa1");
        assertEquals(null, adminGroup);
    }

    @Test
    public void lookupLastValue() {
        String adminGroup = AIRLookupService.lookupRegionalCentre("ze3");
        assertEquals("Glasgow", adminGroup);
    }

    @Test
    public void lookupFirstValue() {
        String adminGroup = AIRLookupService.lookupRegionalCentre("ab1");
        assertEquals("Glasgow", adminGroup);
    }
}
