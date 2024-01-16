package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.isDateInTheFuture;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.isDateInThePast;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseDaysOffset;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class AdjournCaseMidEventValidationHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final Validator validator;

    @Autowired
    public AdjournCaseMidEventValidationHandler(Validator validator) {
        this.validator = validator;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
            && callback.getEvent() == EventType.ADJOURN_CASE
            && nonNull(callback.getCaseDetails())
            && nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        validateSscsCaseDataConstraints(sscsCaseData, preSubmitCallbackResponse);
        validateAdjournCaseDirectionsDueDateIsInFuture(sscsCaseData, preSubmitCallbackResponse);
        validateAdjournCaseEventValues(sscsCaseData, preSubmitCallbackResponse);

        return preSubmitCallbackResponse;
    }

    private void validateAdjournCaseEventValues(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        try {

            if (isYes(sscsCaseData.getAdjournment().getAreDirectionsBeingMadeToParties())) {
                checkDirectionsDueDateInvalid(sscsCaseData);
            }
            if (adjournCaseNextHearingDateOrPeriodIsProvideDate(sscsCaseData)
                    && adjournCaseNextHearingDateTypeIsFirstAvailableDateAfter(sscsCaseData)
                    && isNextHearingFirstAvailableDateAfterDateInvalid(sscsCaseData)) {
                preSubmitCallbackResponse.addError("'First available date after' date cannot be in the past");
            }

        } catch (IllegalStateException e) {
            log.error(e.getMessage() + ". Something has gone wrong for caseId: ", sscsCaseData.getCcdCaseId());
            preSubmitCallbackResponse.addError(e.getMessage());
        }
    }

    private boolean adjournCaseNextHearingDateOrPeriodIsProvideDate(SscsCaseData sscsCaseData) {
        return sscsCaseData.getAdjournment().getNextHearingDateOrPeriod() == AdjournCaseNextHearingDateOrPeriod.PROVIDE_DATE;
    }

    private boolean adjournCaseNextHearingDateTypeIsFirstAvailableDateAfter(SscsCaseData sscsCaseData) {
        return sscsCaseData.getAdjournment().getNextHearingDateType() == AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER;
    }


    private void validateSscsCaseDataConstraints(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        Set<ConstraintViolation<SscsCaseData>> violations = validator.validate(sscsCaseData);
        violations.stream()
                .map(ConstraintViolation::getMessage)
                .forEach(preSubmitCallbackResponse::addError);
    }

    private void validateAdjournCaseDirectionsDueDateIsInFuture(
        SscsCaseData sscsCaseData,
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (nonNull(sscsCaseData.getAdjournment().getDirectionsDueDate())
            && !isDateInTheFuture(sscsCaseData.getAdjournment().getDirectionsDueDate())
        ) {
            preSubmitCallbackResponse.addError("Directions due date must be in the future");
        }
    }

    private boolean isNextHearingFirstAvailableDateAfterDateInvalid(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getAdjournment().getNextHearingFirstAvailableDateAfterDate() == null) {
            throw new IllegalStateException("'First available date after' date must be provided");
        }
        return isDateInThePast(sscsCaseData.getAdjournment().getNextHearingFirstAvailableDateAfterDate());
    }

    private void checkDirectionsDueDateInvalid(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getAdjournment().getDirectionsDueDate() != null) {
            if (directionDueDayIsNotEmptyOrZero(sscsCaseData)) {
                throw new IllegalStateException(("Cannot specify both directions due date and directions due days offset"));
            }
        } else {
            if (directionDueDaysIsEmpty(sscsCaseData)) {
                throw new IllegalStateException(("At least one of directions due date or directions due date offset must be specified"));
            }
        }
    }

    private boolean directionDueDaysIsEmpty(SscsCaseData sscsCaseData) {
        return sscsCaseData.getAdjournment().getDirectionsDueDateDaysOffset() == null;
    }

    private boolean directionDueDayIsNotEmptyOrZero(SscsCaseData sscsCaseData) {
        return sscsCaseData.getAdjournment().getDirectionsDueDateDaysOffset() != null
            && sscsCaseData.getAdjournment().getDirectionsDueDateDaysOffset() != AdjournCaseDaysOffset.OTHER;
    }
}
