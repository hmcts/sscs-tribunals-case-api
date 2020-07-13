package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.text.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody.AdjournCaseTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.util.StringUtils;

@Component
@Slf4j
public class AdjournCasePreviewService extends IssueDocumentHandler {

    private final GenerateFile generateFile;
    private final String templateId;
    private final IdamClient idamClient;
    private boolean showIssueDate;

    @Autowired
    public AdjournCasePreviewService(GenerateFile generateFile, IdamClient idamClient,
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

    @Override
    protected NoticeIssuedTemplateBody createPayload(SscsCaseData caseData, String documentTypeLabel, LocalDate dateAdded, LocalDate generatedDate, boolean isScottish,
        String userAuthorisation) {
        NoticeIssuedTemplateBody formPayload = super
            .createPayload(caseData, documentTypeLabel, dateAdded, LocalDate.parse(caseData.getWriteFinalDecisionGeneratedDate(), DateTimeFormatter.ISO_DATE), isScottish, userAuthorisation);
        AdjournCaseTemplateBodyBuilder adjournCaseBuilder = AdjournCaseTemplateBody.builder();

        final NoticeIssuedTemplateBodyBuilder builder = formPayload.toBuilder();

        builder.userName(buildSignedInJudgeName(userAuthorisation));

        adjournCaseBuilder.heldBefore(buildHeldBefore(caseData, userAuthorisation));

        setHearings(adjournCaseBuilder, caseData);
        adjournCaseBuilder.appellantName(buildName(caseData));

        if (caseData.getAdjournCaseReasons() != null && !caseData.getAdjournCaseReasons().isEmpty()) {
            adjournCaseBuilder.reasonsForDecision(
                caseData.getAdjournCaseReasons().stream().map(CollectionItem::getValue).collect(Collectors.toList()));
        } else {
            adjournCaseBuilder.reasonsForDecision(null);
        }

        adjournCaseBuilder.anythingElse(caseData.getAdjournCaseAnythingElse());

        adjournCaseBuilder.hearingType(caseData.getAdjournCaseTypeOfHearing());

        adjournCaseBuilder.nextHearingVenue(caseData.getAdjournCaseNextHearingVenue());
        adjournCaseBuilder.nextHearingType(caseData.getAdjournCaseTypeOfHearing());
        adjournCaseBuilder.nextHearingTimeslot(caseData.getAdjournCaseNextHearingListingDuration()
            + " " + caseData.getAdjournCaseNextHearingListingDurationUnits());

        AdjournCaseTemplateBody payload = adjournCaseBuilder.build();

        validateRequiredProperties(payload);

        if (showIssueDate) {
            builder.dateIssued(LocalDate.now());
        } else {
            builder.dateIssued(null);
        }


        builder.adjournCaseTemplateBody(payload);

        return builder.build();

    }

    private void setHearings(AdjournCaseTemplateBodyBuilder adjournCaseBuilder, SscsCaseData caseData) {
        if (CollectionUtils.isNotEmpty(caseData.getHearings())) {
            Hearing finalHearing = caseData.getHearings().get(0);
            if (finalHearing != null && finalHearing.getValue() != null) {
                if (finalHearing.getValue().getHearingDate() != null) {
                    adjournCaseBuilder.heldOn(LocalDate.parse(finalHearing.getValue().getHearingDate()));
                }
                if (finalHearing.getValue().getVenue() != null) {
                    adjournCaseBuilder.heldAt(finalHearing.getValue().getVenue().getName());
                }
            }
        } else {
            adjournCaseBuilder.heldOn(LocalDate.now());
            adjournCaseBuilder.heldAt("In chambers");
        }
    }


    protected String buildName(SscsCaseData caseData) {
        return WordUtils.capitalizeFully(caseData.getAppeal().getAppellant().getName()
            .getFullNameNoTitle(), ' ', '.');
    }


    private void validateRequiredProperties(AdjournCaseTemplateBody payload) {
        if (payload.getHeldAt() == null && payload.getHeldOn() == null) {
            throw new IllegalStateException("Unable to determine hearing date or venue");
        } else if (payload.getHeldOn() == null) {
            throw new IllegalStateException("Unable to determine hearing date");
        } else if (payload.getHeldAt() == null) {
            throw new IllegalStateException("Unable to determine hearing venue");
        }
    }

    @Override
    protected void setDocumentOnCaseData(SscsCaseData caseData, DocumentLink file) {
        caseData.setAdjournCasePreviewDocument(file);
    }

    @Override
    protected DocumentLink getDocumentFromCaseData(SscsCaseData caseData) {
        return caseData.getWriteFinalDecisionPreviewDocument();
    }

    private String buildSignedInJudgeName(String userAuthorisation) {
        UserDetails userDetails = idamClient.getUserDetails(userAuthorisation);
        if (userDetails == null) {
            throw new IllegalStateException("Unable to obtain signed in user details");
        }
        return userDetails.getFullName();
    }

    private String buildHeldBefore(SscsCaseData caseData, String userAuthorisation) {
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
