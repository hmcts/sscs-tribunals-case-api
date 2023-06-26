package uk.gov.hmcts.reform.sscs.ccd.presubmit.adminactioncorrection;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.CORRECTED_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminActionCorrectionMidEventHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final DocumentConfiguration documentConfiguration;
    private final GenerateFile generateFile;
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
        } else if (AdminCorrectionType.HEADER.equals(adminCorrectionType)
            && isYes(sscsCaseData.getFinalDecisionNoticeGenerated())) {
            log.info("Admin Action Correction: Regenerating final decision for caseId {}", caseId);
            String templateId = documentConfiguration.getDocuments()
                .get(sscsCaseData.getLanguagePreference()).get(DECISION_ISSUED);
            preSubmitCallbackResponse = issueDocument(callback, CORRECTED_DECISION_NOTICE, templateId, generateFile, userAuthorisation);
        }

        return preSubmitCallbackResponse;
    }

}