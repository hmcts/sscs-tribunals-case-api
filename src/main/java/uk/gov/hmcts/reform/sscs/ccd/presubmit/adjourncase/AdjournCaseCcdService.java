package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.service.venue.VenueRpcDetails;
import uk.gov.hmcts.reform.sscs.service.venue.VenueRpcDetailsService;

@Component
@Slf4j
public class AdjournCaseCcdService {

    private final VenueRpcDetailsService venueRpcDetailsService;

    @Autowired
    public AdjournCaseCcdService(VenueRpcDetailsService venueRpcDetailsService) {
        this.venueRpcDetailsService = venueRpcDetailsService;
    }

    private List<DynamicListItem> getSortedVenueItems(Predicate<VenueRpcDetails> predicate, boolean prefixWithRpc) {
        List<DynamicListItem> venueItems = venueRpcDetailsService.getVenues(predicate).stream().map(v ->
                new DynamicListItem(v.getVenueId(),
                        v.getVenueDisplayString(prefixWithRpc))).sorted(new DynamicListItemComparator()).collect(Collectors.toList());
        return venueItems;
    }

    public DynamicList getVenueDynamicListForRpcName(String rpcName) {

        List<DynamicListItem> fullVenueList = new ArrayList<>();

        // Add the sorted venues for this RPC, without prefixing with RPC name
        fullVenueList.addAll(getSortedVenueItems(v -> v.getRpcInCaseDataFormat().equalsIgnoreCase(rpcName), false));

        // Add the sorted venues for other RPCs, prefixing with RPC name
        fullVenueList.addAll(getSortedVenueItems(v -> !v.getRpcInCaseDataFormat().equalsIgnoreCase(rpcName), true));

        return new DynamicList(new DynamicListItem("", ""), fullVenueList);
    }
}
