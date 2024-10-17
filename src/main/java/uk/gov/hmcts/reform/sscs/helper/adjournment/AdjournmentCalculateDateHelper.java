package uk.gov.hmcts.reform.sscs.helper.adjournment;

import java.time.LocalDate;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Slf4j
public final class AdjournmentCalculateDateHelper {

    public static final int DAYS_TO_ADD_HEARING_WINDOW_TODAY_ADJOURNMENT = 14;
    public static final String FIRST_AVAILABLE_DATE = AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE.toString();
    public static final String FIRST_AVAILABLE_DATE_AFTER = AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER.toString();
    public static final String DATE_TO_BE_FIXED = AdjournCaseNextHearingDateType.DATE_TO_BE_FIXED.toString();
    public static final String PROVIDE_DATE = AdjournCaseNextHearingDateOrPeriod.PROVIDE_DATE.toString();
    public static final String PROVIDE_PERIOD = AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD.toString();

    private AdjournmentCalculateDateHelper() {
    }

    public static LocalDate getHearingWindowStart(@Valid SscsCaseData caseData) {
        log.debug("Get Adjournment Hearing Window Start Date for Case ID {}", caseData.getCcdCaseId());
        String dateType = String.valueOf(caseData.getAdjournment().getNextHearingDateType());

        if (FIRST_AVAILABLE_DATE.equals(dateType)) {
            return handleFirstAvailableDate(caseData);
        } else if (FIRST_AVAILABLE_DATE_AFTER.equals(dateType)) {
            return handleFirstAvailableDateAfter(caseData);
        } else if (DATE_TO_BE_FIXED.equals(dateType)) {
            return null;
        } else {
            throw new IllegalArgumentException(String.format(
                "Unexpected nextHearingDateType for case id %s: '%s'",
                caseData.getCcdCaseId(),
                dateType));
        }
    }

    public static LocalDate handleFirstAvailableDate(SscsCaseData caseData) {
        LocalDate result = LocalDate.now().plusDays(DAYS_TO_ADD_HEARING_WINDOW_TODAY_ADJOURNMENT);
        logAdjournmentDate(caseData, "first available date: " + result);
        return result;
    }

    public static LocalDate handleFirstAvailableDateAfter(SscsCaseData caseData) {
        String dateOrPeriod = String.valueOf(caseData.getAdjournment().getNextHearingDateOrPeriod());
        if (PROVIDE_DATE.equals(dateOrPeriod)) {
            return calculateProvideDate(caseData);
        } else if (PROVIDE_PERIOD.equals(dateOrPeriod)) {
            return calculateProvidePeriod(caseData);
        } else {
            throw new IllegalArgumentException(String.format(
                "Unexpected nextHearingDateOrPeriod for case id %s: '%s'",
                caseData.getCcdCaseId(),
                dateOrPeriod));
        }
    }

    public static LocalDate calculateProvidePeriod(SscsCaseData caseData) {
        AdjournCaseNextHearingPeriod dateAfterPeriod =
            caseData.getAdjournment().getNextHearingFirstAvailableDateAfterPeriod();

        if (dateAfterPeriod == null) {
            throw new IllegalArgumentException(String.format(
                "firstAvailableDateAfterPeriod unexpectedly null for case id %s",
                caseData.getCcdCaseId()));
        }

        Integer days = dateAfterPeriod.getCcdDefinition();
        LocalDate result = LocalDate.now().plusDays(days);
        logAdjournmentDate(caseData, String.format("first available date after %s days: %s", days, result));
        return result;
    }

    public static LocalDate calculateProvideDate(SscsCaseData caseData) {
        LocalDate date = caseData.getAdjournment().getNextHearingFirstAvailableDateAfterDate();

        if (date == null) {
            throw new IllegalArgumentException(String.format(
                "firstAvailableDateAfterDate unexpectedly null for case id %s",
                caseData.getCcdCaseId()));
        }

        logAdjournmentDate(caseData, String.format("first available date after %s", date));
        return date;
    }

    public static void logAdjournmentDate(SscsCaseData caseData, String details) {
        log.info("Case {} adjourned to {}", caseData.getCcdCaseId(), details);
    }
}
