package uk.gov.hmcts.reform.sscs.ccd.presubmit.adminamendlocationdetails;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.VenueService;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminAmendLocationDetailsAboutToStart implements PreSubmitCallbackHandler<SscsCaseData> {
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final VenueService venueService;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.ADMIN_AMEND_LOCATION_DETAILS;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        List<DynamicListItem> rpcDropdownList = getRpcDropdown();
        log.info("Found {} RPCs", rpcDropdownList.size());
        int caseRpcIndex = IntStream.range(0, rpcDropdownList.size())
                .filter(i -> rpcDropdownList.get(i).getCode().equals(sscsCaseData.getRegionalProcessingCenter().getName()))
                .findFirst()
                .orElse(0);

        if (rpcDropdownList.isEmpty()) {
            response.addError("Couldn't get list of RPCs");
        } else {
            sscsCaseData.getExtendedSscsCaseData().setLocationDetailsRpc(null);
            sscsCaseData.getExtendedSscsCaseData()
                    .setLocationDetailsRpc(new DynamicList(rpcDropdownList.get(caseRpcIndex), rpcDropdownList));
        }

        List<DynamicListItem> gapsVenuesDropdownList = getGapsVenueDropdown();
        log.info("Found {} venues", gapsVenuesDropdownList.size());
        int regionalVenueIndex = IntStream.range(0, gapsVenuesDropdownList.size())
                .filter(i -> gapsVenuesDropdownList.get(i).getLabel().equals(sscsCaseData.getProcessingVenue()))
                .findFirst()
                .orElse(0);

        if (gapsVenuesDropdownList.isEmpty()) {
            response.addError("Couldn't get list of venues");
        } else {
            sscsCaseData.getExtendedSscsCaseData().setLocationDetailsProcessingVenue(null);
            sscsCaseData.getExtendedSscsCaseData().setLocationDetailsProcessingVenue(
                    new DynamicList(gapsVenuesDropdownList.get(regionalVenueIndex), gapsVenuesDropdownList));

            String latestHearingEpims;
            if (sscsCaseData.getLatestHearing() != null) {
                latestHearingEpims = sscsCaseData.getLatestHearing().getValue().getEpimsId();
            } else {
                latestHearingEpims = null;
            }

            int defaultHearingVenueIndex = IntStream.range(0, gapsVenuesDropdownList.size())
                .filter(i -> gapsVenuesDropdownList.get(i).getCode().equals(latestHearingEpims))
                .findFirst()
                .orElse(0);
            sscsCaseData.getExtendedSscsCaseData().setLocationDetailsHearingVenue(null);
            sscsCaseData.getExtendedSscsCaseData().setLocationDetailsHearingVenue(
                    new DynamicList(gapsVenuesDropdownList.get(defaultHearingVenueIndex), gapsVenuesDropdownList));
        }

        return response;
    }

    private List<DynamicListItem> getRpcDropdown() {
        ArrayList<DynamicListItem> rpcList = regionalProcessingCenterService.getRegionalProcessingCenterMap().values().stream()
                .map(regionalProcessingCenter -> new DynamicListItem(regionalProcessingCenter.getName(), regionalProcessingCenter.getName()))
                .collect(Collectors.toCollection(ArrayList::new));

        rpcList.addFirst(new DynamicListItem("null", "Choose a processing centre"));
        return rpcList;
    }

    private List<DynamicListItem> getGapsVenueDropdown() {
        ArrayList<DynamicListItem> venueList = venueService.getAllVenuesMap().values().stream()
                .map(venueDetails -> new DynamicListItem(venueDetails.getEpimsId(), venueDetails.getGapsVenName()))
                .collect(Collectors.toCollection(ArrayList::new));

        venueList.addFirst(new DynamicListItem("null", "Choose a Venue"));
        return venueList;
    }
}
