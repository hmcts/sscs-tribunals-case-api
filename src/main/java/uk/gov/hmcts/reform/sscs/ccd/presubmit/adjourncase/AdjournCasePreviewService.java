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
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueNoticeHandler;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody.AdjournCaseTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.service.LanguageService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;
import uk.gov.hmcts.reform.sscs.util.StringUtils;

@Component
@Slf4j
public class AdjournCasePreviewService extends IssueNoticeHandler {

    private final VenueDataLoader venueDataLoader;
    private final LanguageService languageService;
    private static final String DOCUMENT_DATE_PATTERN = "dd/MM/YYYY";
    public static final String IN_CHAMBERS = "In chambers";

    @Autowired
    public AdjournCasePreviewService(GenerateFile generateFile, IdamClient idamClient, VenueDataLoader venueDataLoader,
        LanguageService languageService, @Value("${doc_assembly.adjourn_case}") String templateId) {
        super(generateFile, idamClient, languagePreference -> templateId);
        this.venueDataLoader = venueDataLoader;
        this.languageService = languageService;
    }

    private LocalDate getDateForPeriodAfterIssueDate(LocalDate issueDate, String period) {
        return issueDate.plusDays(Integer.parseInt(period));
    }

    @Override
    protected NoticeIssuedTemplateBody createPayload(SscsCaseData caseData, String documentTypeLabel, LocalDate dateAdded, LocalDate generatedDate, boolean isScottish,
        String userAuthorisation) {
        NoticeIssuedTemplateBody formPayload = super
            .createPayload(caseData, documentTypeLabel, dateAdded, LocalDate.parse(caseData.getAdjournCaseGeneratedDate()), isScottish, userAuthorisation);
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

        adjournCaseBuilder.additionalDirections(caseData.getAdjournCaseAdditionalDirections());

        adjournCaseBuilder.hearingType(caseData.getAdjournCaseTypeOfHearing());

        HearingType nextHearingType = HearingType.getByKey(caseData.getAdjournCaseTypeOfNextHearing());

        if (HearingType.FACE_TO_FACE.equals(nextHearingType)) {
            if (caseData.getAdjournCaseNextHearingVenueSelected() != null) {
                VenueDetails venueDetails =
                    venueDataLoader.getVenueDetailsMap().get(caseData.getAdjournCaseNextHearingVenueSelected());
                if (venueDetails == null) {
                    throw new IllegalStateException("Unable to load venue details for id:" + caseData.getAdjournCaseNextHearingVenueSelected());
                }
                adjournCaseBuilder.nextHearingVenue(venueDetails.getVenName());
                adjournCaseBuilder.nextHearingAtVenue(true);
            } else {
                adjournCaseBuilder.nextHearingAtVenue(!IN_CHAMBERS.equals(venueName));
                adjournCaseBuilder.nextHearingVenue(venueName);
            }
        } else {
            adjournCaseBuilder.nextHearingAtVenue(false);
            if (caseData.getAdjournCaseNextHearingVenueSelected() != null) {
                throw new IllegalStateException("adjournCaseNextHearingVenueSelected field should not be set");
            }
        }
        if (nextHearingType.isOralHearingType()) {
            if (caseData.getAdjournCaseNextHearingListingDuration() != null) {
                if (caseData.getAdjournCaseNextHearingListingDurationUnits() == null) {
                    throw new IllegalStateException("Timeslot duration units not supplied on case data");
                }
                adjournCaseBuilder.nextHearingTimeslot(getTimeslotString(caseData.getAdjournCaseNextHearingListingDuration(),
                    caseData.getAdjournCaseNextHearingListingDurationUnits()));
            } else {
                adjournCaseBuilder.nextHearingTimeslot("a standard time slot");
            }
        }
        adjournCaseBuilder.nextHearingType(nextHearingType.getValue());

        adjournCaseBuilder.panelMembersExcluded(caseData.getAdjournCasePanelMembersExcluded());

        setIntepreterDescriptionIfRequired(adjournCaseBuilder, caseData);

        LocalDate issueDate = LocalDate.now();

        setNextHearingDateAndTime(adjournCaseBuilder, caseData, issueDate);

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

    private void setIntepreterDescriptionIfRequired(AdjournCaseTemplateBodyBuilder adjournCaseBuilder, SscsCaseData caseData) {
        if ("yes".equalsIgnoreCase(caseData.getAdjournCaseInterpreterRequired())) {
            if (caseData.getAdjournCaseInterpreterLanguage() != null) {
                adjournCaseBuilder.interpreterDescription(
                    languageService.getInterpreterDescriptionForLanguageKey(
                        caseData.getAdjournCaseInterpreterLanguage()));
            } else {
                throw new IllegalStateException("An interpreter is required but no language is set");
            }
        }
    }

    private String getTimeslotString(String nextHearingListingDuration, String nextHearingListingDurationUnits) {
        if (nextHearingListingDuration.equals("1")) {
            return "1 " + nextHearingListingDurationUnits.substring(0, nextHearingListingDurationUnits.length() - 1);
        } else {
            return nextHearingListingDuration + " " +  nextHearingListingDurationUnits;
        }
    }


    private void setNextHearingDateAndTime(AdjournCaseTemplateBodyBuilder adjournCaseBuilder, SscsCaseData caseData, LocalDate issueDate) {
        String dateString = null;
        String timeString = null;
        if ("firstAvailableDate".equals(caseData.getAdjournCaseNextHearingDateType())) {
            dateString = "the first available date";
        } else if ("firstAvailableDateAfter".equals(caseData.getAdjournCaseNextHearingDateType())) {
            if ("provideDate".equals(caseData.getAdjournCaseNextHearingDateOrPeriod())) {
                if (caseData.getAdjournCaseNextHearingFirstAvailableDateAfterDate() == null) {
                    throw new IllegalStateException("No value set for adjournCaseNextHearingFirstAvailableDateAfterDate in case data");
                }
                dateString = "the first available date after " + LocalDate.parse(caseData.getAdjournCaseNextHearingFirstAvailableDateAfterDate())
                    .format(DateTimeFormatter.ofPattern(DOCUMENT_DATE_PATTERN));
            } else if ("providePeriod".equals(caseData.getAdjournCaseNextHearingDateOrPeriod())) {
                if (caseData.getAdjournCaseNextHearingFirstAvailableDateAfterPeriod() == null) {
                    throw new IllegalStateException("No value set for adjournCaseNextHearingFirstAvailableDateAfterPeriod in case data");
                }
                dateString = "the first available date after " + getDateForPeriodAfterIssueDate(issueDate,
                    caseData.getAdjournCaseNextHearingFirstAvailableDateAfterPeriod()).format(DateTimeFormatter.ofPattern(DOCUMENT_DATE_PATTERN));
            } else {
                throw new IllegalStateException("Date or period indicator not available in case data");
            }
        } else if ("specificDateAndTime".equals(caseData.getAdjournCaseNextHearingDateType())) {
            if (caseData.getAdjournCaseNextHearingSpecificDate() == null) {
                throw new IllegalStateException("adjournCaseNextHearingSpecificDate not available in case data");
            }
            if (caseData.getAdjournCaseNextHearingSpecificTime() == null) {
                throw new IllegalStateException("adjournCaseNextHearingSpecificTime not available in case data");
            }
            dateString = LocalDate.parse(caseData.getAdjournCaseNextHearingSpecificDate())
                .format(DateTimeFormatter.ofPattern(DOCUMENT_DATE_PATTERN));
            timeString = caseData.getAdjournCaseNextHearingSpecificTime();
        } else if ("specificTime".equals(caseData.getAdjournCaseNextHearingDateType())) {
            if (caseData.getAdjournCaseNextHearingSpecificTime() == null) {
                throw new IllegalStateException("adjournCaseNextHearingSpecificTime not available in case data");
            }
            dateString = "a date to be decided";
            timeString = caseData.getAdjournCaseNextHearingSpecificTime();
        } else if ("dateToBeFixed".equals(caseData.getAdjournCaseNextHearingDateType())) {
            dateString = "a date to be fixed";
        } else {
            throw new IllegalStateException("Unknown next hearing date type for:" + caseData.getAdjournCaseNextHearingDateType());
        }

        adjournCaseBuilder.nextHearingDate(dateString);
        adjournCaseBuilder.nextHearingTime(timeString);

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
        String venue = IN_CHAMBERS;
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
        if (org.apache.commons.lang3.StringUtils.isNotBlank(caseData.getAdjournCaseDisabilityQualifiedPanelMemberName())) {
            names.add(caseData.getAdjournCaseDisabilityQualifiedPanelMemberName());
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(caseData.getAdjournCaseMedicallyQualifiedPanelMemberName())) {
            names.add(caseData.getAdjournCaseMedicallyQualifiedPanelMemberName());
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(caseData.getAdjournCaseOtherPanelMemberName())) {
            names.add(caseData.getAdjournCaseOtherPanelMemberName());
        }
        return StringUtils.getGramaticallyJoinedStrings(names);
    }

    @Override
    protected void setGeneratedDateIfRequired(SscsCaseData sscsCaseData, EventType eventType) {
        // Update the generated date if (and only if) the event type is Adjourn Case
        // ( not for EventType.ISSUE_ADJOURNMENT)
        if (eventType == EventType.ADJOURN_CASE) {
            sscsCaseData.setAdjournCaseGeneratedDate(LocalDate.now().toString());
        }
    }
}
