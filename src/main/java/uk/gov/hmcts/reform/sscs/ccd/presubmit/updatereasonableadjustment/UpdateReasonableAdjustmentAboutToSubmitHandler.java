package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatereasonableadjustment;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class UpdateReasonableAdjustmentAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
            && callback.getEvent() == EventType.UPDATE_REASONABLE_ADJUSTMENT
            && nonNull(callback.getCaseDetails())
            && nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        if (isYes(sscsCaseData.getAppeal().getAppellant().getIsAppointee()) || nonNull(sscsCaseData.getReasonableAdjustment().getAppellant()) && isNoOrNull(sscsCaseData.getReasonableAdjustment().getAppellant().getWantsReasonableAdjustment())) {
            sscsCaseData.getReasonableAdjustment().setAppellant(null);
        }

        if (isNoOrNull(sscsCaseData.getAppeal().getAppellant().getIsAppointee()) || nonNull(sscsCaseData.getReasonableAdjustment().getAppointee()) && isNoOrNull(sscsCaseData.getReasonableAdjustment().getAppointee().getWantsReasonableAdjustment())) {
            sscsCaseData.getReasonableAdjustment().setAppointee(null);
        }

        if (isNoOrNull(sscsCaseData.getJointParty()) || nonNull(sscsCaseData.getReasonableAdjustment().getJointParty()) && isNoOrNull(sscsCaseData.getReasonableAdjustment().getJointParty().getWantsReasonableAdjustment())) {
            sscsCaseData.getReasonableAdjustment().setJointParty(null);
        }

        Representative representative = sscsCaseData.getAppeal().getRep();
        if (isNull(representative) || isNoOrNull(representative.getHasRepresentative()) || nonNull(sscsCaseData.getReasonableAdjustment().getRepresentative()) && isNoOrNull(sscsCaseData.getReasonableAdjustment().getRepresentative().getWantsReasonableAdjustment())) {
            sscsCaseData.getReasonableAdjustment().setRepresentative(null);
        }
        sscsCaseData.setUpdateReasonableAdjustment(null);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
