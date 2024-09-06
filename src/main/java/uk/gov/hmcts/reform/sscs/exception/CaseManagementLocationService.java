package uk.gov.hmcts.reform.sscs.exception;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseManagementLocation;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;
import uk.gov.hmcts.reform.sscs.service.RefDataService;
import uk.gov.hmcts.reform.sscs.service.VenueService;

import java.util.Optional;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
public class CaseManagementLocationService {

    private final RefDataService refDataService;
    private final boolean caseAccessManagementFeature;
    private final VenueService venueService;

    public CaseManagementLocationService(RefDataService refDataService,
                                         VenueService venueService,
                                         @Value("${feature.case-access-management.enabled}") boolean caseAccessManagementFeature) {
        this.refDataService = refDataService;
        this.venueService = venueService;
        this.caseAccessManagementFeature = caseAccessManagementFeature;
    }

    public Optional<CaseManagementLocation> retrieveCaseManagementLocation(String processingVenue, RegionalProcessingCenter
            regionalProcessingCenter) {
        if (caseAccessManagementFeature
            && isNotBlank(processingVenue)
            && nonNull(regionalProcessingCenter)) {

            String venueEpimsId = venueService.getEpimsIdForVenue(processingVenue);
            CourtVenue courtVenue = refDataService.getCourtVenueRefDataByEpimsId(venueEpimsId);

            if (nonNull(courtVenue)
                && isNotBlank(courtVenue.getRegionId())) {
                return Optional.of(CaseManagementLocation.builder()
                    .baseLocation(regionalProcessingCenter.getEpimsId())
                    .region(courtVenue.getRegionId())
                    .build());
            }
        }
        return Optional.empty();
    }

}
