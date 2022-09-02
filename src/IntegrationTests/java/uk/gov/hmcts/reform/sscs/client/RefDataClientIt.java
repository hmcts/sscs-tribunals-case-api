package uk.gov.hmcts.reform.sscs.client;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;
import uk.gov.hmcts.reform.sscs.service.VenueService;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Intention of this test is to ensure that for all active venues in sscs-venues.csv, reference data
 * returns valid court venue information. The main incidence for this test is in sscs-common's RefDataService
 * but the test is here to cache auth information for performance purposes.
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_refdata_it.properties")
public class RefDataClientIt {
    public static final String SSCS_COURT_TYPE_ID = "31";

    @Autowired
    private VenueService venueService;

    @Autowired
    private RefDataApi refDataApi;

    @Autowired
    private IdamService idamService;

    @Autowired
    private VenueDataLoader venueDataLoader;

    @Test
    public void testVenueRefDataForVenueName() {
        List<String> failedIds = new ArrayList<>();
        List<String> missingInformationIds = new ArrayList<>();

        List<String> epimsIds = venueDataLoader.getActiveVenueDetailsMapByEpimsId()
            .keySet().stream()
            .distinct()
            .collect(Collectors.toList());

        IdamTokens idamTokens = idamService.getIdamTokens();

        String serviceToken = idamTokens.getServiceAuthorization();

        String oauth2Token = idamTokens.getIdamOauth2Token();

        for (String epimsId : epimsIds) {
            try {
                List<CourtVenue> sscsCourtVenues = refDataApi.courtVenueByEpimsId(serviceToken,
                    oauth2Token, epimsId).stream().filter(venue -> SSCS_COURT_TYPE_ID.equals(venue.getCourtTypeId()))
                    .collect(Collectors.toList());

                if (sscsCourtVenues.size() != 1) {
                    failedIds.add(epimsId);
                }

                checkForMissingRegionId(missingInformationIds, epimsId, sscsCourtVenues.get(0));
            } catch (Exception ex) {
                failedIds.add(epimsId);
            }
        }

        assertThat(failedIds).isEmpty();
        assertThat(missingInformationIds).isEmpty();
    }

    private static void checkForMissingRegionId(List<String> missingInformationIds, String epimsId,
                                  CourtVenue sscsCourtVenue) {
        if (StringUtils.isEmpty(sscsCourtVenue.getRegionId())) {
            missingInformationIds.add(epimsId);
        }
    }


}
