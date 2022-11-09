package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionposthearingapplication;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.ActionPostHearingTypes;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
@RequiredArgsConstructor
public class ActionPostHearingApplicationMidEventHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    public static final String PAGE_ID_GENERATE_NOTICE = "generateNotice";
    private final DocumentConfiguration documentConfiguration;
    private final GenerateFile generateFile;
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType.equals(CallbackType.MID_EVENT)
            && callback.getEvent() == EventType.ACTION_POST_HEARING_APPLICATION
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        String pageId = callback.getPageId();
        String caseId = caseData.getCcdCaseId();
        log.info("Action Post Hearing Application: handling callback with pageId {} for caseId {}", pageId, caseId);

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        if (!SscsUtil.isSAndLCase(caseData)) {
            log.info("Action Post Hearing Application: Cannot process non Scheduling & Listing Case for Case ID {}",
                caseId);
            response.addError("Cannot process Action Post Hearing Application on non Scheduling & Listing Case");
            return response;
        }

        ActionPostHearingTypes typeSelected = caseData.getActionPostHearingApplication().getTypeSelected();
        log.info("Action Post Hearing Application: handing action {} for case {}", typeSelected,  caseId);

        if (PAGE_ID_GENERATE_NOTICE.equals(pageId) && isYes(caseData.getDocumentGeneration().getGenerateNotice())) {
            log.info("Action Post Hearing Application: Generating notice for caseId {}", caseId);
            String templateId = documentConfiguration.getDocuments()
                .get(caseData.getLanguagePreference()).get(DECISION_ISSUED);
            response = issueDocument(callback, DECISION_NOTICE, templateId, generateFile, userAuthorisation);
        }

        return response;
    }
}
