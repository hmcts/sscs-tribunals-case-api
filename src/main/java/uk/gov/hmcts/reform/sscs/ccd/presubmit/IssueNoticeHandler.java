package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.WordUtils;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionBenefitTypeHelper;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;

@Slf4j
public abstract class IssueNoticeHandler extends IssueDocumentHandler {

    protected final GenerateFile generateFile;
    protected final Function<Pair<LanguagePreference, String>, String> templateId;
    protected final IdamClient idamClient;
    protected boolean showIssueDate;

    public IssueNoticeHandler(GenerateFile generateFile, IdamClient idamClient,
                              Function<Pair<LanguagePreference, String>, String> templateId) {
        this.generateFile = generateFile;
        this.templateId = templateId;
        this.idamClient = idamClient;
    }

    protected abstract void setGeneratedDateIfRequired(SscsCaseData caseData, EventType eventType);

    public PreSubmitCallbackResponse<SscsCaseData> preview(Callback<SscsCaseData> callback, DocumentType documentType, String userAuthorisation, boolean showIssueDate) {

        this.showIssueDate = showIssueDate;

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        setGeneratedDateIfRequired(sscsCaseData, callback.getEvent());

        try {
            String benefitType = WriteFinalDecisionBenefitTypeHelper.getBenefitType(sscsCaseData);

            if (benefitType == null) {
                throw new IllegalStateException("Unable to determine benefit type");
            }

            return issueDocument(callback, documentType, templateId.apply(new ImmutablePair<>(sscsCaseData.getLanguagePreference(), benefitType)), generateFile, userAuthorisation);
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
