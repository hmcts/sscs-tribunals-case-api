package uk.gov.hmcts.reform.sscs.functional.client;

import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.service.RefDataService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;


@Slf4j
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class RefDataClientFunctionalTest {
    @Autowired
    VenueDataLoader venueDataLoader;

    @Autowired
    RefDataService refDataService;

    @Before
    public void setUp() {

    }

    @Test
    public void testAllVenues() {
        Map<String, VenueDetails> venues = venueDataLoader.getVenueDetailsMap();
        Set<String> keys = venues.keySet();
        for (String key:keys) {
            VenueDetails venueDetails = venues.get(key);
            log.info(venueDetails.getGapsVenName());
            if (refDataService.getVenueRefData(venueDetails.getGapsVenName()) == null) {
                log.info("Not in API " + venueDetails.getGapsVenName());
            }
        }

    }
}
