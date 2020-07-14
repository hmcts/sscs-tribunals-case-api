package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;

@Component
@Slf4j
public abstract class IssueNoticeHandler extends IssueDocumentHandler {

    protected final GenerateFile generateFile;
    protected final String templateId;
    protected final IdamClient idamClient;
    protected boolean showIssueDate;

    @Autowired
    public IssueNoticeHandler(GenerateFile generateFile, IdamClient idamClient,
        @Value("${doc_assembly.issue_final_decision}") String templateId) {
        this.generateFile = generateFile;
        this.templateId = templateId;
        this.idamClient = idamClient;
    }

    protected abstract void setGeneratedDateIfNotAlreadySet(SscsCaseData caseData);

    public PreSubmitCallbackResponse<SscsCaseData> preview(Callback<SscsCaseData> callback, DocumentType documentType, String userAuthorisation, boolean showIssueDate) {

        this.showIssueDate = showIssueDate;

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        setGeneratedDateIfNotAlreadySet(sscsCaseData);

        try {
            return issueDocument(callback, documentType, templateId, generateFile, userAuthorisation);
        } catch (IllegalStateException e) {
            log.error(e.getMessage() + ". Something has gone wrong for caseId: ", sscsCaseData.getCcdCaseId());
            preSubmitCallbackResponse.addError(e.getMessage());
        }

        return preSubmitCallbackResponse;
    }


    protected String buildName(SscsCaseData caseData) {
        return WordUtils.capitalizeFully(caseData.getAppeal().getAppellant().getName()
            .getFullNameNoTitle(), ' ', '.');
    }

    protected String buildSignedInJudgeName(String userAuthorisation) {
        UserDetails userDetails = idamClient.getUserDetails(userAuthorisation);
        if (userDetails == null) {
            throw new IllegalStateException("Unable to obtain signed in user details");
        }
        return userDetails.getFullName();
    }

}
