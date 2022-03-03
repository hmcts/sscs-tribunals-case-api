package uk.gov.hmcts.reform.sscs.service.venue;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;

@Service
@Slf4j
public class VenueRpcDetailsService {

    private final VenueDataLoader venueDataLoader;

    @Autowired
    public VenueRpcDetailsService(VenueDataLoader venueDataLoader) {
        this.venueDataLoader = venueDataLoader;
    }

    public List<VenueRpcDetails> getVenues(Predicate<VenueRpcDetails> predicate) {
        return venueDataLoader.getVenueDetailsMap().values().stream().filter(this::isActiveVenue)
            .map(VenueRpcDetails::new).filter(predicate).collect(Collectors.toList());
    }

    private boolean isActiveVenue(VenueDetails venueDetails) {
        return venueDetails != null && isYes(venueDetails.getActive());
    }
}
