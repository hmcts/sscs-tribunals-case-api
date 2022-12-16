package uk.gov.hmcts.reform.sscs.functional.client;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.client.RefDataApi;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;
import uk.gov.hmcts.reform.sscs.service.VenueService;

/**
 * Intention of this test is to ensure that for all active venues in sscs-venues.csv, reference data
 * returns valid court venue information. The main incidence for this test is in sscs-common's RefDataService
 * but the test is here to cache auth information for performance purposes.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
@TestPropertySource(locations = "classpath:config/application_refdata_test.properties")
public class RefDataClientTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();
    public static final String SSCS_COURT_TYPE_ID = "31";
    public static final String OPEN = "Open";

    @Autowired
    private VenueService venueService;

    @Autowired
    private RefDataApi refDataApi;

    @Autowired
    private IdamService idamService;

    @Autowired
    private VenueDataLoader venueDataLoader;

    // TODO: SSCS-11275 - re-enable when ref data is fixed
    @Ignore
    @Test
    public void testVenueRefDataForVenueName() {
        List<String> failedIds = new ArrayList<>();
        List<String> missingInformationIds = new ArrayList<>();
        List<String> duplicateIds = new ArrayList<>();

        List<String> epimsIds = venueDataLoader.getActiveVenueDetailsMapByEpimsId()
            .keySet().stream()
            .distinct()
            .collect(Collectors.toList());

        IdamTokens idamTokens = idamService.getIdamTokens();

        String serviceToken = idamTokens.getServiceAuthorization();

        String oauth2Token = idamTokens.getIdamOauth2Token();

        for (String epimsId : epimsIds) {
            try {
                List<CourtVenue> sscsCourtVenues = refDataApi.courtVenueByEpimsId(oauth2Token,
                        serviceToken, epimsId).stream().filter(venue -> SSCS_COURT_TYPE_ID.equals(venue.getCourtTypeId())
                        && OPEN.equalsIgnoreCase(venue.getCourtStatus()))
                    .collect(Collectors.toList());

                if (sscsCourtVenues.size() > 1) {
                    duplicateIds.add(epimsId);
                }

                checkForMissingRegionId(missingInformationIds, epimsId, sscsCourtVenues.get(0));
            } catch (Exception ex) {
                log.error(ex.getMessage());
                failedIds.add(epimsId);
            }
        }

        softly.assertThat(failedIds)
            .as("No court venues were found for some epims ids.")
            .isEmpty();
        softly.assertThat(duplicateIds)
            .as("Multiple court venues were found for some epims ids.")
            .isEmpty();
        softly.assertThat(missingInformationIds)
            .as("Not all required court venue information was found for some epims ids.")
            .isEmpty();
    }

    private static void checkForMissingRegionId(List<String> missingInformationIds, String epimsId,
                                  CourtVenue sscsCourtVenue) {
        if (StringUtils.isEmpty(sscsCourtVenue.getRegionId())) {
            missingInformationIds.add(epimsId);
        }
    }
}
