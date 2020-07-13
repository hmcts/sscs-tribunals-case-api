package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.util.StringUtils;

@Component
@Slf4j
public class IssueNoticeHandler extends IssueDocumentHandler {

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

    public PreSubmitCallbackResponse<SscsCaseData> preview(Callback<SscsCaseData> callback, DocumentType documentType, String userAuthorisation, boolean showIssueDate) {

        this.showIssueDate = showIssueDate;

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (sscsCaseData.getWriteFinalDecisionGeneratedDate() == null) {
            sscsCaseData.setWriteFinalDecisionGeneratedDate(LocalDate.now().toString());
        }

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

    @Override
    protected void setDocumentOnCaseData(SscsCaseData caseData, DocumentLink file) {
        caseData.setAdjournCasePreviewDocument(file);
    }

    @Override
    protected DocumentLink getDocumentFromCaseData(SscsCaseData caseData) {
        return caseData.getWriteFinalDecisionPreviewDocument();
    }

    protected String buildSignedInJudgeName(String userAuthorisation) {
        UserDetails userDetails = idamClient.getUserDetails(userAuthorisation);
        if (userDetails == null) {
            throw new IllegalStateException("Unable to obtain signed in user details");
        }
        return userDetails.getFullName();
    }

    protected String buildHeldBefore(SscsCaseData caseData, String userAuthorisation) {
        List<String> names = new ArrayList<>();
        String signedInJudgeName = buildSignedInJudgeName(userAuthorisation);
        if (signedInJudgeName == null) {
            throw new IllegalStateException("Unable to obtain signed in user name");
        }
        names.add(signedInJudgeName);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(caseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName())) {
            names.add(caseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(caseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName())) {
            names.add(caseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        }
        return StringUtils.getGramaticallyJoinedStrings(names);
    }
}
