package uk.gov.hmcts.reform.sscs.helper;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse.DwpUploadResponseAboutToSubmitHandler.NEW_OTHER_PARTY_RESPONSE_DUE_DAYS;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.getLocalDateTime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.util.DateTimeUtils;
import uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil;

@Slf4j
public class SscsHelper {

    private static final List<State> PRE_VALID_STATES = new ArrayList<>(Arrays.asList(INCOMPLETE_APPLICATION, INCOMPLETE_APPLICATION_INFORMATION_REQUESTED, INTERLOCUTORY_REVIEW_STATE));

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

    public static void updateDirectionDueDateByAnAmountOfDays(SscsCaseData sscsCaseData) {
        if (!OtherPartyDataUtil.isValidBenefitTypeForConfidentiality(sscsCaseData)) {
            return;
        }
        log.info("Attempting to update direction due date for caseId: {}", sscsCaseData.getCcdCaseId());
        if (isEmpty(sscsCaseData.getDirectionDueDate()) && hasNewOtherPartyEntryAdded(sscsCaseData)) {
            sscsCaseData.setDirectionDueDate(DateTimeUtils.generateDwpResponseDueDate(NEW_OTHER_PARTY_RESPONSE_DUE_DAYS));
        } else if (!isEmpty(sscsCaseData.getDirectionDueDate())) {
            LocalDate directionDueDate = LocalDate.parse(sscsCaseData.getDirectionDueDate());
            long dueDateLength = ChronoUnit.DAYS.between(LocalDate.now(), directionDueDate);
            if (dueDateLength <= 14) {
                sscsCaseData.setDirectionDueDate(DateTimeUtils.generateDwpResponseDueDate(NEW_OTHER_PARTY_RESPONSE_DUE_DAYS));
            }
        }
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
