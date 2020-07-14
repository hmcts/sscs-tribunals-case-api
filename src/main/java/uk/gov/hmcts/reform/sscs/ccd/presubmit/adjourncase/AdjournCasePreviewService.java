package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody.AdjournCaseTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.service.ReferenceDataService;
import uk.gov.hmcts.reform.sscs.util.StringUtils;

@Component
@Slf4j
public class AdjournCasePreviewService extends IssueNoticeHandler {

    private final ReferenceDataService referenceDataService;

    @Autowired
    public AdjournCasePreviewService(GenerateFile generateFile, IdamClient idamClient, ReferenceDataService referenceDataService,
        @Value("${doc_assembly.issue_final_decision}") String templateId) {
        super(generateFile, idamClient, templateId);
        this.referenceDataService = referenceDataService;
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

        if (caseData.getAdjournCaseNextHearingVenueSelected() != null) {

            VenueDetails venueDetails =
                referenceDataService.getVenueDetails(caseData.getAdjournCaseNextHearingVenueSelected());
            adjournCaseBuilder.nextHearingVenue(venueDetails.getVenName());

        }
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
        return caseData.getAdjournCasePreviewDocument();
    }

    protected String buildHeldBefore(SscsCaseData caseData, String userAuthorisation) {
        List<String> names = new ArrayList<>();
        String signedInJudgeName = buildSignedInJudgeName(userAuthorisation);
        if (signedInJudgeName == null) {
            throw new IllegalStateException("Unable to obtain signed in user name");
        }
        names.add(signedInJudgeName);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(caseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName())) {
            names.add(caseData.getAdjournCaseDisabilityQualifiedPanelMemberName());
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(caseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName())) {
            names.add(caseData.getAdjournCaseMedicallyQualifiedPanelMemberName());
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(caseData.getAdjournCaseOtherPanelMemberName())) {
            names.add(caseData.getAdjournCaseOtherPanelMemberName());
        }
        return StringUtils.getGramaticallyJoinedStrings(names);
    }

    @Override
    protected void setGeneratedDateIfNotAlreadySet(SscsCaseData sscsCaseData) {
       // No-op for now
    }
}
