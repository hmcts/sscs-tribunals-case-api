package uk.gov.hmcts.reform.sscs.service.venue;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
        return venueDataLoader.getVenueDetailsMap().values().stream()
            .map(v -> new VenueRpcDetails(v)).filter(predicate).collect(Collectors.toList());
    }
}
