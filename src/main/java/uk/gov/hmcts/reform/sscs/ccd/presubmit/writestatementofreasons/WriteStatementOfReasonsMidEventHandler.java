package uk.gov.hmcts.reform.sscs.ccd.presubmit.writestatementofreasons;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.STATEMENT_OF_REASONS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
@AllArgsConstructor
public class WriteStatementOfReasonsMidEventHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    public static final String PAGE_ID_GENERATE_DOCUMENT = "generateDocument";
    private final WriteStatementOfReasonsPreviewService writeStatementOfReasonsPreviewService;
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

    @Value("${feature.postHearingsB.enabled}")
    private final boolean isPostHearingsBEnabled;


    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
            && callback.getEvent() == EventType.SOR_WRITE
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        String pageId = callback.getPageId();
        String caseId = caseData.getCcdCaseId();
        log.info("Write Statement of Reasons: handling callback with pageId {} for caseId {}", pageId, caseId);

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        if (PAGE_ID_GENERATE_DOCUMENT.equals(pageId) && isYes(caseData.getDocumentGeneration().getGenerateNotice())) {
            log.info("Write Statement of Reasons: Generating notice for caseId {}", caseId);

            caseData.getPostHearing().setReviewType(null);
            response = writeStatementOfReasonsPreviewService.preview(callback, STATEMENT_OF_REASONS, userAuthorisation, false, isPostHearingsEnabled, isPostHearingsBEnabled);
        }

        return response;
    }

}
