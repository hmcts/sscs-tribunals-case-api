package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionBenefitTypeHelper;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceBase;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@AllArgsConstructor
public class IssueFinalDecisionAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final DecisionNoticeService decisionNoticeService;
    @Value("${feature.postHearings.enabled}")
    private boolean isPostHearingsEnabled;
    @Value("${feature.postHearingsB.enabled}")
    private boolean isPostHearingsBEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.ISSUE_FINAL_DECISION;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        DocumentLink previewDocument = sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument();

        if (nonNull(previewDocument)) {
            String benefitType = WriteFinalDecisionBenefitTypeHelper.getBenefitType(sscsCaseData);

            if (benefitType == null) {
                response.addError("Unexpected error - benefit type is null");
            }

            DocumentType docType = SscsUtil.getIssueFinalDecisionDocumentType(sscsCaseData, isPostHearingsEnabled);

            WriteFinalDecisionPreviewDecisionServiceBase previewDecisionService = decisionNoticeService.getPreviewService(benefitType);

            if (YesNo.isYes(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())) {
                previewDecisionService.preview(callback, docType, userAuthorisation, true, isPostHearingsEnabled, isPostHearingsBEnabled);
            }
        } else {
            response.addError("No draft final decision notice found on case. Please use 'Write final decision' event before trying to issue.");
        }

        return response;
    }

}
