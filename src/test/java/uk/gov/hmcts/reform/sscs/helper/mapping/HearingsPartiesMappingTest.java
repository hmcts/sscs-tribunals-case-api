package uk.gov.hmcts.reform.sscs.helper.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.DayOfWeekUnavailabilityType.ALL_DAY;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.APPELLANT;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.PartyRelationshipType.INTERPRETER;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.PartyRelationshipType.SOLICITOR;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.PartyType.ORGANISATION;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.FACE_TO_FACE;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.NOT_ATTENDING;
import static uk.gov.hmcts.reform.sscs.utility.HearingChannelUtil.getIndividualPreferredHearingChannel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.exception.InvalidMappingException;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.DayOfWeekUnavailabilityType;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.PartyType;
import uk.gov.hmcts.reform.sscs.model.single.hearing.OrganisationDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.PartyDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.UnavailabilityDayOfWeek;
import uk.gov.hmcts.reform.sscs.model.single.hearing.UnavailabilityRange;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;

class HearingsPartiesMappingTest extends HearingsMappingBase {

    public static final String EMAIL_ADDRESS = "test@test.com";
    public static final String TELEPHONE_NUMBER = "01000000000";
    public static final String PARTY_ID = "a2b837d5-ee28-4bc9-a3d8-ce2d2de9fb29";
    public static final String OTHER_PARTY_ID = "4dd6b6fa-6562-4699-8e8b-6c70cf8a333e";
    public static final String DWP_ID = "DWP";

    @DisplayName("When a valid hearing wrapper with language interpreter is given buildHearingPartiesDetails returns the correct Hearing Parties Details")
    @Test
    void buildHearingPartiesDetailsAdjournCaseInterpreterLanguageProvided() throws ListingException {
        SscsCaseData caseData = SscsCaseData.builder()
            .adjournment(Adjournment.builder()
                 .interpreterRequired(YES)
                 .interpreterLanguage(new DynamicList("French"))
            .build())
            .appeal(Appeal.builder()
                .hearingOptions(HearingOptions.builder().wantsToAttend("yes").build())
                .hearingType("test")
                .hearingSubtype(HearingSubtype.builder().hearingVideoEmail("email@email.com").wantsHearingTypeFaceToFace("yes").build())
                .appellant(Appellant.builder()
                     .id(PARTY_ID)
                     .name(Name.builder()
                         .title("title")
                           .firstName("first")
                           .lastName("last")
                           .build())
                     .build())
                .build())
            .benefitCode("002")
            .build();
        HearingWrapper wrapper = HearingWrapper.builder()
            .caseData(caseData)
            .caseData(caseData)
            .build();

        List<PartyDetails> hearingPartiesDetails = HearingsPartiesMapping.buildHearingPartiesDetails(wrapper, refData);

        PartyDetails partyDetails = hearingPartiesDetails.stream().filter(o -> PARTY_ID.substring(0,15).equalsIgnoreCase(o.getPartyID())).findFirst().orElse(
            null);
        assertThat(partyDetails).isNotNull();
        assertThat(partyDetails.getPartyType()).isNotNull();
        assertThat(partyDetails.getPartyRole()).isNotNull();
        assertThat(partyDetails.getIndividualDetails()).isNotNull();
        assertThat(partyDetails.getOrganisationDetails()).isNull();
        assertThat(partyDetails.getUnavailabilityDayOfWeek()).isEmpty();
        assertThat(partyDetails.getUnavailabilityRanges()).isEmpty();

        assertThat(hearingPartiesDetails.stream().filter(o -> DWP_ID.equalsIgnoreCase(o.getPartyID())).findFirst()).isPresent();
    }

    @DisplayName("When a valid hearing wrapper without OtherParties or joint party is given buildHearingPartiesDetails returns the correct Hearing Parties Details")
    @Test
    void buildHearingPartiesDetails() throws ListingException {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .hearingOptions(HearingOptions.builder().wantsToAttend("yes").build())
                .hearingType("test")
                .hearingSubtype(HearingSubtype.builder().hearingVideoEmail("email@email.com").wantsHearingTypeFaceToFace("yes").build())
                .appellant(Appellant.builder()
                   .id(PARTY_ID)
                   .name(Name.builder()
                       .title("title")
                       .firstName("first")
                       .lastName("last")
                       .build())
                   .build())
                .build())
            .benefitCode("002")
            .build();
        HearingWrapper wrapper = HearingWrapper.builder()
            .caseData(caseData)
            .caseData(caseData)
            .build();

        List<PartyDetails> partiesDetails = HearingsPartiesMapping.buildHearingPartiesDetails(wrapper, refData);

        PartyDetails partyDetails = partiesDetails.stream().filter(o -> PARTY_ID.substring(0,15).equalsIgnoreCase(o.getPartyID())).findFirst().orElse(
            null);
        assertThat(partyDetails).isNotNull();
        assertThat(partyDetails.getPartyType()).isNotNull();
        assertThat(partyDetails.getPartyRole()).isNotNull();
        assertThat(partyDetails.getIndividualDetails()).isNotNull();
        assertThat(partyDetails.getOrganisationDetails()).isNull();
        assertThat(partyDetails.getUnavailabilityDayOfWeek()).isEmpty();
        assertThat(partyDetails.getUnavailabilityRanges()).isEmpty();

        assertThat(partiesDetails.stream().filter(o -> DWP_ID.equalsIgnoreCase(o.getPartyID())).findFirst()).isPresent();
    }

    @DisplayName("When a valid hearing wrapper when PO attending is given buildHearingPartiesDetails returns the correct Hearing Parties Details")
    @Test
    void buildHearingPartiesDetailsPoAttending() throws ListingException {
        SscsCaseData caseData = SscsCaseData.builder()
            .dwpIsOfficerAttending("Yes")
            .benefitCode("001")
            .appeal(Appeal.builder()
                .hearingOptions(HearingOptions.builder().wantsToAttend("yes").build())
                .hearingType("test")
                .hearingSubtype(HearingSubtype.builder().hearingVideoEmail("email@email.com").wantsHearingTypeFaceToFace("yes").build())
                .appellant(Appellant.builder()
                     .id(PARTY_ID)
                     .name(Name.builder()
                         .title("title")
                         .firstName("first")
                         .lastName("last")
                         .build())
                     .build())
                .build())
            .build();
        HearingWrapper wrapper = HearingWrapper.builder()
            .caseData(caseData)
            .caseData(caseData)
            .build();

        List<PartyDetails> partiesDetails = HearingsPartiesMapping.buildHearingPartiesDetails(wrapper, refData);

        assertThat(partiesDetails.stream().filter(o -> PARTY_ID.substring(0,15).equalsIgnoreCase(o.getPartyID())).findFirst()).isPresent();

        assertThat(partiesDetails)
            .filteredOn(partyDetails -> DWP_ID.equals(partyDetails.getPartyID()))
            .hasSize(1)
            .extracting(
                PartyDetails::getPartyType,
                PartyDetails::getPartyRole,
                PartyDetails::getOrganisationDetails,
                PartyDetails::getUnavailabilityDayOfWeek,
                PartyDetails::getUnavailabilityRanges)
            .contains(tuple(ORGANISATION, "RESP", OrganisationDetails.builder()
                .name("DWP")
                .organisationType("ORG")
                .build(), List.of(), List.of()));
    }

    @DisplayName("When a valid hearing wrapper when PO attending is not Yes given buildHearingPartiesDetails returns the correct Hearing Parties Details")
    @ParameterizedTest
    @ValueSource(strings = {"No"})
    @NullAndEmptySource
    void buildHearingPartiesDetailsPoAttending(String officerAttending) throws ListingException {
        SscsCaseData caseData = SscsCaseData.builder()
            .dwpIsOfficerAttending(officerAttending)
            .appeal(Appeal.builder()
                .hearingOptions(HearingOptions.builder().wantsToAttend("yes").build())
                .hearingType("test")
                .hearingSubtype(HearingSubtype.builder().hearingVideoEmail("email@email.com").wantsHearingTypeFaceToFace("yes").build())
                .appellant(Appellant.builder()
                    .id(PARTY_ID)
                    .name(Name.builder()
                        .title("title")
                        .firstName("first")
                        .lastName("last")
                        .build())
                   .build())
                .build())
            .benefitCode("002")
            .build();
        HearingWrapper wrapper = HearingWrapper.builder()
            .caseData(caseData)
            .caseData(caseData)
            .build();

        List<PartyDetails> partiesDetails = HearingsPartiesMapping.buildHearingPartiesDetails(wrapper, refData);

        assertThat(partiesDetails.stream().filter(o -> PARTY_ID.substring(0,15).equalsIgnoreCase(o.getPartyID())).findFirst()).isPresent();

        assertThat(partiesDetails.stream().filter(o -> DWP_ID.equalsIgnoreCase(o.getPartyID())).findFirst()).isPresent();
    }

    @DisplayName("When a valid hearing wrapper is given with OtherParties buildHearingPartiesDetails returns the correct Hearing Parties Details")
    @Test
    void buildHearingPartiesDetailsOtherParties() throws ListingException {
        String otherPartyId = OTHER_PARTY_ID;
        List<CcdValue<OtherParty>> otherParties = new ArrayList<>();
        otherParties.add(new CcdValue<>(OtherParty.builder()
                        .hearingOptions(HearingOptions.builder().wantsToAttend("yes").build())
                        .hearingSubtype(HearingSubtype.builder().hearingVideoEmail("email@email.com").wantsHearingTypeFaceToFace("yes").build())
                        .id(otherPartyId)
                        .name(Name.builder()
                                  .title("title")
                                  .firstName("first")
                                  .lastName("last")
                                  .build())
                        .build()));
        SscsCaseData caseData = SscsCaseData.builder()
            .otherParties(otherParties)
            .appeal(Appeal.builder()
                        .hearingOptions(HearingOptions.builder().wantsToAttend("yes").build())
                        .hearingType("test")
                        .hearingSubtype(HearingSubtype.builder().hearingVideoEmail("email@email.com").wantsHearingTypeFaceToFace("yes").build())
                        .appellant(Appellant.builder()
                                       .id(PARTY_ID)
                                       .name(Name.builder()
                                                 .title("title")
                                                 .firstName("first")
                                                 .lastName("last")
                                                 .build())
                                       .build())
                        .build())
            .benefitCode("002")
            .build();
        HearingWrapper wrapper = HearingWrapper.builder()
            .caseData(caseData)
            .build();

        List<PartyDetails> partiesDetails = HearingsPartiesMapping.buildHearingPartiesDetails(wrapper, refData);

        assertThat(partiesDetails.stream().filter(o -> PARTY_ID.substring(0,15).equalsIgnoreCase(o.getPartyID())).findFirst()).isPresent();

        PartyDetails partyDetails = partiesDetails.stream().filter(o -> otherPartyId.substring(0,15).equalsIgnoreCase(o.getPartyID())).findFirst().orElse(
            null);
        assertThat(partyDetails).isNotNull();
        assertThat(partyDetails.getPartyType()).isNotNull();
        assertThat(partyDetails.getPartyRole()).isNotNull();
        assertThat(partyDetails.getIndividualDetails()).isNotNull();
        assertThat(partyDetails.getOrganisationDetails()).isNull();
        assertThat(partyDetails.getUnavailabilityDayOfWeek()).isEmpty();
        assertThat(partyDetails.getUnavailabilityRanges()).isEmpty();
    }

    @DisplayName("When a valid hearing wrapper with joint party given buildHearingPartiesDetails returns the correct Hearing Parties Details")
    @ParameterizedTest
    @EnumSource(value = YesNo.class)
    @NullSource
    void buildHearingPartiesDetailsJointParty(YesNo jointParty) throws ListingException {
        Name name = Name.builder()
            .title("title")
            .firstName("first")
            .lastName("last")
            .build();

        JointParty jointPartyDetails = JointParty.builder().id(OTHER_PARTY_ID)
            .hasJointParty(jointParty)
            .name(name)
            .build();

        SscsCaseData caseData = SscsCaseData.builder()
            .jointParty(jointPartyDetails)
            .appeal(Appeal.builder()
                        .hearingOptions(HearingOptions.builder().wantsToAttend("yes").build())
                        .hearingType("test")
                        .hearingSubtype(HearingSubtype.builder().hearingVideoEmail("email@email.com").wantsHearingTypeFaceToFace("yes").build())
                        .appellant(Appellant.builder()
                                       .id(PARTY_ID)
                                       .name(name)
                                       .build())
                        .build())
            .benefitCode("002")
            .build();

        HearingWrapper wrapper = HearingWrapper.builder()
            .caseData(caseData)
            .build();

        List<PartyDetails> partiesDetails = HearingsPartiesMapping.buildHearingPartiesDetails(wrapper, refData);

        assertThat(partiesDetails.stream().filter(o -> PARTY_ID.substring(0,15).equalsIgnoreCase(o.getPartyID())).findFirst()).isPresent();
        assertThat(partiesDetails.stream().anyMatch(o -> OTHER_PARTY_ID.substring(0,15).equalsIgnoreCase(o.getPartyID())));
        assertThat(partiesDetails.stream().filter(o -> DWP_ID.equalsIgnoreCase(o.getPartyID())).findFirst()).isPresent();
    }

    @DisplayName("When HearingOption is  Null return empty string")
    @Test
    void getIndividualInterpreterLanguageWhenHearingOptionsNull() throws InvalidMappingException {

        String individualInterpreterLanguage = HearingsPartiesMapping.getIndividualInterpreterLanguage(
            null, null, refData, null
        );

        assertThat(individualInterpreterLanguage).isNull();
    }

    @DisplayName("When HearingOption is Null and adjournLanguage is provided then return adjournLanguage")
    @Test
    void getIndividualInterpreterLanguageWhenHearingOptionsNullAndAdjournLanguageProvided() throws InvalidMappingException {

        String individualInterpreterLanguage = HearingsPartiesMapping.getIndividualInterpreterLanguage(
            null, null, refData, "TestLanguage"
        );

        assertThat(individualInterpreterLanguage).isEqualTo("TestLanguage");
    }

    @DisplayName("buildHearingPartiesPartyDetails when Appointee is not null Parameterised Tests")
    @ParameterizedTest
    @CsvSource(value = {
        "Yes,true",
        "No,false",
        "null,false",
        ",false",
    }, nullValues = {"null"})
    void buildHearingPartiesPartyDetailsAppointee(String isAppointee, boolean expected) throws ListingException {
        Appointee appointee = Appointee.builder()
            .id(OTHER_PARTY_ID)
            .name(Name.builder()
                      .title("title")
                      .firstName("first")
                      .lastName("last")
                      .build())
            .build();
        Party party = Appellant.builder()
            .id(PARTY_ID)
            .isAppointee(isAppointee)
            .name(Name.builder()
                      .title("title")
                      .firstName("first")
                      .lastName("last")
                      .build())
            .appointee(appointee)
            .build();
        HearingOptions hearingOptions = HearingOptions.builder().build();

        List<PartyDetails> partiesDetails = HearingsPartiesMapping.buildHearingPartiesPartyDetails(
            party,
            null,
            hearingOptions,
            HearingSubtype.builder().hearingVideoEmail("email@email.com").build(),
            null,
            refData,
            null
        );

        assertThat(partiesDetails.stream().filter(o -> PARTY_ID.substring(0,15).equalsIgnoreCase(o.getPartyID())).findFirst()).isPresent();

        PartyDetails appointeeDetails = partiesDetails.stream().filter(o -> OTHER_PARTY_ID.substring(0,15).equalsIgnoreCase(o.getPartyID())).findFirst().orElse(
            null);
        if (expected) {
            assertThat(appointeeDetails).isNotNull();
            assertThat(appointeeDetails.getPartyType()).isNotNull();
            assertThat(appointeeDetails.getPartyRole()).isNotNull();
            assertThat(appointeeDetails.getIndividualDetails()).isNotNull();
            assertThat(appointeeDetails.getOrganisationDetails()).isNull();
            assertThat(appointeeDetails.getUnavailabilityDayOfWeek()).isEmpty();
            assertThat(appointeeDetails.getUnavailabilityRanges()).isEmpty();
        } else {
            assertThat(appointeeDetails).isNull();
        }
    }


    @DisplayName("buildHearingPartiesPartyDetails when Rep is not null Parameterised Tests")
    @ParameterizedTest
    @CsvSource(value = {
        "Yes,true",
        "Yes,true",
        "No,false",
        "No,false",
        "null,false",
        ",false",
    }, nullValues = {"null"})
    void buildHearingPartiesPartyDetailsRep(String hasRepresentative, boolean expected) throws ListingException {
        Representative rep = Representative.builder()
            .id(OTHER_PARTY_ID)
            .hasRepresentative(hasRepresentative)
            .name(Name.builder()
                      .title("title")
                      .firstName("first")
                      .lastName("last")
                      .build())
            .build();

        Party party = Appellant.builder()
            .id(PARTY_ID)
            .organisation("organisation")
            .name(Name.builder()
                      .title("title")
                      .firstName("first")
                      .lastName("last")
                      .build())
            .build();
        HearingOptions hearingOptions = HearingOptions.builder().wantsToAttend("yes").build();
        HearingSubtype hearingSubtype = HearingSubtype.builder().hearingVideoEmail("email@email.com").wantsHearingTypeFaceToFace("yes").build();

        List<PartyDetails> partiesDetails = HearingsPartiesMapping.buildHearingPartiesPartyDetails(
            party,
            rep,
            hearingOptions,
            hearingSubtype,
            null,
            refData,
            null
        );

        assertThat(partiesDetails.stream().filter(o -> PARTY_ID.substring(0,15).equalsIgnoreCase(o.getPartyID())).findFirst()).isPresent();

        PartyDetails repDetails = partiesDetails.stream().filter(o -> OTHER_PARTY_ID.substring(0,15).equalsIgnoreCase(o.getPartyID())).findFirst().orElse(
            null);
        if (expected) {
            assertThat(repDetails).isNotNull();
            assertThat(repDetails.getPartyType()).isNotNull();
            assertThat(repDetails.getPartyRole()).isNotNull();
            assertThat(repDetails.getIndividualDetails()).isNotNull();
            assertThat(repDetails.getIndividualDetails().getFirstName()).isNotEmpty();
            assertThat(repDetails.getOrganisationDetails()).isNull();
            assertThat(repDetails.getUnavailabilityDayOfWeek()).isEmpty();
            assertThat(repDetails.getUnavailabilityRanges()).isEmpty();
        } else {
            assertThat(repDetails).isNull();
        }
    }

    @DisplayName("buildHearingPartiesPartyDetails when Appointee and Rep are both null Parameterised Tests")
    @ParameterizedTest
    @CsvSource(value = {
        "Yes,true",
        "No,false",
        "null,false",
        ",false",
    }, nullValues = {"null"})
    void buildHearingPartiesPartyDetailsAppointeeRepNull() throws ListingException {
        Party party = Appellant.builder()
            .id(PARTY_ID)
            .name(Name.builder()
                      .title("title")
                      .firstName("first")
                      .lastName("last")
                      .build())
            .build();
        HearingOptions hearingOptions = HearingOptions.builder().build();

        List<PartyDetails> partiesDetails = HearingsPartiesMapping.buildHearingPartiesPartyDetails(
            party,
            null,
            hearingOptions,
            HearingSubtype.builder().hearingVideoEmail("email@email.com").build(),
            null,
            refData,
            null
        );

        PartyDetails partyDetails = partiesDetails.stream().filter(o -> PARTY_ID.substring(0,15).equalsIgnoreCase(o.getPartyID())).findFirst().orElse(
            null);
        assertThat(partyDetails).isNotNull();
        assertThat(partyDetails.getPartyType()).isNotNull();
        assertThat(partyDetails.getPartyRole()).isNotNull();
        assertThat(partyDetails.getIndividualDetails()).isNotNull();
        assertThat(partyDetails.getOrganisationDetails()).isNull();
        assertThat(partyDetails.getUnavailabilityDayOfWeek()).isEmpty();
        assertThat(partyDetails.getUnavailabilityRanges()).isEmpty();
    }

    @DisplayName("createHearingPartyDetails Test")
    @Test
    void createHearingPartyDetails() throws ListingException {
        Entity entity = Appellant.builder()
            .id(PARTY_ID)
            .name(Name.builder()
                      .title("title")
                      .firstName("first")
                      .lastName("last")
                      .build())
            .build();
        HearingOptions hearingOptions = HearingOptions.builder().build();
        PartyDetails partyDetails = HearingsPartiesMapping.createHearingPartyDetails(
            entity,
            hearingOptions,
            HearingSubtype.builder().hearingVideoEmail("email@email.com").build(),
            PARTY_ID,
            null,
            refData,
            null
        );

        assertThat(partyDetails.getPartyID()).isNotNull();
        assertThat(partyDetails.getPartyType()).isNotNull();
        assertThat(partyDetails.getPartyRole()).isNotNull();
        assertThat(partyDetails.getIndividualDetails()).isNotNull();
        assertThat(partyDetails.getOrganisationDetails()).isNull();
        assertThat(partyDetails.getUnavailabilityDayOfWeek()).isEmpty();
        assertThat(partyDetails.getUnavailabilityRanges()).isEmpty();
    }

    @DisplayName("When a entity is given with an id getPartyId returns that value")
    @Test
    void testGetPartyId() {
        Entity entity = Appellant.builder().id(PARTY_ID).build();
        String result = HearingsPartiesMapping.getPartyId(entity);

        assertThat(result).isEqualTo(PARTY_ID.substring(0,15));
    }

    @DisplayName("When a entity is given with an id getPartyId returns that value")
    @Test
    void testGetPartyIdShort() {
        Entity entity = Appellant.builder().id("1").build();
        String result = HearingsPartiesMapping.getPartyId(entity);

        assertThat(result).isEqualTo("1");
    }

    @DisplayName("When a entity is given no id getPartyId returns a newly generated id")
    @Test
    void testGetPartyIdNull() {
        Entity entity = Appellant.builder().build();
        String result = HearingsPartiesMapping.getPartyId(entity);

        assertThat(result)
            .isNotNull()
            .hasSize(15);
    }

    @DisplayName("getPartyType Parameterised Tests")
    @ParameterizedTest
    @CsvSource(value = {
        "test,ORGANISATION",
        "null,INDIVIDUAL",
    }, nullValues = {"null"})
    void getPartyType(String value, PartyType expected) {
        Entity entity = Appellant.builder().organisation(value).build();
        PartyType result = HearingsPartiesMapping.getPartyType(entity);

        assertEquals(expected, result);
    }

    @DisplayName("getPartyType Parameterised Tests")
    @ParameterizedTest
    @CsvSource(value = {
        "test,INDIVIDUAL",
        "null,INDIVIDUAL",
    }, nullValues = {"null"})
    void shouldReturnPartyTypeIndividualWhenRepresentative(String value, PartyType expected) {
        Entity entity = Representative.builder().organisation(value).build();
        PartyType result = HearingsPartiesMapping.getPartyType(entity);

        assertEquals(expected, result);
    }

    @ParameterizedTest
    @MethodSource("getPartyReferenceArguments")
    void testGetPartyRole(Entity entity, String reference) {
        String result = HearingsPartiesMapping.getPartyRole(entity);

        assertThat(result).isEqualTo(reference);
    }

    @DisplayName("getIndividualFirstName when null")
    @Test
    void getIndividualFirstNameWhenNull() {
        Entity entity = Appellant.builder().name(Name.builder().build()).build();

        assertThrows(ListingException.class, () -> HearingsPartiesMapping.getIndividualFirstName(entity));
    }

    @DisplayName("getIndividualFirstName")
    @Test
    void getIndividualFirstName() throws ListingException {
        String firstName = "firstname";
        Entity entity = Appellant.builder().name(Name.builder().firstName(firstName).build()).build();
        String result = HearingsPartiesMapping.getIndividualFirstName(entity);

        assertEquals(firstName, result);
    }

    @DisplayName("getIndividualLastName")
    @Test
    void getIndividualLastName() throws ListingException {
        String lastName = "lastname";
        Entity entity = Appellant.builder().name(Name.builder().lastName(lastName).build()).build();
        String result = HearingsPartiesMapping.getIndividualLastName(entity);

        assertEquals(lastName, result);
    }

    @DisplayName("getIndividualLastName when null")
    @Test
    void getIndividualLastNameWhenNull() {
        Entity entity = Appellant.builder().name(Name.builder().build()).build();
        assertThrows(ListingException.class, () -> HearingsPartiesMapping.getIndividualFirstName(entity));
    }

    @DisplayName("When language passed in should return correct LOV format")
    @ParameterizedTest
    @CsvSource({"Acholi,ach", "Afrikaans,afr", "Akan,aka", "Albanian,alb", "Zaza,zza", "Zulu,zul"})
    void testGetIndividualInterpreterLanguage(String lang, String expected) throws InvalidMappingException {
        given(verbalLanguages.getVerbalLanguage(lang))
            .willReturn(new Language(expected,"Test",null,null,null,List.of(lang)));

        given(refData.getVerbalLanguages()).willReturn(verbalLanguages);

        HearingOptions hearingOptions = HearingOptions.builder()
            .languageInterpreter("Yes")
            .languages(lang)
            .build();

        String result = HearingsPartiesMapping.getIndividualInterpreterLanguage(hearingOptions, null, refData, null);
        assertThat(result).isEqualTo(expected);
    }

    @DisplayName("When a invalid verbal language is given getIndividualInterpreterLanguage should throw the correct error and message")
    @ParameterizedTest
    @ValueSource(strings = {"Test"})
    @NullAndEmptySource
    void testGetIndividualInterpreterLanguage(String value) {
        given(verbalLanguages.getVerbalLanguage(value))
            .willReturn(null);

        given(refData.getVerbalLanguages()).willReturn(verbalLanguages);

        HearingOptions hearingOptions = HearingOptions.builder()
            .languageInterpreter("Yes")
            .languages(value)
            .build();

        assertThatExceptionOfType(InvalidMappingException.class)
            .isThrownBy(() -> HearingsPartiesMapping.getIndividualInterpreterLanguage(hearingOptions, null, refData, null))
            .withMessageContaining("The language %s cannot be mapped", value);
    }

    @DisplayName("When a valid sign language is given getIndividualInterpreterLanguage should return correct hmcReference")
    @ParameterizedTest
    @CsvSource({"American Sign Language (ASL),americanSignLanguage",
        "Hands on signing,handsOnSigning",
        "Deaf Relay,deafRelay",
        "Palantypist / Speech to text,palantypist"})
    void testGetIndividualInterpreterSignLanguage(String signLang, String expected) throws InvalidMappingException {

        given(signLanguages.getSignLanguage(signLang))
            .willReturn(new Language(expected,"Test",null,null,null,List.of(signLang)));

        given(refData.getSignLanguages()).willReturn(signLanguages);

        HearingOptions hearingOptions = HearingOptions.builder()
            .arrangements(List.of("signLanguageInterpreter"))
            .signLanguageType(signLang)
            .build();

        String result = HearingsPartiesMapping.getIndividualInterpreterLanguage(hearingOptions, null, refData, null);
        assertThat(result).isEqualTo(expected);
    }

    @DisplayName("When a invalid sign language is given getIndividualInterpreterLanguage should throw the correct error and message")
    @ParameterizedTest
    @ValueSource(strings = {"Test"})
    @NullAndEmptySource
    void testGetIndividualInterpreterSignLanguage(String value) {
        given(signLanguages.getSignLanguage(value))
            .willReturn(null);

        given(refData.getSignLanguages()).willReturn(signLanguages);

        HearingOptions hearingOptions = HearingOptions.builder()
            .arrangements(List.of("signLanguageInterpreter"))
            .signLanguageType(value)
            .build();

        assertThatExceptionOfType(InvalidMappingException.class)
            .isThrownBy(() -> HearingsPartiesMapping.getIndividualInterpreterLanguage(hearingOptions, null, refData, null))
            .withMessageContaining("The language %s cannot be mapped", value);
    }

    @DisplayName("When override is Interpreter Wanted is Yes and a override language passed in getIndividualInterpreterLanguage should return the override reference")
    @Test
    void testGetIndividualInterpreterLanguageOverride() throws InvalidMappingException {
        HearingOptions hearingOptions = HearingOptions.builder()
            .languageInterpreter("Yes")
            .languages("Acholi")
            .build();

        OverrideFields overrideFields = OverrideFields.builder()
            .appellantInterpreter(HearingInterpreter.builder()
                .isInterpreterWanted(YES)
                .interpreterLanguage(new DynamicList(new DynamicListItem("test", "Test Language"),List.of()))
                .build())
            .build();
        String result = HearingsPartiesMapping.getIndividualInterpreterLanguage(hearingOptions, overrideFields, refData, null);
        assertThat(result).isEqualTo("test");
    }

    @DisplayName("When override is Interpreter Wanted is No getIndividualInterpreterLanguage should return null")
    @Test
    void testGetIndividualInterpreterLanguageOverrideNo() throws InvalidMappingException {

        HearingOptions hearingOptions = HearingOptions.builder()
            .languageInterpreter("Yes")
            .languages("Acholi")
            .build();

        OverrideFields overrideFields = OverrideFields.builder()
            .appellantInterpreter(HearingInterpreter.builder()
                .isInterpreterWanted(NO)
                .interpreterLanguage(new DynamicList(new DynamicListItem("test", "Test Language"),List.of()))
                .build())
            .build();
        String result = HearingsPartiesMapping.getIndividualInterpreterLanguage(hearingOptions, overrideFields, refData, null);
        assertThat(result).isNull();
    }

    @DisplayName("When override is Interpreter Wanted is null getIndividualInterpreterLanguage should return the default value")
    @Test
    void testGetIndividualInterpreterLanguageOverrideNullIsInterpreterWanted() throws InvalidMappingException {
        given(verbalLanguages.getVerbalLanguage("Acholi"))
            .willReturn(new Language("ach","Test",null,null,null,List.of()));

        given(refData.getVerbalLanguages()).willReturn(verbalLanguages);

        HearingOptions hearingOptions = HearingOptions.builder()
            .languageInterpreter("Yes")
            .languages("Acholi")
            .build();

        OverrideFields overrideFields = OverrideFields.builder()
            .appellantInterpreter(HearingInterpreter.builder()
                .isInterpreterWanted(null)
                .interpreterLanguage(new DynamicList(new DynamicListItem("test", "Test Language"),List.of()))
                .build())
            .build();
        String result = HearingsPartiesMapping.getIndividualInterpreterLanguage(hearingOptions, overrideFields, refData, null);
        assertThat(result).isEqualTo("ach");
    }

    @DisplayName("When override Appellant Interpreter is null getIndividualInterpreterLanguage should return the default value")
    @Test
    void testGetIndividualInterpreterLanguageOverrideNullAppellantInterpreter() throws InvalidMappingException {
        given(verbalLanguages.getVerbalLanguage("Acholi"))
            .willReturn(new Language("ach","Test",null,null,null,List.of()));

        given(refData.getVerbalLanguages()).willReturn(verbalLanguages);

        HearingOptions hearingOptions = HearingOptions.builder()
            .languageInterpreter("Yes")
            .languages("Acholi")
            .build();

        OverrideFields overrideFields = OverrideFields.builder()
            .appellantInterpreter(null)
            .build();
        String result = HearingsPartiesMapping.getIndividualInterpreterLanguage(hearingOptions, overrideFields, refData, null);
        assertThat(result).isEqualTo("ach");
    }

    @DisplayName("When is Interpreter Wanted is Yes getIndividualInterpreterLanguage should return the correct reference")
    @Test
    void testGetOverrideInterpreterLanguage() {
        OverrideFields overrideFields = OverrideFields.builder()
            .appellantInterpreter(HearingInterpreter.builder()
                .isInterpreterWanted(YES)
                .interpreterLanguage(new DynamicList(new DynamicListItem("test", "Test Language"),List.of()))
                .build())
            .build();
        String result = HearingsPartiesMapping.getOverrideInterpreterLanguage(overrideFields);
        assertThat(result).isEqualTo("test");
    }

    @DisplayName("When is Interpreter Wanted is No getIndividualInterpreterLanguage should return null")
    @Test
    void testGetOverrideInterpreterLanguageNo() {
        OverrideFields overrideFields = OverrideFields.builder()
            .appellantInterpreter(HearingInterpreter.builder()
                .isInterpreterWanted(NO)
                .interpreterLanguage(new DynamicList(new DynamicListItem("test", "Test Language"),List.of()))
                .build())
            .build();
        String result = HearingsPartiesMapping.getOverrideInterpreterLanguage(overrideFields);
        assertThat(result).isNull();
    }

    @DisplayName("When is Interpreter Wanted is Yes and interpreter Language value is null, getIndividualInterpreterLanguage should return null")
    @Test
    void testGetOverrideInterpreterLanguageNullInterpreterLanguage() {
        OverrideFields overrideFields = OverrideFields.builder()
            .appellantInterpreter(HearingInterpreter.builder()
                .isInterpreterWanted(YES)
                .interpreterLanguage(new DynamicList(null,List.of()))
                .build())
            .build();
        String result = HearingsPartiesMapping.getOverrideInterpreterLanguage(overrideFields);
        assertThat(result).isNull();
    }

    @DisplayName("When hearing type paper then return LOV not attending")
    @Test
    void getIndividualPreferredHearingChannelPaperTest() {
        HearingSubtype hearingSubtype = HearingSubtype.builder().build();
        HearingOptions hearingOptions = HearingOptions.builder().build();

        HearingChannel result = getIndividualPreferredHearingChannel(hearingSubtype, hearingOptions, null);

        assertThat(result).isNotNull();
        assertThat(result.getHmcReference()).isEqualTo(NOT_ATTENDING.getHmcReference());
    }

    @DisplayName("When hearing Subtype and Hearing Options is null return null")
    @Test
    void whenHearingSubtypeAndHearingOptionsIsNull_returnNull() {
        HearingChannel result = getIndividualPreferredHearingChannel(null, null, null);

        assertThat(result).isNull();
    }

    @DisplayName("When hearing type oral and video then return LOV VIDEO")
    @Test
    void getIndividualPreferredHearingChannelOralVideoTest() {
        HearingSubtype hearingSubtype = HearingSubtype.builder().wantsHearingTypeVideo("Yes").hearingVideoEmail(
            "test@email.com").build();
        HearingOptions hearingOptions = HearingOptions.builder().wantsToAttend("yes").build();
        HearingChannel result = getIndividualPreferredHearingChannel(hearingSubtype, hearingOptions, null);
        assertThat(result.getHmcReference()).isEqualTo(HearingChannel.VIDEO.getHmcReference());
    }

    @DisplayName("When hearing type oral and telephone then return LOV TELEPHONE")
    @Test
    void getIndividualPreferredHearingChannelOralTelephoneTest() {
        HearingSubtype hearingSubtype = HearingSubtype.builder().wantsHearingTypeTelephone("Yes").hearingTelephoneNumber(
            "01111234567").build();
        HearingOptions hearingOptions = HearingOptions.builder().wantsToAttend("yes").build();
        HearingChannel result = getIndividualPreferredHearingChannel(hearingSubtype, hearingOptions, null);
        assertThat(result.getHmcReference()).isEqualTo(HearingChannel.TELEPHONE.getHmcReference());
    }

    @DisplayName("When wantsToAttend is yes, and wantsHearingType telephone but hearingTelephoneNumber is not set then return LOV FACE TO FACE")
    @Test
    void getIndividualPreferredHearingChannelNullWhenMissingPartialRequirementsTelephoneExample() {
        HearingSubtype hearingSubtype = HearingSubtype.builder().wantsHearingTypeTelephone("Yes").build();
        HearingOptions hearingOptions = HearingOptions.builder().wantsToAttend("yes").build();
        HearingChannel result = getIndividualPreferredHearingChannel(hearingSubtype, hearingOptions, null);
        assertThat(result).isEqualTo(FACE_TO_FACE);
    }

    @DisplayName("When hearing type oral and face to face then return LOV FACE TO FACE")
    @Test
    void getIndividualPreferredHearingChannelOralFaceToFaceTest() {
        HearingSubtype hearingSubtype = HearingSubtype.builder().wantsHearingTypeFaceToFace("Yes").build();
        HearingOptions hearingOptions = HearingOptions.builder().wantsToAttend("yes").build();
        HearingChannel result = getIndividualPreferredHearingChannel(hearingSubtype, hearingOptions, null);
        assertThat(result).isEqualTo(FACE_TO_FACE);
    }

    @DisplayName("When hearing type is blank and face to face then return LOV not attending")
    @Test
    void getIndividualPreferredHearingChannelBlankFaceToFaceTest() {
        HearingSubtype hearingSubtype = HearingSubtype.builder().wantsHearingTypeFaceToFace("Yes").build();
        HearingOptions hearingOptions = HearingOptions.builder().wantsToAttend("yes").build();
        HearingChannel result = getIndividualPreferredHearingChannel(hearingSubtype, hearingOptions, null);
        assertThat(result).isEqualTo(FACE_TO_FACE);
    }

    @DisplayName("When wantsToAttend is yes, and wantsHearingType video but hearingVideoEmail is not set then return LOV FACE TO FACE")
    @Test
    void getIndividualPreferredHearingChannelNullWhenMissingPartialRequirementsVideoExample() {
        HearingSubtype hearingSubtype = HearingSubtype.builder().wantsHearingTypeVideo("Yes").build();
        HearingOptions hearingOptions = HearingOptions.builder().wantsToAttend("yes").build();
        HearingChannel result = getIndividualPreferredHearingChannel(hearingSubtype, hearingOptions, null);
        assertThat(result).isEqualTo(FACE_TO_FACE);
    }

    @DisplayName("isIndividualVulnerableFlag Test")
    @Test
    void isIndividualVulnerableFlag() {
        // TODO Finish Test when method done
        boolean result = HearingsPartiesMapping.isIndividualVulnerableFlag();

        assertFalse(result);
    }

    @DisplayName("getIndividualVulnerabilityDetails Test")
    @Test
    void getIndividualVulnerabilityDetails() {
        // TODO Finish Test when method done
        String result = HearingsPartiesMapping.getIndividualVulnerabilityDetails();

        assertNull(result);
    }

    @DisplayName("When a hearingVideoEmail has a email, getIndividualHearingChannelEmail "
        + "returns a list with only that email ")
    @Test
    void testGetIndividualHearingChannelEmail() {

        HearingSubtype subtype = HearingSubtype.builder()
            .hearingVideoEmail(EMAIL_ADDRESS)
            .build();

        List<String> result = HearingsPartiesMapping.getIndividualHearingChannelEmail(subtype);

        assertThat(result)
            .hasSize(1)
            .containsOnly(EMAIL_ADDRESS);
    }

    @DisplayName("When a hearingVideoEmail is empty or blank, getIndividualHearingChannelEmail "
        + "returns an empty list")
    @ParameterizedTest
    @NullAndEmptySource
    void testGetIndividualHearingChannelEmail(String value) {

        HearingSubtype subtype = HearingSubtype.builder()
            .hearingVideoEmail(value)
            .build();

        List<String> result = HearingsPartiesMapping.getIndividualHearingChannelEmail(subtype);

        assertThat(result)
            .isEmpty();
    }

    @DisplayName("When a HearingSubtype is null, getIndividualHearingChannelEmail "
        + "returns an empty list")
    @Test
    void testGetIndividualHearingChannelEmailNull() {
        List<String> result = HearingsPartiesMapping.getIndividualHearingChannelEmail(null);

        assertThat(result)
            .isEmpty();
    }

    @DisplayName("When a hearingTelephoneNumber is empty or blank, getIndividualHearingChannelPhone "
        + "returns an empty list")
    @Test
    void testGetIndividualHearingChannelPhone() {
        HearingSubtype subtype = HearingSubtype.builder()
            .hearingTelephoneNumber(TELEPHONE_NUMBER)
            .build();

        List<String> result = HearingsPartiesMapping.getIndividualHearingChannelPhone(subtype);

        assertThat(result)
            .hasSize(1)
            .containsOnly(TELEPHONE_NUMBER);
    }

    @DisplayName("When a hearingTelephoneNumber is empty or blank, getIndividualHearingChannelPhone "
        + "returns an empty list")
    @ParameterizedTest
    @NullAndEmptySource
    void testGetIndividualHearingChannelPhone(String value) {
        HearingSubtype subtype = HearingSubtype.builder()
            .hearingTelephoneNumber(value)
            .build();

        List<String> result = HearingsPartiesMapping.getIndividualHearingChannelPhone(subtype);

        assertThat(result)
            .isEmpty();
    }

    @DisplayName("When a HearingSubtype is null, getIndividualHearingChannelPhone "
        + "returns an empty list")
    @Test
    void testGetIndividualHearingChannelPhoneNull() {
        List<String> result = HearingsPartiesMapping.getIndividualHearingChannelPhone(null);

        assertThat(result)
            .isEmpty();
    }

    @DisplayName("getIndividualRelatedParties Test")
    @Test
    void getIndividualRelatedParties() {
        Entity entity = Representative.builder().build();

        List<uk.gov.hmcts.reform.sscs.model.single.hearing.RelatedParty> result = HearingsPartiesMapping.getIndividualRelatedParties(
            entity, PARTY_ID
        );

        assertThat(result)
            .isNotEmpty()
            .extracting("relatedPartyId", "relationshipType")
            .contains(tuple(PARTY_ID.substring(0,15), SOLICITOR.getRelationshipTypeCode()));
    }

    @DisplayName("getIndividualRelatedParties when given a short id will return it correctly")
    @Test
    void getIndividualRelatedPartiesShortId() {
        Entity entity = Representative.builder().build();

        List<uk.gov.hmcts.reform.sscs.model.single.hearing.RelatedParty> result = HearingsPartiesMapping.getIndividualRelatedParties(
            entity, "1"
        );

        assertThat(result)
            .isNotEmpty()
            .extracting("relatedPartyId", "relationshipType")
            .contains(tuple("1", SOLICITOR.getRelationshipTypeCode()));
    }

    @DisplayName("When relationship type is INTERPRETER, "
        + "related party is returned with correct id and relationship type code ")
    @Test
    void getIndividualRelatedParties_shouldReturnRelatedPartyForInterpreter() {
        Entity entity = Interpreter.builder().build();

        List<uk.gov.hmcts.reform.sscs.model.single.hearing.RelatedParty> result = HearingsPartiesMapping.getIndividualRelatedParties(
            entity,
            PARTY_ID
        );

        assertThat(result)
            .isNotEmpty()
            .extracting("relatedPartyId", "relationshipType")
            .contains(tuple("a2b837d5-ee28-4", INTERPRETER.getRelationshipTypeCode()));
    }

    @DisplayName("getPartyOrganisationDetails Test")
    @Test
    void getPartyOrganisationDetails() {
        OrganisationDetails result = HearingsPartiesMapping.getPartyOrganisationDetails();

        assertNull(result);
    }

    @DisplayName("getPartyUnavailabilityDayOfWeek Test")
    @Test
    void getPartyUnavailabilityDayOfWeek() {
        List<UnavailabilityDayOfWeek> result = HearingsPartiesMapping.getPartyUnavailabilityDayOfWeek();

        assertThat(result).isEmpty();
    }

    @DisplayName("When Valid DateRanges are given getPartyUnavailabilityRange returns the correct list of Unavailability Ranges")
    @Test
    void getPartyUnavailabilityRange() throws ListingException {
        List<ExcludeDate> excludeDates = new ArrayList<>();
        excludeDates.add(ExcludeDate.builder().value(DateRange.builder()
                                                         .start("2022-02-01")
                                                         .end("2022-03-31")
                                                         .build())
                             .build());
        excludeDates.add(ExcludeDate.builder().value(DateRange.builder()
                                                         .start("2022-06-01")
                                                         .end("2022-06-02")
                                                         .build())
                             .build());
        HearingOptions hearingOptions = HearingOptions.builder().excludeDates(excludeDates).build();
        List<UnavailabilityRange> result = HearingsPartiesMapping.getPartyUnavailabilityRange(hearingOptions);

        assertThat(result)
            .extracting("unavailableFromDate", "unavailableToDate", "unavailabilityType")
            .contains(
                tuple(
                    LocalDate.of(2022, 2, 1),
                    LocalDate.of(2022, 3, 31),
                    DayOfWeekUnavailabilityType.ALL_DAY.getLabel()
                ),
                tuple(
                    LocalDate.of(2022, 6, 1),
                    LocalDate.of(2022, 6, 2),
                    DayOfWeekUnavailabilityType.ALL_DAY.getLabel()
                )
            );
    }

    @DisplayName("When null ExcludeDates is given getPartyUnavailabilityRange returns an empty list")
    @Test
    void getPartyUnavailabilityRangeNullValue() throws ListingException {
        HearingOptions hearingOptions = HearingOptions.builder().build();
        List<UnavailabilityRange> result = HearingsPartiesMapping.getPartyUnavailabilityRange(hearingOptions);

        assertThat(result).isEmpty();
    }

    @DisplayName("Unavailability ranges must be set from the excluded dates. Each range must have an UnavailabilityType.")
    @Test
    void buildHearingPartiesDetails_unavailabilityRanges() throws ListingException, InvalidMappingException {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(1);
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                        .hearingOptions(HearingOptions.builder()
                                            .excludeDates(List.of(ExcludeDate.builder()
                                                                      .value(DateRange.builder().start(start.toString())
                                                                                 .end(end.toString()).build()).build()))
                                            .wantsToAttend("yes").build())
                        .hearingType("test")
                        .hearingSubtype(HearingSubtype.builder().hearingVideoEmail("email@email.com").wantsHearingTypeFaceToFace("yes").build())
                        .appellant(Appellant.builder()
                                       .id(PARTY_ID)
                                       .name(Name.builder()
                                                 .title("title")
                                                 .firstName("first")
                                                 .lastName("last")
                                                 .build())
                                       .build())
                        .build())
            .benefitCode("002")
            .build();
        HearingWrapper wrapper = HearingWrapper.builder()
            .caseData(caseData)
            .caseData(caseData)
            .build();

        List<PartyDetails> partiesDetails = HearingsPartiesMapping.buildHearingPartiesDetails(wrapper, refData);

        PartyDetails partyDetails = partiesDetails.stream().filter(o -> PARTY_ID.substring(0,15).equalsIgnoreCase(o.getPartyID())).findFirst().orElse(
            null);
        assertThat(partyDetails).isNotNull();
        assertThat(partyDetails.getUnavailabilityRanges()).hasSize(1);

        UnavailabilityRange unavailabilityRange = partyDetails.getUnavailabilityRanges().get(0);

        assertThat(unavailabilityRange.getUnavailabilityType()).isEqualTo(DayOfWeekUnavailabilityType.ALL_DAY.getLabel());
        assertThat(unavailabilityRange.getUnavailableFromDate()).isEqualTo(start);
        assertThat(unavailabilityRange.getUnavailableToDate()).isEqualTo(end);
    }

    @DisplayName("When the start and end date ranges are both null, then return null")
    @Test
    void getPartyUnavailabilityRangeWhenBothDatesAreNull() throws ListingException {
        List<ExcludeDate> excludeDates = new ArrayList<>();
        excludeDates.add(ExcludeDate.builder().value(DateRange.builder()
                                                         .build())
                             .build());
        HearingOptions hearingOptions = HearingOptions.builder().excludeDates(excludeDates).build();
        List<UnavailabilityRange> result = HearingsPartiesMapping.getPartyUnavailabilityRange(hearingOptions);

        assertThat(result).isEmpty();
    }

    @DisplayName("When the start date is null and end date is valid, then return the unavailability range")
    @Test
    void getPartyUnavailabilityRangeWhenStartDateIsNull() throws ListingException {
        List<ExcludeDate> excludeDates = new ArrayList<>();
        excludeDates.add(ExcludeDate.builder().value(DateRange.builder()
                                                         .end(LocalDate.now().toString())
                                                         .build())
                             .build());
        HearingOptions hearingOptions = HearingOptions.builder().excludeDates(excludeDates).build();
        List<UnavailabilityRange> result = HearingsPartiesMapping.getPartyUnavailabilityRange(hearingOptions);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(UnavailabilityRange.builder()
                                                .unavailableToDate(LocalDate.now())
                                                .unavailableFromDate(LocalDate.now())
                                                .unavailabilityType(ALL_DAY.getLabel()).build());
    }

    @DisplayName("When the start date is in a wrong format and the end date is correct, then return the unavailability range")
    @Test
    void getPartyUnavailabilityRangeWhenOneDateIsInvalidAndOtherIsCorrect() throws ListingException {
        List<ExcludeDate> excludeDates = new ArrayList<>();
        excludeDates.add(ExcludeDate.builder().value(DateRange.builder()
                                                         .start("234234")
                                                         .end(LocalDate.now().toString())
                                                         .build())
                             .build());
        HearingOptions hearingOptions = HearingOptions.builder().excludeDates(excludeDates).build();
        List<UnavailabilityRange> result = HearingsPartiesMapping.getPartyUnavailabilityRange(hearingOptions);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(UnavailabilityRange.builder()
                                                .unavailableToDate(LocalDate.now())
                                                .unavailableFromDate(LocalDate.now())
                                                .unavailabilityType(ALL_DAY.getLabel()).build());
    }

    @DisplayName("Unavailability ranges must be set from the excluded dates. Each range must have an UnavailabilityType.")
    @ParameterizedTest
    @ValueSource(strings = {"015", "016", "030", "034", "050", "053", "054", "055", "057", "058"})
    void buildDwpOrgDetailsForHmrc(String benefitCode) {
        SscsCaseData caseData = SscsCaseData.builder().benefitCode(benefitCode).build();
        OrganisationDetails orgDetails = HearingsPartiesMapping.getDwpOrganisationDetails(caseData);
        assertThat(orgDetails.getOrganisationType()).isEqualTo("ORG");
        assertThat(orgDetails.getName()).isEqualTo("HMRC");
    }

    @DisplayName("Unavailability ranges must be set from the excluded dates. Each range must have an UnavailabilityType.")
    @ParameterizedTest
    @ValueSource(strings = {"001", "002", "003", "011", "012", "067", "073", "079"})
    void buildDwpOrgDetailsForDwp(String benefitCode) {
        SscsCaseData caseData = SscsCaseData.builder().benefitCode(benefitCode).build();
        OrganisationDetails orgDetails = HearingsPartiesMapping.getDwpOrganisationDetails(caseData);
        assertThat(orgDetails.getOrganisationType()).isEqualTo("ORG");
        assertThat(orgDetails.getName()).isEqualTo("DWP");
    }

    private static Stream<Arguments> getPartyReferenceArguments() {
        return Stream.of(
            Arguments.of(Representative.builder().build(), REPRESENTATIVE.getHmcReference()),
            Arguments.of(Appellant.builder().build(), APPELLANT.getHmcReference()),
            Arguments.of(Appointee.builder().build(), APPOINTEE.getHmcReference()),
            Arguments.of(OtherParty.builder().build(), OTHER_PARTY.getHmcReference())
        );
    }
}
