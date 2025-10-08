package uk.gov.hmcts.reform.sscs.helper.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist.ReadyToListAboutToSubmitHandler.EXISTING_HEARING_WARNING;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.Nullable;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.model.HearingEvent;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDaySchedule;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingSubChannel;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;

@Slf4j
@RequiredArgsConstructor
public final class HearingsServiceHelper {

    private final HmcHearingApiService hmcHearingApiService;

    public static void updateHearingId(Hearing hearing, HmcUpdateResponse response) {
        if (nonNull(response.getHearingRequestId())) {
            hearing.getValue().setHearingId(String.valueOf(response.getHearingRequestId()));
        }
    }

    public static void updateVersionNumber(Hearing hearing, HmcUpdateResponse response) {
        hearing.getValue().setVersionNumber(response.getVersionNumber());
    }

    public static HearingEvent getHearingEvent(HearingState state) {
        return HearingEvent.valueOf(state.name());
    }

    public static EventType getCcdEvent(HearingState hearingState) {
        try {
            return getHearingEvent(hearingState).getEventType();
        } catch (IllegalArgumentException ex) {
            return EventType.CASE_UPDATED;
        }
    }

    public static String getHearingId(HearingWrapper wrapper) {
        return Optional.ofNullable(wrapper.getCaseData().getLatestHearing())
            .map(Hearing::getValue)
            .map(HearingDetails::getHearingId)
            .orElse(null);
    }

    public static Long getVersion(HearingWrapper wrapper) {
        return Optional.ofNullable(wrapper.getCaseData().getLatestHearing())
            .map(Hearing::getValue)
            .map(HearingDetails::getVersionNumber)
            .filter(version -> version > 0)
            .orElse(null);
    }

    public static Hearing createHearing(Long hearingId) {
        return Hearing.builder()
            .value(HearingDetails.builder()
                .hearingId(String.valueOf(hearingId))
                .build())
            .build();
    }

    public static void addHearing(Hearing hearing, @Valid SscsCaseData caseData) {
        caseData.getHearings().add(hearing);
    }

    @Nullable
    public static Hearing getHearingById(Long hearingId, @Valid SscsCaseData caseData) {
        if (isNull(caseData.getHearings())) {
            caseData.setHearings(new ArrayList<>());
        }

        return caseData.getHearings().stream()
            .filter(hearing -> doHearingIdsMatch(hearing, hearingId))
            .findFirst()
            .orElse(null);
    }

    private static boolean doHearingIdsMatch(Hearing hearing, Long hearingId) {
        return hearing.getValue().getHearingId()
            .equals(String.valueOf(hearingId));
    }

    @Nullable
    public static CaseHearing findExistingRequestedHearings(HearingsGetResponse hearingsGetResponse, boolean isUpdateHearing) {
        return Optional.ofNullable(hearingsGetResponse)
            .map(HearingsGetResponse::getCaseHearings)
            .orElse(Collections.emptyList()).stream()
            .filter(caseHearing -> isCaseHearingRequestedOrAwaitingListing(caseHearing.getHmcStatus(), isUpdateHearing))
            .min(Comparator.comparing(CaseHearing::getHearingRequestDateTime))
            .orElse(null);
    }

    public static boolean isCaseHearingRequestedOrAwaitingListing(HmcStatus hmcStatus, boolean isUpdateHearing) {
        if (isUpdateHearing) {
            return HmcStatus.HEARING_REQUESTED == hmcStatus
                    || HmcStatus.AWAITING_LISTING == hmcStatus
                    || HmcStatus.UPDATE_REQUESTED == hmcStatus
                    || HmcStatus.UPDATE_SUBMITTED == hmcStatus;
        } else {
            return HmcStatus.HEARING_REQUESTED == hmcStatus
                    || HmcStatus.AWAITING_LISTING == hmcStatus;
        }
    }

    public PreSubmitCallbackResponse<SscsCaseData> validationCheckForListedHearings(SscsCaseData caseData, PreSubmitCallbackResponse<SscsCaseData> response) {
        HearingsGetResponse hearingsGetResponse = hmcHearingApiService.getHearingsRequest(caseData.getCcdCaseId(), HmcStatus.LISTED);
        if (HearingRoute.LIST_ASSIST == caseData.getSchedulingAndListingFields().getHearingRoute()
                && CollectionUtils.isNotEmpty(hearingsGetResponse.getCaseHearings())) {
            response.addError(EXISTING_HEARING_WARNING);
            log.error("Error on case {}: There is already a hearing request in List assist", caseData.getCcdCaseId());
        }
        return response;
    }

    public static HearingChannel getHearingSubChannel(HearingGetResponse hearingGetResponse) {
        List<HearingDaySchedule> hearingDaySchedules = hearingGetResponse.getHearingResponse().getHearingSessions();

        if (isNull(hearingDaySchedules) || hearingDaySchedules.isEmpty()) {
            return null;
        }

        var attendees = hearingDaySchedules.stream()
            .min(Comparator.comparing(HearingDaySchedule::getHearingStartDateTime))
            .map(HearingDaySchedule::getAttendees)
            .orElse(null);

        if (isNull(attendees)) {
            return null;
        }

        var hearingSubChannel = attendees.stream().filter(a -> !"DWP".equals(a.getPartyID()))
            .filter(c -> !isNull(c.getHearingSubChannel()))
            .map(b -> HearingSubChannel.getHearingSubChannel(b.getHearingSubChannel()))
            .findFirst()
            .orElse(Optional.empty());

        return hearingSubChannel.map(HearingSubChannel::getHearingChannel).orElse(null);
    }

    public static void checkBenefitIssueCode(Boolean validIssueBenefit) throws ListingException {
        if (!validIssueBenefit) {
            log.error("sessionCaseCode is null. The benefit/issue code is probably an incorrect combination"
                    + " and cannot be mapped to a session code. Refer to the panel-category-map.json file"
                    + " for the correct combinations.");
            throw new ListingException("Incorrect benefit/issue code combination");
        }
    }
}
