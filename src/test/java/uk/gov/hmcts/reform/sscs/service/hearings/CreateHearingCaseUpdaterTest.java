package uk.gov.hmcts.reform.sscs.service.hearings;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import feign.FeignException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitCode;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.Issue;
import uk.gov.hmcts.reform.sscs.ccd.domain.SessionCategory;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.CaseDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.RequestDetails;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.reference.data.model.SessionCategoryMap;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingsApiService;
import uk.gov.hmcts.reform.sscs.service.VenueService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreateHearingCaseUpdaterTest extends HearingSaveActionBaseTest {

    @Mock
    private HmcHearingApiService hmcHearingApiService;
    @Mock
    private HmcHearingsApiService hmcHearingsApiService;
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
    private CcdClient ccdClient;
    @Mock
    private SscsCcdConvertService sscsCcdConvertService;
    @InjectMocks
    private CreateHearingCaseUpdater createHearingCaseUpdater;

    @BeforeEach
    void setup() {
        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE, ISSUE_CODE, false, false))
            .willReturn(new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                                               false, false, SessionCategory.CATEGORY_03, null));
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);
        given(refData.getVenueService()).willReturn(venueService);
        given(refData.isAdjournmentFlagEnabled()).willReturn(true);
        given(venueService.getEpimsIdForVenue(PROCESSING_VENUE)).willReturn("219164");
    }

    @Test
    void shouldCreateWrapper() {
        HearingRequest hearingRequest = HearingRequest.internalBuilder()
            .hearingState(HearingState.ADJOURN_CREATE_HEARING)
            .hearingRoute(HearingRoute.LIST_ASSIST)
            .ccdCaseId(String.valueOf(CASE_ID))
            .cancellationReason(CancellationReason.PARTY_DID_NOT_ATTEND)
            .build();

        SscsCaseDetails caseDetails = SscsCaseDetails.builder()
            .data(SscsCaseData.builder().ccdCaseId(String.valueOf(CASE_ID)).build())
            .build();

        HearingWrapper hearingWrapper = createHearingCaseUpdater.createWrapper(hearingRequest, caseDetails);

        Assertions.assertEquals(String.valueOf(CASE_ID), hearingWrapper.getCaseData().getCcdCaseId());
        Assertions.assertEquals(CancellationReason.PARTY_DID_NOT_ATTEND.getHmcReference(),
            hearingWrapper.getCancellationReasons().get(0).getHmcReference());
        Assertions.assertEquals(HearingState.ADJOURN_CREATE_HEARING, hearingWrapper.getHearingState());
    }

    @Test
    void shouldApplyUpdatesForCreateHearingWhenNoHearingsExist() {
        given(hmcHearingApiService.sendCreateHearingRequest(any(HearingRequestPayload.class)))
            .willReturn(HmcUpdateResponse.builder().hearingRequestId(123L).versionNumber(1234L).status(HmcStatus.HEARING_REQUESTED).build());
        given(hmcHearingsApiService.getHearingsRequest(anyString(), eq(null)))
            .willReturn(HearingsGetResponse.builder().build());

        SscsCaseDetails sscsCaseDetails = createCaseData();
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());
        given(ccdClient.startEvent(any(), eq(CASE_ID), eq(EventType.ADD_HEARING.getCcdType())))
            .willReturn(StartEventResponse.builder().build());
        given(sscsCcdConvertService.getCaseDetails(any(StartEventResponse.class)))
            .willReturn(sscsCaseDetails);

        HearingRequest hearingRequest = createHearingRequestForState(HearingState.CREATE_HEARING);

        assertDoesNotThrow(() -> createHearingCaseUpdater.createHearingAndUpdateCase(hearingRequest));

        verify(hmcHearingsApiService).getHearingsRequest(String.valueOf(CASE_ID), null);
        verify(hmcHearingApiService).sendCreateHearingRequest(any(HearingRequestPayload.class));
    }

    @Test
    void shouldThrowUpdateCaseExceptionWhenCcdClientThrowsFeignException() throws ListingException {
        given(hmcHearingApiService.sendCreateHearingRequest(any(HearingRequestPayload.class)))
            .willReturn(HmcUpdateResponse.builder().hearingRequestId(123L).versionNumber(1234L).status(HmcStatus.HEARING_REQUESTED).build());
        given(hmcHearingsApiService.getHearingsRequest(anyString(), eq(null)))
            .willReturn(HearingsGetResponse.builder().build());

        SscsCaseDetails sscsCaseDetails = createCaseData();
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());
        given(ccdClient.startEvent(any(), eq(CASE_ID), eq(EventType.ADD_HEARING.getCcdType())))
            .willReturn(StartEventResponse.builder().build());
        given(sscsCcdConvertService.getCaseDetails(any(StartEventResponse.class)))
            .willReturn(sscsCaseDetails);
        given(ccdClient.submitEventForCaseworker(any(), anyLong(), any()))
            .willThrow(FeignException.class);

        HearingRequest hearingRequest = createHearingRequestForState(HearingState.CREATE_HEARING);

        assertThrows(
            UpdateCaseException.class,
            () -> createHearingCaseUpdater.createHearingAndUpdateCase(hearingRequest)
        );

    }

    @Test
    void shouldApplyUpdatesForCreateHearingWhenCaseDataHasExistingHearings() throws Exception {
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

        given(hmcHearingsApiService.getHearingsRequest(anyString(),eq(null)))
            .willReturn(hearingsGetResponse);

        SscsCaseDetails sscsCaseDetails = createCaseDataWithHearings();
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());
        given(ccdClient.startEvent(any(), eq(CASE_ID), eq(EventType.ADD_HEARING.getCcdType())))
            .willReturn(StartEventResponse.builder().build());
        given(sscsCcdConvertService.getCaseDetails(any(StartEventResponse.class)))
            .willReturn(sscsCaseDetails);

        HearingRequest hearingRequest = createHearingRequestForState(HearingState.CREATE_HEARING);

        assertDoesNotThrow(() -> createHearingCaseUpdater.createHearingAndUpdateCase(hearingRequest));

        verify(hmcHearingsApiService).getHearingsRequest(String.valueOf(CASE_ID), null);
        verify(hmcHearingApiService).getHearingRequest(String.valueOf(HEARING_REQUEST_ID));
        verifyNoMoreInteractions(hmcHearingApiService, hmcHearingsApiService);
    }

}
