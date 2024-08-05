package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.isDateInTheFuture;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.isDateInThePast;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseDaysOffset;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Component
@Slf4j
public class AdjournCaseMidEventValidationService {

    private final Validator validator;

    public AdjournCaseMidEventValidationService(Validator validator) {
        this.validator = validator;
    }

    public void validateSscsCaseDataConstraints(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        Set<ConstraintViolation<SscsCaseData>> violations = validator.validate(sscsCaseData);
        violations.stream()
                .map(ConstraintViolation::getMessage)
                .forEach(preSubmitCallbackResponse::addError);
    }

    public boolean adjournCaseNextHearingDateOrPeriodIsProvideDate(SscsCaseData sscsCaseData) {
        return sscsCaseData.getAdjournment().getNextHearingDateOrPeriod() == AdjournCaseNextHearingDateOrPeriod.PROVIDE_DATE;
    }

    public boolean adjournCaseNextHearingDateTypeIsFirstAvailableDateAfter(SscsCaseData sscsCaseData) {
        return sscsCaseData.getAdjournment().getNextHearingDateType() == AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER;
    }

    public boolean isNextHearingFirstAvailableDateAfterDateInvalid(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getAdjournment().getNextHearingFirstAvailableDateAfterDate() == null) {
            throw new IllegalStateException("'First available date after' date must be provided");
        }
        return isDateInThePast(sscsCaseData.getAdjournment().getNextHearingFirstAvailableDateAfterDate());
    }

    public Set<String> checkDirectionsDueDateInvalid(SscsCaseData sscsCaseData) {
        Set<String> errors = new LinkedHashSet<>();
        if (sscsCaseData.getAdjournment().getDirectionsDueDate() != null) {
            if (directionDueDateOffsetIsNotEmptyOrZero(sscsCaseData)) {
                errors.add("Cannot specify both directions due date and directions due days offset");
            }
        } else {
            if (directionDueDateOffsetIsEmpty(sscsCaseData)) {
                errors.add(("At least one of directions due date or directions due date offset must be specified"));
            }
        }
        Boolean isDueDateInvalid =  nonNull(sscsCaseData.getAdjournment().getDirectionsDueDate())
                && !isDateInTheFuture(sscsCaseData.getAdjournment().getDirectionsDueDate());
        if (isDueDateInvalid) {
            errors.add("Directions due date must be in the future");
        }
        return errors;
    }

    public Set<String> checkNextHearingDateInvalid(SscsCaseData sscsCaseData) {
        Set<String> errors = new LinkedHashSet<>();
        try {
            if (adjournCaseNextHearingDateOrPeriodIsProvideDate(sscsCaseData)
                    && adjournCaseNextHearingDateTypeIsFirstAvailableDateAfter(sscsCaseData)
                    && isNextHearingFirstAvailableDateAfterDateInvalid(sscsCaseData)) {
                errors.add("'First available date after' date cannot be in the past");
            }
        } catch (IllegalStateException e) {
            log.error(e.getMessage() + ". Something has gone wrong for caseId: ", sscsCaseData.getCcdCaseId());
            errors.add(e.getMessage());
        }
        return errors;
    }

    private boolean directionDueDateOffsetIsEmpty(SscsCaseData sscsCaseData) {
        return sscsCaseData.getAdjournment().getDirectionsDueDateDaysOffset() == null;
    }

    private boolean directionDueDateOffsetIsNotEmptyOrZero(SscsCaseData sscsCaseData) {
        return sscsCaseData.getAdjournment().getDirectionsDueDateDaysOffset() != null
                && sscsCaseData.getAdjournment().getDirectionsDueDateDaysOffset() != AdjournCaseDaysOffset.OTHER;
    }
}
