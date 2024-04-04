package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.stripToEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.IN_CHAMBERS;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getLastValidHearing;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueNoticeHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody.AdjournCaseTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.JudicialRefDataService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;
import uk.gov.hmcts.reform.sscs.utility.StringUtils;

@Component
@Slf4j
public class AdjournCasePreviewService extends IssueNoticeHandler {
    private final VenueDataLoader venueDataLoader;
    private final JudicialRefDataService judicialRefDataService;
    private static final String DOCUMENT_DATE_PATTERN = "dd/MM/yyyy";
    private final SignLanguagesService signLanguagesService;
    private final AirLookupService airLookupService;
    @Value("${feature.snl.adjournment.enabled}")
    private boolean adjournmentFeature;

    @Autowired
    public AdjournCasePreviewService(GenerateFile generateFile,
                                     UserDetailsService userDetailsService,
                                     VenueDataLoader venueDataLoader,
                                     @Value("${doc_assembly.adjourn_case}") String templateId,
                                     AirLookupService airLookupService,
                                     SignLanguagesService signLanguagesService,
                                     JudicialRefDataService judicialRefDataService,
                                     DocumentConfiguration documentConfiguration) {
        super(generateFile, userDetailsService, languagePreference -> templateId, documentConfiguration);
        this.venueDataLoader = venueDataLoader;
        this.airLookupService = airLookupService;
        this.signLanguagesService = signLanguagesService;
        this.judicialRefDataService = judicialRefDataService;
    }

    @Override
    protected NoticeIssuedTemplateBody createPayload(PreSubmitCallbackResponse<SscsCaseData> response,
                                                     SscsCaseData caseData, String documentTypeLabel,
                                                     LocalDate dateAdded, LocalDate generatedDate,
                                                     boolean isScottish, boolean isPostHearingsEnabled,
                                                     boolean isPostHearingsBEnabled,
                                                     String userAuthorisation) {
        Adjournment adjournment = caseData.getAdjournment();
        NoticeIssuedTemplateBody formPayload = super.createPayload(
            response,
            caseData,
            documentTypeLabel,
            dateAdded,
            adjournment.getGeneratedDate(),
            isScottish,
            isPostHearingsEnabled,
            isPostHearingsBEnabled,
            userAuthorisation);
        AdjournCaseTemplateBodyBuilder adjournCaseBuilder = AdjournCaseTemplateBody.builder();

        final NoticeIssuedTemplateBodyBuilder builder = formPayload.toBuilder();

        builder.userName(buildSignedInJudgeName(userAuthorisation));
        builder.idamSurname(buildSignedInJudgeSurname(userAuthorisation));

        adjournCaseBuilder.heldBefore(buildHeldBefore(caseData, userAuthorisation));

        setHearings(adjournCaseBuilder, caseData);
        String venueName = caseData.getProcessingVenue();
        adjournCaseBuilder.appellantName(buildName(caseData, false));

        if (adjournment.getReasons() != null && !adjournment.getReasons().isEmpty()) {
            adjournCaseBuilder.reasonsForDecision(
                adjournment.getReasons().stream().map(CollectionItem::getValue).toList());
        } else {
            adjournCaseBuilder.reasonsForDecision(null);
        }

        adjournCaseBuilder.tribunalDirectPoToAttend(caseData.getTribunalDirectPoToAttend());

        if (adjournment.getAdditionalDirections() != null) {
            adjournCaseBuilder.additionalDirections(adjournment.getAdditionalDirections().stream()
                .map(CollectionItem::getValue).toList());
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
            if (nonNull(adjournment.getNextHearingVenueSelected())
                && nonNull(adjournment.getNextHearingVenueSelected().getValue())
                && nonNull(adjournment.getNextHearingVenueSelected().getValue().getCode())) {
                VenueDetails venueDetails = venueDataLoader.getVenueDetailsMap().get(
                        adjournment.getNextHearingVenueSelected().getValue().getCode());
                if (isNull(venueDetails)) {
                    throw new IllegalStateException("Unable to load venue details for id:"
                        + adjournment.getNextHearingVenueSelected().getValue().getCode());
                }
                adjournCaseBuilder.nextHearingVenue(venueDetails.getGapsVenName());
                adjournCaseBuilder.nextHearingAtVenue(true);
            } else {
                throw new IllegalStateException("A next hearing venue of somewhere else has been specified but no venue has been selected");
            }
        } else {
            Integer venueId = airLookupService.lookupVenueIdByAirVenueName(venueName);

            if (nonNull(venueId)) {
                VenueDetails venueDetails = venueDataLoader.getVenueDetailsMap().get(venueId.toString());

                if (nonNull(venueDetails)) {
                    adjournCaseBuilder.nextHearingVenue(venueDetails.getGapsVenName());
                    adjournCaseBuilder.nextHearingAtVenue(true);

                    return;
                }
            }

            adjournCaseBuilder.nextHearingVenue(venueName);
            adjournCaseBuilder.nextHearingAtVenue(!IN_CHAMBERS.equals(venueName));
        }
    }

    private void setIntepreterDescriptionIfRequired(AdjournCaseTemplateBodyBuilder adjournCaseBuilder, SscsCaseData caseData) {
        if (isYes(caseData.getAdjournment().getInterpreterRequired())) {
            if (caseData.getAdjournment().getInterpreterLanguage() != null) {
                String languageLabel = caseData.getAdjournment().getInterpreterLanguage().getValue().getLabel();
                String languageKey = caseData.getAdjournment().getInterpreterLanguage().getValue().getCode();
                Language hmrcRefLanguage = signLanguagesService.getLanguageByHmcReference(languageKey);
                String interpreterDescription = String.format("an interpreter in %s", languageLabel);
                if (nonNull(hmrcRefLanguage)) {
                    interpreterDescription = String.format("a sign language interpreter (%s)", languageLabel);
                }
                adjournCaseBuilder.interpreterDescription(interpreterDescription);
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

    protected void setHearings(AdjournCaseTemplateBodyBuilder adjournCaseBuilder, SscsCaseData caseData) {
        HearingDetails finalHearing = getLastValidHearing(caseData);
        if (finalHearing != null) {
            if (finalHearing.getHearingDate() != null) {
                adjournCaseBuilder.heldOn(LocalDate.parse(finalHearing.getHearingDate()));
            }
            if (finalHearing.getVenue() != null) {
                String venueName = venueDataLoader.getGapVenueName(finalHearing.getVenue(), finalHearing.getVenueId());
                if (venueName != null) {
                    adjournCaseBuilder.heldAt(venueName);
                }
            }
        } else {
            setInChambers(adjournCaseBuilder);
        }
    }

    private void setInChambers(AdjournCaseTemplateBodyBuilder adjournCaseBuilder) {
        adjournCaseBuilder.heldOn(LocalDate.now());
        adjournCaseBuilder.heldAt(IN_CHAMBERS);
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

        Adjournment adjournment = caseData.getAdjournment();

        if (adjournmentFeature) {
            List<JudicialUserBase> panelMembers = adjournment.getPanelMembers();
            names.addAll(judicialRefDataService.getAllJudicialUsersFullNames(panelMembers));
        } else {
            List<String> panelMembers = Stream.of(adjournment.getDisabilityQualifiedPanelMemberName(),
                adjournment.getMedicallyQualifiedPanelMemberName(), adjournment.getOtherPanelMemberName())
                .filter(org.apache.commons.lang3.StringUtils::isNotBlank).toList();

            names.addAll(panelMembers);
        }

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
