package uk.gov.hmcts.reform.sscs.ccd.presubmit.adminactioncorrection;

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
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminActionCorrectionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final DecisionNoticeService decisionNoticeService;
    private final PreviewDocumentService previewDocumentService;

    private final FooterService footerService;
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

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse =
                new PreSubmitCallbackResponse<>(sscsCaseData);

        String ccdCaseId = sscsCaseData.getCcdCaseId();

        log.info("Handling admin correction for case: {}", ccdCaseId);
        AdminCorrectionType adminCorrectionType = sscsCaseData.getPostHearing().getCorrection().getAdminCorrectionType();

        if (AdminCorrectionType.HEADER.equals(adminCorrectionType)) {
            log.info("Handling header correction for case: {}", ccdCaseId);
            //FinalDecisionUtil.writePreviewFinalDecisionNotice(sscsCaseData, preSubmitCallbackResponse, previewDocumentService, decisionNoticeService);
            ///FinalDecisionUtil.processDraftFinalDecisionNotice(callback, userAuthorisation, sscsCaseData, preSubmitCallbackResponse, FinalDecisionType.CORRECTED, decisionNoticeService, isPostHearingsEnabled);
            //FinalDecisionUtil.issueFinalDecisionNoticeFromPreviewDraft(preSubmitCallbackResponse, FinalDecisionType.CORRECTED, footerService);
            //FinalDecisionUtil.clearDraftDecisionNotice(preSubmitCallbackResponse);
        }

        return preSubmitCallbackResponse;
    }

}