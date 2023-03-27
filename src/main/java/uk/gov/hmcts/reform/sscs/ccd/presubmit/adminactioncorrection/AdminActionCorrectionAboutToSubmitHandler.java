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
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminActionCorrectionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.ADMIN_ACTION_CORRECTION
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();


        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse =
            new PreSubmitCallbackResponse<>(sscsCaseData);

        handleAdminCorrection(sscsCaseData);

        return preSubmitCallbackResponse;
    }

    private void handleAdminCorrection(SscsCaseData sscsCaseData) {
        String ccdCaseId = sscsCaseData.getCcdCaseId();
        log.info("Handling admin correction for case: {}", ccdCaseId);
        AdminCorrectionType adminCorrectionType = sscsCaseData.getPostHearing().getAdminCorrectionType();
        if (isNull(adminCorrectionType)) {
            throw new IllegalStateException("adminCorrectionType unexpectedly null for case: " + ccdCaseId);
        }

        log.info("This is adminCorrectionType for case {}: {}", ccdCaseId, adminCorrectionType);
        if (AdminCorrectionType.BODY.equals(adminCorrectionType)) {
            log.info("Handling body correction for case: {}", ccdCaseId);
            // do body things
        } else if (AdminCorrectionType.HEADER.equals(adminCorrectionType)) {
            log.info("Handling header correction for case: {}", ccdCaseId);
            // do header things
        }
    }

}
