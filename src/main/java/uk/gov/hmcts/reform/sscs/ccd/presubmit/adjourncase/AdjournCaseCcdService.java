package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType.TRIBUNALS_MEMBER_DISABILITY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType.TRIBUNALS_MEMBER_MEDICAL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType;
import uk.gov.hmcts.reform.sscs.service.RefDataService;
import uk.gov.hmcts.reform.sscs.service.venue.VenueRpcDetails;
import uk.gov.hmcts.reform.sscs.service.venue.VenueRpcDetailsService;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdjournCaseCcdService {

    private final VenueRpcDetailsService venueRpcDetailsService;

    private final RefDataService refDataService;

    private List<DynamicListItem> getSortedVenueItems(Predicate<VenueRpcDetails> predicate, boolean prefixWithRpc) {
        List<DynamicListItem> venueItems = venueRpcDetailsService.getVenues(predicate).stream().map(v ->
            new DynamicListItem(v.getVenueId(),
                v.getVenueDisplayString(prefixWithRpc))).collect(Collectors.toList());
        Collections.sort(venueItems, new DynamicListItemComparator());
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

    public void setPanelMembers(Adjournment adjournment) {
        adjournment.setDisabilityQualifiedPanelMemberName(getPanelMembers(TRIBUNALS_MEMBER_DISABILITY));
        adjournment.setMedicallyQualifiedPanelMemberName(getPanelMembers(TRIBUNALS_MEMBER_MEDICAL));
        adjournment.setOtherPanelMemberName(getPanelMembers(null));
    }

    public DynamicList getPanelMembers(PanelMemberType panelMemberType) {

        List<String> names = refDataService.getPanelMembers(panelMemberType);

        List<DynamicListItem> panelMembers = names.stream().map(panelMember -> new DynamicListItem(panelMember, panelMember)).collect(Collectors.toList());

        panelMembers.add(new DynamicListItem("aaaa", "aaaa"));

        return new DynamicList(new DynamicListItem("", ""), panelMembers);
    }
}
