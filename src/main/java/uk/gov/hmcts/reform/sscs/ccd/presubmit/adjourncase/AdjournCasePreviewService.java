package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.apache.commons.lang3.StringUtils.stripToEmpty;

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
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueNoticeHandler;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody.AdjournCaseTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.service.LanguageService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;
import uk.gov.hmcts.reform.sscs.utility.StringUtils;

@Component
@Slf4j
public class AdjournCasePreviewService extends IssueNoticeHandler {

    private final VenueDataLoader venueDataLoader;
    private final LanguageService languageService;
    private static final String DOCUMENT_DATE_PATTERN = "dd/MM/yyyy";
    public static final String IN_CHAMBERS = "In chambers";

    @Autowired
    public AdjournCasePreviewService(GenerateFile generateFile, UserDetailsService userDetailsService, VenueDataLoader venueDataLoader,
                                     LanguageService languageService, @Value("${doc_assembly.adjourn_case}") String templateId) {
        super(generateFile, userDetailsService, languagePreference -> templateId);
        this.venueDataLoader = venueDataLoader;
        this.languageService = languageService;
    }

    private LocalDate getDateForPeriodAfterIssueDate(LocalDate issueDate, String period) {
        return issueDate.plusDays(Integer.parseInt(period));
    }

    @Override
    protected NoticeIssuedTemplateBody createPayload(PreSubmitCallbackResponse<SscsCaseData> response, SscsCaseData caseData, String documentTypeLabel, LocalDate dateAdded, LocalDate generatedDate, boolean isScottish,
        String userAuthorisation) {
        NoticeIssuedTemplateBody formPayload = super
            .createPayload(response, caseData, documentTypeLabel, dateAdded, LocalDate.parse(caseData.getAdjournCaseGeneratedDate()), isScottish, userAuthorisation);
        AdjournCaseTemplateBodyBuilder adjournCaseBuilder = AdjournCaseTemplateBody.builder();

        final NoticeIssuedTemplateBodyBuilder builder = formPayload.toBuilder();

        builder.userName(buildSignedInJudgeName(userAuthorisation));

        adjournCaseBuilder.heldBefore(buildHeldBefore(caseData, userAuthorisation));

        String venueName = setHearings(adjournCaseBuilder, caseData);
        adjournCaseBuilder.appellantName(buildName(caseData, false));

        if (caseData.getAdjournCaseReasons() != null && !caseData.getAdjournCaseReasons().isEmpty()) {
            adjournCaseBuilder.reasonsForDecision(
                caseData.getAdjournCaseReasons().stream().map(CollectionItem::getValue).collect(Collectors.toList()));
        } else {
            adjournCaseBuilder.reasonsForDecision(null);
        }

        if (caseData.getAdjournCaseAdditionalDirections() != null) {
            adjournCaseBuilder.additionalDirections(caseData.getAdjournCaseAdditionalDirections().stream().map(CollectionItem::getValue).collect(Collectors.toList()));
        }

        adjournCaseBuilder.hearingType(caseData.getAdjournCaseTypeOfHearing());

        HearingType nextHearingType = HearingType.getByKey(caseData.getAdjournCaseTypeOfNextHearing());

        if (HearingType.FACE_TO_FACE.equals(nextHearingType)) {
            if ("somewhereElse".equalsIgnoreCase(caseData.getAdjournCaseNextHearingVenue())) {
                if (caseData.getAdjournCaseNextHearingVenueSelected() != null && caseData.getAdjournCaseNextHearingVenueSelected().getValue() != null
                        && caseData.getAdjournCaseNextHearingVenueSelected().getValue().getCode() != null) {
                    VenueDetails venueDetails =
                            venueDataLoader.getVenueDetailsMap().get(caseData.getAdjournCaseNextHearingVenueSelected().getValue().getCode());
                    if (venueDetails == null) {
                        throw new IllegalStateException("Unable to load venue details for id:" + caseData.getAdjournCaseNextHearingVenueSelected().getValue().getCode());
                    }
                    adjournCaseBuilder.nextHearingVenue(venueDetails.getGapsVenName());
                    adjournCaseBuilder.nextHearingAtVenue(true);
                } else {
                    throw new IllegalStateException("A next hearing venue of somewhere else has been specified but no venue has been selected");
                }
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
        String hearingDateSentence;
        if ("firstAvailableDate".equals(caseData.getAdjournCaseNextHearingDateType())) {
            hearingDateSentence = buildSpecificTimeText(caseData.getAdjournCaseTime(), false);

        } else if ("firstAvailableDateAfter".equals(caseData.getAdjournCaseNextHearingDateType())) {
            hearingDateSentence = buildSpecificTimeText(caseData.getAdjournCaseTime(), false);

            if ("provideDate".equals(caseData.getAdjournCaseNextHearingDateOrPeriod())) {
                if (caseData.getAdjournCaseNextHearingFirstAvailableDateAfterDate() == null) {
                    throw new IllegalStateException("No value set for adjournCaseNextHearingFirstAvailableDateAfterDate in case data");
                }

                hearingDateSentence = hearingDateSentence + " after " + LocalDate.parse(caseData.getAdjournCaseNextHearingFirstAvailableDateAfterDate())
                        .format(DateTimeFormatter.ofPattern(DOCUMENT_DATE_PATTERN));
            } else if ("providePeriod".equals(caseData.getAdjournCaseNextHearingDateOrPeriod())) {
                if (caseData.getAdjournCaseNextHearingFirstAvailableDateAfterPeriod() == null) {
                    throw new IllegalStateException("No value set for adjournCaseNextHearingFirstAvailableDateAfterPeriod in case data");
                }
                hearingDateSentence = hearingDateSentence + " after " + getDateForPeriodAfterIssueDate(issueDate,
                        caseData.getAdjournCaseNextHearingFirstAvailableDateAfterPeriod()).format(DateTimeFormatter.ofPattern(DOCUMENT_DATE_PATTERN));
            } else {
                throw new IllegalStateException("Date or period indicator not available in case data");
            }

        } else if ("dateToBeFixed".equals(caseData.getAdjournCaseNextHearingDateType())) {
            hearingDateSentence = buildSpecificTimeText(caseData.getAdjournCaseTime(), true);


        } else {
            throw new IllegalStateException("Unknown next hearing date type for:" + caseData.getAdjournCaseNextHearingDateType());
        }

        adjournCaseBuilder.nextHearingDate(stripToEmpty(hearingDateSentence));
    }

    private String buildSpecificTimeText(AdjournCaseTime adjournCaseNextHearingSpecificTime, boolean fixDate) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (adjournCaseNextHearingSpecificTime != null) {

            stringBuilder.append("It will be ");

            if (adjournCaseNextHearingSpecificTime.getAdjournCaseNextHearingFirstOnSession() != null
                    && adjournCaseNextHearingSpecificTime.getAdjournCaseNextHearingFirstOnSession().size() > 0) {
                stringBuilder.append("first ");
            }

            if (adjournCaseNextHearingSpecificTime.getAdjournCaseNextHearingSpecificTime() != null
                || (adjournCaseNextHearingSpecificTime.getAdjournCaseNextHearingFirstOnSession() != null
                        && adjournCaseNextHearingSpecificTime.getAdjournCaseNextHearingFirstOnSession().size() > 0)) {
                String session = "";
                if ("am".equalsIgnoreCase(adjournCaseNextHearingSpecificTime.getAdjournCaseNextHearingSpecificTime())) {
                    session = "morning ";
                } else if ("pm".equalsIgnoreCase(adjournCaseNextHearingSpecificTime.getAdjournCaseNextHearingSpecificTime())) {
                    session = "afternoon ";
                }
                stringBuilder.append("in the " + session + "session ");
            }

            if (fixDate) {
                stringBuilder.append("on a date to be fixed");
            } else {
                stringBuilder.append("on the first available date");
            }
        }

        return stringBuilder.toString();
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
                    String venueName = venueDataLoader.getGapVenueName(finalHearing.getValue().getVenue(),
                            finalHearing.getValue().getVenueId());
                    if (venueName != null) {
                        adjournCaseBuilder.heldAt(venueName);
                        venue = venueName;
                    }
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
