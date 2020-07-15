package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import java.time.LocalDate;
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
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;
import uk.gov.hmcts.reform.sscs.util.StringUtils;

@Component
@Slf4j
public class AdjournCasePreviewService extends IssueNoticeHandler {

    private final VenueDataLoader venueDataLoader;

    @Autowired
    public AdjournCasePreviewService(GenerateFile generateFile, IdamClient idamClient, VenueDataLoader venueDataLoader,
        @Value("${doc_assembly.adjourn_case}") String templateId) {
        super(generateFile, idamClient, templateId);
        this.venueDataLoader = venueDataLoader;
    }

    private LocalDate getDateForPeriodAfterIssueDate(LocalDate issueDate, String period) {
        return issueDate.plusDays(Integer.parseInt(period));
    }

    @Override
    protected NoticeIssuedTemplateBody createPayload(SscsCaseData caseData, String documentTypeLabel, LocalDate dateAdded, LocalDate generatedDate, boolean isScottish,
        String userAuthorisation) {
        NoticeIssuedTemplateBody formPayload = super
            .createPayload(caseData, documentTypeLabel, dateAdded, generatedDate, isScottish, userAuthorisation);
        AdjournCaseTemplateBodyBuilder adjournCaseBuilder = AdjournCaseTemplateBody.builder();

        final NoticeIssuedTemplateBodyBuilder builder = formPayload.toBuilder();

        builder.userName(buildSignedInJudgeName(userAuthorisation));

        adjournCaseBuilder.heldBefore(buildHeldBefore(caseData, userAuthorisation));

        String venueName = setHearings(adjournCaseBuilder, caseData);
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
                venueDataLoader.getVenueDetailsMap().get(caseData.getAdjournCaseNextHearingVenueSelected());
            adjournCaseBuilder.nextHearingVenue(venueDetails.getVenName());

        } else {
            adjournCaseBuilder.nextHearingVenue(venueName);

        }
        adjournCaseBuilder.nextHearingType(HearingType.getByKey(caseData.getAdjournCaseTypeOfHearing()).getValue());
        if (caseData.getAdjournCaseNextHearingListingDuration() != null) {
            adjournCaseBuilder.nextHearingTimeslot(caseData.getAdjournCaseNextHearingListingDuration()
                + " " + caseData.getAdjournCaseNextHearingListingDurationUnits());
        } else {
            adjournCaseBuilder.nextHearingTimeslot("a standard time slot");
        }


        adjournCaseBuilder.panelMembersExcluded(caseData.getAdjournCasePanelMembersExcluded());

        LocalDate issueDate = LocalDate.now();


        String dateString = null;
        String timeString = null;
        if ("firstAvailableDate".equals(caseData.getAdjournCaseNextHearingDateType())) {
            dateString = "the first available date";
        } else if ("firstAvailableDateAfter".equals(caseData.getAdjournCaseNextHearingDateType())) {
            if ("provideDate".equals(caseData.getAdjournCaseNextHearingDateOrPeriod())) {
                dateString = "the first available date after " + caseData.getAdjournCaseNextHearingFirstAvailableDateAfterDate();
            } else if ("providePeriod".equals(caseData.getAdjournCaseNextHearingDateOrPeriod())) {
                dateString = "the first available date after " + getDateForPeriodAfterIssueDate(issueDate,
                    caseData.getAdjournCaseNextHearingFirstAvailableDateAfterPeriod());
            }
        } else if ("specificDateAndTime".equals(caseData.getAdjournCaseNextHearingDateType())) {
            dateString = caseData.getAdjournCaseNextHearingSpecificDate();
            timeString = (caseData.getAdjournCaseNextHearingSpecificTime().equals("am") ? "am" : "pm");
        } else if ("specificTime".equals(caseData.getAdjournCaseNextHearingDateType())) {
            dateString = "a date to be decided";
            timeString = (caseData.getAdjournCaseNextHearingSpecificTime().equals("am") ? "am" : "pm");
        } else if ("dateToBeFixed".equals(caseData.getAdjournCaseNextHearingDateType())) {
            dateString = "a date to be fixed";
        }

        adjournCaseBuilder.nextHearingDate(dateString);
        adjournCaseBuilder.nextHearingTime(timeString);

        AdjournCaseTemplateBody payload = adjournCaseBuilder.build();

        validateRequiredProperties(payload);

        if (showIssueDate) {
            builder.dateIssued(issueDate);
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

    protected String setHearings(AdjournCaseTemplateBodyBuilder adjournCaseBuilder, SscsCaseData caseData) {
        String venue = "In chambers";
        if (CollectionUtils.isNotEmpty(caseData.getHearings())) {
            Hearing finalHearing = caseData.getHearings().get(0);
            if (finalHearing != null && finalHearing.getValue() != null) {
                if (finalHearing.getValue().getHearingDate() != null) {
                    adjournCaseBuilder.heldOn(LocalDate.parse(finalHearing.getValue().getHearingDate()));
                }
                if (finalHearing.getValue().getVenue() != null) {
                    adjournCaseBuilder.heldAt(finalHearing.getValue().getVenue().getName());
                    venue = finalHearing.getValue().getVenue().getName();
                }
            }
        } else {
            adjournCaseBuilder.heldOn(LocalDate.now());
            adjournCaseBuilder.heldAt("In chambers");
        }
        return venue;
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
