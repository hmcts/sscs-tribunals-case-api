package uk.gov.hmcts.reform.sscs.client;


import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.stream.Stream;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.RefDataService;
import uk.gov.hmcts.reform.sscs.service.VenueService;


@SpringBootTest
@RunWith(JUnitParamsRunner.class)
@TestPropertySource(locations = "classpath:config/application_refdata_it.properties")
public class RefDataClientIt {
    /**
     * Intention of this test is to assert that reference data api returns case management location
     * for all the venue names in air lookup service. This is one time test to run when Air lookup data is changed.
     */

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private RefDataService refDataService;

    @Autowired
    private VenueService venueService;

    private static final AirLookupService airLookupService;

    static {
        airLookupService = new AirLookupService();
        airLookupService.init();
    }

    @Test
    @Parameters(method = "getVenueName")
    @Ignore
    public void testVenueRefDataForVenueName(String epimsId) {
        CourtVenue venue = refDataService.getCourtVenueRefDataByEpimsId(epimsId);
        assertNotNull(venue);
        assertNotNull(venue.getEpimsId());
        assertNotNull(venue.getRegionId());
    }

    private Object[] getEpimsId() {
        return Stream.of(Benefit.values())
                .map(airLookupService::lookupAirVenueNamesByBenefitCode)
                .flatMap(List::stream)
                .map(venueService::getEpimsIdForVenue)
                .distinct()
                .toArray(Object[]::new);
    }
}
