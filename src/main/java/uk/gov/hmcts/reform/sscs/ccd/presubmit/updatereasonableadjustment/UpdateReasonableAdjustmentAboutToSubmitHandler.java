package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatereasonableadjustment;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNo;
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

        if (isNo(sscsCaseData.getAppeal().getAppellant().getWantsReasonableAdjustment())) {
            sscsCaseData.getAppeal().getAppellant().setWantsReasonableAdjustment(null);
            sscsCaseData.getAppeal().getAppellant().setReasonableAdjustmentRequirements(null);
        }

        Appointee appointee = sscsCaseData.getAppeal().getAppellant().getAppointee();
        if (isYes(sscsCaseData.getAppeal().getAppellant().getIsAppointee()) && nonNull(appointee) && isNo(appointee.getWantsReasonableAdjustment())) {
            appointee.setWantsReasonableAdjustment(null);
            appointee.setReasonableAdjustmentRequirements(null);
        }

        if (isYes(sscsCaseData.getJointParty()) && isNo(sscsCaseData.getJointPartyWantsReasonableAdjustment())) {
            sscsCaseData.setJointPartyWantsReasonableAdjustment(null);
            sscsCaseData.setJointPartyReasonableAdjustmentRequirements(null);
        }

        Representative representative = sscsCaseData.getAppeal().getRep();
        if (nonNull(representative) && isYes(representative.getHasRepresentative()) && isNo(representative.getWantsReasonableAdjustment())) {
            representative.setWantsReasonableAdjustment(null);
            representative.setReasonableAdjustmentRequirements(null);
        }
        sscsCaseData.setUpdateReasonableAdjustment(null);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
