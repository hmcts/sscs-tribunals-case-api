package uk.gov.hmcts.reform.sscs.helper;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse.DwpUploadResponseAboutToSubmitHandler.NEW_OTHER_PARTY_RESPONSE_DUE_DAYS;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
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

    private static void addErrorsForExcludeDate(List<String> listOfErrors, ExcludeDate excludeDate) {
        boolean isStartDateEmpty = StringUtils.isEmpty(excludeDate.getValue().getStart());
        boolean isEndDateEmpty = StringUtils.isEmpty(excludeDate.getValue().getEnd());

        if (isStartDateEmpty) {
            listOfErrors.add("Add a start date for unavailable dates");
        }
        if (isEndDateEmpty) {
            listOfErrors.add("Add an end date for unavailable dates");
        }

        if (!isStartDateEmpty && !isEndDateEmpty) {
            LocalDate startDate = LocalDate.parse(excludeDate.getValue().getStart());
            LocalDate endDate = LocalDate.parse(excludeDate.getValue().getEnd());

            if (startDate.isAfter(endDate)) {
                listOfErrors.add("Start date must be before end date");
            }
        }
    }

    public static void validateHearingOptionsAndExcludeDates(PreSubmitCallbackResponse<SscsCaseData> response, HearingOptions hearingOptions) {
        List<String> listOfErrors = new ArrayList<>();
        List<ExcludeDate> excludeDates = Optional.ofNullable(hearingOptions.getExcludeDates()).orElse(Collections.emptyList());

        if (excludeDates.isEmpty()) {
            // when the user select yes to unavailable dates, but does not add an entry.
            listOfErrors.add("Add a start date for unavailable dates");
            listOfErrors.add("Add an end date for unavailable dates");
        }

        excludeDates.forEach(excludeDate -> addErrorsForExcludeDate(listOfErrors, excludeDate));

        Set<String> errorSet = new HashSet<>(listOfErrors);
        response.addErrors(new ArrayList<>(errorSet));
    }
}
