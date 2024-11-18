package uk.gov.hmcts.reform.sscs.service.hmc.topic;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.HEARING_DATE_ISSUED;
import static uk.gov.hmcts.reform.sscs.helper.service.CaseHearingLocationHelper.mapVenueDetailsToVenue;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.LISTED;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.JudicialUserPanel;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.ccd.domain.WorkBasketFields;
import uk.gov.hmcts.reform.sscs.exception.InvalidHearingDataException;
import uk.gov.hmcts.reform.sscs.exception.InvalidMappingException;
import uk.gov.hmcts.reform.sscs.exception.MessageProcessingException;
import uk.gov.hmcts.reform.sscs.helper.service.HearingsServiceHelper;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDaySchedule;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.service.JudicialRefDataService;
import uk.gov.hmcts.reform.sscs.service.VenueService;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.ExcessiveImports")
public class HearingUpdateService {
    public static final int EXPECTED_SESSIONS = 1;
    private final VenueService venueService;
    private final JudicialRefDataService judicialRefDataService;
    @Value("${flags.postHearings.enabled}")
    private boolean isPostHearingsEnabled;

    public void updateHearing(HearingGetResponse hearingGetResponse, @Valid SscsCaseData sscsCaseData)
            throws MessageProcessingException, InvalidMappingException {
        Long hearingId = Long.valueOf(hearingGetResponse.getRequestDetails().getHearingRequestId());
        HearingDaySchedule hearingDaySchedule = getHearingDaySchedule(hearingGetResponse, sscsCaseData, hearingId);
        String hearingEpimsId = hearingDaySchedule.getHearingVenueEpimsId();
        VenueDetails venueDetails = venueService.getVenueDetailsForActiveVenueByEpimsId(hearingEpimsId);

        if (isNull(venueDetails)) {
            throw new InvalidMappingException(String.format(
                    "Invalid epims Id %s, unable to find active venue with that id, regarding Case Id %s",
                    hearingEpimsId,
                    sscsCaseData.getCcdCaseId()
            ));
        }

        Venue venue = mapVenueDetailsToVenue(venueDetails);

        Hearing hearing = HearingsServiceHelper.getHearingById(hearingId, sscsCaseData);

        if (isNull(hearing)) {
            hearing = HearingsServiceHelper.createHearing(hearingId);
            HearingsServiceHelper.addHearing(hearing, sscsCaseData);
        }

        HearingDetails hearingDetails = hearing.getValue();
        hearingDetails.setEpimsId(hearingEpimsId);
        hearingDetails.setVenue(venue);
        LocalDateTime hearingStartDateTime = hearingDaySchedule.getHearingStartDateTime();
        LocalDateTime hearingEndDateTime = hearingDaySchedule.getHearingEndDateTime();
        hearingDetails.setStart(convertUtcToUk(hearingStartDateTime));
        hearingDetails.setEnd(convertUtcToUk(hearingEndDateTime));
        String hearingDate = hearingStartDateTime.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        hearingDetails.setHearingDate(hearingDate);
        String hearingTime = hearingStartDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        hearingDetails.setTime(hearingTime);

        var hearingChannel = HearingsServiceHelper.getHearingSubChannel(hearingGetResponse);
        if (!isNull(hearingChannel)) {
            hearingDetails.setHearingChannel(hearingChannel);
        }
        List<String> panelMemberIds = hearingDaySchedule.getPanelMemberIds();

        if (isPostHearingsEnabled && nonNull(panelMemberIds)) {
            JudicialUserPanel panel = JudicialUserPanel.builder()
                    .assignedTo(judicialRefDataService.getJudicialUserFromPersonalCode(hearingDaySchedule.getHearingJudgeId()))
                    .panelMembers(panelMemberIds.stream().map(id -> new CollectionItem<>(id, judicialRefDataService.getJudicialUserFromPersonalCode(id))).toList())
                    .build();

            hearingDetails.setPanel(panel);
        }

        log.info(
                "Venue has been updated from epimsId '{}' to '{}' for Case Id: {} with hearingId {}",
                hearingDetails.getEpimsId(),
                hearingEpimsId,
                sscsCaseData.getCcdCaseId(),
                hearingId
        );
    }

    private static HearingDaySchedule getHearingDaySchedule(HearingGetResponse hearingGetResponse, SscsCaseData sscsCaseData,
                                                            Long hearingId) throws InvalidHearingDataException {
        List<HearingDaySchedule> hearingSessions = hearingGetResponse.getHearingResponse().getHearingSessions();

        if (hearingSessions.size() != EXPECTED_SESSIONS) {
            throw new InvalidHearingDataException(
                    String.format(
                            "Invalid HearingDaySchedule, should have 1 session but instead has %d sessions, for Case Id %s and Hearing Id %s",
                            hearingSessions.size(),
                            sscsCaseData.getCcdCaseId(),
                            hearingId
                    ));
        }
        return hearingSessions.get(0);
    }

    public void setHearingStatus(String hearingId, @Valid SscsCaseData sscsCaseData, HmcStatus hmcStatus) {
        HearingStatus hearingStatus = hmcStatus.getHearingStatus();
        if (isNull(hearingStatus)) {
            return;
        }

        Hearing hearing = HearingsServiceHelper.getHearingById(Long.valueOf(hearingId), sscsCaseData);
        if (isNull(hearing)) {
            return;
        }

        hearing.getValue().setHearingStatus(hearingStatus);
    }

    public DwpState resolveDwpState(HmcStatus hmcStatus) {
        if (isCaseListed(hmcStatus)) {
            return HEARING_DATE_ISSUED;
        } else {
            return null;
        }
    }

    public void setWorkBasketFields(String hearingId, @Valid SscsCaseData sscsCaseData, HmcStatus listAssistCaseStatus) {

        WorkBasketFields workBasketFields = sscsCaseData.getWorkBasketFields();

        if (isCaseListed(listAssistCaseStatus)) {
            LocalDate hearingDate = getHearingDate(hearingId, sscsCaseData);
            workBasketFields.setHearingDate(hearingDate);

            if (isNull(workBasketFields.getHearingDateIssued())) {
                LocalDateTime hearingDateIssuedTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                String hearingDateIssued = hearingDateIssuedTime.format(formatter);
                log.debug(
                        "Setting workBasketField hearingDateIssued {} for case id reference {}",
                        hearingDateIssued,
                        sscsCaseData.getCcdCaseId()
                );
                workBasketFields.setHearingDateIssued(hearingDateIssued);
            }

            String epimsId = getHearingEpimsId(hearingId, sscsCaseData);
            workBasketFields.setHearingEpimsId(epimsId);
        } else {
            workBasketFields.setHearingDate(null);
            workBasketFields.setHearingDateIssued(null);
            workBasketFields.setHearingEpimsId(null);
        }
    }

    public LocalDate getHearingDate(String hearingId, @Valid SscsCaseData sscsCaseData) {
        return Optional.ofNullable(HearingsServiceHelper.getHearingById(Long.valueOf(hearingId), sscsCaseData))
                .map(Hearing::getValue)
                .map(HearingDetails::getStart)
                .map(LocalDateTime::toLocalDate)
                .orElse(null);
    }

    public String getHearingEpimsId(String hearingId, @Valid SscsCaseData sscsCaseData) {
        return Optional.ofNullable(HearingsServiceHelper.getHearingById(Long.valueOf(hearingId), sscsCaseData))
                .map(Hearing::getValue)
                .map(HearingDetails::getEpimsId)
                .filter(StringUtils::isNotBlank)
                .orElse(null);
    }

    public boolean isCaseListed(HmcStatus hmcStatus) {
        return LISTED == hmcStatus;
    }

    public LocalDateTime convertUtcToUk(LocalDateTime utcLocalDateTime) {
        ZonedDateTime utcZone = utcLocalDateTime.atZone(ZoneId.of("UTC"));
        return utcZone.withZoneSameInstant(ZoneId.of("Europe/London")).toLocalDateTime();
    }
}
