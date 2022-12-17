package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.apache.commons.lang3.StringUtils.stripToEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationUnits;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingVenue;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTime;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
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

    @Override
    protected NoticeIssuedTemplateBody createPayload(
        PreSubmitCallbackResponse<SscsCaseData> response,
        SscsCaseData caseData,
        String documentTypeLabel,
        LocalDate dateAdded,
        LocalDate generatedDate,
        boolean isScottish,
        String userAuthorisation
    ) {
        Adjournment adjournment = caseData.getAdjournment();
        NoticeIssuedTemplateBody formPayload = super.createPayload(
            response,
            caseData,
            documentTypeLabel,
            dateAdded,
            adjournment.getGeneratedDate(),
            isScottish,
            userAuthorisation);
        AdjournCaseTemplateBodyBuilder adjournCaseBuilder = AdjournCaseTemplateBody.builder();

        final NoticeIssuedTemplateBodyBuilder builder = formPayload.toBuilder();

        builder.userName(buildSignedInJudgeName(userAuthorisation));
        builder.idamSurname(buildSignedInJudgeSurname(userAuthorisation));

        adjournCaseBuilder.heldBefore(buildHeldBefore(caseData, userAuthorisation));

        String venueName = setHearings(adjournCaseBuilder, caseData);
        adjournCaseBuilder.appellantName(buildName(caseData, false));

        if (adjournment.getReasons() != null && !adjournment.getReasons().isEmpty()) {
            adjournCaseBuilder.reasonsForDecision(
                adjournment.getReasons().stream().map(CollectionItem::getValue).collect(Collectors.toList()));
        } else {
            adjournCaseBuilder.reasonsForDecision(null);
        }

        if (adjournment.getAdditionalDirections() != null) {
            adjournCaseBuilder.additionalDirections(adjournment.getAdditionalDirections().stream()
                .map(CollectionItem::getValue).collect(Collectors.toList()));
        }

        String hearingType = adjournment.getTypeOfHearing() != null
            ? adjournment.getTypeOfHearing().toString()
            : null;

        adjournCaseBuilder.hearingType(hearingType);

        HearingType nextHearingType = HearingType.getByKey(String.valueOf(adjournment.getTypeOfNextHearing()));

        if (HearingType.FACE_TO_FACE.equals(nextHearingType)) {
            handleFaceToFaceHearing(adjournment, adjournCaseBuilder, venueName);
        } else {
            adjournCaseBuilder.nextHearingAtVenue(false);
            if (adjournment.getNextHearingVenueSelected() != null) {
                throw new IllegalStateException("adjournCaseNextHearingVenueSelected field should not be set");
            }
        }
        if (nextHearingType.isOralHearingType()) {
            handleOralHearing(adjournment, adjournCaseBuilder);
        }
        adjournCaseBuilder.nextHearingType(nextHearingType.getValue());

        adjournCaseBuilder.panelMembersExcluded(String.valueOf(adjournment.getPanelMembersExcluded()));

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

    private void handleOralHearing(Adjournment adjournment, AdjournCaseTemplateBodyBuilder adjournCaseBuilder) {
        if (adjournment.getNextHearingListingDuration() != null) {
            if (adjournment.getNextHearingListingDurationUnits() == null) {
                throw new IllegalStateException("Timeslot duration units not supplied on case data");
            }
            adjournCaseBuilder.nextHearingTimeslot(getTimeslotString(adjournment.getNextHearingListingDuration(),
                    adjournment.getNextHearingListingDurationUnits()));
        } else {
            adjournCaseBuilder.nextHearingTimeslot("a standard time slot");
        }
    }

    private void handleFaceToFaceHearing(Adjournment adjournment, AdjournCaseTemplateBodyBuilder adjournCaseBuilder, String venueName) {
        if (adjournment.getNextHearingVenue() == AdjournCaseNextHearingVenue.SOMEWHERE_ELSE) {
            if (adjournment.getNextHearingVenueSelected() != null
                && adjournment.getNextHearingVenueSelected().getValue() != null
                && adjournment.getNextHearingVenueSelected().getValue().getCode() != null
            ) {
                VenueDetails venueDetails = venueDataLoader.getVenueDetailsMap().get(
                    adjournment.getNextHearingVenueSelected().getValue().getCode());
                if (venueDetails == null) {
                    throw new IllegalStateException("Unable to load venue details for id:"
                        + adjournment.getNextHearingVenueSelected().getValue().getCode());
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
    }

    private void setIntepreterDescriptionIfRequired(AdjournCaseTemplateBodyBuilder adjournCaseBuilder, SscsCaseData caseData) {
        if (isYes(caseData.getAdjournment().getInterpreterRequired())) {
            if (caseData.getAdjournment().getInterpreterLanguage() != null) {
                adjournCaseBuilder.interpreterDescription(
                    languageService.getInterpreterDescriptionForLanguageKey(
                        caseData.getAdjournment().getInterpreterLanguage()));
            } else {
                throw new IllegalStateException("An interpreter is required but no language is set");
            }
        }
    }

    private String getTimeslotString(Integer duration, AdjournCaseNextHearingDurationUnits durationUnits) {
        if (duration == null || durationUnits == null) {
            return "";
        }
        String formattedUnits = duration == 1
            ? durationUnits.toString().substring(0, durationUnits.toString().length() - 1)
            : durationUnits.toString();
        return String.format("%d %s", duration, formattedUnits);
    }


    private void setNextHearingDateAndTime(AdjournCaseTemplateBodyBuilder adjournCaseBuilder, SscsCaseData caseData, LocalDate issueDate) {
        String hearingDateSentence = "";
        Adjournment adjournment = caseData.getAdjournment();
        if (adjournment.getNextHearingDateType() == AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE) {
            hearingDateSentence = buildSpecificTimeText(adjournment.getTime(), false);

        } else if (adjournment.getNextHearingDateType() == AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER) {
            hearingDateSentence = buildSpecificTimeText(adjournment.getTime(), false);

            if (adjournment.getNextHearingDateOrPeriod() == AdjournCaseNextHearingDateOrPeriod.PROVIDE_DATE) {
                if (adjournment.getNextHearingFirstAvailableDateAfterDate() == null) {
                    throw new IllegalStateException("No value set for adjournCaseNextHearingFirstAvailableDateAfterDate in case data");
                }

                hearingDateSentence = hearingDateSentence + " after " + adjournment.getNextHearingFirstAvailableDateAfterDate()
                        .format(DateTimeFormatter.ofPattern(DOCUMENT_DATE_PATTERN));
            } else if (adjournment.getNextHearingDateOrPeriod() == AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD) {
                if (adjournment.getNextHearingFirstAvailableDateAfterPeriod() == null) {
                    throw new IllegalStateException("No value set for adjournCaseNextHearingFirstAvailableDateAfterPeriod in case data");
                }
                hearingDateSentence = String.format("%s after %s",
                    hearingDateSentence,
                    issueDate.plusDays(adjournment.getNextHearingFirstAvailableDateAfterPeriod().getCcdDefinition())
                        .format(DateTimeFormatter.ofPattern(DOCUMENT_DATE_PATTERN)));
            } else {
                throw new IllegalStateException("Date or period indicator not available in case data");
            }

        } else if (adjournment.getNextHearingDateType() == AdjournCaseNextHearingDateType.DATE_TO_BE_FIXED) {
            hearingDateSentence = buildSpecificTimeText(adjournment.getTime(), true);
        }

        adjournCaseBuilder.nextHearingDate(stripToEmpty(hearingDateSentence));
    }

    private String buildSpecificTimeText(AdjournCaseTime adjournCaseNextHearingSpecificTime, boolean fixDate) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("It will be ");

        if (adjournCaseNextHearingSpecificTime != null
                && (adjournCaseNextHearingSpecificTime.getAdjournCaseNextHearingSpecificTime() != null
                || CollectionUtils.isNotEmpty(adjournCaseNextHearingSpecificTime.getAdjournCaseNextHearingFirstOnSession()))
        ) {
            handleSpecificTime(adjournCaseNextHearingSpecificTime, stringBuilder);
        } else {
            stringBuilder.append("re-scheduled ");
        }

        if (fixDate) {
            stringBuilder.append("on a date to be fixed");
        } else {
            stringBuilder.append("on the first available date");
        }

        return stringBuilder.toString();
    }

    private static void handleSpecificTime(AdjournCaseTime adjournCaseNextHearingSpecificTime, StringBuilder stringBuilder) {
        if (adjournCaseNextHearingSpecificTime.getAdjournCaseNextHearingFirstOnSession() != null
                && !adjournCaseNextHearingSpecificTime.getAdjournCaseNextHearingFirstOnSession().isEmpty()) {
            stringBuilder.append("first ");
        }

        if (adjournCaseNextHearingSpecificTime.getAdjournCaseNextHearingSpecificTime() != null
                || CollectionUtils.isNotEmpty(adjournCaseNextHearingSpecificTime.getAdjournCaseNextHearingFirstOnSession())) {
            String session = "";
            if ("am".equalsIgnoreCase(adjournCaseNextHearingSpecificTime.getAdjournCaseNextHearingSpecificTime())) {
                session = "morning ";
            } else if ("pm".equalsIgnoreCase(adjournCaseNextHearingSpecificTime.getAdjournCaseNextHearingSpecificTime())) {
                session = "afternoon ";
            }
            stringBuilder.append("in the ").append(session).append("session ");
        }
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
            adjournCaseBuilder.heldAt(IN_CHAMBERS);
        }
        return venue;
    }

    @Override
    protected void setDocumentOnCaseData(SscsCaseData caseData, DocumentLink file) {
        caseData.getAdjournment().setPreviewDocument(file);
    }

    @Override
    protected DocumentLink getDocumentFromCaseData(SscsCaseData caseData) {
        return caseData.getAdjournment().getPreviewDocument();
    }

    protected String buildHeldBefore(SscsCaseData caseData, String userAuthorisation) {
        List<String> names = new ArrayList<>();
        String signedInJudgeName = buildSignedInJudgeName(userAuthorisation);
        if (signedInJudgeName == null) {
            throw new IllegalStateException("Unable to obtain signed in user name");
        }
        names.add(signedInJudgeName);

        List<DynamicList> panelMembers = caseData.getAdjournment().getPanelMembers();

        names.addAll(panelMembers.stream()
            .map(panelMember -> panelMember.getValue().getLabel())
            .collect(Collectors.toList()));

        return StringUtils.getGramaticallyJoinedStrings(names);
    }

    @Override
    protected void setGeneratedDateIfRequired(SscsCaseData sscsCaseData, EventType eventType) {
        // Update the generated date if (and only if) the event type is Adjourn Case
        // ( not for EventType.ISSUE_ADJOURNMENT)
        if (eventType == EventType.ADJOURN_CASE) {
            sscsCaseData.getAdjournment().setGeneratedDate(LocalDate.now());
        }
    }
}
