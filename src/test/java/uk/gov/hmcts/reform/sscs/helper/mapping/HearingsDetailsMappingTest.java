package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingVenue.SAME_VENUE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingVenue.SOMEWHERE_ELSE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HearingType.SUBSTANTIVE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.LocationType.COURT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.model.HearingLocation;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HearingType;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDetails;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.reference.data.model.SessionCategoryMap;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.VenueService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

class HearingsDetailsMappingTest extends HearingsMappingBase {

    @Mock
    private HearingDurationsService hearingDurations;

    @Mock
    private SessionCategoryMapService sessionCategoryMaps;

    @Mock
    private ReferenceDataServiceHolder refData;

    @Mock
    private VenueService venueService;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    private static final String PROCESSING_VENUE_1 = "The Scarborough Justice Centre";

    private static final String REGIONAL_PROCESSING_CENTRE = "SSCS Leeds";
    private static final String PHONE_NUMBER = "07483871426";

    private static final String VENUE_ID = "10";

    private static final String EPIMS_ID_1 = "744412";
    private static final String EPIMS_ID_2 = "332211";
    private static final String EPIMS_ID_3 = "123123";
    private static final String EPIMS_ID_4 = "321321";
    private static final List<VenueDetails> EPIMS_ID_LIST = Arrays.asList(
        VenueDetails.builder().epimsId(EPIMS_ID_1).build(),
        VenueDetails.builder().epimsId(EPIMS_ID_2).build(),
        VenueDetails.builder().epimsId(EPIMS_ID_3).build(),
        VenueDetails.builder().epimsId(EPIMS_ID_4).build());

    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        OverrideFields defaultListingValues = OverrideFields.builder()
            .duration(60)
            .build();
        SchedulingAndListingFields slFields = SchedulingAndListingFields.builder()
            .defaultListingValues(defaultListingValues)
            .build();
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                        .hearingOptions(HearingOptions.builder().wantsToAttend("yes").build())
                        .hearingSubtype(HearingSubtype.builder()
                                            .wantsHearingTypeTelephone("yes")
                                            .hearingTelephoneNumber(PHONE_NUMBER).build()).build())
            .processingVenue(PROCESSING_VENUE_1)
            .schedulingAndListingFields(slFields)
            .build();
    }

    @DisplayName("When a valid hearing wrapper is given buildHearingDetails returns the correct Hearing Details")
    @Test
    void buildHearingDetails() throws ListingException {
        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE, ISSUE_CODE, false, false))
            .willReturn(new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                                               false, false, SessionCategory.CATEGORY_03, null));

        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);
        given(refData.getVenueService()).willReturn(venueService);

        OverrideFields defaultListingValues = OverrideFields.builder()
            .duration(60)
            .build();
        SchedulingAndListingFields slFields = SchedulingAndListingFields.builder()
            .defaultListingValues(defaultListingValues)
            .build();

        // TODO Finish Test when method done
        caseData = SscsCaseData.builder()
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .appeal(Appeal.builder()
                        .hearingSubtype(HearingSubtype.builder()
                                            .wantsHearingTypeFaceToFace("Yes")
                                            .build())
                        .hearingOptions(HearingOptions.builder()
                                            .wantsToAttend("Yes")
                                            .build())
                        .build())
            .caseManagementLocation(CaseManagementLocation.builder()
                                        .baseLocation(EPIMS_ID)
                                        .region(REGION)
                                        .build())
            .dwpIsOfficerAttending("Yes")
            .schedulingAndListingFields(slFields)
            .build();

        HearingWrapper wrapper = HearingWrapper.builder()
            .caseData(caseData)
            .build();

        HearingDetails hearingDetails = HearingsDetailsMapping.buildHearingDetails(wrapper, refData);

        assertNotNull(hearingDetails.getHearingType());
        assertNotNull(hearingDetails.getHearingWindow());
        assertNotNull(hearingDetails.getDuration());
        assertNotNull(hearingDetails.getHearingPriorityType());
        assertEquals(2, hearingDetails.getNumberOfPhysicalAttendees());
        assertNotNull(hearingDetails.getHearingLocations());
        assertNull(hearingDetails.getListingComments());
        assertNull(hearingDetails.getHearingRequester());
        assertNull(hearingDetails.getLeadJudgeContractType());
        assertNotNull(hearingDetails.getPanelRequirements());
    }

    @DisplayName("shouldBeHearingsInWelshFlag Test")
    @Test
    void shouldBeHearingsInWelshFlag() {
        boolean result = HearingsDetailsMapping.shouldBeHearingsInWelshFlag();

        assertFalse(result);
    }

    @DisplayName("When case has a linked case isCaseLinked returns true")
    @Test
    void testIsCaseLinked() {
        caseData = SscsCaseData.builder()
            .linkedCase(List.of(CaseLink.builder()
                                    .value(CaseLinkDetails.builder()
                                               .caseReference("123456")
                                               .build())
                                    .build()))
            .build();
        boolean result = HearingsDetailsMapping.isCaseLinked(caseData);

        assertThat(result).isTrue();
    }

    @DisplayName("When case has multiple linked cases isCaseLinked returns true")
    @Test
    void testIsCaseLinkedMultiple() {
        caseData = SscsCaseData.builder()
            .linkedCase(List.of(
                CaseLink.builder()
                    .value(CaseLinkDetails.builder()
                               .caseReference("123456")
                               .build())
                    .build(),
                CaseLink.builder()
                    .value(CaseLinkDetails.builder()
                               .caseReference("654321")
                               .build())
                    .build()))
            .build();
        boolean result = HearingsDetailsMapping.isCaseLinked(caseData);

        assertThat(result).isTrue();
    }

    @DisplayName("When case has empty linkedCase isCaseLinked returns true")
    @Test
    void testIsCaseLinkedEmpty() {
        caseData = SscsCaseData.builder()
            .linkedCase(Collections.emptyList())
            .build();
        boolean result = HearingsDetailsMapping.isCaseLinked(caseData);

        assertThat(result).isFalse();
    }

    @DisplayName("When case has null linkedCase isCaseLinked returns true")
    @Test
    void testIsCaseLinkedNull() {
        caseData = SscsCaseData.builder()
            .build();
        boolean result = HearingsDetailsMapping.isCaseLinked(caseData);

        assertThat(result).isFalse();
    }

    @DisplayName("Hearing type should be substantive.")
    @Test
    void getHearingType() {
        HearingType result = HearingsDetailsMapping.getHearingType();

        assertThat(result).isEqualTo(SUBSTANTIVE);
    }

    @DisplayName("When urgentCase is yes, isCaseUrgent return True")
    @Test
    void testIsCaseUrgent() {
        caseData = SscsCaseData.builder()
            .urgentCase("Yes")
            .build();
        boolean result = HearingsDetailsMapping.isCaseUrgent(caseData);

        assertThat(result).isTrue();
    }

    @DisplayName("When urgentCase is No or blank, isCaseUrgent return False")
    @ParameterizedTest
    @ValueSource(strings = {"No"})
    @NullAndEmptySource
    void testIsCaseUrgent(String value) {
        caseData = SscsCaseData.builder()
            .urgentCase(value)
            .build();
        boolean result = HearingsDetailsMapping.isCaseUrgent(caseData);

        assertThat(result).isFalse();
    }


    @Test
    void getHearingLocations_shouldReturnCorrespondingEpimsIdForVenue() throws ListingException {
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                        .hearingSubtype(HearingSubtype.builder()
                                            .wantsHearingTypeFaceToFace("Yes")
                                            .build())
                        .hearingOptions(HearingOptions.builder()
                                            .wantsToAttend("Yes")
                                            .build())
                        .build())
            .processingVenue(PROCESSING_VENUE_1)
            .dwpIsOfficerAttending("Yes")
            .build();

        given(venueService.getEpimsIdForVenue(caseData.getProcessingVenue())).willReturn(EPIMS_ID_1);
        given(refData.getVenueService()).willReturn(venueService);

        checkHearingLocationResults(HearingsLocationMapping.getHearingLocations(caseData, refData),
                                    EPIMS_ID_1);
    }

    @DisplayName("Multiple hearing location Test")
    @Test
    void getMultipleHearingLocations_shouldReturnCorrespondingMultipleEpimsIdForVenue() throws ListingException {
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                        .hearingSubtype(HearingSubtype.builder()
                                            .wantsHearingTypeFaceToFace("Yes")
                                            .build())
                        .hearingOptions(HearingOptions.builder()
                                            .wantsToAttend("Yes")
                                            .build())
                        .build())
            .processingVenue(PROCESSING_VENUE_1)
            .dwpIsOfficerAttending("Yes")
            .build();
        Map<String, List<String>> multipleHearingLocations = new HashMap<>();
        multipleHearingLocations.put("Chester",new ArrayList<>(Arrays.asList("226511", "443014")));
        multipleHearingLocations.put("Manchester",new ArrayList<>(Arrays.asList("512401","701411")));
        multipleHearingLocations.put("Plymouth",new ArrayList<>(Arrays.asList("764728","235590")));
        given(venueService.getEpimsIdForVenue(caseData.getProcessingVenue())).willReturn("443014");
        given(refData.getVenueService()).willReturn(venueService);
        given(refData.getMultipleHearingLocations()).willReturn(multipleHearingLocations);
        List<HearingLocation> result = HearingsLocationMapping.getHearingLocations(
            caseData, refData);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLocationId()).isEqualTo("226511");
        assertThat(result.get(1).getLocationId()).isEqualTo("443014");

    }

    @DisplayName("When hearing is paper case, return list of regional hearing locations based on RPC name")
    @Test
    void getRegionalHearingLocations_shouldReturnCorrespondingEpimsIdsForVenuesWithSameRpc() throws ListingException {
        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .name(REGIONAL_PROCESSING_CENTRE)
            .hearingRoute(HearingRoute.LIST_ASSIST)
            .postcode("LS1 2ED").build();
        caseData = SscsCaseData.builder()
            .dwpIsOfficerAttending("No")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder()
                        .hearingOptions(HearingOptions.builder()
                                            .wantsToAttend("N")
                                            .build())
                        .build())
            .processingVenue(PROCESSING_VENUE_1)
            .build();
        given(refData.getRegionalProcessingCenterService()).willReturn(regionalProcessingCenterService);
        given(venueService.getActiveRegionalEpimsIdsForRpc(caseData.getRegionalProcessingCenter().getEpimsId()))
            .willReturn(EPIMS_ID_LIST);
        given(refData.getVenueService()).willReturn(venueService);
        given(regionalProcessingCenterService.getByPostcode(eq("LS1 2ED"), anyBoolean())).willReturn(rpc);

        List<HearingLocation> result = HearingsLocationMapping.getHearingLocations(caseData, refData);

        checkHearingLocationResults(result, EPIMS_ID_1, EPIMS_ID_2, EPIMS_ID_3, EPIMS_ID_4);
    }

    @DisplayName("When hearing is paper case and rpc is invalid, throw listing exception")
    @Test
    void getRegionalHearingLocationsWithInvalidRpc_thenThrowListingException() {
        caseData = SscsCaseData.builder()
            .dwpIsOfficerAttending("No")
            .regionalProcessingCenter(RegionalProcessingCenter.builder().name(REGIONAL_PROCESSING_CENTRE).build())
            .appeal(Appeal.builder()
                        .hearingOptions(HearingOptions.builder()
                                            .wantsToAttend("N")
                                            .build())
                        .build())
            .processingVenue(PROCESSING_VENUE_1)
            .build();
        given(refData.getRegionalProcessingCenterService()).willReturn(regionalProcessingCenterService);

        assertThrows(ListingException.class, () -> HearingsLocationMapping.getHearingLocations(caseData, refData));
    }

    @DisplayName("When hearing is paper case and rpc is null, throw listing exception")
    @Test
    void getRegionalHearingLocationsWithNullRpc_thenThrowListingException() {
        caseData = SscsCaseData.builder()
            .dwpIsOfficerAttending("No")
            .appeal(Appeal.builder()
                        .hearingOptions(HearingOptions.builder()
                                            .wantsToAttend("N")
                                            .build())
                        .build())
            .processingVenue(PROCESSING_VENUE_1)
            .build();

        assertThrows(ListingException.class, () -> HearingsLocationMapping.getHearingLocations(caseData, refData));
    }

    @DisplayName("getHearingPriority Parameterized Tests")
    @ParameterizedTest
    @CsvSource(value = {
        "YES,Yes,Urgent",
        "YES,No,Urgent",
        "NO,Yes,Urgent",
        "NO,No,Standard",
        "RESERVED,Yes,Urgent",
        "RESERVED,No,Standard",
        "YES,null,Urgent",
        "NO,null,Standard",
        "RESERVED,null,Standard",
        "null,Yes,Urgent",
        "null,No,Standard",
        "null,null,Standard",
        "YES,,Urgent",
        "NO,,Standard",
        "RESERVED,,Standard"
    }, nullValues = {"null"})
    void getHearingPriority(AdjournCasePanelMembersExcluded panelMembersExcluded, String isUrgentCase, String expected) {
        // TODO Finish Test when method done
        SscsCaseData caseData = SscsCaseData.builder()
            .urgentCase(isUrgentCase)
            .adjournment(Adjournment.builder()
                             .panelMembersExcluded(panelMembersExcluded)
                             .build()
            )
            .build();
        String result = HearingsDetailsMapping.getHearingPriority(caseData);

        assertEquals(expected, result);
    }

    @DisplayName("When case data with a valid processing venue is given, getHearingLocations returns the correct venues")
    @ParameterizedTest
    @CsvSource(value = {"219164,court"}, nullValues = {"null"})
    void getHearingLocations() throws ListingException {
        SscsCaseData caseData = SscsCaseData.builder()
            .adjournment(Adjournment.builder().adjournmentInProgress(YesNo.NO).build())
            .appeal(Appeal.builder()
                        .hearingSubtype(HearingSubtype.builder()
                                            .wantsHearingTypeFaceToFace("Yes")
                                            .build())
                        .hearingOptions(HearingOptions.builder()
                                            .wantsToAttend("Yes")
                                            .build())
                        .build())
            .processingVenue(PROCESSING_VENUE_1)
            .dwpIsOfficerAttending("Yes")
            .build();

        given(venueService.getEpimsIdForVenue(caseData.getProcessingVenue())).willReturn(EPIMS_ID_1);
        given(refData.getVenueService()).willReturn(venueService);

        checkHearingLocationResults(HearingsLocationMapping.getHearingLocations(caseData, refData),
                                    EPIMS_ID_1);
    }

    @DisplayName("When override Hearing Venue Epims Ids is not empty getHearingLocations returns the override values")
    @Test
    void getHearingLocationsOverride() throws ListingException {
        buildOverrideHearingLocations();

        checkHearingLocationResults(HearingsLocationMapping.getHearingLocations(caseData, refData),
                                    EPIMS_ID_1, EPIMS_ID_2);
    }

    @DisplayName("When a case has been adjourned and a different venue has been selected, return the new venue")
    @Test
    void getHearingLocationsAdjournmentNewVenue() throws ListingException {
        given(refData.isAdjournmentFlagEnabled()).willReturn(true);
        caseData.getAdjournment().setAdjournmentInProgress(YesNo.YES);

        given(refData.getVenueService()).willReturn(venueService);

        given(venueService.getEpimsIdForVenueId(VENUE_ID)).willReturn(EPIMS_ID_1);

        setupAdjournedHearingVenue(SOMEWHERE_ELSE, VENUE_ID);

        List<HearingLocation> results = HearingsLocationMapping.getHearingLocations(caseData, refData);

        checkHearingLocationResults(results, EPIMS_ID_1);
    }

    @DisplayName("When a case has been adjourned and the same venue has been selected, return the same venue")
    @Test
    void getHearingLocationsAdjournmentSameVenue() throws ListingException {
        given(refData.isAdjournmentFlagEnabled()).willReturn(true);
        caseData.getAdjournment().setAdjournmentInProgress(YesNo.YES);

        given(venueService.getEpimsIdForVenueId(EPIMS_ID_1)).willReturn(EPIMS_ID_2);
        given(refData.getVenueService()).willReturn(venueService);

        setupAdjournedHearingVenue(SAME_VENUE, EPIMS_ID_1);

        caseData.setHearings(Collections.singletonList(Hearing.builder()
                                                           .value(uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails.builder()
                                                                      .epimsId(EPIMS_ID_2).build())
                                                           .build()));

        checkHearingLocationResults(
            HearingsLocationMapping.getHearingLocations(caseData, refData),
            EPIMS_ID_2);
    }

    @DisplayName("When a case has been adjourned but the next hearing is paper, return the override hearing locations")
    @Test
    void getHearingLocationsAdjournmentNewVenuePaperCase() throws ListingException {
        buildOverrideHearingLocations();

        setupAdjournedHearingVenue(SOMEWHERE_ELSE, EPIMS_ID_1);

        caseData.getSchedulingAndListingFields().getOverrideFields().setAppellantHearingChannel(HearingChannel.PAPER);

        checkHearingLocationResults(HearingsLocationMapping.getHearingLocations(caseData, refData),
                                    EPIMS_ID_1, EPIMS_ID_2);
    }

    @DisplayName("Checks that the flag will make sure the code isn't run and returns the override values")
    @Test
    void getHearingLocationsCheckFlag() throws ListingException {
        given(venueService.getEpimsIdForVenue(caseData.getProcessingVenue())).willReturn(EPIMS_ID_1);
        given(refData.getVenueService()).willReturn(venueService);

        given(refData.isAdjournmentFlagEnabled()).willReturn(false); //TODO: remove flag

        checkHearingLocationResults(HearingsLocationMapping.getHearingLocations(caseData, refData),
                                    EPIMS_ID_1);
    }

    void setupAdjournedHearingVenue(AdjournCaseNextHearingVenue nextHearingVenue, String adjournedId) {
        DynamicListItem item = new DynamicListItem(adjournedId, "");
        DynamicList list = new DynamicList(item, Collections.emptyList());

        caseData.getAdjournment().setNextHearingVenue(nextHearingVenue);
        caseData.getAdjournment().setNextHearingVenueSelected(list);
    }

    void checkHearingLocationResults(List<HearingLocation> hearingLocations, String... expectedResults) {
        assertThat(hearingLocations)
            .hasSize(expectedResults.length)
            .allSatisfy(hearingLocation ->
                            assertThat(hearingLocation.getLocationType()).isEqualTo(COURT))
            .extracting(HearingLocation::getLocationId)
            .containsExactlyInAnyOrder(expectedResults);
    }

    void buildOverrideHearingLocations() {
        caseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder()
                                                                       .hearingVenueEpimsIds(List.of(
                                                                           CcdValue.<CcdValue<String>>builder()
                                                                               .value(CcdValue.<String>builder()
                                                                                          .value(EPIMS_ID_1)
                                                                                          .build())
                                                                               .build(),
                                                                           CcdValue.<CcdValue<String>>builder()
                                                                               .value(CcdValue.<String>builder()
                                                                                          .value(EPIMS_ID_2)
                                                                                          .build())
                                                                               .build()))
                                                                       .build());
    }

    @DisplayName("getFacilitiesRequired returns an empty list")
    @Test
    void testGetFacilitiesRequired() {
        List<String> individualReasonableAdjustments = HearingsDetailsMapping.getFacilitiesRequired();
        assertThat(individualReasonableAdjustments).isEmpty();
    }

    @DisplayName("When appellant and other parties Hearing Options other comments are given "
        + "getListingComments returns all the comments separated by newlines")
    @ParameterizedTest
    @CsvSource(value = {
        "AppellantComments,'OpComments1',Appellant - Mx Test Appellant:\\nAppellantComments\\n\\n"
            + "Other Party - Mx Test OtherParty:\\nOpComments1",
        "AppellantComments,'',Appellant - Mx Test Appellant:\\nAppellantComments",
        "AppellantComments,null,Appellant - Mx Test Appellant:\\nAppellantComments",
    }, nullValues = {"null"})
    void getListingComments(String appellant, String otherPartiesComments, String expected) {
        List<CcdValue<OtherParty>> otherParties = new ArrayList<>();

        if (nonNull(otherPartiesComments)) {
            otherParties.add(new CcdValue<>(OtherParty.builder()
                                                .name(Name.builder()
                                                          .title("Mx")
                                                          .firstName("Test")
                                                          .lastName("OtherParty")
                                                          .build())
                                                .hearingOptions(HearingOptions.builder()
                                                                    .other(otherPartiesComments)
                                                                    .build())
                                                .build()));
        }

        Appeal appeal = Appeal.builder()
            .appellant(Appellant.builder()
                           .name(Name.builder()
                                     .title("Mx")
                                     .firstName("Test")
                                     .lastName("Appellant")
                                     .build())
                           .build())
            .build();
        if (nonNull(appellant)) {
            appeal.setHearingOptions(HearingOptions.builder().other(appellant).build());
        }

        caseData = SscsCaseData.builder()
            .appeal(appeal)
            .otherParties(otherParties)
            .build();

        String result = HearingsDetailsMapping.getListingComments(caseData);

        assertThat(result).isEqualToNormalizingNewlines(expected.replace("\\n", String.format("%n")));
    }

    @Test
    void listingCommentsShouldNotThrowNpeWhenHearingOptionsNull() {
        List<CcdValue<OtherParty>> otherParties = new ArrayList<>();
        otherParties.add(new CcdValue<>(OtherParty.builder()
                                            .name(Name.builder()
                                                      .title("Mr")
                                                      .firstName("Null")
                                                      .lastName("Comment")
                                                      .build())
                                            .hearingOptions(null)
                                            .build()));

        otherParties.add(new CcdValue<>(OtherParty.builder()
                                            .name(Name.builder()
                                                      .title("Mr")
                                                      .firstName("OtherParty")
                                                      .lastName("Comment")
                                                      .build())
                                            .hearingOptions(HearingOptions.builder().other("Test").build())
                                            .build()));

        Appeal appeal = Appeal.builder().appellant(Appellant.builder()
                 .name(Name.builder()
                           .title("Mr")
                           .firstName("Test")
                           .lastName("Appellant")
                           .build())
                 .build()).build();

        caseData = SscsCaseData.builder()
            .appeal(appeal)
            .otherParties(otherParties)
            .build();

        String result = HearingsDetailsMapping.getListingComments(caseData);

        assertThat(result).isEqualToNormalizingNewlines("Other Party - Mr OtherParty Comment:\n" + "Test");
    }

    @DisplayName("When all null or empty comments are given getListingComments returns null")
    @ParameterizedTest
    @CsvSource(value = {
        "'',''",
        "null,''",
        "null,null",
        "'','||'",
    }, nullValues = {"null"})
    void getListingCommentsReturnsNull(String appellant, String otherPartiesComments) {
        List<CcdValue<OtherParty>> otherParties = new ArrayList<>();
        if (nonNull(otherPartiesComments)) {
            for (String otherPartyComment : splitCsvParamArray(otherPartiesComments)) {
                otherParties.add(new CcdValue<>(OtherParty.builder()
                                                    .hearingOptions(HearingOptions.builder().other(otherPartyComment).build())
                                                    .build()));
            }
        } else {
            otherParties = null;
        }
        Appeal appeal = Appeal.builder().build();
        if (nonNull(appellant)) {
            appeal.setHearingOptions(HearingOptions.builder().other(appellant).build());
        }

        caseData = SscsCaseData.builder()
            .appeal(appeal)
            .otherParties(otherParties)
            .build();

        String result = HearingsDetailsMapping.getListingComments(caseData);

        assertNull(result);
    }


    @DisplayName("getPartyRole with Role Test")
    @Test
    void getPartyRole() {
        Party party = Appellant.builder()
            .role(Role.builder()
                      .name("Test Role")
                      .build())
            .build();
        String result = HearingsDetailsMapping.getPartyRole(party);

        assertThat(result).isEqualTo("Test Role");
    }

    @DisplayName("getPartyRole without Role Test")
    @Test
    void getPartyRoleNoRole() {
        Party party = Appellant.builder().build();
        String result = HearingsDetailsMapping.getPartyRole(party);

        assertThat(result).isEqualTo("Appellant");
    }

    @DisplayName("getPartyRole with empty/null Role Name Test")
    @ParameterizedTest
    @NullAndEmptySource
    void getPartyRoleNoRole(String role) {
        Party party = Appellant.builder()
            .role(Role.builder()
                      .name(role)
                      .build())
            .build();
        String result = HearingsDetailsMapping.getPartyRole(party);

        assertThat(result).isEqualTo("Appellant");
    }

    @DisplayName("getPartyName where Appointee is null Test")
    @ParameterizedTest
    @ValueSource(strings = {"Yes", "No"})
    @NullAndEmptySource
    void getPartyName(String isAppointee) {
        Party party = Appellant.builder()
            .isAppointee(isAppointee)
            .name(Name.builder()
                      .title("Mx")
                      .firstName("Test")
                      .lastName("Appellant")
                      .build())
            .build();
        String result = HearingsDetailsMapping.getEntityName(party);

        assertThat(result).isEqualTo("Mx Test Appellant");
    }

    @DisplayName("getPartyName No Appointee Test")
    @ParameterizedTest
    @ValueSource(strings = {"No"})
    @NullAndEmptySource
    void getPartyNameAppellant(String isAppointee) {
        Party party = Appellant.builder()
            .isAppointee(isAppointee)
            .appointee(Appointee.builder()
                           .name(Name.builder()
                                     .title("Mx")
                                     .firstName("Test")
                                     .lastName("Appointee")
                                     .build())
                           .build())
            .name(Name.builder()
                      .title("Mx")
                      .firstName("Test")
                      .lastName("Appellant")
                      .build())
            .build();
        String result = HearingsDetailsMapping.getEntityName(party);

        assertThat(result).isEqualTo("Mx Test Appellant");
    }

    @DisplayName("getHearingRequester Test")
    @Test
    void getHearingRequester() {
        String result = HearingsDetailsMapping.getHearingRequester();

        assertNull(result);
    }

    @DisplayName("When .. is given getLeadJudgeContractType returns the correct LeadJudgeContractType")
    @Test
    void getLeadJudgeContractType() {
        // TODO Finish Test when method done
        String result = HearingsDetailsMapping.getLeadJudgeContractType();

        assertNull(result);
    }

    @DisplayName("When dwpIsOfficerAttending is yes, isPoOfficerAttending return True")
    @Test
    void testIsPoOfficerAttending() {
        caseData = SscsCaseData.builder()
            .dwpIsOfficerAttending("Yes")
            .build();

        boolean result = HearingsDetailsMapping.isPoOfficerAttending(caseData);

        assertThat(result).isTrue();
    }

    @DisplayName("When dwpIsOfficerAttending is No or blank, isPoOfficerAttending return False")
    @ParameterizedTest
    @ValueSource(strings = {"No"})
    @NullAndEmptySource
    void testIsPoOfficerAttending(String value) {
        caseData = SscsCaseData.builder()
            .dwpIsOfficerAttending(value)
            .build();

        boolean result = HearingsDetailsMapping.isPoOfficerAttending(caseData);

        assertThat(result).isFalse();
    }

    @DisplayName("When adjournCaseNextHearingSpecificTime or adjournCaseNextHearingFirstOnSession should return it in getListingComments")
    @Test
    void testGetListingCommentsWithAdjournCaseTimeSettingsDefined() {
        Adjournment adjournment = caseData.getAdjournment();
        AdjournCaseTime adjournCaseTime = AdjournCaseTime
            .builder()
            .adjournCaseNextHearingSpecificTime("pm")
            .adjournCaseNextHearingFirstOnSession(List.of("firstOnSession"))
            .build();

        adjournment.setNextHearingDateType(AdjournCaseNextHearingDateType.DATE_TO_BE_FIXED);
        adjournment.setTime(adjournCaseTime);

        var result = HearingsDetailsMapping.getListingComments(caseData);

        assertTrue(isNotEmpty(result));
        assertThat(result)
            .contains("Provide time: PM")
            .contains("List first on the session")
            .doesNotContainPattern("AM");
    }

    @DisplayName("When no additional comment fields are empty should return empty string")
    @Test
    void testEmptyListingComments() {
        var result = HearingsDetailsMapping.getListingComments(caseData);

        assertTrue(isEmpty(result));
    }
}
