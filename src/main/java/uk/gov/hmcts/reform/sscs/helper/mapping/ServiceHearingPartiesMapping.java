package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMapping.DWP_ID;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.RESPONDENT;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.PartyType.INDIVIDUAL;

import java.util.ArrayList;
import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.IndividualDetails;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;
import uk.gov.hmcts.reform.sscs.utility.HearingChannelUtil;

@SuppressWarnings("PMD.ExcessiveImports")
public final class ServiceHearingPartiesMapping {

    private ServiceHearingPartiesMapping() {
        throw new IllegalStateException("Utility class");
    }

    public static List<PartyDetails> buildServiceHearingPartiesDetails(SscsCaseData caseData, ReferenceDataServiceHolder refData)
            throws ListingException {

        Appeal appeal = caseData.getAppeal();
        Appellant appellant = appeal.getAppellant();

        List<PartyDetails> partiesDetails = new ArrayList<>();

        if (HearingsDetailsMapping.isPoOfficerAttending(caseData)) {
            partiesDetails.add(createDwpPartyDetails(caseData));
        }

        if (isYes(caseData.getJointParty().getHasJointParty())) {
            partiesDetails.add(createJointPartyDetails());
        }

        partiesDetails.addAll(buildServiceHearingPartiesPartyDetails(
                appellant, appeal.getRep(), appeal.getHearingOptions(), appeal.getHearingSubtype(), refData));

        List<CcdValue<OtherParty>> otherParties = caseData.getOtherParties();

        if (nonNull(otherParties)) {
            for (CcdValue<OtherParty> ccdOtherParty : otherParties) {
                OtherParty otherParty = ccdOtherParty.getValue();
                partiesDetails.addAll(buildServiceHearingPartiesPartyDetails(
                        otherParty, otherParty.getRep(), otherParty.getHearingOptions(), otherParty.getHearingSubtype(), refData));
            }
        }

        return partiesDetails;
    }

    public static List<PartyDetails> buildServiceHearingPartiesPartyDetails(Party party, Representative rep,
                                                                            HearingOptions hearingOptions,
                                                                            HearingSubtype hearingSubtype,
                                                                            ReferenceDataServiceHolder refData)
            throws ListingException {
        List<PartyDetails> partyDetails = new ArrayList<>();
        partyDetails.add(createHearingPartyDetails(party, hearingOptions, hearingSubtype, party.getId(), refData));
        if (nonNull(party.getAppointee()) && isYes(party.getIsAppointee())) {
            partyDetails.add(createHearingPartyDetails(party.getAppointee(), hearingOptions, hearingSubtype, party.getId(), refData));
        }
        if (nonNull(rep) && isYes(rep.getHasRepresentative())) {
            partyDetails.add(createHearingPartyDetails(rep, hearingOptions, hearingSubtype, party.getId(), refData));
        }
        return partyDetails;
    }

    public static PartyDetails createHearingPartyDetails(Entity entity, HearingOptions hearingOptions,
                                                         HearingSubtype hearingSubtype,
                                                         String partyId, ReferenceDataServiceHolder refData)
            throws ListingException {
        PartyDetails.PartyDetailsBuilder partyDetails = PartyDetails.builder();
        partyDetails.partyID(HearingsPartiesMapping.getPartyId(entity));
        partyDetails.partyType(HearingsPartiesMapping.getPartyType(entity));
        partyDetails.partyRole(HearingsPartiesMapping.getPartyRole(entity));
        partyDetails.partyName(HearingsPartiesMapping.getIndividualFullName(entity));
        partyDetails.individualDetails(getPartyIndividualDetails(entity, hearingOptions, hearingSubtype, partyId, refData));
        HearingChannel partyChannel = HearingChannelUtil.getIndividualPreferredHearingChannel(hearingSubtype, hearingOptions, null);
        partyDetails.partyChannel(partyChannel != null ? partyChannel.getHmcReference() : null);
        partyDetails.organisationDetails(HearingsPartiesMapping.getPartyOrganisationDetails());
        partyDetails.unavailabilityDow(HearingsPartiesMapping.getPartyUnavailabilityDayOfWeek());
        partyDetails.unavailabilityRanges(HearingsPartiesMapping.getPartyUnavailabilityRange(hearingOptions));

        return partyDetails.build();
    }

    public static PartyDetails createDwpPartyDetails(SscsCaseData caseData) {
        return PartyDetails.builder()
                .partyID(DWP_ID)
                .partyType(INDIVIDUAL)
                .partyRole(RESPONDENT.getHmcReference())
                .organisationDetails(HearingsPartiesMapping.getDwpOrganisationDetails(caseData))
                .unavailabilityDow(HearingsPartiesMapping.getDwpUnavailabilityDayOfWeek())
                .unavailabilityRanges(null)
                .build();
    }

    public static PartyDetails createJointPartyDetails() {
        // TODO SSCS-10378 - Add joint party logic using caseData or refData
        return PartyDetails.builder().build();
    }

    public static IndividualDetails getPartyIndividualDetails(Entity entity, HearingOptions hearingOptions,
                                                              HearingSubtype hearingSubtype,
                                                              String partyId, ReferenceDataServiceHolder refData)
            throws ListingException {
        return IndividualDetails.builder()
                .firstName(HearingsPartiesMapping.getIndividualFirstName(entity))
                .lastName(HearingsPartiesMapping.getIndividualLastName(entity))
                .preferredHearingChannel(HearingChannelUtil.getIndividualPreferredHearingChannel(hearingSubtype, hearingOptions, null))
                .interpreterLanguage(HearingsPartiesMapping.getIndividualInterpreterLanguage(hearingOptions, null, refData, null))
                .reasonableAdjustments(HearingsAdjustmentMapping.getIndividualsAdjustments(hearingOptions))
                .vulnerableFlag(HearingsPartiesMapping.isIndividualVulnerableFlag())
                .vulnerabilityDetails(HearingsPartiesMapping.getIndividualVulnerabilityDetails())
                .hearingChannelEmail(HearingsPartiesMapping.getIndividualHearingChannelEmail(hearingSubtype))
                .hearingChannelPhone(HearingsPartiesMapping.getIndividualHearingChannelPhone(hearingSubtype))
                .relatedParties(HearingsPartiesMapping.getIndividualRelatedParties(entity, partyId))
                .custodyStatus(HearingsPartiesMapping.getIndividualCustodyStatus())
                .otherReasonableAdjustmentDetails(HearingsPartiesMapping.getIndividualOtherReasonableAdjustmentDetails())
                .build();
    }
}
