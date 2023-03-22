package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

@Slf4j
public abstract class IssueNoticeHandler extends IssueDocumentHandler {

    protected final GenerateFile generateFile;
    protected final Function<LanguagePreference, String> templateId;
    protected boolean showIssueDate;
    protected final UserDetailsService userDetailsService;

    public IssueNoticeHandler(GenerateFile generateFile, UserDetailsService userDetailsService,
                              Function<LanguagePreference, String> templateId) {
        this.generateFile = generateFile;
        this.templateId = templateId;
        this.userDetailsService = userDetailsService;
    }

    protected abstract void setGeneratedDateIfRequired(SscsCaseData caseData, EventType eventType);

    public PreSubmitCallbackResponse<SscsCaseData> preview(Callback<SscsCaseData> callback, DocumentType documentType, String userAuthorisation, boolean showIssueDate) {

        this.showIssueDate = showIssueDate;

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        setGeneratedDateIfRequired(sscsCaseData, callback.getEvent());

        try {
            return issueDocument(callback, documentType, templateId.apply(sscsCaseData.getLanguagePreference()), generateFile, userAuthorisation);
        } catch (IllegalStateException e) {
            log.error(e.getMessage() + ". Something has gone wrong for caseId: ", sscsCaseData.getCcdCaseId());
            preSubmitCallbackResponse.addError(e.getMessage());
        }
        return preSubmitCallbackResponse;
    }


    protected String buildName(SscsCaseData caseData, boolean displayAppointeeName) {
        if (displayAppointeeName && "yes".equalsIgnoreCase(caseData.getAppeal().getAppellant().getIsAppointee())
            && null != caseData.getAppeal().getAppellant().getAppointee()) {
            return WordUtils.capitalizeFully(caseData.getAppeal().getAppellant().getAppointee().getName()
                    .getFullNameNoTitle(), ' ', '.');
        }
        return WordUtils.capitalizeFully(caseData.getAppeal().getAppellant().getName()
                .getFullNameNoTitle(), ' ', '.');
    }

    protected HearingDetails getLastValidHearing(SscsCaseData caseData) {
        for (Hearing hearing : caseData.getHearings()) {
            if (hearing != null) {
                HearingDetails hearingDetails = hearing.getValue();
                if (hearingDetails != null
                    && StringUtils.isNotBlank(hearingDetails.getHearingDate())
                    && hearingDetails.getVenue() != null
                    && StringUtils.isNotBlank(hearingDetails.getVenue().getName())) {
                    return hearingDetails;

                }
            }
        }
        return null;
    }

    protected String buildSignedInJudgeName(String userAuthorisation) {
        return userDetailsService.buildLoggedInUserName(userAuthorisation);
    }

    protected String buildSignedInJudgeSurname(String userAuthorisation) {
        return userDetailsService.buildLoggedInUserSurname(userAuthorisation);
    }

}
