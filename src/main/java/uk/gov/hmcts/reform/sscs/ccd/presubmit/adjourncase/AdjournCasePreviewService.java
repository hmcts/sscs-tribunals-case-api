package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueNoticeHandler;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody.AdjournCaseTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder;

@Component
@Slf4j
public class AdjournCasePreviewService extends IssueNoticeHandler {

    @Autowired
    public AdjournCasePreviewService(GenerateFile generateFile, IdamClient idamClient,
        @Value("${doc_assembly.issue_final_decision}") String templateId) {
        super(generateFile, idamClient, templateId);
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

    protected void validateRequiredProperties(AdjournCaseTemplateBody payload) {
        if (payload.getHeldAt() == null && payload.getHeldOn() == null) {
            throw new IllegalStateException("Unable to determine hearing date or venue");
        } else if (payload.getHeldOn() == null) {
            throw new IllegalStateException("Unable to determine hearing date");
        } else if (payload.getHeldAt() == null) {
            throw new IllegalStateException("Unable to determine hearing venue");
        }
    }

    protected void setHearings(AdjournCaseTemplateBodyBuilder adjournCaseBuilder, SscsCaseData caseData) {
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

    @Override
    protected void setDocumentOnCaseData(SscsCaseData caseData, DocumentLink file) {
        caseData.setAdjournCasePreviewDocument(file);
    }

    @Override
    protected DocumentLink getDocumentFromCaseData(SscsCaseData caseData) {
        return caseData.getWriteFinalDecisionPreviewDocument();
    }

}
