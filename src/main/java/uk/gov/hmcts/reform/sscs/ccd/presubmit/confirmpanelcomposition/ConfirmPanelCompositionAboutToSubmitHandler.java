package uk.gov.hmcts.reform.sscs.ccd.presubmit.confirmpanelcomposition;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;


@Service
@Slf4j
public class ConfirmPanelCompositionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Value("${feature.default-panel-comp.enabled}")
    private boolean isDefaultPanelCompEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.CONFIRM_PANEL_COMPOSITION;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (isDefaultPanelCompEnabled) {
            setFqpmInPanelMemberComposition(sscsCaseData);
        }

        processInterloc(sscsCaseData);
        return response;
    }

    private void processInterloc(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getIsFqpmRequired() != null && sscsCaseData.getInterlocReviewState() != null
                && sscsCaseData.getInterlocReviewState().equals(InterlocReviewState.REVIEW_BY_JUDGE)) {
            sscsCaseData.setInterlocReferralReason(null);
            sscsCaseData.setInterlocReviewState(null);
        }
    }

    private void setFqpmInPanelMemberComposition(SscsCaseData sscsCaseData) {
        if (isNull(sscsCaseData.getPanelMemberComposition())) {
            sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder().build());
        }

        boolean isFqpmRequired = isYes(sscsCaseData.getIsFqpmRequired());
        boolean hasFqpm = sscsCaseData.getPanelMemberComposition().hasFqpm();

        if (isFqpmRequired && !hasFqpm) {
            sscsCaseData.getPanelMemberComposition().addFqpm();
        } else if (!isFqpmRequired && hasFqpm) {
            sscsCaseData.getPanelMemberComposition().removeFqpm();
        }
    }
}
