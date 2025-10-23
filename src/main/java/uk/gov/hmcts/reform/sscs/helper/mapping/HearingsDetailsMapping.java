package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsChannelMapping.getHearingChannels;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsDurationMapping.getHearingDuration;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsDurationMapping.getNonStandardHearingDurationReasons;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsLocationMapping.getHearingLocations;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsNumberAttendeesMapping.getNumberOfPhysicalAttendees;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsWindowMapping.buildHearingWindow;
import static uk.gov.hmcts.reform.sscs.helper.mapping.OverridesMapping.getAmendReasonCodes;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingPriority.STANDARD;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingPriority.URGENT;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getHmcHearingType;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCasePanelMembersExcluded;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTime;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Entity;
import uk.gov.hmcts.reform.sscs.ccd.domain.HmcHearingType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Party;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDetails;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@Slf4j
@Component
public final class HearingsDetailsMapping {

    private final HearingsPanelMapping hearingsPanelMapping;

    private final HearingsAutoListMapping hearingsAutoListMapping;

    HearingsDetailsMapping(HearingsPanelMapping hearingsPanelMapping, HearingsAutoListMapping hearingsAutoListMapping) {
        this.hearingsPanelMapping = hearingsPanelMapping;
        this.hearingsAutoListMapping = hearingsAutoListMapping;
    }

    public HearingDetails buildHearingDetails(HearingWrapper wrapper, ReferenceDataServiceHolder refData) throws ListingException {
        // get case data and set adjournmentInProgress flag for use in downstream method calls
        SscsCaseData caseData = wrapper.getCaseData();
        boolean adjournmentInProgress = isYes(caseData.getAdjournment().getAdjournmentInProgress());
        return HearingDetails.builder()
                .autolistFlag(hearingsAutoListMapping.shouldBeAutoListed(caseData))
                .hearingType(getHearingType(caseData))
                .hearingWindow(buildHearingWindow(caseData, refData))
                .duration(getHearingDuration(caseData, refData))
                .nonStandardHearingDurationReasons(getNonStandardHearingDurationReasons())
                .hearingPriorityType(getHearingPriority(caseData))
                .numberOfPhysicalAttendees(getNumberOfPhysicalAttendees(caseData, adjournmentInProgress))
                .hearingInWelshFlag(shouldBeHearingsInWelshFlag())
                .hearingLocations(getHearingLocations(caseData, refData))
                .facilitiesRequired(getFacilitiesRequired())
                .listingComments(getListingComments(caseData))
                .hearingRequester(getHearingRequester())
                .privateHearingRequiredFlag(isPrivateHearingRequired())
                .leadJudgeContractType(getLeadJudgeContractType())
                .panelRequirements(hearingsPanelMapping.getPanelRequirements(caseData))
                .hearingIsLinkedFlag(isCaseLinked(caseData))
                .amendReasonCodes(getAmendReasonCodes(caseData))
                .hearingChannels(getHearingChannels(caseData, adjournmentInProgress))
                .build();
    }

    public static HmcHearingType getHearingType(SscsCaseData sscsCaseData) {
        return getHmcHearingType(sscsCaseData) != null
                ? getHmcHearingType(sscsCaseData) : HmcHearingType.SUBSTANTIVE;
    }

    public static boolean isCaseUrgent(@Valid SscsCaseData caseData) {
        return isYes(caseData.getUrgentCase());
    }

    public static String getHearingPriority(SscsCaseData caseData) {
        // urgentCase Should go to top of queue in LA - also consider case created date
        // Flag to Lauren - how  can this be captured in HMC queue?
        // If there's an adjournment - date shouldn't reset - should also go to top priority

        // TODO Adjournment - Check what should be used to check if there is adjournment
        if (isCaseUrgent(caseData) || caseData.getAdjournment().getPanelMembersExcluded() == AdjournCasePanelMembersExcluded.YES) {
            return URGENT.getHmcReference();
        }
        return STANDARD.getHmcReference();
    }

    public static boolean shouldBeHearingsInWelshFlag() {
        // TODO Future Work
        return false;
    }

    public static List<String> getFacilitiesRequired() {
        return Collections.emptyList();
    }

    public static String getListingComments(SscsCaseData caseData) {
        Appeal appeal = caseData.getAppeal();
        List<CcdValue<OtherParty>> otherParties = caseData.getOtherParties();

        List<String> listingComments = new ArrayList<>(addAdjournmentTimeSelectionComments(caseData));

        if (nonNull(appeal.getHearingOptions()) && isNotBlank(appeal.getHearingOptions().getOther())) {
            listingComments.add(getComment(appeal.getAppellant(), appeal.getHearingOptions().getOther()));
        }
        if (nonNull(otherParties) && !otherParties.isEmpty()) {
            listingComments.addAll(otherParties.stream()
                    .map(CcdValue::getValue)
                    .filter(o -> o.getHearingOptions() != null)
                    .filter(o -> isNotBlank(o.getHearingOptions().getOther()))
                    .map(o -> getComment(o, o.getHearingOptions().getOther()))
                    .toList());
        }

        if (listingComments.isEmpty()) {
            return null;
        }

        return String.join(String.format("%n%n"), listingComments);
    }

    private static List<String> addAdjournmentTimeSelectionComments(SscsCaseData caseData) {
        List<String> listingComments = new ArrayList<>();

        var adjournment = caseData.getAdjournment();
        AdjournCaseTime adjournCaseTime = adjournment.getTime();

        if (isNotEmpty(adjournment.getNextHearingDateType()) && isNotEmpty(adjournCaseTime)) {
            if (isNotEmpty(adjournCaseTime.getAdjournCaseNextHearingFirstOnSession())) {
                var firstOnSession = "List first on the session";
                listingComments.add(firstOnSession);
            }

            String adjournCaseNextHearingSpecificTime = adjournCaseTime.getAdjournCaseNextHearingSpecificTime();
            if (isNotEmpty(adjournCaseNextHearingSpecificTime)) {
                var provideTime = String.format("Provide time: %S", adjournCaseNextHearingSpecificTime);
                listingComments.add(provideTime);
            }

        }

        return listingComments;
    }

    public static String getComment(Party party, String comment) {
        return String.format("%s%n%s", getCommentSubheader(party), comment);
    }

    public static String getCommentSubheader(Party party) {
        return String.format("%s - %s:", getPartyRole(party), getEntityName(party));
    }

    public static String getPartyRole(Party party) {
        return nonNull(party.getRole()) && isNotBlank(party.getRole().getName())
                ? party.getRole().getName()
                : HearingsMapping.getEntityRoleCode(party).getValueEn();
    }

    public static String getEntityName(Entity entity) {
        return entity.getName().getFullName();
    }

    public static String getHearingRequester() {
        // TODO Implementation to be done by SSCS-10260. Optional?
        return null;
    }

    public static boolean isPrivateHearingRequired() {
        // TODO Future Work
        return false;
    }

    public static String getLeadJudgeContractType() {
        // TODO Implementation to be done by SSCS-10260
        return null;
    }


    public static boolean isCaseLinked(@Valid SscsCaseData caseData) {
        return isNotEmpty(caseData.getLinkedCase());
    }

    public static boolean isPoOfficerAttending(@Valid SscsCaseData caseData) {
        return isYes(caseData.getDwpIsOfficerAttending());
    }
}
