package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

@Slf4j
public abstract class IssueNoticeHandler extends IssueDocumentHandler {
    protected final GenerateFile generateFile;
    protected final Function<LanguagePreference, String> templateId;
    protected boolean showIssueDate;
    protected final UserDetailsService userDetailsService;
    protected final DocumentConfiguration documentConfiguration;

    public IssueNoticeHandler(GenerateFile generateFile, UserDetailsService userDetailsService,
                              Function<LanguagePreference, String> templateId,
                              DocumentConfiguration documentConfiguration) {
        this.generateFile = generateFile;
        this.templateId = templateId;
        this.userDetailsService = userDetailsService;
        this.documentConfiguration = documentConfiguration;
    }

    protected abstract void setGeneratedDateIfRequired(SscsCaseData caseData, EventType eventType);

    public PreSubmitCallbackResponse<SscsCaseData> preview(Callback<SscsCaseData> callback, DocumentType documentType, String userAuthorisation, boolean showIssueDate) {
        return preview(callback, documentType, userAuthorisation, showIssueDate, false, false);
    }

    public PreSubmitCallbackResponse<SscsCaseData> preview(Callback<SscsCaseData> callback, DocumentType documentType,
                                                           String userAuthorisation, boolean showIssueDate,
                                                           boolean isPostHearingsEnabled, boolean isPostHearingsBEnabled) {
        this.showIssueDate = showIssueDate;
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        setGeneratedDateIfRequired(sscsCaseData, callback.getEvent());

        try {
            String templateIdString = templateId.apply(sscsCaseData.getLanguagePreference());

            if (isPostHearingsEnabled
                    && (DocumentType.CORRECTION_GRANTED.equals(documentType)
                    || DocumentType.DRAFT_CORRECTED_NOTICE.equals(documentType)
                    || DocumentType.CORRECTED_DECISION_NOTICE.equals(documentType))) {
                templateIdString = documentConfiguration.getDocuments().get(sscsCaseData.getLanguagePreference()).get(EventType.CORRECTION_GRANTED);
            }

            return issueDocument(callback, documentType, templateIdString, generateFile, userAuthorisation, isPostHearingsEnabled, isPostHearingsBEnabled);
        } catch (IllegalStateException e) {
            log.error(e.getMessage() + ". Something has gone wrong for caseId: ", sscsCaseData.getCcdCaseId());
            preSubmitCallbackResponse.addError(e.getMessage());
        }
        return preSubmitCallbackResponse;
    }


    protected String buildName(SscsCaseData caseData, boolean displayAppointeeName) {
        if (displayAppointeeName && "yes".equalsIgnoreCase(caseData.getAppeal().getAppellant().getIsAppointee())
            && null != caseData.getAppeal().getAppellant().getAppointee()) {
            return caseData.getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle();
        }
        return caseData.getAppeal().getAppellant().getName().getFullNameNoTitle();
    }

    protected String buildSignedInJudgeName(String userAuthorisation) {
        return userDetailsService.buildLoggedInUserName(userAuthorisation);
    }

    protected String buildSignedInJudgeSurname(String userAuthorisation) {
        return userDetailsService.buildLoggedInUserSurname(userAuthorisation);
    }

}
