package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.ADJOURN_CREATE_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CANCEL_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CREATE_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.UPDATED_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.UPDATE_HEARING;
import static uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason.OTHER;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitCode;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseManagementLocation;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingSubtype;
import uk.gov.hmcts.reform.sscs.ccd.domain.Issue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SessionCategory;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.exception.GetHearingException;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.exception.UnhandleableHearingStateException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMapping;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.HearingEvent;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.CaseDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingCancelRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.PanelRequirements;
import uk.gov.hmcts.reform.sscs.model.single.hearing.RequestDetails;
import uk.gov.hmcts.reform.sscs.reference.data.model.SessionCategoryMap;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@ExtendWith(MockitoExtension.class)
class HearingsServiceTest {
    private static final long HEARING_REQUEST_ID = 12345;
    private static final long CASE_ID = 1625080769409918L;
    private static final String BENEFIT_CODE = "002";
    private static final String ISSUE_CODE = "DD";
    private static final String PROCESSING_VENUE = "Processing Venue";


    private HearingWrapper wrapper;
    private HearingRequest request;
    private SscsCaseDetails expectedCaseDetails;

    @Mock
    private HmcHearingApiService hmcHearingApiService;
    @Mock
    private CcdCaseService ccdCaseService;
    @Mock
    private ReferenceDataServiceHolder refData;
    @Mock
    public HearingDurationsService hearingDurations;
    @Mock
    public SessionCategoryMapService sessionCategoryMaps;
    @Mock
    private VenueService venueService;
    @Mock
    private IdamService idamService;
    @Mock
    private UpdateCcdCaseService updateCcdCaseService;
    @Mock
    private HearingServiceConsumer hearingServiceConsumer;
    @Mock
    private HearingsMapping hearingsMapping;
    @Mock
    private Consumer<SscsCaseDetails> sscsCaseDetailsConsumer;
    @Mock
    private PanelCompositionService panelCompositionService;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> caseDataConsumerCaptor;

    @InjectMocks
    private HearingsService hearingsService;

    @BeforeEach
    void setup() {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId(String.valueOf(CASE_ID))
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .caseManagementLocation(CaseManagementLocation.builder().build())
            .adjournment(Adjournment.builder().adjournmentInProgress(YesNo.NO).build())
            .appeal(Appeal.builder()
                .rep(Representative.builder().hasRepresentative("No").build())
                .hearingOptions(HearingOptions.builder().wantsToAttend("yes").build())
                .hearingType("test")
                .hearingSubtype(HearingSubtype.builder().hearingVideoEmail("email@email.com").wantsHearingTypeFaceToFace("yes").build())
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("first").lastName("surname").build())
                    .build())
                .build())
            .processingVenue(PROCESSING_VENUE)
            .build();

        wrapper = HearingWrapper.builder()
            .hearingState(CREATE_HEARING)
            .caseData(caseData)
            .caseState(State.READY_TO_LIST)
            .build();

        request = HearingRequest
                .builder(String.valueOf(CASE_ID))
                .hearingState(CREATE_HEARING)
                .hearingRoute(LIST_ASSIST)
                .build();

        expectedCaseDetails = SscsCaseDetails.builder()
            .data(SscsCaseData.builder()
                .ccdCaseId(String.valueOf(CASE_ID))
                .build())
            .build();
    }

    @DisplayName("When wrapper with a valid Hearing State is given addHearingResponse should run without error")
    @ParameterizedTest
    @EnumSource(
        value = HearingState.class,
        names = {"UPDATED_CASE","PARTY_NOTIFIED"})
    void processHearingRequest(HearingState state) {
        given(ccdCaseService.getStartEventResponse(eq(CASE_ID), any())).willReturn(expectedCaseDetails);

        request.setHearingState(state);
        assertThatNoException()
                .isThrownBy(() -> hearingsService.processHearingRequest(request));
    }

    @DisplayName("When wrapper with a valid Hearing State and Cancellation reason is given addHearingResponse should run without error")
    @Test
    void processHearingRequest() {
        given(ccdCaseService.getStartEventResponse(eq(CASE_ID), any())).willReturn(expectedCaseDetails);

        request.setHearingState(UPDATED_CASE);
        request.setCancellationReason(OTHER);
        assertThatNoException()
            .isThrownBy(() -> hearingsService.processHearingRequest(request));
    }

    @DisplayName("When wrapper with a invalid Hearing State is given addHearingResponse should throw an Unhandled HearingState error")
    @ParameterizedTest
    @NullSource
    void processHearingRequestInvalidState(HearingState state) {
        request.setHearingState(state);

        UnhandleableHearingStateException thrown = assertThrows(UnhandleableHearingStateException.class, () -> hearingsService.processHearingRequest(request));

        assertThat(thrown.getMessage()).isNotEmpty();
    }

    @DisplayName("When wrapper with a valid adjourn create Hearing State is given addHearingResponse should run without error")
    @Test
    void processHearingWrapperAdjournmentCreate() throws ListingException {
        mockHearingResponseForAdjournmentCreate();

        HearingEvent hearingEvent = HearingEvent.ADJOURN_CREATE_HEARING;
        wrapper.setHearingState(ADJOURN_CREATE_HEARING);
        wrapper.setEventId(hearingEvent.getEventType().getCcdType());
        wrapper.getCaseData().getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        var panelComposition = PanelMemberComposition.builder().panelCompositionJudge("58").build();
        when(hearingServiceConsumer.getCreateHearingCaseDetailsConsumerV2(
                eq(panelComposition), any(), any(), anyBoolean())
        ).thenReturn(sscsCaseDetailsConsumer);
        var hearingPayload = HearingRequestPayload.builder()
                .hearingDetails(uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDetails.builder()
                        .panelRequirements(PanelRequirements.builder().roleTypes(List.of("58"))
                                .build()).build()).build();
        when(hearingsMapping.buildHearingPayload(any(), any())).thenReturn(hearingPayload);
        when(panelCompositionService.createPanelCompositionFromJohTiers(eq(List.of("58"))))
                .thenReturn(panelComposition);

        assertThatNoException()
                .isThrownBy(() -> hearingsService.processHearingWrapper(wrapper));

        verify(updateCcdCaseService).updateCaseV2(
                eq(CASE_ID),
                eq(hearingEvent.getEventType().getCcdType()),
                eq(hearingEvent.getSummary()),
                eq(hearingEvent.getDescription()),
                any(),
                caseDataConsumerCaptor.capture()
        );
        SscsCaseData caseData = wrapper.getCaseData();
        assertNull(caseData.getHearings());
        assertEquals(caseData.getPanelMemberComposition(), panelComposition);

        Consumer<SscsCaseDetails> sscsCaseDataConsumer = caseDataConsumerCaptor.getValue();
        assertThat(sscsCaseDataConsumer).isEqualTo(sscsCaseDetailsConsumer);
    }

    @Test
    void shouldThrowUpdateCaseExceptionWhenCaseUpdateWithHearingResponseV2Fails() throws ListingException {
        mockHearingResponseForAdjournmentCreate();
        wrapper.getCaseData().getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        HearingEvent event = HearingEvent.ADJOURN_CREATE_HEARING;
        wrapper.setHearingState(ADJOURN_CREATE_HEARING);
        wrapper.setEventId(event.getEventType().getCcdType());
        Request request =
                Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        var hearingPayload = HearingRequestPayload.builder()
                .hearingDetails(uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDetails.builder()
                        .panelRequirements(PanelRequirements.builder().roleTypes(List.of("58"))
                                .build()).build()).build();
        when(hearingsMapping.buildHearingPayload(any(), any())).thenReturn(hearingPayload);
        when(panelCompositionService.createPanelCompositionFromJohTiers(eq(List.of("58"))))
                .thenReturn(PanelMemberComposition.builder().build());
        given(updateCcdCaseService.updateCaseV2(
            eq(CASE_ID),
            eq(event.getEventType().getCcdType()),
            eq(event.getSummary()),
            eq(event.getDescription()),
            any(),
            any()
        )).willThrow(new FeignException.InternalServerError("test error", request, null, null));

        assertThatExceptionOfType(UpdateCaseException.class).isThrownBy(
            () -> hearingsService.processHearingWrapper(wrapper));
    }

    private void mockHearingResponseForAdjournmentCreate() {

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE, ISSUE_CODE, false, false))
            .willReturn(new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                                               false, false, SessionCategory.CATEGORY_03, null));

        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);
        given(refData.getVenueService()).willReturn(venueService);

        given(venueService.getEpimsIdForVenue(PROCESSING_VENUE)).willReturn("219164");

        given(hmcHearingApiService.sendCreateHearingRequest(any()))
                .willReturn(HmcUpdateResponse.builder().hearingRequestId(123L).versionNumber(1234L).status(HmcStatus.HEARING_REQUESTED).build());

        given(hmcHearingApiService.getHearingsRequest(anyString(), eq(null)))
            .willReturn(HearingsGetResponse.builder().build());
    }

    @DisplayName("When wrapper with a valid create Hearing State is given addHearingResponse should run without error")
    @Test
    void processHearingWrapperCreate() throws ListingException {
        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE,ISSUE_CODE,false,false))
            .willReturn(new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                false,false,SessionCategory.CATEGORY_03,null));
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);
        given(refData.getVenueService()).willReturn(venueService);
        given(venueService.getEpimsIdForVenue(PROCESSING_VENUE)).willReturn("219164");
        given(hmcHearingApiService.sendCreateHearingRequest(any()))
                .willReturn(HmcUpdateResponse.builder().build());
        given(hmcHearingApiService.getHearingsRequest(anyString(),eq(null)))
            .willReturn(HearingsGetResponse.builder().build());
        wrapper.setHearingState(CREATE_HEARING);
        var hearingPayload = HearingRequestPayload.builder()
                .hearingDetails(uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDetails.builder()
                        .panelRequirements(PanelRequirements.builder().roleTypes(List.of("58"))
                                .build()).build()).build();
        when(hearingsMapping.buildHearingPayload(any(), any())).thenReturn(hearingPayload);
        when(panelCompositionService.createPanelCompositionFromJohTiers(eq(List.of("58"))))
                .thenReturn(PanelMemberComposition.builder().build());

        assertThatNoException()
            .isThrownBy(() -> hearingsService.processHearingWrapper(wrapper));
    }

    @DisplayName("When create Hearing is given and there is already a hearing requested/awaiting listing addHearingResponse should run without error")
    @Test
    void processHearingWrapperCreateExistingHearing() throws GetHearingException {
        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE,ISSUE_CODE,false,false))
                .willReturn(new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                        false,false,SessionCategory.CATEGORY_03,null));
        given(refData.getVenueService()).willReturn(venueService);
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);

        var details = uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDetails.builder().build();
        RequestDetails requestDetails = RequestDetails.builder().versionNumber(2L).build();
        HearingGetResponse hearingGetResponse = HearingGetResponse.builder()
            .hearingDetails(details)
            .requestDetails(requestDetails)
            .caseDetails(CaseDetails.builder().build())
            .partyDetails(List.of())
            .hearingResponse(HearingResponse.builder().build())
            .build();
        given(hmcHearingApiService.getHearingRequest(anyString())).willReturn(hearingGetResponse);
        HearingsGetResponse hearingsGetResponse = HearingsGetResponse.builder()
            .caseHearings(List.of(CaseHearing.builder()
                .hearingId(HEARING_REQUEST_ID)
                .hmcStatus(HmcStatus.HEARING_REQUESTED)
                .requestVersion(1L)
                .build()))
            .build();

        given(hmcHearingApiService.getHearingsRequest(anyString(),eq(null)))
            .willReturn(hearingsGetResponse);

        wrapper.setHearingState(CREATE_HEARING);

        assertThatNoException()
            .isThrownBy(() -> hearingsService.processHearingWrapper(wrapper));
    }

    @Test
    void processHearingWrapperCreateExistingHearingWhenHearingDoesntExists() throws GetHearingException {
        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE,ISSUE_CODE,false,false))
            .willReturn(new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                                               false,false,SessionCategory.CATEGORY_03,null));
        given(refData.getVenueService()).willReturn(venueService);
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);
        given(hmcHearingApiService.getHearingRequest(anyString())).willThrow(new GetHearingException(""));
        HearingsGetResponse hearingsGetResponse = HearingsGetResponse.builder()
            .caseHearings(List.of(CaseHearing.builder()
                                      .hearingId(HEARING_REQUEST_ID)
                                      .hmcStatus(HmcStatus.HEARING_REQUESTED)
                                      .requestVersion(1L)
                                      .build()))
            .build();

        given(hmcHearingApiService.getHearingsRequest(anyString(),eq(null)))
            .willReturn(hearingsGetResponse);

        wrapper.setHearingState(CREATE_HEARING);

        assertThatNoException()
            .isThrownBy(() -> hearingsService.processHearingWrapper(wrapper));
    }

    @DisplayName("When wrapper with a valid create Hearing State is given addHearingResponse should run without error")
    @Test
    void processHearingWrapperUpdate() throws ListingException {
        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE,ISSUE_CODE,false,false))
            .willReturn(new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                false,false,SessionCategory.CATEGORY_03,null));

        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);

        given(venueService.getEpimsIdForVenue(PROCESSING_VENUE)).willReturn("219164");

        given(refData.getVenueService()).willReturn(venueService);

        given(hearingsMapping.buildHearingPayload(any(), any())).willReturn(HearingRequestPayload.builder().build());

        given(hmcHearingApiService.sendUpdateHearingRequest(any(HearingRequestPayload.class), anyString()))
                .willReturn(HmcUpdateResponse.builder().build());

        wrapper.setHearingState(UPDATE_HEARING);
        wrapper.getCaseData()
            .setHearings(new ArrayList<>(Collections.singletonList(Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingId(String.valueOf(HEARING_REQUEST_ID))
                    .build())
                .build())));

        assertThatNoException()
            .isThrownBy(() -> hearingsService.processHearingWrapper(wrapper));
    }

    @DisplayName("When wrapper with a valid cancel Hearing State is given addHearingResponse should run without error")
    @Test
    void processHearingWrapperCancel() {
        given(hmcHearingApiService.sendCancelHearingRequest(any(HearingCancelRequestPayload.class), anyString()))
                .willReturn(HmcUpdateResponse.builder().build());

        wrapper.setHearingState(CANCEL_HEARING);
        wrapper.getCaseData()
            .setHearings(Collections.singletonList(Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingId(String.valueOf(HEARING_REQUEST_ID))
                    .build())
                .build()));
        wrapper.setCancellationReasons(List.of(OTHER));

        assertThatNoException().isThrownBy(() -> hearingsService.processHearingWrapper(wrapper));
    }

    @DisplayName("When wrapper with a valid create Hearing State is given but hearing duration is not multiple of five then send to listing error")
    @ParameterizedTest
    @CsvSource(value = {
        "31",
        "32",
        "33",
        "34",
    })
    void testGetServiceHearingValueWithListingDurationNotMultipleOfFive(Integer hearingDuration) throws Exception {

        wrapper.setHearingState(UPDATE_HEARING);
        wrapper.getCaseData().getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().duration(hearingDuration).build());

        assertThrows(ListingException.class, () -> hearingsService.processHearingWrapper(wrapper));
    }

    @DisplayName("When wrapper with a valid create Hearing State is given and hearing duration is multiple of five or null then updateHearing should run without error")
    @ParameterizedTest
    @CsvSource(value = {
        "null",
        "25",
        "60"
    }, nullValues = "null")
    void testGetServiceHearingValueWithListingDurationAsNullOrMultipleOfFive(Integer hearingDuration) throws Exception {
        ReflectionTestUtils.setField(hearingsService, "isHearingDurationEnabled", true);
        given(hearingsMapping.buildHearingPayload(any(), any())).willReturn(HearingRequestPayload.builder().build());

        given(hmcHearingApiService.sendUpdateHearingRequest(any(HearingRequestPayload.class), anyString()))
                .willReturn(HmcUpdateResponse.builder().build());

        wrapper.setHearingState(UPDATE_HEARING);
        wrapper.getCaseData()
                .setHearings(Collections.singletonList(Hearing.builder()
                        .value(HearingDetails.builder()
                                .hearingId(String.valueOf(HEARING_REQUEST_ID))
                                .build())
                        .build()));
        wrapper.getCaseData().getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().duration(hearingDuration).build());


        assertThatNoException().isThrownBy(() -> hearingsService.processHearingWrapper(wrapper));
    }


}
