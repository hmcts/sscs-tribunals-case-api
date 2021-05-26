package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.isDateInTheFuture;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Component
@Slf4j
public class MidEventValidationHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final Validator validator;

    @Autowired
    public MidEventValidationHandler(Validator validator) {
        this.validator = validator;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
            && (callback.getEvent() == EventType.NOT_LISTABLE
                || callback.getEvent() == EventType.UPDATE_NOT_LISTABLE)
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

        validateDirectionDueDateIsInTheFuture(sscsCaseData, preSubmitCallbackResponse);
        return preSubmitCallbackResponse;
    }

    private void validateSscsCaseDataConstraints(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        Set<ConstraintViolation<SscsCaseData>> violations = validator.validate(sscsCaseData);
        violations.stream()
                .map(ConstraintViolation::getMessage)
                .forEach(preSubmitCallbackResponse::addError);
    }

    private void validateDirectionDueDateIsInTheFuture(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        validateDirectionDueDateMustBeInTheFuture(preSubmitCallbackResponse, sscsCaseData.getUpdateNotListableDueDate());
        validateDirectionDueDateMustBeInTheFuture(preSubmitCallbackResponse, sscsCaseData.getNotListableDueDate());
    }

    private void validateDirectionDueDateMustBeInTheFuture(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, String directionsDueDate) {
        if (nonNull(directionsDueDate) && !isDateInTheFuture(directionsDueDate)) {
            preSubmitCallbackResponse.addError("Directions due date must be in the future");
        }
    }

}
