package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import static java.util.Objects.requireNonNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase.AdjournCasePreviewService;

@Service
public class IssueAdjournmentNoticeAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final AdjournCasePreviewService previewService;

    @Autowired
    public IssueAdjournmentNoticeAboutToStartHandler(AdjournCasePreviewService previewService) {
        this.previewService = previewService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.ISSUE_ADJOURNMENT_NOTICE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        // Given the user selects No to generate notice option and has uploaded their custom adjournment notice document.
        // Then don't generate the system default adjournment notice document.
        if (YesNo.isYes(sscsCaseData.getAdjournment().getGenerateNotice())) {
            if (sscsCaseData.getAdjournment().getGeneratedDate() == null) {
                response.addError("Adjourn case generated date not found. Please use 'Adjourn case' event or upload your adjourn case document.");
            } else {
                previewService.preview(callback, DocumentType.ADJOURNMENT_NOTICE, userAuthorisation, true);
            }
        } else if (sscsCaseData.getAdjournment().getPreviewDocument() == null) {
            response.addError("No draft adjournment notice found on case. Please use 'Adjourn case' event or upload your adjourn case document.");
        }
        return response;
    }
}
