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

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        final ReasonableAdjustments reasonableAdjustments = sscsCaseData.getReasonableAdjustments();
        if (isYes(sscsCaseData.getAppeal().getAppellant().getIsAppointee()) || nonNull(reasonableAdjustments.getAppellant()) && isNoOrNull(sscsCaseData.getReasonableAdjustments().getAppellant().getWantsReasonableAdjustment())) {
            sscsCaseData.getReasonableAdjustments().setAppellant(null);
        }

        if (isNoOrNull(sscsCaseData.getAppeal().getAppellant().getIsAppointee()) || nonNull(sscsCaseData.getReasonableAdjustments().getAppointee()) && isNoOrNull(sscsCaseData.getReasonableAdjustments().getAppointee().getWantsReasonableAdjustment())) {
            sscsCaseData.getReasonableAdjustments().setAppointee(null);
        }

        if (isNoOrNull(sscsCaseData.getJointParty()) || nonNull(sscsCaseData.getReasonableAdjustments().getJointParty()) && isNoOrNull(sscsCaseData.getReasonableAdjustments().getJointParty().getWantsReasonableAdjustment())) {
            sscsCaseData.getReasonableAdjustments().setJointParty(null);
        }

        final Representative representative = sscsCaseData.getAppeal().getRep();
        if (isNull(representative) || isNoOrNull(representative.getHasRepresentative()) || nonNull(sscsCaseData.getReasonableAdjustments().getRepresentative()) && isNoOrNull(sscsCaseData.getReasonableAdjustments().getRepresentative().getWantsReasonableAdjustment())) {
            sscsCaseData.getReasonableAdjustments().setRepresentative(null);
        }

        if (nonNull(sscsCaseData.getReasonableAdjustments()) && isNull(reasonableAdjustments.getAppellant()) && isNull(reasonableAdjustments.getAppointee()) && isNull(reasonableAdjustments.getRepresentative()) && isNull(reasonableAdjustments.getJointParty())) {
            sscsCaseData.setReasonableAdjustments(null);
        }

        sscsCaseData.setReasonableAdjustmentChoice(null);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }
}
