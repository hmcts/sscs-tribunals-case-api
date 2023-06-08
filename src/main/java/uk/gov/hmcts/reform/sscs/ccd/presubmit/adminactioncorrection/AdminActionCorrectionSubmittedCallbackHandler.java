package uk.gov.hmcts.reform.sscs.ccd.presubmit.adminactioncorrection;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdminCorrectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdCallbackMap;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminActionCorrectionSubmittedCallbackHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final CcdCallbackMapService ccdCallbackMapService;
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent().equals(EventType.ADMIN_ACTION_CORRECTION)
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
        String userAuthorisation) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        String caseId = caseData.getCcdCaseId();
        PostHearing postHearing = caseData.getPostHearing();
        AdminCorrectionType adminCorrectionType = postHearing.getCorrection().getAdminCorrectionType();
        log.info("Admin Action Correction: handling adminActionCorrection {} for case {}", adminCorrectionType,  caseId);

        CcdCallbackMap callbackMap = postHearing.getCorrection().getAdminCorrectionType();
        if (isNull(callbackMap)) {
            response.addError("Invalid Admin Correction Type Selected or correction "
                + "selected as callback is null");
            return response;
        }

        caseData = ccdCallbackMapService.handleCcdCallbackMap(callbackMap, caseData);
        return new PreSubmitCallbackResponse<>(caseData);
    }
}