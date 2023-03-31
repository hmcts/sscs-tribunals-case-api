package uk.gov.hmcts.reform.sscs.ccd.presubmit.adminactioncorrection;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdminCorrectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminActionCorrectionMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
            && callback.getEvent() == EventType.ADMIN_ACTION_CORRECTION
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        String pageId = callback.getPageId();
        String caseId = sscsCaseData.getCcdCaseId();
        log.info("Admin Action Correction: handling callback with pageId {} for caseId {}", pageId, caseId);

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse =
            new PreSubmitCallbackResponse<>(sscsCaseData);
        AdminCorrectionType adminCorrectionType = sscsCaseData.getPostHearing().getCorrection().getAdminCorrectionType();

        if (isNull(adminCorrectionType)) {
            log.error(String.format("adminCorrectionType unexpectedly null for case: %s", caseId));
            preSubmitCallbackResponse.addError(String.format("adminCorrectionType unexpectedly null for case: %s", caseId));
        } else if (AdminCorrectionType.HEADER.equals(adminCorrectionType)) {
            // Only header correction requires further action
            // Body correction is sent straight to review by judge

            log.info("Handling header correction for case: {}", caseId);
            // identify if the final decision notice was generated or uploaded
            // getWriteFinalDecisionGenerateNotice may not work due to being cleared during issueFinalDecisionNotice
            String generateNotice = sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice();
            if (YesNo.YES.getValue().equals(generateNotice)) {
                // IF generated: automatically regenerate final decision with the current details
            } else {
                // IF uploaded: go to upload screen and expect user to upload new document
            }
        }

        return preSubmitCallbackResponse;
    }

}
