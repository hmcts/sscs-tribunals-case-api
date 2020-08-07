package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse.ElementsDisputed.*;

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
public class DwpUploadResponseMidEventValidationHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse;

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

        checkForDuplicateIssueCodes(sscsCaseData);

        return preSubmitCallbackResponse;
    }

    private void checkForDuplicateIssueCodes(SscsCaseData sscsCaseData) {
        String duplicateMessage = " element contains duplicate issue codes";

        if (sscsCaseData.getElementsDisputedGeneral() != null
                && sscsCaseData.getElementsDisputedGeneral().size() != sscsCaseData.getElementsDisputedGeneral().stream().distinct().count()) {
            preSubmitCallbackResponse.addError(GENERAL.getValue() + duplicateMessage);
        }
        if (sscsCaseData.getElementsDisputedSanctions() != null
                && sscsCaseData.getElementsDisputedSanctions().size() != sscsCaseData.getElementsDisputedSanctions().stream().distinct().count()) {
            preSubmitCallbackResponse.addError(STANDARD_ALLOWANCE_SANCTIONS.getValue() + duplicateMessage);
        }
        if (sscsCaseData.getElementsDisputedOverpayment() != null
                && sscsCaseData.getElementsDisputedOverpayment().size() != sscsCaseData.getElementsDisputedOverpayment().stream().distinct().count()) {
            preSubmitCallbackResponse.addError(STANDARD_ALLOWANCE_OVERPAYMENT.getValue() + duplicateMessage);
        }
        if (sscsCaseData.getElementsDisputedHousing() != null
                && sscsCaseData.getElementsDisputedHousing().size() != sscsCaseData.getElementsDisputedHousing().stream().distinct().count()) {
            preSubmitCallbackResponse.addError(HOUSING.getValue() + duplicateMessage);
        }
        if (sscsCaseData.getElementsDisputedChildCare() != null
                && sscsCaseData.getElementsDisputedChildCare().size() != sscsCaseData.getElementsDisputedChildCare().stream().distinct().count()) {
            preSubmitCallbackResponse.addError(CHILDCARE.getValue() + duplicateMessage);
        }
        if (sscsCaseData.getElementsDisputedCare() != null
                && sscsCaseData.getElementsDisputedCare().size() != sscsCaseData.getElementsDisputedCare().stream().distinct().count()) {
            preSubmitCallbackResponse.addError(CARE.getValue() + duplicateMessage);
        }
        if (sscsCaseData.getElementsDisputedChildElement() != null
                && sscsCaseData.getElementsDisputedChildElement().size() != sscsCaseData.getElementsDisputedChildElement().stream().distinct().count()) {
            preSubmitCallbackResponse.addError(CHILD_ELEMENT.getValue() + duplicateMessage);
        }
        if (sscsCaseData.getElementsDisputedChildDisabled() != null
                && sscsCaseData.getElementsDisputedChildDisabled().size() != sscsCaseData.getElementsDisputedChildDisabled().stream().distinct().count()) {
            preSubmitCallbackResponse.addError(CHILD_DISABLED.getValue() + duplicateMessage);
        }
    }
}
