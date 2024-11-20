package uk.gov.hmcts.reform.sscs.helper.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsWindowMapping.DAYS_TO_ADD_HEARING_WINDOW_TODAY;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.CaseCategoryType.CASE_SUBTYPE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.CaseCategoryType.CASE_TYPE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HearingType.SUBSTANTIVE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.PartyType.INDIVIDUAL;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.FACE_TO_FACE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.CaseFlags;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyDetails;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlags;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.ServiceHearingValues;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingWindow;
import uk.gov.hmcts.reform.sscs.model.single.hearing.RelatedParty;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingPriority;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.model.SessionCategoryMap;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;
import uk.gov.hmcts.reform.sscs.service.VenueService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@ExtendWith(MockitoExtension.class)
class ServiceHearingValuesMappingTest extends HearingsMappingBase {

    private static final String NOTE_FROM_OTHER_PARTY = "other party note";
    private static final String NOTE_FROM_APPELLANT = "appellant note";

    public static final String BENEFIT = "Benefit";

    public static final String APPELLANT_PARTY_ID = "a2b837d5-ee28-4bc9-a3d8-ce2d2de9fb296292997e-14d4-4814-a163-e64018d2c441";
    public static final String REPRESENTATIVE_PARTY_ID = "a2b837d5-ee28-4bc9-a3d8-ce2d2de9fb29";
    public static final String OTHER_PARTY_ID = "4dd6b6fa-6562-4699-8e8b-6c70cf8a333e";

    @Mock
    public VerbalLanguagesService verbalLanguages;

    @Mock
    public SignLanguagesService signLanguages;

    @Mock
    private ReferenceDataServiceHolder refData;

    @Mock
    private SessionCategoryMapService sessionCategoryMaps;

    @Mock
    private VenueService venueService;
    private SscsCaseData caseData;

    @BeforeEach
    public void setUp() {
        caseData = SscsCaseData.builder()
            .ccdCaseId("1234")
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .urgentCase("Yes")
            .adjournment(Adjournment.builder()
                .adjournmentInProgress(YesNo.NO)
                .canCaseBeListedRightAway(YesNo.YES)
                .build())
            .caseManagementLocation(CaseManagementLocation.builder()
                .baseLocation("LIVERPOOL SOCIAL SECURITY AND CHILD SUPPORT TRIBUNAL")
                .region("North West")
                .build())
            .appeal(Appeal.builder()
                .hearingType("final")
                .appellant(Appellant.builder()
                    .id(APPELLANT_PARTY_ID)
                    .name(Name.builder()
                        .firstName("Fred")
                        .lastName("Flintstone")
                        .title("Mr")
                        .build())
                    .build())
                .hearingSubtype(HearingSubtype.builder()
                    .hearingTelephoneNumber("0999733733")
                    .hearingVideoEmail("test@gmail.com")
                    .wantsHearingTypeFaceToFace("Yes")
                    .wantsHearingTypeTelephone("No")
                    .wantsHearingTypeVideo("No")
                    .build())
                .hearingOptions(HearingOptions.builder()
                    .wantsToAttend("Yes")
                    .wantsSupport("Yes")
                    .languageInterpreter("Yes")
                    .languages("Bulgarian")
                    .signLanguageType("Makaton")
                    .arrangements(Arrays.asList(
                        "signLanguageInterpreter",
                        "hearingLoop",
                        "disabledAccess"
                    ))
                    .scheduleHearing("No")
                    .excludeDates(getExcludeDates())
                    .agreeLessNotice("No")
                    .other(NOTE_FROM_APPELLANT)
                    .build())
                .rep(Representative.builder()
                    .id(REPRESENTATIVE_PARTY_ID)
                    .hasRepresentative("Yes")
                    .name(Name.builder()
                        .title("Mr")
                        .firstName("Harry")
                        .lastName("Potter")
                        .build())
                    .address(Address.builder()
                        .line1("123 Hairy Lane")
                        .line2("Off Hairy Park")
                        .town("Town")
                        .county("County")
                        .postcode("CM14 4LQ")
                        .build())
                    .contact(Contact.builder()
                        .email("harry.potter@wizards.com")
                        .mobile("07411999999")
                        .phone(null)
                        .build())
                    .build())
                .build())
            .events(getEventsOfCaseData())
            .languagePreferenceWelsh("No")
            .otherParties(getOtherParties())
            .linkedCasesBoolean("No")
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .overrideFields(OverrideFields.builder()
                    .duration(30).build()).build())
            .sscsIndustrialInjuriesData(SscsIndustrialInjuriesData.builder()
                .panelDoctorSpecialism("cardiologist")
                .secondPanelDoctorSpecialism("eyeSurgeon")
                .build())
            .build();

        SessionCategoryMap sessionCategoryMap = new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                false, false, SessionCategory.CATEGORY_06, null);

        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE, ISSUE_CODE,true,false))
                .willReturn(sessionCategoryMap);
        given(sessionCategoryMaps.getCategoryTypeValue(sessionCategoryMap))
                .willReturn("BBA3-002");
        given(sessionCategoryMaps.getCategorySubTypeValue(sessionCategoryMap))
                .willReturn("BBA3-002-DD");

        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);

        given(refData.getVerbalLanguages()).willReturn(verbalLanguages);

        given(refData.getSignLanguages()).willReturn(signLanguages);

        given(refData.getVerbalLanguages().getVerbalLanguage("Bulgarian"))
                .willReturn(new Language("bul","Test", null, null,null,List.of("Bulgarian")));

        given(refData.getSignLanguages().getSignLanguage("Makaton"))
                .willReturn(new Language("sign-mkn","Test",null, null,null,List.of("Makaton")));
    }

    @Test
    void shouldMapServiceHearingValuesSuccessfully() throws ListingException {
        // given
        given(refData.getVenueService()).willReturn(venueService);

        // when
        final ServiceHearingValues serviceHearingValues = ServiceHearingValuesMapping.mapServiceHearingValues(caseData, refData);
        final HearingWindow expectedHearingWindow = HearingWindow.builder()
            .dateRangeStart(LocalDate.now().plusDays(DAYS_TO_ADD_HEARING_WINDOW_TODAY))
            .build();
        //then
        assertFalse(serviceHearingValues.isAutoListFlag());
        assertEquals(60, serviceHearingValues.getDuration());
        assertEquals(SUBSTANTIVE, serviceHearingValues.getHearingType());
        assertEquals(BENEFIT, serviceHearingValues.getCaseType());
        assertThat(serviceHearingValues.getCaseCategories())
            .extracting("categoryType","categoryValue")
            .containsExactlyInAnyOrder(
                tuple(CASE_TYPE,"BBA3-002"),
                tuple(CASE_SUBTYPE,"BBA3-002-DD"));
        assertEquals(expectedHearingWindow, serviceHearingValues.getHearingWindow());
        assertEquals(HearingPriority.URGENT.getHmcReference(), serviceHearingValues.getHearingPriorityType());
        assertEquals(4, serviceHearingValues.getNumberOfPhysicalAttendees());
        assertFalse(serviceHearingValues.isHearingInWelshFlag());
        assertEquals(1, serviceHearingValues.getHearingLocations().size());
        assertTrue(serviceHearingValues.getCaseAdditionalSecurityFlag());
        assertThat(serviceHearingValues.getFacilitiesRequired()).isEmpty();
        assertThat(serviceHearingValues.getListingComments())
            .isEqualToNormalizingNewlines("Appellant - Mr Fred Flintstone:\n" + NOTE_FROM_APPELLANT
                + "\n\n" + "party_role - Mr Barny Boulderstone:\n" + NOTE_FROM_OTHER_PARTY);
        assertNull(serviceHearingValues.getHearingRequester());
        assertFalse(serviceHearingValues.isPrivateHearingRequiredFlag());
        assertNull(serviceHearingValues.getLeadJudgeContractType());
        assertThat(serviceHearingValues.getJudiciary()).isNotNull();
        assertFalse(serviceHearingValues.isHearingIsLinkedFlag());
        assertEquals(getCaseFlags(), serviceHearingValues.getCaseFlags());
        assertNull(serviceHearingValues.getVocabulary());
        assertEquals(List.of(FACE_TO_FACE), serviceHearingValues.getHearingChannels());
        assertEquals(true, serviceHearingValues.isCaseInterpreterRequiredFlag());
    }

    @Test
    void shouldMapPartiesInServiceHearingValues() throws ListingException {
        // given
        caseData.setDwpIsOfficerAttending(YesNo.YES.getValue());
        given(refData.getVenueService()).willReturn(venueService);
        // when
        final ServiceHearingValues serviceHearingValues = ServiceHearingValuesMapping.mapServiceHearingValues(caseData, refData);
        //then
        assertThat(serviceHearingValues.getParties())
            .hasSize(4)
            .anySatisfy(partyDetails -> {
                assertThat(partyDetails.getPartyID()).isEqualTo(APPELLANT_PARTY_ID.substring(0,15));
                assertThat(partyDetails.getPartyRole()).isEqualTo(EntityRoleCode.APPELLANT.getHmcReference());
            })
            .anySatisfy(partyDetails -> {
                assertThat(partyDetails.getPartyID()).isEqualTo(REPRESENTATIVE_PARTY_ID.substring(0,15));
                assertThat(partyDetails.getPartyRole()).isEqualTo(EntityRoleCode.REPRESENTATIVE.getHmcReference());
            })
            .anySatisfy(partyDetails -> {
                assertThat(partyDetails.getPartyID()).isEqualTo(OTHER_PARTY_ID.substring(0,15));
                assertThat(partyDetails.getPartyRole()).isEqualTo(EntityRoleCode.OTHER_PARTY.getHmcReference());
            })
            .anySatisfy(partyDetails -> {
                assertThat(partyDetails.getPartyID()).isEqualTo("DWP");
                assertThat(partyDetails.getPartyRole()).isEqualTo(EntityRoleCode.RESPONDENT.getHmcReference());
            });
    }

    @Test
    void shouldRepresentativeNotHaveOrganisation() throws ListingException {
        // given

        given(refData.getVenueService()).willReturn(venueService);
        // when
        final ServiceHearingValues serviceHearingValues = ServiceHearingValuesMapping.mapServiceHearingValues(caseData, refData);
        //then
        assertThat(serviceHearingValues.getParties())
            .filteredOn(partyDetails -> EntityRoleCode.REPRESENTATIVE.getHmcReference().equals(partyDetails.getPartyRole()))
            .extracting(PartyDetails::getPartyType)
            .containsOnly(INDIVIDUAL);
    }

    @Test
    void shouldNotThrowErrorWhenOtherPartyHearingOptionsNull() throws ListingException {
        given(refData.getVenueService()).willReturn(venueService);
        SscsCaseData editedCaseData = caseData;
        CcdValue<OtherParty> otherParty = new CcdValue<>(
            OtherParty.builder()
                .name(Name.builder().firstName("Test").lastName("Test").build())
                .hearingOptions(null).build());
        editedCaseData.setOtherParties(List.of(otherParty));
        final ServiceHearingValues serviceHearingValues = ServiceHearingValuesMapping.mapServiceHearingValues(caseData, refData);
        assertThat(serviceHearingValues.getParties())
            .filteredOn(partyDetails -> EntityRoleCode.OTHER_PARTY.getHmcReference().equals(partyDetails.getPartyRole()))
            .extracting(PartyDetails::getPartyChannel)
            .containsOnlyNulls();
    }

    private List<Event> getEventsOfCaseData() {
        return new ArrayList<>() {{
                add(Event.builder()
                        .value(EventDetails.builder()
                                   .date("2022-02-12T20:30:00")
                                   .type("responseReceived")
                                   .description("Dwp respond")
                                   .build())
                        .build());
            }
        };
    }


    private List<CcdValue<OtherParty>> getOtherParties() {
        return new ArrayList<>() {
            {
                add(new CcdValue<>(OtherParty.builder()
                                   .id(OTHER_PARTY_ID)
                                   .name(Name.builder()
                                             .firstName("Barny")
                                             .lastName("Boulderstone")
                                             .title("Mr")
                                             .build())
                                   .address(Address.builder().build())
                                   .confidentialityRequired(YesNo.NO)
                                   .unacceptableCustomerBehaviour(YesNo.YES)
                                   .hearingSubtype(HearingSubtype.builder()
                                                       .hearingTelephoneNumber("0999733735")
                                                       .hearingVideoEmail("test2@gmail.com")
                                                       .wantsHearingTypeFaceToFace("Yes")
                                                       .wantsHearingTypeTelephone("No")
                                                       .wantsHearingTypeVideo("No")
                                                       .build())
                                   .hearingOptions(HearingOptions.builder()
                                                       .wantsToAttend("Yes")
                                                       .wantsSupport("Yes")
                                                       .languageInterpreter("Yes")
                                                       .languages("Bulgarian")
                                                       .scheduleHearing("No")
                                                       .excludeDates(getExcludeDates())
                                                       .agreeLessNotice("No")
                                                       .other(NOTE_FROM_OTHER_PARTY)
                                                       .build())
                                   .isAppointee("No")
                                   .appointee(Appointee.builder().build())
                                   .rep(Representative.builder().build())
                                   .otherPartySubscription(Subscription.builder().build())
                                   .otherPartyAppointeeSubscription(Subscription.builder().build())
                                   .otherPartyRepresentativeSubscription(Subscription.builder().build())
                                   .sendNewOtherPartyNotification(YesNo.NO)
                                   .reasonableAdjustment(ReasonableAdjustmentDetails.builder()
                                                             .reasonableAdjustmentRequirements("Some adjustments...")
                                                             .wantsReasonableAdjustment(YesNo.YES)
                                                             .build())
                                   .appointeeReasonableAdjustment(ReasonableAdjustmentDetails.builder().build())
                                   .repReasonableAdjustment(ReasonableAdjustmentDetails.builder().build())
                                   .role(Role.builder()
                                             .name("party_role")
                                             .description("description")
                                             .build())
                                   .build()));
            }
        };
    }

    private List<RelatedParty> getRelatedParties() {
        return new ArrayList<>();
    }


    private List<ExcludeDate> getExcludeDates() {
        return new ArrayList<>() {
            {
                add(ExcludeDate.builder()
                    .value(DateRange.builder()
                               .start("2022-01-12")
                               .end("2022-01-19")
                               .build())
                    .build());
            }
        };
    }

    // TODO it will be populated when the method is provided
    private CaseFlags getCaseFlags() {
        return CaseFlags.builder()
            .flags(getPartyFlags())
            .flagAmendUrl("")
            .build();
    }

    private List<PartyFlags> getPartyFlags() {
        return new ArrayList<>() {{
                add(PartyFlags.builder()
                    .partyName(null)
                    .flagParentId("10")
                    .flagId("44")
                    .flagDescription("Sign Language Interpreter")
                    .flagStatus(null)
                    .build());
                add(PartyFlags.builder()
                    .partyName(null)
                    .flagParentId("6")
                    .flagId("21")
                    .flagDescription("Step free / wheelchair access")
                    .flagStatus(null)
                    .build());
                add(PartyFlags.builder()
                    .partyName(null)
                    .flagParentId("11")
                    .flagId("45")
                    .flagDescription("Hearing loop (hearing enhancement system)")
                    .flagStatus(null)
                    .build());
                add(PartyFlags.builder()
                    .partyName(null)
                    .flagParentId("1")
                    .flagId("67")
                    .flagDescription("Urgent flag")
                    .flagStatus(null)
                    .build());
                add(PartyFlags.builder()
                    .partyName(null)
                    .flagParentId("2")
                    .flagId("70")
                    .flagDescription("Language Interpreter")
                    .flagStatus(null)
                    .build());
            }
        };
    }
}
