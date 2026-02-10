package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.confirmpanelcomposition;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.callback.handlers.PreSubmitCallbackHandler;


@Service
@Slf4j
public class ConfirmPanelCompositionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

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

        syncUpdateListingRequirements(sscsCaseData);
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

    private void syncUpdateListingRequirements(SscsCaseData sscsCaseData) {
        if (nonNull(sscsCaseData.getPanelMemberComposition()) && !sscsCaseData.getPanelMemberComposition().isEmpty()) {
            boolean isFqpmRequired = isYes(sscsCaseData.getIsFqpmRequired());

            if (isFqpmRequired) {
                sscsCaseData.getPanelMemberComposition().addFqpm();
            } else {
                sscsCaseData.getPanelMemberComposition().removeFqpm();
            }

            if (sscsCaseData.isIbcCase()) {
                if (isYes(sscsCaseData.getIsMedicalMemberRequired())
                    && !sscsCaseData.getPanelMemberComposition().hasMedicalMember()) {

                    sscsCaseData.getPanelMemberComposition()
                        .setPanelCompositionMemberMedical1(PanelMemberType.TRIBUNAL_MEMBER_MEDICAL.toRef());

                } else if (!isYes(sscsCaseData.getIsMedicalMemberRequired())) {
                    sscsCaseData.getPanelMemberComposition().clearMedicalMembers();
                }
            }
        }
    }
}
