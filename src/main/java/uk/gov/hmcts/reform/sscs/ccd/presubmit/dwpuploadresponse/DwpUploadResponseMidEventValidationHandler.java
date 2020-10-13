package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse.ElementsDisputed.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.ElementDisputed;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class DwpUploadResponseMidEventValidationHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse;

    private static String DUPLICATE_MESSAGE = " element contains duplicate issue codes";

    private Validator validator;

    @Autowired
    public DwpUploadResponseMidEventValidationHandler(Validator validator) {
        this.validator = validator;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
            && callback.getEvent() == EventType.DWP_UPLOAD_RESPONSE
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        Set<ConstraintViolation<SscsCaseData>> violations = validator.validate(sscsCaseData);
        for (ConstraintViolation<SscsCaseData> violation : violations) {
            preSubmitCallbackResponse.addError(violation.getMessage());
        }

        checkAt38DocIsPresent(sscsCaseData);

        checkForDuplicateIssueCodes(sscsCaseData);

        return preSubmitCallbackResponse;
    }
    
    private void checkAt38DocIsPresent(SscsCaseData sscsCaseData) {
        if ((!"uc".equalsIgnoreCase(sscsCaseData.getAppeal().getBenefitType().getCode())
                && sscsCaseData.getDwpAT38Document() == null)
            || ("uc".equalsIgnoreCase(sscsCaseData.getAppeal().getBenefitType().getCode())
                && "yes".equalsIgnoreCase(sscsCaseData.getDwpFurtherInfo())
                && sscsCaseData.getDwpAT38Document() == null)) {

            preSubmitCallbackResponse.addError("AT38 document is missing");
        }
    }

    private void checkForDuplicateIssueCodes(SscsCaseData sscsCaseData) {

        validateElementDisputedList(sscsCaseData.getElementsDisputedGeneral(), GENERAL);
        validateElementDisputedList(sscsCaseData.getElementsDisputedSanctions(), STANDARD_ALLOWANCE_SANCTIONS);
        validateElementDisputedList(sscsCaseData.getElementsDisputedOverpayment(), STANDARD_ALLOWANCE_OVERPAYMENT);
        validateElementDisputedList(sscsCaseData.getElementsDisputedHousing(), HOUSING);
        validateElementDisputedList(sscsCaseData.getElementsDisputedChildCare(), CHILDCARE);
        validateElementDisputedList(sscsCaseData.getElementsDisputedCare(), CARE);
        validateElementDisputedList(sscsCaseData.getElementsDisputedChildElement(), CHILD_ELEMENT);
        validateElementDisputedList(sscsCaseData.getElementsDisputedChildDisabled(), CHILD_DISABLED);

    }

    private void validateElementDisputedList(List<ElementDisputed> list, ElementsDisputed elementsDisputedType) {
        if (list != null
                && list.size() != list.stream().map(e -> e.getValue().getIssueCode()).distinct().count()) {
            preSubmitCallbackResponse.addError(elementsDisputedType.getValue() + DUPLICATE_MESSAGE);
        }
    }
}
