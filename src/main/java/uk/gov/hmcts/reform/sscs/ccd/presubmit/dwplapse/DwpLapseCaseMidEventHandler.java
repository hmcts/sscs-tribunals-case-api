package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwplapse;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.*;
import org.springframework.stereotype.*;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

import java.util.*;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.IBCA_BENEFIT_CODE;

@Service
@Slf4j
public class DwpLapseCaseMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
                && callback.getEvent() == EventType.DWP_LAPSE_CASE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
        // TODO replace with common isIba function when available
        if (isIbcaCase(caseData)) {
            if (caseData.getInterlocReviewState() != InterlocReviewState.REVIEW_BY_JUDGE) {
                sscsCaseDataPreSubmitCallbackResponse.addError("Interlocutory review state must be set to 'Review by Judge'");
            }
        } else {
            if (caseData.getDwpLT203() == null) {
                sscsCaseDataPreSubmitCallbackResponse.addError("Select or fill the required Select document for upload field");
            }
        }

        return sscsCaseDataPreSubmitCallbackResponse;
    }

    // TODO remove for common isIba function when available
    private boolean isIbcaCase(SscsCaseData caseData) {
        final String selectedBenefitType = Optional.of(caseData)
            .map(SscsCaseData::getAppeal)
            .map(Appeal::getBenefitType)
            .map(BenefitType::getDescriptionSelection)
            .map(DynamicList::getValue)
            .filter(ObjectUtils::isNotEmpty)
            .map(DynamicListItem::getCode)
            .orElse(null);

        return IBCA_BENEFIT_CODE.equals(caseData.getBenefitCode()) || IBCA_BENEFIT_CODE.equals(selectedBenefitType);
    }
}
