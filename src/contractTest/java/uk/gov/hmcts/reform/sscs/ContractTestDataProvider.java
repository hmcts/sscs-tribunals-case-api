package uk.gov.hmcts.reform.sscs;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AmendReason.ADMIN_REQUEST;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.Adjustment.HEARING_LOOP;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.Adjustment.SIGN_LANGUAGE_INTERPRETER;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.Adjustment.STEP_FREE_WHEELCHAIR_ACCESS;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.CaseCategoryType.CASE_TYPE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.DayOfWeekUnavailabilityType.AM;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.DayOfWeekUnavailabilityType.PM;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HearingType.SUBSTANTIVE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.LocationType.COURT;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.PartyType.INDIVIDUAL;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.RequirementType.EXCLUDE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.RequirementType.MUST_INCLUDE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.RequirementType.OPTIONAL_INCLUDE;
import static uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason.WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.FACE_TO_FACE;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import uk.gov.hmcts.reform.sscs.model.HearingLocation;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.Adjustment;
import uk.gov.hmcts.reform.sscs.model.single.hearing.*;

public class ContractTestDataProvider {

    public static final String CONSUMER_NAME = "sscs_tribunalsCaseApi";
    public static final String PROVIDER_NAME = "hmcHearingServiceProvider";

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String IDAM_OAUTH2_TOKEN = "pact-test-idam-token";
    public static final String UNAUTHORISED_IDAM_OAUTH2_TOKEN = "unauthorised-pact-test-idam-token";
    public static final String SERVICE_AUTHORIZATION_TOKEN = "pact-test-s2s-token";
    public static final String UNAUTHORISED_SERVICE_AUTHORIZATION_TOKEN = "unauthorised-pact-test-s2s-token";

    public static final String MSG_200_HEARING = "Success (with content)";
    public static final String MSG_400_HEARING = "Invalid request";
    public static final String MSG_401_HEARING = "Unauthorised request";
    public static final String MSG_403_HEARING = "Forbidden request";
    public static final String MSG_404_HEARING = "Not Found request";



    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";

    public static final String HEARING_PATH = "/hearing";
    public static final String FIELD_STATUS = "status";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_ERRORS = "errors";
    public static final int ZERO_LENGTH = 0;
    public static final Number ZERO_NUMBER_LENGTH = 0;
    public static final String VALID_CASE_ID = "123";
    public static final String FORBIDDEN_CASE_ID = "456";
    public static final String NOT_FOUND_CASE_ID = "789";
    public static final String HEARING_DATE = "2030-08-20T12:40";
    public static final String ACTIVE = "ACTIVE";

    private ContractTestDataProvider() {

    }

    public static final Map<String, String> authorisedHeaders = Map.of(
        AUTHORIZATION, IDAM_OAUTH2_TOKEN,
        SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN,
        CONTENT_TYPE, APPLICATION_JSON
    );

    public static final Map<String, String> authorisedHeadersGet = Map.of(
            AUTHORIZATION, IDAM_OAUTH2_TOKEN,
            SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN
    );

    public static final Map<String, String> unauthorisedHeaders = Map.of(
        AUTHORIZATION, UNAUTHORISED_IDAM_OAUTH2_TOKEN,
        SERVICE_AUTHORIZATION, UNAUTHORISED_SERVICE_AUTHORIZATION_TOKEN,
        CONTENT_TYPE, APPLICATION_JSON
    );

    public static final Map<String, String> unauthorisedHeadersGet = Map.of(
            AUTHORIZATION, UNAUTHORISED_IDAM_OAUTH2_TOKEN,
            SERVICE_AUTHORIZATION, UNAUTHORISED_SERVICE_AUTHORIZATION_TOKEN
    );

    public static HearingRequestPayload generateHearingRequest() {
        HearingRequestPayload request = new HearingRequestPayload();

        request.setHearingDetails(hearingDetails());
        request.setCaseDetails(caseDetails());
        request.setPartiesDetails(partyDetails1());

        return request;
    }

    public static HearingRequestPayload generateInvalidHearingRequest() {
        HearingRequestPayload request = new HearingRequestPayload();
        request.setHearingDetails(hearingDetails());
        request.setPartiesDetails(partyDetails1());
        return request;
    }

    public static HearingCancelRequestPayload generateHearingDeleteRequest() {
        HearingCancelRequestPayload request = new HearingCancelRequestPayload();
        request.setCancellationReasonCodes(List.of(WITHDRAWN));
        return request;
    }

    public static HearingCancelRequestPayload generateInvalidHearingDeleteRequest() {
        HearingCancelRequestPayload request = new HearingCancelRequestPayload();
        request.setCancellationReasonCodes(null);
        return request;
    }

    public static String toJsonString(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String jsonString = "";
        try {
            jsonString = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return jsonString;
    }

    protected static RequestDetails requestDetails() {
        RequestDetails requestDetails = new RequestDetails();
        requestDetails.setVersionNumber(123L);
        return requestDetails;
    }

    protected static HearingDetails hearingDetails() {
        HearingDetails hearingDetails = new HearingDetails();
        hearingDetails.setAutolistFlag(true);
        hearingDetails.setHearingType(SUBSTANTIVE);
        hearingDetails.setHearingWindow(hearingWindow());
        hearingDetails.setDuration(1);
        hearingDetails.setNonStandardHearingDurationReasons(Arrays.asList("First reason", "Second reason"));
        hearingDetails.setHearingPriorityType("Priority type");
        HearingLocation location1 = new HearingLocation();
        location1.setLocationId("court");
        location1.setLocationType(COURT);
        List<HearingLocation> hearingLocation = new ArrayList<>();
        hearingLocation.add(location1);
        hearingDetails.setHearingLocations(hearingLocation);
        hearingDetails.setPanelRequirements(panelRequirements1());
        hearingDetails.setAmendReasonCodes(List.of(ADMIN_REQUEST));
        hearingDetails.setHearingChannels(new ArrayList<>());
        return hearingDetails;
    }

    protected static HearingWindow hearingWindow() {
        HearingWindow hearingWindow = new HearingWindow();
        hearingWindow.setDateRangeStart(LocalDate.parse("2020-02-01"));
        hearingWindow.setDateRangeEnd(LocalDate.parse("2020-02-12"));

        return hearingWindow;
    }

    protected static CaseDetails caseDetails() {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setHmctsServiceCode("ABBA1");
        caseDetails.setCaseId("12");
        caseDetails.setCaseDeepLink("https://www.google.com");
        caseDetails.setHmctsInternalCaseName("Internal case name");
        caseDetails.setPublicCaseName("Public case name");
        caseDetails.setCaseManagementLocationCode("CMLC123");
        caseDetails.setCaseRestrictedFlag(false);
        caseDetails.setCaseSlaStartDate("2030-08-20");
        CaseCategory category = new CaseCategory();
        category.setCategoryType(CASE_TYPE);
        category.setCategoryValue("PROBATE");
        category.setCategoryParent("categoryParent");
        List<CaseCategory> caseCategories = new ArrayList<>();
        caseCategories.add(category);
        caseDetails.setCaseCategories(caseCategories);
        return caseDetails;
    }

    protected static PanelRequirements panelRequirements1() {
        List<String> roleType = new ArrayList<>();
        roleType.add("role 1");
        roleType.add("role 2");
        List<String> authorisationTypes = new ArrayList<>();
        authorisationTypes.add("authorisation type 1");
        authorisationTypes.add("authorisation type 2");
        authorisationTypes.add("authorisation type 3");
        List<String> authorisationSubType = new ArrayList<>();
        authorisationSubType.add("authorisation sub 1");
        authorisationSubType.add("authorisation sub 2");
        authorisationSubType.add("authorisation sub 3");
        authorisationSubType.add("authorisation sub 4");

        final PanelPreference panelPreference1 = new PanelPreference();
        panelPreference1.setMemberID("Member 1");
        panelPreference1.setMemberType(MemberType.JOH);
        panelPreference1.setRequirementType(MUST_INCLUDE);
        final PanelPreference panelPreference2 = new PanelPreference();
        panelPreference2.setMemberID("Member 2");
        panelPreference2.setMemberType(MemberType.JOH);
        panelPreference2.setRequirementType(OPTIONAL_INCLUDE);
        final PanelPreference panelPreference3 = new PanelPreference();
        panelPreference3.setMemberID("Member 3");
        panelPreference3.setMemberType(MemberType.JOH);
        panelPreference3.setRequirementType(EXCLUDE);
        List<PanelPreference> panelPreferences = new ArrayList<>();
        panelPreferences.add(panelPreference1);
        panelPreferences.add(panelPreference2);
        panelPreferences.add(panelPreference3);
        List<String> panelSpecialisms = new ArrayList<>();
        panelSpecialisms.add("Specialism 1");
        panelSpecialisms.add("Specialism 2");
        panelSpecialisms.add("Specialism 3");
        panelSpecialisms.add("Specialism 4");
        panelSpecialisms.add("Specialism 5");

        PanelRequirements panelRequirements = new PanelRequirements();
        panelRequirements.setRoleTypes(roleType);
        panelRequirements.setAuthorisationSubTypes(authorisationSubType);
        panelRequirements.setPanelPreferences(panelPreferences);
        panelRequirements.setPanelSpecialisms(panelSpecialisms);
        panelRequirements.setAuthorisationTypes(authorisationTypes);

        return panelRequirements;
    }

    protected static List<PartyDetails> partyDetails1() {
        ArrayList<PartyDetails> partyDetailsArrayList = new ArrayList<>();
        partyDetailsArrayList.add(createPartyDetails("P1", "DEF", null, createOrganisationDetails()));
        partyDetailsArrayList.add(createPartyDetails("P2", "DEF2", createIndividualDetails(), null));
        partyDetailsArrayList.add(createPartyDetails("P3", "DEF3", createIndividualDetails(),
                                                     createOrganisationDetails()
        ));
        return partyDetailsArrayList;
    }

    private static OrganisationDetails createOrganisationDetails() {
        OrganisationDetails organisationDetails = new OrganisationDetails();
        organisationDetails.setName("name");
        organisationDetails.setOrganisationType("organisationType");
        organisationDetails.setCftOrganisationID("cftOrganisationId01001");
        return organisationDetails;
    }

    private static IndividualDetails createIndividualDetails() {
        IndividualDetails individualDetails = new IndividualDetails();
        individualDetails.setFirstName("Harry");
        individualDetails.setLastName("Styles");
        List<String> hearingChannelEmail = new ArrayList<String>();
        hearingChannelEmail.add("harry.styles.neveragin1@gmailsss.com");
        hearingChannelEmail.add("harry.styles.neveragin2@gmailsss.com");
        hearingChannelEmail.add("harry.styles.neveragin3@gmailsss.com");
        individualDetails.setHearingChannelEmail(hearingChannelEmail);
        List<String> hearingChannelPhone = new ArrayList<String>();
        hearingChannelPhone.add("+447398087560");
        hearingChannelPhone.add("+447398087561");
        hearingChannelPhone.add("+447398087562");
        individualDetails.setHearingChannelPhone(hearingChannelPhone);
        individualDetails.setInterpreterLanguage("German");
        individualDetails.setPreferredHearingChannel(FACE_TO_FACE);
        individualDetails.setReasonableAdjustments(createReasonableAdjustments());
        individualDetails.setRelatedParties(createRelatedParties());
        individualDetails.setVulnerableFlag(false);
        individualDetails.setVulnerabilityDetails("Vulnerability details 1");
        individualDetails.setCustodyStatus("ACTIVE");
        individualDetails.setOtherReasonableAdjustmentDetails("Other Reasonable Adjustment Details");
        return individualDetails;
    }

    private static List<RelatedParty> createRelatedParties() {
        RelatedParty relatedParty1 = new RelatedParty();
        relatedParty1.setRelatedPartyId("relatedParty1111");
        relatedParty1.setRelationshipType("Family");
        RelatedParty relatedParty2 = new RelatedParty();
        relatedParty2.setRelatedPartyId("relatedParty3333");
        relatedParty2.setRelationshipType("Blood Brother");

        List<RelatedParty> relatedParties = new ArrayList<>();
        relatedParties.add(relatedParty1);
        relatedParties.add(relatedParty2);
        return relatedParties;
    }

    private static PartyDetails createPartyDetails(String partyID, String partyRole,
                                                   IndividualDetails individualDetails,
                                                   OrganisationDetails organisationDetails) {
        PartyDetails partyDetails = new PartyDetails();
        partyDetails.setPartyID(partyID);
        partyDetails.setPartyType(INDIVIDUAL);
        partyDetails.setPartyRole(partyRole);
        partyDetails.setIndividualDetails(individualDetails);
        partyDetails.setOrganisationDetails(organisationDetails);
        partyDetails.setUnavailabilityRanges(createUnavailableDateRanges());
        partyDetails.setUnavailabilityDayOfWeek(createUnavailabilityDows());
        return partyDetails;
    }

    private static List<Adjustment> createReasonableAdjustments() {
        List<Adjustment> reasonableAdjustments = new ArrayList<>();
        reasonableAdjustments.add(HEARING_LOOP);
        reasonableAdjustments.add(SIGN_LANGUAGE_INTERPRETER);
        reasonableAdjustments.add(STEP_FREE_WHEELCHAIR_ACCESS);
        return reasonableAdjustments;
    }

    private static List<UnavailabilityDayOfWeek> createUnavailabilityDows() {
        List<UnavailabilityDayOfWeek> unavailabilityDows = new ArrayList<>();
        UnavailabilityDayOfWeek unavailabilityDow1 = new UnavailabilityDayOfWeek();
        unavailabilityDow1.setDayOfWeek(MONDAY);
        unavailabilityDow1.setDayOfWeekUnavailabilityType(AM);
        unavailabilityDows.add(unavailabilityDow1);
        UnavailabilityDayOfWeek unavailabilityDow2 = new UnavailabilityDayOfWeek();
        unavailabilityDow2.setDayOfWeek(TUESDAY);
        unavailabilityDow2.setDayOfWeekUnavailabilityType(PM);
        unavailabilityDows.add(unavailabilityDow2);
        return unavailabilityDows;
    }

    private static List<UnavailabilityRange> createUnavailableDateRanges() {
        UnavailabilityRange unavailabilityRanges1 = new UnavailabilityRange();
        unavailabilityRanges1.setUnavailableFromDate(LocalDate.parse("2021-01-01"));
        unavailabilityRanges1.setUnavailableToDate(LocalDate.parse("2021-01-15"));
        UnavailabilityRange unavailabilityRanges2 = new UnavailabilityRange();
        unavailabilityRanges2.setUnavailableFromDate(LocalDate.parse("2021-06-01"));
        unavailabilityRanges2.setUnavailableToDate(LocalDate.parse("2021-06-21"));

        List<UnavailabilityRange> listUnavailabilityRanges = new ArrayList<>();
        listUnavailabilityRanges.add(unavailabilityRanges1);
        listUnavailabilityRanges.add(unavailabilityRanges2);
        return listUnavailabilityRanges;
    }


    public static PactDslJsonBody generateValidHearingGetResponsePactDslJsonBody(LocalDateTime date) {
        PactDslJsonBody result = new PactDslJsonBody();

        result
            .object("requestDetails")
            .integerType("versionNumber", 123)
            .stringType("hearingRequestID", "hearingRequestId123")
            .stringType("status", "HEARING_REQUESTED")
            .stringType("timestamp", date.toString())
            .stringType("hearingGroupRequestId", "hearingGroupRequestId123")
            .stringType("partiesNotified", date.toString())
            .closeObject()
            .object("hearingDetails")
            .booleanValue("autolistFlag", true)
            .booleanValue("hearingIsLinkedFlag", true)
            .booleanValue("privateHearingRequiredFlag", true)
            .booleanValue("hearingInWelshFlag", true)
            .stringType("hearingType", "BBA3-SUB")
            .stringType("leadJudgeContractType", "leadJudgeContractType123")
            .stringType("listingComments", "listingComments123")
            .stringType("hearingRequester", "hearingRequester123")
            .stringType("hearingPriorityType", "hearingPriorityType123")
            .integerType("numberOfPhysicalAttendees", 123)
            .integerType("duration", 123)
            .array("nonStandardHearingDurationReasons")
            .string("nonStandardHearingDurationReasons1")
            .string("nonStandardHearingDurationReasons2")
            .closeArray()
            .object("hearingWindow")
            .stringType("dateRangeStart", date.toString())
            .stringType("dateRangeEnd", date.toString())
            .stringType("firstDateTimeMustBe", date.toString())
            .closeObject()
            .object("panelRequirements")
            .array("roleType")
            .string("roleType1")
            .string("roleType2")
            .closeArray()
            .array("authorisationSubType")
            .string("authorisationSubType1")
            .string("authorisationSubType2")
            .closeArray()
            .array("panelSpecialisms")
            .string("panelSpecialisms1")
            .string("panelSpecialisms2")
            .closeArray()
            .minArrayLike("panelPreferences", 0, 1)
            .stringType("memberID", "memberID123")
            .stringType("memberType", "JOH")
            .stringType("requirementType", "EXCLUDE")
            .closeObject().closeArray()
            .closeObject()
            .minArrayLike("hearingLocations", 0, 1)
            .stringType("locationType", "court")
            .stringType("locationId", "locationId123")
            .closeObject().closeArray()
            .array("facilitiesRequired")
            .string("facilitiesRequired1")
            .closeArray()
            .closeObject()
            .object("caseDetails")
            .booleanValue("caseAdditionalSecurityFlag", true)
            .booleanValue("caseInterpreterRequiredFlag", true)
            .booleanValue("caserestrictedFlag", true)
            .stringType("hmctsServiceCode", "1234")
            .stringType("caseRef", "1234123412341234")
            .stringType("externalCaseReference", "externalCaseReference123")
            .stringType("caseDeepLink", "caseDeepLink123")
            .stringType("hmctsInternalCaseName", "hmctsInternalCaseName123")
            .stringType("publicCaseName", "publicCaseName123")
            .stringType("caseManagementLocationCode", "caseManagementLocationCode123")
            .stringType("caseSLAStartDate", date.toString())
            .minArrayLike("caseCategories", 0, 1)
            .stringType("categoryType", "caseType")
            .stringType("categoryValue", "categoryValue123")
            .stringType("categoryParent", "categoryParent123")
            .closeObject().closeArray()
            .closeObject()
            .minArrayLike("partyDetails", 0, 1)
            .stringType("partyID", "partyID123")
            .stringType("partyType", "IND")
            .stringType("partyRole", "partyRole")
            .object("individualDetails")
            .stringType("firstName", "firstName123")
            .stringType("lastName", "lastName123")
            .stringType("preferredHearingChannel", "ONPPRS")
            .stringType("interpreterLanguage", "interpreterLanguage123")
            .stringType("vulnerabilityDetails", "vulnerabilityDetails123")
            .stringType("custodyStatus", "custodyStatus123")
            .stringType("otherReasonableAdjustmentDetails", "otherReasonableAdjustmentDetails123")
            .booleanValue("vulnerableFlag", true)
            .array("hearingChannelEmail")
            .string("hearingChannelEmail123@gmaild.com")
            .closeArray()
            .array("hearingChannelPhone")
            .string("07345960795")
            .closeArray()
            .array("reasonableAdjustments")
            .string("RA0043")
            .closeArray()
            .minArrayLike("relatedParties", 0, 1)
            .stringType("relatedPartyID", "relatedPartyID123")
            .stringType("relationshipType", "relationshipType123")
            .closeObject().closeArray()
            .closeObject()
            .object("organisationDetails")
            .stringType("name", "name123")
            .stringType("organisationType", "organisationType123")
            .stringType("cftOrganisationID", "cftOrganisationID123")
            .closeObject()
            .minArrayLike("unavailabilityDOW", 0, 1)
            .stringType("DOW", "MONDAY")
            .stringType("DOWUnavailabilityType", "AM")
            .closeObject().closeArray()
            .minArrayLike("unavailabilityRanges", 0, 1)
            .stringType("unavailableFromDate", date.toString())
            .stringType("unavailableToDate", date.toString())
            .closeObject().closeArray()
            .closeObject().closeArray()
            .object("hearingResponse")
            .stringType("listAssistTransactionID", "ListAssistTransactionID123123")
            .stringType("receivedDateTime", date.toString())
            .integerType("responseVersion", 321)
            .stringType("laCaseStatus", "LISTED")
            .stringType("listingStatus", "FIXED")
            .stringType("hearingCancellationReason", "notatt")
            .minArrayLike("hearingDaySchedule", 0, 1)
            .stringType("listAssistSessionID", "listAssistSessionID123")
            .stringType("hearingVenueId", "hearingVenueId123")
            .stringType("hearingRoomId", "hearingRoomId123")
            .stringType("hearingJudgeId", "hearingJudgeId123")
            .minArrayLike("panelMemberIds", 0, 1)
            .minArrayLike("attendees", 0, 1)
            .stringType("partyID", "partyID123")
            .stringType("hearingSubChannel", "hearingSubChannel123")
            .closeObject().closeArray()
            .closeObject().closeArray()
            .closeObject();


        return result;
    }

}
