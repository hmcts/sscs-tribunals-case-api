package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import java.time.LocalDate;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class AdjournCaseMidEventValidationHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
            && callback.getEvent() == EventType.ADJOURN_CASE
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        try {

            if (sscsCaseData.isAdjournCaseDirectionsMadeToParties() && isDirectionsDueDateInvalid(sscsCaseData)) {
                preSubmitCallbackResponse.addError("Directions due date must be in the future");
            }
            if (("specificDateAndTime".equalsIgnoreCase(sscsCaseData.getAdjournCaseNextHearingDateType()))
                && isNextHearingSpecifiedDateInvalid(sscsCaseData)) {
                preSubmitCallbackResponse.addError("Specified date cannot be in the past");
            } else if ("provideDate".equalsIgnoreCase(sscsCaseData.getAdjournCaseNextHearingDateOrPeriod()) && "firstAvailableDateAfter".equalsIgnoreCase(sscsCaseData.getAdjournCaseNextHearingDateType())
                && isNextHearingFirstAvailableDateAfterDateInvalid(sscsCaseData)) {
                preSubmitCallbackResponse.addError("'First available date after' date cannot be in the past");
            }


        } catch (IllegalStateException e) {
            log.error(e.getMessage() + ". Something has gone wrong for caseId: ", sscsCaseData.getCcdCaseId());
            preSubmitCallbackResponse.addError(e.getMessage());
        }

        return preSubmitCallbackResponse;
    }

    private boolean isNextHearingSpecifiedDateInvalid(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getAdjournCaseNextHearingSpecificDate() != null) {
            LocalDate now = LocalDate.now();
            return LocalDate.parse(sscsCaseData.getAdjournCaseNextHearingSpecificDate()).isBefore(now);
        } else {
            throw new IllegalStateException("Specified date must be provided");
        }
    }

    private boolean isNextHearingFirstAvailableDateAfterDateInvalid(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getAdjournCaseNextHearingFirstAvailableDateAfterDate() != null) {
            LocalDate now = LocalDate.now();
            return LocalDate.parse(sscsCaseData.getAdjournCaseNextHearingFirstAvailableDateAfterDate()).isBefore(now);
        } else {
            throw new IllegalStateException("'First available date after' date must be provided");
        }
    }

    private boolean isDirectionsDueDateInvalid(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getAdjournCaseDirectionsDueDate() != null) {
            if (sscsCaseData.getAdjournCaseDirectionsDueDateDaysOffset() != null && !"0".equals(sscsCaseData.getAdjournCaseDirectionsDueDateDaysOffset())) {
                throw new IllegalStateException(("Cannot specify both directions due date and directions due days offset"));
            }
            LocalDate directionsDueDate = LocalDate.parse(sscsCaseData.getAdjournCaseDirectionsDueDate());
            LocalDate now = LocalDate.now();
            return !directionsDueDate.isAfter(now);
        } else {
            if (sscsCaseData.getAdjournCaseDirectionsDueDateDaysOffset() == null) {
                throw new IllegalStateException(("At least one of directions due date or directions due date offset must be specified"));
            }
        }
        return false;
    }
}
