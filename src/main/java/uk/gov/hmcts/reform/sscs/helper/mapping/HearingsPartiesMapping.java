package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMapping.DWP_ID;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMapping.getEntityRoleCode;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.DayOfWeekUnavailabilityType.ALL_DAY;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.INTERPRETER;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.RESPONDENT;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.PartyType.INDIVIDUAL;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.PartyType.ORGANISATION;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.lang.NonNull;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.exception.InvalidMappingException;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.PartyType;
import uk.gov.hmcts.reform.sscs.model.single.hearing.IndividualDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.OrganisationDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.PartyDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.RelatedParty;
import uk.gov.hmcts.reform.sscs.model.single.hearing.UnavailabilityDayOfWeek;
import uk.gov.hmcts.reform.sscs.model.single.hearing.UnavailabilityRange;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;
import uk.gov.hmcts.reform.sscs.utility.HearingChannelUtil;

@SuppressWarnings({"PMD.GodClass", "PMD.ExcessiveImports", "PMD.TooManyMethods", "PMD.CyclomaticComplexity"})
// TODO Unsuppress in future
@Slf4j
public final class HearingsPartiesMapping {

    public static final String LANGUAGE_REFERENCE_TEMPLATE = "%s%s";
    public static final String LANGUAGE_DIALECT_TEMPLATE = "-%s";
    public static final String DWP_PO_FIRST_NAME = "Presenting";
    public static final String DWP_PO_LAST_NAME = "Officer";

    public static final String ORGANISATION_NAME_REPLACEMENT = "-";

    private HearingsPartiesMapping() {

    }

    public static List<PartyDetails> buildHearingPartiesDetails(HearingWrapper wrapper, ReferenceDataServiceHolder refData)
            throws ListingException {
        return buildHearingPartiesDetails(wrapper.getCaseData(), refData);
    }

    public static List<PartyDetails> buildHearingPartiesDetails(SscsCaseData caseData, ReferenceDataServiceHolder refData)
            throws ListingException {

        Appeal appeal = caseData.getAppeal();
        Appellant appellant = appeal.getAppellant();

        List<PartyDetails> partiesDetails = new ArrayList<>();

        partiesDetails.add(createDwpPartyDetails(caseData));

        if (isYes(caseData.getJointParty().getHasJointParty())) {
            partiesDetails.addAll(
                    buildHearingPartiesPartyDetails(
                            caseData.getJointParty(),
                            null,
                            null,
                            null,
                            null,
                            refData,
                            null
                    ));
        }

        OverrideFields overrideFields = OverridesMapping.getOverrideFields(caseData);

        String adjournLanguageRef = Optional.of(caseData)
                .filter(caseD -> isYes(caseD.getAdjournment().getInterpreterRequired()))
                .map(SscsCaseData::getAdjournment)
                .map(Adjournment::getInterpreterLanguage)
                .map(DynamicList::getValue)
                .map(DynamicListItem::getCode)
                .filter(StringUtils::isNotBlank)
                .orElse(null);

        partiesDetails.addAll(buildHearingPartiesPartyDetails(
                appellant, appeal.getRep(), appeal.getHearingOptions(),
                appeal.getHearingSubtype(), overrideFields, refData, adjournLanguageRef));

        List<CcdValue<OtherParty>> otherParties = caseData.getOtherParties();

        if (nonNull(otherParties)) {
            for (CcdValue<OtherParty> ccdOtherParty : otherParties) {
                OtherParty otherParty = ccdOtherParty.getValue();
                partiesDetails.addAll(buildHearingPartiesPartyDetails(
                        otherParty, otherParty.getRep(), otherParty.getHearingOptions(),
                        otherParty.getHearingSubtype(), null, refData, null));
            }
        }

        return partiesDetails;
    }

    public static List<PartyDetails> buildHearingPartiesPartyDetails(Party party,
                                                                     Representative rep,
                                                                     HearingOptions hearingOptions,
                                                                     HearingSubtype hearingSubtype,
                                                                     OverrideFields overrideFields,
                                                                     ReferenceDataServiceHolder refData,
                                                                     String adjournLanguage)
            throws ListingException {

        List<PartyDetails> partyDetails = new ArrayList<>();
        partyDetails.add(createHearingPartyDetails(party,
                hearingOptions,
                hearingSubtype,
                party.getId(),
                overrideFields,
                refData,
                adjournLanguage));

        if (nonNull(party.getAppointee()) && isYes(party.getIsAppointee())) {
            partyDetails.add(createHearingPartyDetails(party.getAppointee(),
                    hearingOptions,
                    hearingSubtype,
                    party.getId(),
                    null,
                    refData,
                    null));
        }

        if (nonNull(rep) && isYes(rep.getHasRepresentative())) {
            partyDetails.add(createHearingPartyDetails(rep,
                    hearingOptions,
                    hearingSubtype,
                    party.getId(),
                    null,
                    refData,
                    null));
        }

        return partyDetails;
    }

    public static PartyDetails createHearingPartyDetails(Entity entity,
                                                         HearingOptions hearingOptions,
                                                         HearingSubtype hearingSubtype,
                                                         String partyId,
                                                         OverrideFields overrideFields,
                                                         ReferenceDataServiceHolder refData,
                                                         String adjournLanguage)
            throws ListingException {

        PartyDetails.PartyDetailsBuilder partyDetails = PartyDetails.builder();

        partyDetails.partyID(getPartyId(entity));
        partyDetails.partyType(getPartyType(entity));
        partyDetails.partyRole(getPartyRole(entity));
        partyDetails.individualDetails(getPartyIndividualDetails(entity,
                hearingOptions,
                hearingSubtype,
                partyId,
                overrideFields,
                refData,
                adjournLanguage));
        partyDetails.partyChannelSubType(getPartyChannelSubType());
        partyDetails.unavailabilityDayOfWeek(getPartyUnavailabilityDayOfWeek());
        partyDetails.unavailabilityRanges(getPartyUnavailabilityRange(hearingOptions));

        return partyDetails.build();
    }

    public static PartyDetails createDwpPartyDetails(SscsCaseData caseData) throws ListingException {
        return PartyDetails.builder()
                .partyID(DWP_ID)
                .partyType(ORGANISATION)
                .partyRole(RESPONDENT.getHmcReference())
                .organisationDetails(getDwpOrganisationDetails(caseData))
                .unavailabilityDayOfWeek(getDwpUnavailabilityDayOfWeek())
                .unavailabilityRanges(getPartyUnavailabilityRange(null))
                .build();
    }

    public static String getPartyId(Entity entity) {
        return entity.getId().length() > 15 ? entity.getId().substring(0, 15) : entity.getId();
    }

    public static PartyType getPartyType(Entity entity) {
        return isBlank(entity.getOrganisation()) || (entity instanceof Representative) ? INDIVIDUAL : ORGANISATION;
    }

    public static String getPartyRole(Entity entity) {
        return getEntityRoleCode(entity).getHmcReference();
    }

    public static IndividualDetails getPartyIndividualDetails(Entity entity, HearingOptions hearingOptions,
                                                              HearingSubtype hearingSubtype,
                                                              String partyId,
                                                              OverrideFields overrideFields,
                                                              ReferenceDataServiceHolder refData,
                                                              String adjournLanguage)
            throws ListingException {

        return IndividualDetails.builder()
                .firstName(getIndividualFirstName(entity))
                .lastName(getIndividualLastName(entity))
                .preferredHearingChannel(HearingChannelUtil.getIndividualPreferredHearingChannel(hearingSubtype, hearingOptions, overrideFields))
                .interpreterLanguage(getIndividualInterpreterLanguage(hearingOptions, overrideFields, refData, adjournLanguage))
                .reasonableAdjustments(HearingsAdjustmentMapping.getIndividualsAdjustments(hearingOptions))
                .vulnerableFlag(isIndividualVulnerableFlag())
                .vulnerabilityDetails(getIndividualVulnerabilityDetails())
                .hearingChannelEmail(getIndividualHearingChannelEmail(hearingSubtype))
                .hearingChannelPhone(getIndividualHearingChannelPhone(hearingSubtype))
                .relatedParties(getIndividualRelatedParties(entity, partyId))
                .custodyStatus(getIndividualCustodyStatus())
                .otherReasonableAdjustmentDetails(getIndividualOtherReasonableAdjustmentDetails())
                .build();
    }

    public static String getIndividualFirstName(Entity entity) throws ListingException {
        String org = getIndividualOrganisation(entity);
        if (isNotEmpty(org)) {
            return ORGANISATION_NAME_REPLACEMENT;
        }

        String firstName = entity.getName().getFirstName();
        if (isEmpty(firstName)) {
            throw new ListingException("Missing first name");
        }

        return firstName;
    }

    public static String getIndividualLastName(Entity entity) throws ListingException {
        String org = getIndividualOrganisation(entity);
        if (isNotEmpty(org)) {
            return ORGANISATION_NAME_REPLACEMENT;
        }

        String firstName = entity.getName().getLastName();
        if (isEmpty(firstName)) {
            throw new ListingException("Missing last name");
        }

        return firstName;
    }

    public static String getIndividualOrganisation(Entity entity) {
        return entity.getOrganisation();
    }

    public static String getIndividualFullName(Entity entity) {
        return entity.getName().getFullNameNoTitle();
    }

    public static String getIndividualInterpreterLanguage(HearingOptions hearingOptions,
                                                          OverrideFields overrideFields,
                                                          ReferenceDataServiceHolder refData,
                                                          String adjournLanguage)
            throws InvalidMappingException {

        if (nonNull(adjournLanguage)) {
            return adjournLanguage;
        }

        if (nonNull(overrideFields)
                && nonNull(overrideFields.getAppellantInterpreter())
                && nonNull(overrideFields.getAppellantInterpreter().getIsInterpreterWanted())) {
            return getOverrideInterpreterLanguage(overrideFields);
        }

        if (isNull(hearingOptions)
                || isFalse(hearingOptions.wantsSignLanguageInterpreter())
                && isNoOrNull(hearingOptions.getLanguageInterpreter())) {
            return null;
        }

        return getLanguageReference(getLanguage(hearingOptions, refData));
    }

    @Nullable
    public static Language getLanguage(HearingOptions hearingOptions, ReferenceDataServiceHolder refData)
            throws InvalidMappingException {

        Language language = null;

        if (isTrue(hearingOptions.wantsSignLanguageInterpreter())) {
            String signLanguage = hearingOptions.getSignLanguageType();
            language = refData.getSignLanguages().getSignLanguage(signLanguage);
            if (isNull(language)) {
                throw new InvalidMappingException(String.format("The language %s cannot be mapped", signLanguage));
            }
        }

        if (isYes(hearingOptions.getLanguageInterpreter())) {
            String verbalLanguage = hearingOptions.getLanguages();
            language = refData.getVerbalLanguages().getVerbalLanguage(verbalLanguage);
            if (isNull(language)) {
                throw new InvalidMappingException(String.format("The language %s cannot be mapped", verbalLanguage));
            }
        }
        return language;
    }

    public static String getLanguageReference(Language language) {
        if (isNull(language)) {
            return null;
        }
        return String.format(LANGUAGE_REFERENCE_TEMPLATE,
                language.getReference(), getDialectReference(language));
    }

    private static String getDialectReference(Language language) {
        if (isBlank(language.getDialectReference())) {
            return "";
        }
        return String.format(LANGUAGE_DIALECT_TEMPLATE, language.getDialectReference());
    }

    @Nullable
    public static String getOverrideInterpreterLanguage(OverrideFields overrideFields) {
        if (isYes(overrideFields.getAppellantInterpreter().getIsInterpreterWanted())) {
            return Optional.ofNullable(overrideFields.getAppellantInterpreter().getInterpreterLanguage())
                    .map(DynamicList::getValue)
                    .map(DynamicListItem::getCode)
                    .orElse(null);
        }
        return null;
    }

    public static boolean isIndividualVulnerableFlag() {
        // TODO Future Work
        return false;
    }

    public static String getIndividualVulnerabilityDetails() {
        // TODO Future Work
        return null;
    }

    public static List<String> getIndividualHearingChannelEmail(HearingSubtype hearingSubtype) {
        List<String> emails = new ArrayList<>();
        if (nonNull(hearingSubtype) && isNotBlank(hearingSubtype.getHearingVideoEmail())) {
            emails.add(hearingSubtype.getHearingVideoEmail());
        }
        return emails;
    }

    public static List<String> getIndividualHearingChannelPhone(HearingSubtype hearingSubtype) {
        List<String> phoneNumbers = new ArrayList<>();
        if (nonNull(hearingSubtype) && isNotBlank(hearingSubtype.getHearingTelephoneNumber())) {
            phoneNumbers.add(hearingSubtype.getHearingTelephoneNumber());
        }
        return phoneNumbers;
    }

    public static List<RelatedParty> getIndividualRelatedParties(Entity entity, String partyId) {
        List<RelatedParty> relatedParties = new ArrayList<>();
        EntityRoleCode roleCode = getEntityRoleCode(entity);

        if (REPRESENTATIVE.equals(roleCode) || INTERPRETER.equals(roleCode)) {
            relatedParties.add(getRelatedParty(partyId, roleCode.getPartyRelationshipType().getRelationshipTypeCode()));
        }

        return relatedParties;
    }


    public static RelatedParty getRelatedParty(@NonNull String id, String relationshipType) {
        String shortenId = id.length() > 15 ? id.substring(0, 15) : id;
        return RelatedParty.builder()
                .relatedPartyId(shortenId)
                .relationshipType(relationshipType)
                .build();
    }

    public static String getIndividualCustodyStatus() {
        // TODO Future work
        return null;
    }

    public static String getIndividualOtherReasonableAdjustmentDetails() {
        // TODO Future work
        return null;
    }

    public static String getPartyChannelSubType() {
        // TODO Future work
        return null;
    }

    public static OrganisationDetails getPartyOrganisationDetails() {
        // Not used as of now
        return null;
    }

    public static OrganisationDetails getDwpOrganisationDetails(SscsCaseData caseData) {
        return OrganisationDetails.builder()
                .name(getOrganisationName(caseData.getBenefitCode()))
                .organisationType("ORG")
                .build();
    }

    private static String getOrganisationName(String benefitCode) {
        return List.of("015", "016", "030", "034", "050", "053", "054", "055", "057", "058").contains(benefitCode) ? "HMRC" : "DWP";
    }

    public static List<UnavailabilityDayOfWeek> getPartyUnavailabilityDayOfWeek() {
        // Not used as of now
        return Collections.emptyList();
    }

    public static List<UnavailabilityDayOfWeek> getDwpUnavailabilityDayOfWeek() {
        // Not used as of now
        return getPartyUnavailabilityDayOfWeek();
    }

    public static List<UnavailabilityRange> getPartyUnavailabilityRange(HearingOptions hearingOptions) throws ListingException {
        if (isNull(hearingOptions) || isNull(hearingOptions.getExcludeDates())) {
            return Collections.emptyList();
        }

        List<UnavailabilityRange> unavailabilityRanges = new LinkedList<>();

        for (ExcludeDate excludeDate : hearingOptions.getExcludeDates()) {
            UnavailabilityRange unavailabilityRange = getUnavailabilityRange(excludeDate.getValue());

            if (nonNull(unavailabilityRange)) {
                unavailabilityRanges.add(unavailabilityRange);
            }
        }

        return unavailabilityRanges;
    }

    private static UnavailabilityRange getUnavailabilityRange(DateRange dateRange) throws ListingException {
        LocalDate startDate = dateRange.getStartDate();
        LocalDate endDate = dateRange.getEndDate();


        if (isNull(startDate) && isNull(endDate)) {
            log.info("startDate and endDate are both null, returning null for UnavailabilityRange object");
            return null;
        }

        if (!isNull(startDate) && endDate.isBefore(startDate)) {
            throw new ListingException("endDate is before startDate");
        }

        return UnavailabilityRange.builder()
                .unavailableFromDate(startDate)
                .unavailableToDate(endDate)
                .unavailabilityType(ALL_DAY.getLabel())
                .build();
    }
}


