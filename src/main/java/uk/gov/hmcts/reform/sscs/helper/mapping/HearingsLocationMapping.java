package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing.PAPER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.LocationType.COURT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.model.HearingLocation;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.VenueService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;
import uk.gov.hmcts.reform.sscs.utility.HearingChannelUtil;

@Slf4j
public final class HearingsLocationMapping {

    private HearingsLocationMapping() {
    }

    public static List<HearingLocation> getHearingLocations(SscsCaseData caseData,
                                                            ReferenceDataServiceHolder refData) throws ListingException {
        String caseId = caseData.getCcdCaseId();

        List<HearingLocation> locations = getAdjournedLocations(caseData, refData);
        if (isNotEmpty(locations)) {
            log.debug("Hearing Locations for Case ID {} set as Adjournment values", caseId);
            return locations;
        }

        locations = getOverrideLocations(caseData);
        if (isNotEmpty(locations)) {
            log.debug("Hearing Locations for Case ID {} set as Override field values", caseId);
            return locations;
        }

        locations = getPaperCaseLocations(caseData, refData);
        if (isNotEmpty(locations)) {
            log.debug("Hearing Locations for Case ID {} set as Paper Case values", caseId);
            return locations;
        }

        log.debug("Hearing Locations for Case ID {} set as multiple locations values", caseId);
        return getMultipleLocations(caseData, refData);
    }

    private static List<HearingLocation> getOverrideLocations(SscsCaseData caseData) {
        OverrideFields overrideFields = OverridesMapping.getOverrideFields(caseData);

        if (isNotEmpty(overrideFields.getHearingVenueEpimsIds())) {
            return overrideFields.getHearingVenueEpimsIds().stream()
                    .map(CcdValue::getValue)
                    .map(CcdValue::getValue)
                    .map(epimsId -> HearingLocation.builder()
                            .locationId(epimsId)
                            .locationType(COURT)
                            .build())
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private static List<HearingLocation> getPaperCaseLocations(SscsCaseData caseData, ReferenceDataServiceHolder refData) throws ListingException {
        if (HearingChannelUtil.isPaperCase(caseData)) {
            RegionalProcessingCenter rpc = caseData.getRegionalProcessingCenter();
            validatedRpc(rpc, refData, caseData.isIbcCase());

            List<VenueDetails> venueDetailsList = refData
                    .getVenueService()
                    .getActiveRegionalEpimsIdsForRpc(rpc.getEpimsId());

            log.info("Found {} venues under RPC {} for paper case {}", venueDetailsList.size(),
                    rpc.getName(), caseData.getCcdCaseId());

            return venueDetailsList.stream()
                    .map(VenueDetails::getEpimsId)
                    .map(id -> HearingLocation.builder()
                            .locationId(id)
                            .locationType(COURT)
                            .build())
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private static void validatedRpc(RegionalProcessingCenter regionalProcessingCenter, ReferenceDataServiceHolder refData, boolean isIbca) throws ListingException {
        if (nonNull(regionalProcessingCenter)) {
            String regionalProcessingCenterPostCode = regionalProcessingCenter.getPostcode();
            RegionalProcessingCenterService regionalProcessingCenterService = refData.getRegionalProcessingCenterService();
            RegionalProcessingCenter processingCenterByPostCode = regionalProcessingCenterService.getByPostcode(regionalProcessingCenterPostCode, isIbca);

            log.info("rpc by postcode {}", processingCenterByPostCode);

            if (nonNull(processingCenterByPostCode) && LIST_ASSIST.equals(processingCenterByPostCode.getHearingRoute())) {
                return;
            }
        }

        throw new ListingException("Invalid RPC");
    }

    private static List<HearingLocation> getAdjournedLocations(SscsCaseData caseData,
                                                               ReferenceDataServiceHolder refData) throws ListingException {
        if (refData.isAdjournmentFlagEnabled() && isYes(caseData.getAdjournment().getAdjournmentInProgress())) {
            List<String> epimsIds;

            Adjournment adjournment = caseData.getAdjournment();
            VenueService venueService = refData.getVenueService();
            if (PAPER.equals(adjournment.getTypeOfNextHearing())) {
                RegionalProcessingCenter rpc = caseData.getRegionalProcessingCenter();
                validatedRpc(rpc, refData, caseData.isIbcCase());

                List<VenueDetails> paperVenues = venueService.getActiveRegionalEpimsIdsForRpc(rpc.getEpimsId());

                epimsIds = paperVenues.stream().map(VenueDetails::getEpimsId).toList();
            } else {
                var nextHearingVenueSelected = adjournment.getNextHearingVenueSelected();

                String epimsId = nonNull(nextHearingVenueSelected)
                        ? venueService.getEpimsIdForVenueId(nextHearingVenueSelected.getValue().getCode())
                        : venueService.getEpimsIdForVenue(caseData.getProcessingVenue());

                epimsIds = List.of(epimsId);
            }

            return epimsIds.stream().map(epimsId -> HearingLocation.builder().locationId(epimsId).locationType(COURT).build()).toList();
        }

        return Collections.emptyList();
    }

    private static List<HearingLocation> getMultipleLocations(SscsCaseData caseData, ReferenceDataServiceHolder refData) {
        String epimsId = refData
                .getVenueService()
                .getEpimsIdForVenue(caseData.getProcessingVenue());

        Map<String, List<String>> multipleHearingLocations = refData.getMultipleHearingLocations();

        return multipleHearingLocations.values().stream()
                .filter(listValues -> listValues.contains(epimsId))
                .findFirst()
                .orElseGet(() -> Collections.singletonList(epimsId))
                .stream().map(epims -> HearingLocation.builder().locationId(epims).locationType(COURT).build())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
