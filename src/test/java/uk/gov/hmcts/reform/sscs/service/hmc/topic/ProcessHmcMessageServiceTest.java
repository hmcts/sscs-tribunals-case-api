package uk.gov.hmcts.reform.sscs.service.hmc.topic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.UNKNOWN;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.ADJOURNED;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.CANCELLED;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.EXCEPTION;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.HEARING_REQUESTED;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.LISTED;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.ListingStatus.FIXED;
import static uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason.WITHDRAWN;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HearingUpdate;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HmcMessage;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.ListingStatus;
import uk.gov.hmcts.reform.sscs.model.single.hearing.CaseDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.RequestDetails;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.CcdCaseService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;

@ExtendWith(MockitoExtension.class)
class ProcessHmcMessageServiceTest {

    public static final String HEARING_ID = "abcdef";
    public static final long CASE_ID = 123L;

    @Mock
    private HmcHearingApiService hmcHearingApiService;

    @Mock
    private CcdCaseService ccdCaseService;

    @Mock
    private HearingUpdateService hearingUpdateService;

    @InjectMocks
    private ProcessHmcMessageService processHmcMessageService;

    private SscsCaseDetails sscsCaseDetails;
    private SscsCaseData caseData;
    private HearingGetResponse hearingGetResponse;
    private HmcMessage hmcMessage;

    @BeforeEach
    void setUp() {
        hearingGetResponse = HearingGetResponse.builder()
                .requestDetails(RequestDetails.builder().build())
                .hearingDetails(HearingDetails.builder().build())
                .caseDetails(CaseDetails.builder().build())
                .partyDetails(new ArrayList<>())
                .hearingResponse(HearingResponse.builder().build())
                .build();

        caseData = SscsCaseData.builder()
                .state(UNKNOWN)
                .ccdCaseId(String.valueOf(CASE_ID))
                .build();

        sscsCaseDetails = SscsCaseDetails.builder()
                .data(caseData)
                .build();

        hmcMessage = HmcMessage.builder()
                .hmctsServiceCode("BBA3")
                .caseId(CASE_ID)
                .hearingId(HEARING_ID)
                .hearingUpdate(HearingUpdate.builder()
                        .hmcStatus(ADJOURNED)
                        .build())
                .build();
    }

    @DisplayName("When listing Status is Fixed and and HmcStatus is valid, "
            + "updateHearing and updateCaseData are called once")
    @ParameterizedTest
    @EnumSource(value = HmcStatus.class, names = {"LISTED", "AWAITING_LISTING", "UPDATE_SUBMITTED"})
    void testUpdateHearing(HmcStatus hmcStatus) throws Exception {
        // given

        hearingGetResponse.getRequestDetails().setStatus(hmcStatus);
        hearingGetResponse.getHearingResponse().setListingStatus(FIXED);

        hmcMessage.getHearingUpdate().setHmcStatus(hmcStatus);

        given(hmcHearingApiService.getHearingRequest(HEARING_ID))
                .willReturn(hearingGetResponse);

        given(ccdCaseService.getCaseDetails(CASE_ID))
                .willReturn(sscsCaseDetails);

        // when
        processHmcMessageService.processEventMessage(hmcMessage);

        // then
        verifyUpdateCaseDataCalledCorrectlyForHmcStatus(caseData, hmcStatus);
        verify(hearingUpdateService).updateHearing(hearingGetResponse, caseData);
    }

    @DisplayName("When listing Status is Cancelled and and HmcStatus is valid, "
        + "updateHearing and updateCaseData are called")
    @ParameterizedTest
    @EnumSource(value = HmcStatus.class, names = {"LISTED", "AWAITING_LISTING", "UPDATE_SUBMITTED"})
    void testUpdateHearingListingStatusCancelled(HmcStatus hmcStatus) throws Exception {
        // given

        hearingGetResponse.getRequestDetails().setStatus(hmcStatus);
        hearingGetResponse.getHearingResponse().setListingStatus(ListingStatus.CNCL);

        hmcMessage.getHearingUpdate().setHmcStatus(hmcStatus);

        given(hmcHearingApiService.getHearingRequest(HEARING_ID))
            .willReturn(hearingGetResponse);

        given(ccdCaseService.getCaseDetails(CASE_ID))
            .willReturn(sscsCaseDetails);

        // when
        processHmcMessageService.processEventMessage(hmcMessage);

        // then
        verifyUpdateCaseDataCalledCorrectlyForHmcStatus(caseData, hmcStatus);
        verify(hearingUpdateService, never()).updateHearing(any(),any());
        verify(hearingUpdateService).setHearingStatus(HEARING_ID, caseData, hmcStatus);
        verify(hearingUpdateService).setWorkBasketFields(HEARING_ID, caseData, hmcStatus);
    }

    @Test
    void testReturnedStatusShouldUpdateDwpStateForCaseData() throws Exception {
        // given
        hearingGetResponse.getRequestDetails().setStatus(LISTED);
        hearingGetResponse.getHearingResponse().setListingStatus(FIXED);
        hmcMessage.getHearingUpdate().setHmcStatus(LISTED);

        given(hmcHearingApiService.getHearingRequest(HEARING_ID))
            .willReturn(hearingGetResponse);

        given(ccdCaseService.getCaseDetails(CASE_ID))
            .willReturn(sscsCaseDetails);

        given(hearingUpdateService.resolveDwpState(LISTED))
            .willReturn(DwpState.HEARING_DATE_ISSUED);

        // when
        processHmcMessageService.processEventMessage(hmcMessage);

        // then
        assertThat(sscsCaseDetails.getData().getDwpState()).isEqualTo(DwpState.HEARING_DATE_ISSUED);
    }

    @Test
    void testNoStatusShouldNotUpdateDwpStateForCaseData() throws Exception {
        // given
        hearingGetResponse.getRequestDetails().setStatus(LISTED);
        hearingGetResponse.getHearingResponse().setListingStatus(FIXED);
        hmcMessage.getHearingUpdate().setHmcStatus(LISTED);

        given(hmcHearingApiService.getHearingRequest(HEARING_ID))
            .willReturn(hearingGetResponse);

        given(ccdCaseService.getCaseDetails(CASE_ID))
            .willReturn(sscsCaseDetails);

        // when
        processHmcMessageService.processEventMessage(hmcMessage);

        // then
        assertThat(sscsCaseDetails.getData().getDwpState()).isNull();
    }

    @DisplayName("When listing Status is null or cannot be mapped, updateHearing and updateCaseData are not called")
    @Test
    void testUpdateHearingListingStatusNull() throws Exception {
        // given
        hearingGetResponse.getRequestDetails().setStatus(LISTED);
        hearingGetResponse.getHearingResponse().setListingStatus(null);
        hmcMessage.getHearingUpdate().setHmcStatus(LISTED);

        given(hmcHearingApiService.getHearingRequest(HEARING_ID))
            .willReturn(hearingGetResponse);

        // when
        processHmcMessageService.processEventMessage(hmcMessage);

        // then
        verify(ccdCaseService, never()).updateCaseData(any(),any(),any(),any());
        verify(hearingUpdateService, never()).updateHearing(any(), any());
    }

    @DisplayName("When listing Status is Draft or Provisional and and HmcStatus is valid, "
        + "updateHearing and updateCaseData are not called ")
    @ParameterizedTest
    @EnumSource(value = ListingStatus.class, names = {"DRAFT", "PROVISIONAL"})
    void testUpdateHearingDraftOrProvisional(ListingStatus listingStatus) throws Exception {
        // given
        hearingGetResponse.getRequestDetails().setStatus(LISTED);
        hearingGetResponse.getHearingResponse().setListingStatus(listingStatus);
        hmcMessage.getHearingUpdate().setHmcStatus(LISTED);

        given(hmcHearingApiService.getHearingRequest(HEARING_ID))
                .willReturn(hearingGetResponse);

        // when
        processHmcMessageService.processEventMessage(hmcMessage);

        // then
        verify(ccdCaseService, never()).updateCaseData(any(),any(),any(),any());
        verify(hearingUpdateService, never()).updateHearing(any(), any());
    }

    @DisplayName("When (non) Cancelled status given in but hearingCancellationReason is valid, "
        + "updateCancelled and updateCaseData are called")
    @ParameterizedTest
    @EnumSource(
        value = HmcStatus.class,
        mode = EnumSource.Mode.INCLUDE,
        names = {"CANCELLED", "UPDATE_SUBMITTED"})
    void testShouldSetCcdStateForCancelledHearingsCorrectly(HmcStatus status) throws Exception {
        // given
        hearingGetResponse.getRequestDetails().setStatus(status);
        hearingGetResponse.getRequestDetails().setCancellationReasonCodes(List.of(WITHDRAWN));
        hmcMessage.getHearingUpdate().setHmcStatus(status);

        given(hmcHearingApiService.getHearingRequest(HEARING_ID))
                .willReturn(hearingGetResponse);

        given(ccdCaseService.getCaseDetails(CASE_ID))
                .willReturn(sscsCaseDetails);

        // when
        processHmcMessageService.processEventMessage(hmcMessage);

        // then
        verifyUpdateCaseDataCalledCorrectlyForHmcStatus(caseData, status);
    }

    @DisplayName("Should update the case state to Dormant for correct cancellation reasons")
    @ParameterizedTest
    @EnumSource(
        value = CancellationReason.class,
        mode = EnumSource.Mode.INCLUDE,
        names = {"WITHDRAWN", "STRUCK_OUT", "LAPSED"})
    void testShouldUpdateCcdStateDormantForCancelledHearings(CancellationReason reason) throws Exception {
        // given
        hearingGetResponse.getRequestDetails().setStatus(CANCELLED);
        hearingGetResponse.getRequestDetails().setCancellationReasonCodes(List.of(reason));
        hmcMessage.getHearingUpdate().setHmcStatus(CANCELLED);

        given(hmcHearingApiService.getHearingRequest(HEARING_ID))
            .willReturn(hearingGetResponse);

        given(ccdCaseService.getCaseDetails(CASE_ID))
            .willReturn(sscsCaseDetails);

        // when
        processHmcMessageService.processEventMessage(hmcMessage);

        // then
        verifyUpdateCaseDataCalledCorrectlyForHmcStatus(caseData, CANCELLED);
    }

    @DisplayName("Should not update the case state to Dormant for wrong cancellation reasons")
    @ParameterizedTest
    @EnumSource(
        value = CancellationReason.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = {"WITHDRAWN", "STRUCK_OUT", "LAPSED"})
    void testShouldNotUpdateCcdStateForCancelledHearings(CancellationReason reason) throws Exception {
        // given
        hearingGetResponse.getRequestDetails().setStatus(CANCELLED);
        hearingGetResponse.getRequestDetails().setCancellationReasonCodes(List.of(reason));
        hmcMessage.getHearingUpdate().setHmcStatus(CANCELLED);

        given(hmcHearingApiService.getHearingRequest(HEARING_ID))
            .willReturn(hearingGetResponse);

        given(ccdCaseService.getCaseDetails(CASE_ID))
            .willReturn(sscsCaseDetails);

        // when
        processHmcMessageService.processEventMessage(hmcMessage);

        // then
        verifyUpdateCaseDataCalledCorrectlyForHmcStatus(caseData, CANCELLED);
    }

    @DisplayName("When no cancellation reason is given but status is Cancelled, "
            + "updateCancelled and updateCaseData are not called")
    @Test
    void testUpdateCancelledNullReason() throws Exception {
        // given
        hearingGetResponse.getRequestDetails().setStatus(CANCELLED);
        hmcMessage.getHearingUpdate().setHmcStatus(CANCELLED);

        given(hmcHearingApiService.getHearingRequest(HEARING_ID))
                .willReturn(hearingGetResponse);

        given(ccdCaseService.getCaseDetails(CASE_ID))
                .willReturn(sscsCaseDetails);

        // when
        processHmcMessageService.processEventMessage(hmcMessage);

        // then
        verifyUpdateCaseDataCalledCorrectlyForHmcStatus(caseData, CANCELLED);

    }

    @DisplayName("When HmcStatus is Exception updateFailed and updateCaseData are called")
    @Test
    void testUpdateCancelledInvalidStatusNullReason() throws Exception {
        // given
        hearingGetResponse.getRequestDetails().setStatus(EXCEPTION);
        hmcMessage.getHearingUpdate().setHmcStatus(EXCEPTION);

        given(hmcHearingApiService.getHearingRequest(HEARING_ID))
                .willReturn(hearingGetResponse);

        given(ccdCaseService.getCaseDetails(CASE_ID))
                .willReturn(sscsCaseDetails);

        // when
        processHmcMessageService.processEventMessage(hmcMessage);

        // then
        verifyUpdateCaseDataCalledCorrectlyForHmcStatus(caseData, EXCEPTION);
    }

    @DisplayName("When HmcStatus is Exception updateFailed and updateCaseData are called")
    @Test
    void testHmcStatusWithNoEventMapperShouldNotUpdateCaseData() throws Exception {
        // given
        hearingGetResponse.getRequestDetails().setStatus(HEARING_REQUESTED);
        hmcMessage.getHearingUpdate().setHmcStatus(HEARING_REQUESTED);

        given(hmcHearingApiService.getHearingRequest(HEARING_ID))
            .willReturn(hearingGetResponse);

        // when
        processHmcMessageService.processEventMessage(hmcMessage);

        // then
        verify(ccdCaseService, never()).updateCaseData(any(),any(),any(),any());
    }

    @DisplayName("When not listed, updated, canceled or exception nothing is called")
    @ParameterizedTest
    @EnumSource(
            value = HmcStatus.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"LISTED", "UPDATE_SUBMITTED", "CANCELLED", "EXCEPTION"})
    void testProcessEventMessageInvalidHmcStatus(HmcStatus value) throws Exception {
        // given
        hearingGetResponse.getRequestDetails().setStatus(value);

        hmcMessage.getHearingUpdate().setHmcStatus(value);

        given(hmcHearingApiService.getHearingRequest(HEARING_ID))
                .willReturn(hearingGetResponse);

        // when
        processHmcMessageService.processEventMessage(hmcMessage);

        // then
        verify(ccdCaseService, never()).updateCaseData(any(),any(),any(),any());
    }

    private void verifyUpdateCaseDataCalledCorrectlyForHmcStatus(SscsCaseData caseData, HmcStatus hmcStatus) throws UpdateCaseException {
        String ccdUpdateDescription = String.format(hmcStatus.getCcdUpdateDescription(), HEARING_ID);
        verify(ccdCaseService, times(1))
                .updateCaseData(caseData,
                        hmcStatus.getEventMapper().apply(hearingGetResponse, caseData),
                        hmcStatus.getCcdUpdateSummary(),
                        ccdUpdateDescription);
    }

}
