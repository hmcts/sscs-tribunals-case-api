package uk.gov.hmcts.reform.sscs.helper;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INCOMPLETE_APPLICATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INCOMPLETE_APPLICATION_INFORMATION_REQUESTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse.DwpUploadResponseAboutToSubmitHandler.NEW_OTHER_PARTY_RESPONSE_DUE_DAYS;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.generateDwpResponseDueDate;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.getLocalDateTime;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isValidBenefitTypeForConfidentiality;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@Slf4j
public class SscsHelper {

    private static final List<State> PRE_VALID_STATES =
            asList(INCOMPLETE_APPLICATION, INCOMPLETE_APPLICATION_INFORMATION_REQUESTED, INTERLOCUTORY_REVIEW_STATE);

    private SscsHelper() {
    }

    public static List<State> getPreValidStates() {
        return PRE_VALID_STATES;
    }

    private static boolean hasNewOtherPartyEntryAdded(SscsCaseData sscsCaseData) {
        Optional<CcdValue<OtherParty>> otherParty = Optional.ofNullable(sscsCaseData.getOtherParties()).orElse(Collections.emptyList()).stream().filter(
                op -> YesNo.isYes(op.getValue().getSendNewOtherPartyNotification())
        ).findFirst();
        return otherParty.isPresent();
    }

    public static String getUpdatedDirectionDueDate(final SscsCaseData sscsCaseData) {
        if (!isValidBenefitTypeForConfidentiality(sscsCaseData.getAppeal().getBenefitType())) {
            return sscsCaseData.getDirectionDueDate();
        }
        log.info("Attempting to update direction due date for caseId: {}", sscsCaseData.getCcdCaseId());
        if (isEmpty(sscsCaseData.getDirectionDueDate()) && hasNewOtherPartyEntryAdded(sscsCaseData)) {
            return generateDwpResponseDueDate(NEW_OTHER_PARTY_RESPONSE_DUE_DAYS);
        } else if (!isEmpty(sscsCaseData.getDirectionDueDate())) {
            LocalDate directionDueDate = LocalDate.parse(sscsCaseData.getDirectionDueDate());
            long dueDateLength = ChronoUnit.DAYS.between(LocalDate.now(), directionDueDate);
            if (dueDateLength <= 14) {
                return generateDwpResponseDueDate(NEW_OTHER_PARTY_RESPONSE_DUE_DAYS);
            }
        }
        return sscsCaseData.getDirectionDueDate();
    }

    public static Set<String> validateHearingOptionsAndExcludeDates(final List<ExcludeDate> excludeDates) {
        List<String> listOfErrors = new ArrayList<>();

        if (isNull(excludeDates) || excludeDates.isEmpty()) {
            listOfErrors.add("Add a start date for unavailable dates");
            listOfErrors.add("Add an end date for unavailable dates");
        } else {
            excludeDates.forEach(excludeDate -> listOfErrors.addAll(getErrorsForExcludeDate(excludeDate)));
        }
        return new HashSet<>(listOfErrors);
    }

    private static List<String> getErrorsForExcludeDate(final ExcludeDate excludeDate) {
        List<String> errors = new ArrayList<>();

        boolean isStartDateEmpty = isEmpty(excludeDate.getValue().getStart());
        boolean isEndDateEmpty = isEmpty(excludeDate.getValue().getEnd());

        if (isStartDateEmpty) {
            errors.add("Add a start date for unavailable dates");
        }
        if (isEndDateEmpty) {
            errors.add("Add an end date for unavailable dates");
        }

        if (!isStartDateEmpty && !isEndDateEmpty) {
            LocalDate startDate = LocalDate.parse(excludeDate.getValue().getStart());
            LocalDate endDate = LocalDate.parse(excludeDate.getValue().getEnd());

            if (startDate.isAfter(endDate)) {
                errors.add("Unavailability start date must be before end date");
            }
        }
        return errors;
    }

    private static boolean isValidHearing(HearingDetails hearingDetails) {
        return hearingDetails != null
            && StringUtils.isNotBlank(hearingDetails.getHearingDate())
            && hearingDetails.getVenue() != null
            && StringUtils.isNotBlank(hearingDetails.getVenue().getName());
    }

    private static boolean getValidHearing(Hearing hearing) {
        HearingDetails hearingDetails = hearing.getValue();
        if (isValidHearing(hearingDetails)) {
            LocalDateTime hearingDateTime = getLocalDateTime(hearingDetails.getHearingDate(), hearingDetails.getTime());
            return Optional.of(hearingDateTime).filter(d -> d.isAfter(LocalDateTime.now())).isPresent();
        }
        return false;
    }

    public static boolean hasHearingScheduledInTheFuture(SscsCaseData caseData) {
        Optional<Hearing> futureHearing = Optional.ofNullable(caseData.getHearings())
            .orElse(Collections.emptyList())
            .stream().filter(SscsHelper::getValidHearing).findFirst();
        return futureHearing.isPresent();
    }
}