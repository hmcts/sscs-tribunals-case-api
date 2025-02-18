package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.helper.service.CaseHearingLocationHelper.mapVenueDetailsToVenue;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDaySchedule;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingUpdateService;

@Slf4j
@Service
public class HearingOutcomeService {

    private final HearingUpdateService hearingUpdateService;
    private final VenueService venueService;

    public HearingOutcomeService(HearingUpdateService hearingUpdateService, VenueService venueService) {
        this.hearingUpdateService = hearingUpdateService;
        this.venueService = venueService;
    }


    public DynamicList setHearingOutcomeCompletedHearings(List<Hearing> hearings) {
        return new DynamicList(new DynamicListItem("", ""), hearings.stream()
                .map(Hearing::getValue)
                .map(hearing -> {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.ENGLISH);
                    String hearingLabel = hearing.getStart().format(formatter) + "-" + hearing.getEnd().toLocalTime();
                    if (hearing.getVenue().getName() != null) {
                        hearingLabel += ", " + hearing.getVenue().getName();
                    }
                    return new DynamicListItem(hearing.getHearingId(), hearingLabel);
                }).toList());
    }

    public Hearing mapCaseHearingToHearing(CaseHearing caseHearing) {
        log.info("Processing completed hearing with hearingID {} for AddHearingOutcome", caseHearing.getHearingId().toString());
        HearingDetails hearingDetails = HearingDetails.builder()
                .hearingId(caseHearing.getHearingId().toString())
                .venue(Venue.builder().build())
                .build();
        HearingDaySchedule hearingDaySchedule = caseHearing.getHearingDaySchedule().stream().findFirst().orElse(
                HearingDaySchedule.builder().build());

        if (!isNull(hearingDaySchedule.getHearingVenueEpimsId())) {
            hearingDetails.setEpimsId(hearingDaySchedule.getHearingVenueEpimsId());
            hearingDetails.setVenue(mapEpimsIdToVenue(hearingDaySchedule.getHearingVenueEpimsId()));
        }

        if (!isNull(hearingDaySchedule.getHearingStartDateTime())) {
            hearingDetails.setStart(hearingUpdateService.convertUtcToUk(hearingDaySchedule.getHearingStartDateTime()));
        }

        if (!isNull(hearingDaySchedule.getHearingEndDateTime())) {
            hearingDetails.setEnd(hearingUpdateService.convertUtcToUk(hearingDaySchedule.getHearingEndDateTime()));
        }

        if (!isNull(caseHearing.getHearingChannels())) {
            hearingDetails.setHearingChannel(caseHearing.getHearingChannels().stream().findFirst().orElse(null));
        }

        return Hearing.builder()
                .value(hearingDetails)
                .build();
    }

    private Venue mapEpimsIdToVenue(String epimsId) {
        VenueDetails venueDetails = venueService.getVenueDetailsForActiveVenueByEpimsId(epimsId);
        if (isNull(venueDetails)) {
            log.info("EpimsId {} was not found", epimsId);
        }
        return (isNull(venueDetails)) ? Venue.builder().build() : mapVenueDetailsToVenue(venueDetails);
    }
}
