package uk.gov.hmcts.reform.sscs.service.hearings;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitCode;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.Issue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SessionCategory;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.reference.data.model.SessionCategoryMap;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingsApiService;
import uk.gov.hmcts.reform.sscs.service.VenueService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UpdateHearingCaseUpdaterTest extends HearingSaveActionBaseTest {

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
    private UpdateHearingCaseUpdater updateHearingCaseUpdater;

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

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());
        given(ccdClient.startEvent(any(), anyLong(), eq(EventType.UPDATE_HEARING_TYPE.getCcdType())))
            .willReturn(StartEventResponse.builder().build());
    }

    @Test
    void updateHearingAndCase() {
        given(hmcHearingApiService.sendUpdateHearingRequest(any(HearingRequestPayload.class), anyString()))
            .willReturn(HmcUpdateResponse.builder().hearingRequestId(HEARING_REQUEST_ID).versionNumber(2L).build());

        SscsCaseDetails sscsCaseDetails = createCaseDataWithHearings();

        given(sscsCcdConvertService.getCaseDetails(any(StartEventResponse.class)))
            .willReturn(sscsCaseDetails);

        HearingRequest hearingRequest = createHearingRequestForState(HearingState.UPDATE_HEARING);

        assertDoesNotThrow(() -> updateHearingCaseUpdater.updateHearingAndCase(hearingRequest));

        verify(hmcHearingApiService).sendUpdateHearingRequest(
            any(HearingRequestPayload.class), eq(String.valueOf(HEARING_REQUEST_ID)));
        verifyNoMoreInteractions(hmcHearingsApiService, hmcHearingApiService);
    }

    @Test
    void shouldThrowFeignExceptionOnupdateHearingAndCase() throws ListingException {
        given(hmcHearingApiService.sendUpdateHearingRequest(any(HearingRequestPayload.class), anyString()))
            .willReturn(HmcUpdateResponse.builder().hearingRequestId(HEARING_REQUEST_ID).versionNumber(2L).build());

        SscsCaseDetails sscsCaseDetails = createCaseDataWithHearings();

        given(sscsCcdConvertService.getCaseDetails(any(StartEventResponse.class)))
            .willReturn(sscsCaseDetails);

        HearingRequest hearingRequest = createHearingRequestForState(HearingState.UPDATE_HEARING);
        UpdateHearingCaseUpdater spy = spy(updateHearingCaseUpdater);
        FeignException feignException = mock(FeignException.class);

        willThrow(feignException)
            .given(spy)
            .updateCase(any(), any(), any(), any());

        assertThatExceptionOfType(UpdateCaseException.class).isThrownBy(
                () -> spy.updateHearingAndCase(hearingRequest))
            .withMessageContaining("Failed to update case with Case id");
    }

    @DisplayName("When wrapper with a valid create Hearing State is given but hearing duration is not multiple of five then send to listing error")
    @ParameterizedTest
    @CsvSource(value = {
        "31",
        "32",
        "33",
        "34",
    })
    void processHearingMessageForUpdateHearingWithListingDurationNotMultipleOfFive(Integer hearingDuration) throws Exception {

        given(hmcHearingApiService.sendUpdateHearingRequest(any(HearingRequestPayload.class), anyString()))
            .willReturn(HmcUpdateResponse.builder().hearingRequestId(HEARING_REQUEST_ID).versionNumber(2L).build());

        SscsCaseDetails sscsCaseDetails = createCaseDataWithHearings();
        sscsCaseDetails.getData().getSchedulingAndListingFields()
            .setOverrideFields(OverrideFields.builder().duration(hearingDuration).build());

        HearingRequest hearingRequest = createHearingRequestForState(HearingState.UPDATE_HEARING);

        ListingException exception = assertThrows(
            ListingException.class,
            () -> updateHearingCaseUpdater.applyUpdate(sscsCaseDetails, hearingRequest)
        );

        assertEquals("Listing duration must be multiple of 5.0 minutes", exception.getMessage());
    }

}
