package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.ADJOURN_CREATE_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CANCEL_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CREATE_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.UPDATED_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.UPDATE_HEARING;
import static uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason.OTHER;

import java.util.ArrayList;
import java.util.Collections;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.exception.GetHearingException;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.exception.UnhandleableHearingStateException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
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
import uk.gov.hmcts.reform.sscs.model.single.hearing.RequestDetails;
import uk.gov.hmcts.reform.sscs.reference.data.model.SessionCategoryMap;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@ExtendWith(MockitoExtension.class)
class HearingsServiceV1Test {
    private static final long HEARING_REQUEST_ID = 12345;
    private static final long CASE_ID = 1625080769409918L;
    private static final String BENEFIT_CODE = "002";
    private static final String ISSUE_CODE = "DD";
    private static final String PROCESSING_VENUE = "Processing Venue";


    private HearingWrapper wrapper;
    private HearingRequest hearingRequest;
    private SscsCaseDetails expectedCaseDetails;
    private SscsCaseDetails sscsCaseDetails;

    @Mock
    private HmcHearingApiService hmcHearingApiService;

    @Mock
    private HmcHearingsApiService hmcHearingsApiService;

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
    private HearingServiceConsumer hearingServiceConsumer;

    @Mock
    private Consumer<SscsCaseDetails> sscsCaseDetailsConsumer;

    @Mock
    private Consumer<SscsCaseData> sscsCaseDataConsumer;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> caseDataConsumerCaptor;

    @InjectMocks
    private HearingsServiceV1 hearingsService;

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

        hearingRequest = HearingRequest
                .builder(String.valueOf(CASE_ID))
                .hearingState(CREATE_HEARING)
                .hearingRoute(LIST_ASSIST)
                .build();

        expectedCaseDetails = SscsCaseDetails.builder()
            .data(SscsCaseData.builder()
                .ccdCaseId(String.valueOf(CASE_ID))
                .build())
            .build();

        sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();
    }

    @DisplayName("When wrapper with a valid Hearing State is given addHearingResponse should run without error")
    @ParameterizedTest
    @EnumSource(
        value = HearingState.class,
        names = {"UPDATED_CASE","PARTY_NOTIFIED"})
    void processHearingRequest(HearingState state) {
        given(ccdCaseService.getStartEventResponse(eq(CASE_ID), any())).willReturn(expectedCaseDetails);

        hearingRequest.setHearingState(state);
        assertThatNoException()
                .isThrownBy(() -> hearingsService.processHearingRequest(hearingRequest));
    }

    @DisplayName("When wrapper with a valid Hearing State and Cancellation reason is given addHearingResponse should run without error")
    @Test
    void processHearingRequest() {
        given(ccdCaseService.getStartEventResponse(eq(CASE_ID), any())).willReturn(expectedCaseDetails);

        hearingRequest.setHearingState(UPDATED_CASE);
        hearingRequest.setCancellationReason(OTHER);
        assertThatNoException()
            .isThrownBy(() -> hearingsService.processHearingRequest(hearingRequest));
    }

    @DisplayName("When wrapper with a invalid Hearing State is given addHearingResponse should throw an Unhandled HearingState error")
    @ParameterizedTest
    @NullSource
    void processHearingRequestInvalidState(HearingState state) {
        hearingRequest.setHearingState(state);

        UnhandleableHearingStateException thrown = assertThrows(UnhandleableHearingStateException.class, () -> hearingsService.processHearingRequest(hearingRequest));

        assertThat(thrown.getMessage()).isNotEmpty();
    }

    @DisplayName("When wrapper with a case in an invalid case state is given should run without error")
    @Test
    void processHearingWrapperInvalidState() {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId(String.valueOf(CASE_ID))
            .build();
        wrapper.setHearingState(CREATE_HEARING);
        wrapper.setCaseData(caseData);
        for (State invalidState : HearingsServiceV1.INVALID_CASE_STATES) {
            wrapper.setCaseState(invalidState);
            assertThatNoException()
                .isThrownBy(() -> hearingsService.processHearingRequest(hearingRequest));
        }
    }

    @DisplayName("When wrapper with a valid adjourn create Hearing State is given addHearingResponse should run without error")
    @Test
    void processHearingWrapperAdjournmentCreate() throws UpdateCaseException {
        mockHearingResponseForAdjournmentCreate();
        given(ccdCaseService.getStartEventResponse(eq(CASE_ID), any())).willReturn(sscsCaseDetails);

        HearingEvent hearingEvent = HearingEvent.ADJOURN_CREATE_HEARING;
        hearingRequest.setHearingState(ADJOURN_CREATE_HEARING);
        wrapper.setEventId(hearingEvent.getEventType().getCcdType());

        when(hearingServiceConsumer.getCreateHearingCaseDataConsumer(any(), any())).thenReturn(sscsCaseDataConsumer);


        assertThatNoException()
            .isThrownBy(() -> hearingsService.processHearingRequest(hearingRequest));

        verify(ccdCaseService).updateCaseData(
            eq(sscsCaseDetails.getData()),
            any(HearingWrapper.class),
            any(HearingEvent.class)
        );

    }

    private void mockHearingResponseForAdjournmentCreate() {
        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE, ISSUE_CODE, false, false))
            .willReturn(new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                                               false, false, SessionCategory.CATEGORY_03, null));

        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);
        given(refData.getVenueService()).willReturn(venueService);

        given(venueService.getEpimsIdForVenue(PROCESSING_VENUE)).willReturn("219164");

        given(hmcHearingApiService.sendCreateHearingRequest(any(HearingRequestPayload.class)))
            .willReturn(HmcUpdateResponse.builder().hearingRequestId(123L).versionNumber(1234L).status(HmcStatus.HEARING_REQUESTED).build());

        given(hmcHearingsApiService.getHearingsRequest(anyString(), eq(null)))
            .willReturn(HearingsGetResponse.builder().build());
    }


    @DisplayName("When wrapper with a valid create Hearing State is given addHearingResponse should run without error")
    @Test
    void processHearingWrapperCreate() {
        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE,ISSUE_CODE,false,false))
            .willReturn(new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                false,false,SessionCategory.CATEGORY_03,null));
        given(ccdCaseService.getStartEventResponse(eq(CASE_ID), any())).willReturn(sscsCaseDetails);
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);
        given(refData.getVenueService()).willReturn(venueService);

        given(venueService.getEpimsIdForVenue(PROCESSING_VENUE)).willReturn("219164");
        given(hearingServiceConsumer.getCreateHearingCaseDataConsumer(any(), any())).willReturn(sscsCaseDataConsumer);

        given(hmcHearingApiService.sendCreateHearingRequest(any(HearingRequestPayload.class)))
                .willReturn(HmcUpdateResponse.builder().build());

        given(hmcHearingsApiService.getHearingsRequest(anyString(),eq(null)))
            .willReturn(HearingsGetResponse.builder().build());

        hearingRequest.setHearingState(CREATE_HEARING);

        assertThatNoException()
            .isThrownBy(() -> hearingsService.processHearingRequest(hearingRequest));
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
        given(hearingServiceConsumer.getCreateHearingCaseDataConsumer(any(), any())).willReturn(sscsCaseDataConsumer);
        var details = uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDetails.builder().build();
        RequestDetails requestDetails = RequestDetails.builder().versionNumber(2L).build();
        HearingGetResponse hearingGetResponse = HearingGetResponse.builder()
            .hearingDetails(details)
            .requestDetails(requestDetails)
            .caseDetails(CaseDetails.builder().build())
            .partyDetails(List.of())
            .hearingResponse(HearingResponse.builder().build())
            .build();
        given(ccdCaseService.getStartEventResponse(eq(CASE_ID), any())).willReturn(sscsCaseDetails);
        given(hmcHearingApiService.getHearingRequest(anyString())).willReturn(hearingGetResponse);
        HearingsGetResponse hearingsGetResponse = HearingsGetResponse.builder()
            .caseHearings(List.of(CaseHearing.builder()
                .hearingId(HEARING_REQUEST_ID)
                .hmcStatus(HmcStatus.HEARING_REQUESTED)
                .requestVersion(1L)
                .build()))
            .build();

        given(hmcHearingsApiService.getHearingsRequest(anyString(),eq(null)))
            .willReturn(hearingsGetResponse);

        wrapper.setHearingState(CREATE_HEARING);

        assertThatNoException()
            .isThrownBy(() -> hearingsService.processHearingRequest(hearingRequest));
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
        given(hearingServiceConsumer.getCreateHearingCaseDataConsumer(any(), any())).willReturn(sscsCaseDataConsumer);
        given(ccdCaseService.getStartEventResponse(eq(CASE_ID), any())).willReturn(sscsCaseDetails);
        HearingsGetResponse hearingsGetResponse = HearingsGetResponse.builder()
            .caseHearings(List.of(CaseHearing.builder()
                                      .hearingId(HEARING_REQUEST_ID)
                                      .hmcStatus(HmcStatus.HEARING_REQUESTED)
                                      .requestVersion(1L)
                                      .build()))
            .build();

        given(hmcHearingsApiService.getHearingsRequest(anyString(),eq(null)))
            .willReturn(hearingsGetResponse);

        hearingRequest.setHearingState(CREATE_HEARING);

        assertThatNoException()
            .isThrownBy(() -> hearingsService.processHearingRequest(hearingRequest));
    }

    @DisplayName("When wrapper with a valid create Hearing State is given addHearingResponse should run without error")
    @Test
    void processHearingWrapperUpdate() {
        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE,ISSUE_CODE,false,false))
            .willReturn(new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                false,false,SessionCategory.CATEGORY_03,null));

        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);

        given(venueService.getEpimsIdForVenue(PROCESSING_VENUE)).willReturn("219164");

        given(refData.getVenueService()).willReturn(venueService);

        given(ccdCaseService.getStartEventResponse(eq(CASE_ID), any())).willReturn(sscsCaseDetails);

        given(hmcHearingApiService.sendUpdateHearingRequest(any(HearingRequestPayload.class), anyString()))
                .willReturn(HmcUpdateResponse.builder().build());
        given(hearingServiceConsumer.getCreateHearingCaseDataConsumer(any(), any())).willReturn(sscsCaseDataConsumer);

        hearingRequest.setHearingState(UPDATE_HEARING);
        wrapper.getCaseData()
            .setHearings(new ArrayList<>(Collections.singletonList(Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingId(String.valueOf(HEARING_REQUEST_ID))
                    .build())
                .build())));

        assertThatNoException()
            .isThrownBy(() -> hearingsService.processHearingRequest(hearingRequest));
    }

    @DisplayName("When wrapper with a valid cancel Hearing State is given addHearingResponse should run without error")
    @Test
    void processHearingWrapperCancel() {
        given(hmcHearingApiService.sendCancelHearingRequest(any(HearingCancelRequestPayload.class), anyString()))
                .willReturn(HmcUpdateResponse.builder().build());
        given(ccdCaseService.getStartEventResponse(eq(CASE_ID), any())).willReturn(sscsCaseDetails);
        hearingRequest.setHearingState(CANCEL_HEARING);
        wrapper.getCaseData()
            .setHearings(Collections.singletonList(Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingId(String.valueOf(HEARING_REQUEST_ID))
                    .build())
                .build()));
        wrapper.setCancellationReasons(List.of(OTHER));

        assertThatNoException().isThrownBy(() -> hearingsService.processHearingRequest(hearingRequest));
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

        given(ccdCaseService.getStartEventResponse(eq(CASE_ID), any())).willReturn(sscsCaseDetails);
        hearingRequest.setHearingState(UPDATE_HEARING);
        wrapper.getCaseData().getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().duration(hearingDuration).build());

        assertThrows(ListingException.class, () -> hearingsService.processHearingRequest(hearingRequest));
    }

}
