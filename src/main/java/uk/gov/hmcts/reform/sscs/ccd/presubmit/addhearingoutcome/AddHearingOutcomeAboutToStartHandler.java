package uk.gov.hmcts.reform.sscs.ccd.presubmit.addhearingoutcome;

import static uk.gov.hmcts.reform.sscs.helper.service.CaseHearingLocationHelper.mapVenueDetailsToVenue;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.service.HmcHearingsApiService;
import uk.gov.hmcts.reform.sscs.service.VenueService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingUpdateService;


@Component
@Slf4j
public class AddHearingOutcomeAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final HmcHearingsApiService hmcHearingsApiService;
    private final HearingUpdateService hearingUpdateService;
    private final VenueService venueService;

    public AddHearingOutcomeAboutToStartHandler(HmcHearingsApiService hmcHearingsApiService,
                                                HearingUpdateService hearingUpdateService, VenueService venueService) {
        this.hmcHearingsApiService = hmcHearingsApiService;
        this.hearingUpdateService = hearingUpdateService;
        this.venueService = venueService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.ADD_HEARING_OUTCOME;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        try {
            HearingsGetResponse response = hmcHearingsApiService.getHearingsRequest(Long.toString(caseDetails.getId()), HmcStatus.COMPLETED);
            List<CaseHearing> hmcHearings = response.getCaseHearings();
            log.info("Retrieved {} completed hearings for caseId {}", hmcHearings.size(), callback.getCaseDetails().getId());
            if (!hmcHearings.isEmpty()) {

                if (sscsCaseData.getCompletedHearingsList() == null) {
                    sscsCaseData.setCompletedHearingsList(new ArrayList<>());
                }

                sscsCaseData.getCompletedHearingsList().addAll(hmcHearings.stream()
                    .map(this::mapCaseHearingToHearing)
                    .filter(value -> value.getValue().getStart() != null)
                    .sorted(Comparator.reverseOrder())
                    .toList());

                sscsCaseData.setHearingOutcomeValue(HearingOutcomeValue.builder().build());
                sscsCaseData.getHearingOutcomeValue().setCompletedHearings(setHearingOutcomeCompletedHearings(sscsCaseData.getCompletedHearingsList()));
            } else {
                preSubmitCallbackResponse.addError("There are no completed hearings on the case.");
            }
        } catch (Exception e) {
            log.info("AddHearingOutcome failed for caseId {} with error {}", callback.getCaseDetails().getId(), e.getMessage());

            preSubmitCallbackResponse.addError("There was an error while retrieving hearing details; please try again after some time.");
        }
        return preSubmitCallbackResponse;
    }

    private DynamicList setHearingOutcomeCompletedHearings(List<Hearing> hearings) {
        return new DynamicList(new DynamicListItem("", ""), hearings.stream()
            .map(Hearing::getValue)
            .map(hearing -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.ENGLISH);
                String hearingLabel = hearing.getStart().format(formatter)
                    + "-" + hearing.getEnd().toLocalTime()
                    + ", " + hearing.getVenue().getName();
                return new DynamicListItem(hearing.getHearingId(), hearingLabel);
            }).toList());
    }

    private Hearing mapCaseHearingToHearing(CaseHearing caseHearing) {
        log.info("Processing completed hearing with hearingID {} for AddHearingOutcome", caseHearing.getHearingId().toString());
        VenueDetails venueDetails = venueService.getVenueDetailsForActiveVenueByEpimsId(caseHearing.getHearingDaySchedule().get(0).getHearingVenueEpimsId());
        Venue venue = mapVenueDetailsToVenue(venueDetails);

        HearingDetails hearingDetails = HearingDetails.builder()
            .hearingId(caseHearing.getHearingId().toString())
            .start(hearingUpdateService.convertUtcToUk(caseHearing.getHearingDaySchedule().get(0).getHearingStartDateTime()))
            .end(hearingUpdateService.convertUtcToUk(caseHearing.getHearingDaySchedule().get(0).getHearingEndDateTime()))
            .hearingChannel(caseHearing.getHearingChannels().get(0))
            .venue(venue)
            .epimsId(caseHearing.getHearingDaySchedule().get(0).getHearingVenueEpimsId())
            .build();

        return Hearing.builder()
            .value(hearingDetails)
            .build();
    }
}
