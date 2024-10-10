package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_CASE_ONLY;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMappingBase.BENEFIT_CODE;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMappingBase.ISSUE_CODE;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.exception.GetCaseException;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.model.service.ServiceHearingRequest;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.ServiceHearingValues;
import uk.gov.hmcts.reform.sscs.model.service.linkedcases.ServiceLinkedCases;
import uk.gov.hmcts.reform.sscs.reference.data.model.SessionCategoryMap;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@ExtendWith(MockitoExtension.class)
class ServiceHearingsServiceTest {

    private static final long CASE_ID = 12345L;

    @Mock
    private SessionCategoryMapService sessionCategoryMaps;

    @Mock
    private VenueService venueService;

    @Mock
    private ReferenceDataServiceHolder refData;

    @Mock
    private CcdCaseService ccdCaseService;

    @InjectMocks
    private ServiceHearingsService serviceHearingsService;

    private SscsCaseData caseData;
    private SscsCaseDetails caseDetails;


    @BeforeEach
    void setup() {
        caseData = SscsCaseData.builder()
            .ccdCaseId("1234")
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .urgentCase("Yes")
            .adjournment(Adjournment.builder()
                .adjournmentInProgress(YesNo.NO)
                .canCaseBeListedRightAway(YesNo.YES)
                .build())
            .dwpResponseDate("2022-07-07")
            .caseManagementLocation(CaseManagementLocation.builder()
                .baseLocation("LIVERPOOL SOCIAL SECURITY AND CHILD SUPPORT TRIBUNAL")
                .region("North West")
                .build())
            .appeal(Appeal.builder()
                .hearingType("final")
                .appellant(Appellant.builder()
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
                    .build())
                .rep(Representative.builder()
                    .hasRepresentative("Yes")
                    .name(Name.builder()
                        .title("Mr")
                        .firstName("Harry")
                        .lastName("Potter")
                        .build())
                    .build())
                .build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .overrideFields(OverrideFields.builder()
                    .duration(30).build()).build())
            .languagePreferenceWelsh("No")
            .linkedCasesBoolean("No")
            .sscsIndustrialInjuriesData(SscsIndustrialInjuriesData.builder()
                .panelDoctorSpecialism("cardiologist")
                .secondPanelDoctorSpecialism("eyeSurgeon")
                .build())
            .build();

        caseDetails = SscsCaseDetails.builder()
            .data(caseData)
            .build();
    }

    @DisplayName("When a case data is retrieved an entity which does not have a Id, that a new Id will be generated and the method updateCaseData will be called once")
    @Test
    void testGetServiceHearingValuesNoIds() throws Exception {
        ServiceHearingRequest request = ServiceHearingRequest.builder()
            .caseId(String.valueOf(CASE_ID))
            .build();

        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE,ISSUE_CODE,true,false))
            .willReturn(new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                false,false, SessionCategory.CATEGORY_03,null));

        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);

        given(venueService.getEpimsIdForVenue(caseData.getProcessingVenue())).willReturn("9876");

        given(refData.getVenueService()).willReturn(venueService);

        given(ccdCaseService.getCaseDetails(String.valueOf(CASE_ID))).willReturn(caseDetails);

        ServiceHearingValues result = serviceHearingsService.getServiceHearingValues(request);

        assertThat(result.getParties())
            .extracting("partyID")
            .doesNotContainNull();

        verify(ccdCaseService, times(1)).updateCaseData(any(SscsCaseData.class), eq(UPDATE_CASE_ONLY), anyString(), anyString());
    }

    @DisplayName("When a listing error is throw due to invalid excluded dates, then catch the error, send a listing error event and rethrow the error")
    @Test
    void testGetServiceHearingValuesInvalidDateRange() throws Exception {
        caseData.getAppeal().getAppellant().setId("87399f1d-fcf9-416f-a3d0-f5ab0eb1109d");
        caseData.getAppeal().getRep().setId("9f6fe72e-7e6e-4ad5-9a47-e70fc37e9de4");
        caseData.getJointParty().setId("c11dc4a2-0447-4cd2-80fe-250df5c8d0a9");
        caseData.getAppeal().getHearingOptions().setExcludeDates(List.of(ExcludeDate.builder()
             .value(DateRange.builder().start(LocalDate.now().toString())
                .end(LocalDate.now().minus(1, ChronoUnit.DAYS).toString())
                .build())
             .build()));

        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE,ISSUE_CODE,true,false))
            .willReturn(new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                                               false,false, SessionCategory.CATEGORY_03,null));

        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);

        given(venueService.getEpimsIdForVenue(caseData.getProcessingVenue())).willReturn("9876");

        given(refData.getVenueService()).willReturn(venueService);

        given(ccdCaseService.getCaseDetails(String.valueOf(CASE_ID))).willReturn(caseDetails);

        ServiceHearingRequest request = ServiceHearingRequest.builder()
            .caseId(String.valueOf(CASE_ID))
            .build();

        assertThrows(ListingException.class, () -> serviceHearingsService.getServiceHearingValues(request));

        verify(ccdCaseService, never()).updateCaseData(any(SscsCaseData.class), eq(UPDATE_CASE_ONLY), anyString(), anyString());
    }

    @DisplayName("When a case data is retrieved where all valid entities have a Id the method updateCaseData will never be called")
    @Test
    void testGetServiceHearingValuesWithIds() throws Exception {
        ServiceHearingRequest request = ServiceHearingRequest.builder()
            .caseId(String.valueOf(CASE_ID))
            .build();

        caseData.getAppeal().getAppellant().setId("87399f1d-fcf9-416f-a3d0-f5ab0eb1109d");
        caseData.getAppeal().getRep().setId("9f6fe72e-7e6e-4ad5-9a47-e70fc37e9de4");
        caseData.getJointParty().setId("c11dc4a2-0447-4cd2-80fe-250df5c8d0a9");

        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE,ISSUE_CODE,true,false))
            .willReturn(new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                false,false, SessionCategory.CATEGORY_03,null));

        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);

        given(venueService.getEpimsIdForVenue(caseData.getProcessingVenue())).willReturn("9876");

        given(refData.getVenueService()).willReturn(venueService);

        given(ccdCaseService.getCaseDetails(String.valueOf(CASE_ID))).willReturn(caseDetails);

        ServiceHearingValues result = serviceHearingsService.getServiceHearingValues(request);

        assertThat(result.getParties())
            .extracting("partyID")
            .doesNotContainNull();

        verify(ccdCaseService, never()).updateCaseData(any(SscsCaseData.class), any(EventType.class), anyString(), anyString());
    }

    @ParameterizedTest
    @MethodSource("invalidCasesParameters")
    @DisplayName("One case should be returned when looking up case data for one case")
    void testGetServiceLinkedCasesIncorrectNumberOfCasesReturned(List<SscsCaseDetails> searchResult) throws Exception {
        ServiceHearingRequest request = ServiceHearingRequest.builder()
            .caseId(String.valueOf(CASE_ID))
            .build();

        given(ccdCaseService.getCasesViaElastic(List.of(String.valueOf(CASE_ID)))).willReturn(searchResult);

        assertThatExceptionOfType(GetCaseException.class)
            .isThrownBy(() -> serviceHearingsService.getServiceLinkedCases(request));
    }

    @Test
    void testGetServiceLinkedCasesNoLinkedCases() throws Exception {
        ServiceHearingRequest request = ServiceHearingRequest.builder()
            .caseId(String.valueOf(CASE_ID))
            .build();

        given(ccdCaseService.getCasesViaElastic(List.of(String.valueOf(CASE_ID)))).willReturn(
            List.of(SscsCaseDetails.builder()
                .data(SscsCaseData.builder()
            .build()).build()));

        List<ServiceLinkedCases> result = serviceHearingsService.getServiceLinkedCases(request);

        assertThat(result).isEmpty();
    }

    @Test
    void testGetServiceLinkedCaseReturnsLinkedCases() throws Exception {
        String linkedCaseReference = "1234";
        String linkedCaseNamePublic = "Some Name";

        ServiceHearingRequest request = ServiceHearingRequest.builder()
            .caseId(String.valueOf(CASE_ID))
            .build();

        SscsCaseDetails linkedSscsCaseData = SscsCaseDetails.builder()
            .id(Long.valueOf(linkedCaseReference))
            .data(SscsCaseData.builder()
            .ccdCaseId(linkedCaseReference)
            .caseReference(linkedCaseReference)
            .caseAccessManagementFields(CaseAccessManagementFields.builder()
                .caseNamePublic(linkedCaseNamePublic)
                .build())
            .build())
            .build();

        SscsCaseDetails sscsCaseData = SscsCaseDetails.builder()
            .id(CASE_ID)
            .data(SscsCaseData.builder()
            .ccdCaseId(String.valueOf(CASE_ID))
            .caseReference(String.valueOf(CASE_ID))
            .linkedCase(List.of(CaseLink.builder()
                .value(CaseLinkDetails.builder()
                    .caseReference(linkedCaseReference).build())
                .build()))
            .build())
            .build();

        given(ccdCaseService.getCasesViaElastic(List.of(String.valueOf(CASE_ID)))).willReturn(List.of(sscsCaseData));

        given(ccdCaseService.getCasesViaElastic(List.of(linkedCaseReference))).willReturn(List.of(linkedSscsCaseData));

        List<ServiceLinkedCases> result = serviceHearingsService.getServiceLinkedCases(request);

        assertThat(result).hasSize(1);

        ServiceLinkedCases expected = ServiceLinkedCases.builder()
            .caseReference(linkedCaseReference)
            .caseName(linkedCaseNamePublic)
            .reasonsForLink(new ArrayList<>())
            .build();

        assertThat(result.get(0)).isEqualTo(expected);
    }



    private static Stream<Arguments> invalidCasesParameters() {
        return Stream.of(
            null,
            Arguments.of(new ArrayList<>()),
            Arguments.of(List.of(SscsCaseDetails.builder()
                .data(SscsCaseData.builder()
                .ccdCaseId("1")
                .build())
                    .build(),
                SscsCaseDetails.builder()
                    .data(SscsCaseData.builder()
                    .ccdCaseId("2")
                    .build())
                    .build()
            ))
        );
    }
}
