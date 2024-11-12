package uk.gov.hmcts.reform.sscs.service.hmc.topic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.exception.CaseException;
import uk.gov.hmcts.reform.sscs.exception.GetCaseException;
import uk.gov.hmcts.reform.sscs.exception.MessageProcessingException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.helper.processing.ProcessHmcMessageHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
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
abstract class AbstractProcessHmcMessageServiceTest {

    public static final String HEARING_ID = "abcdef";
    public static final long CASE_ID = 123L;

    @Mock
    protected HmcHearingApiService hmcHearingApiService;

    @Mock
    protected CcdCaseService ccdCaseService;

    @Mock
    protected HearingUpdateService hearingUpdateService;

    @Mock
    protected UpdateCcdCaseService updateCcdCaseService;

    @Mock
    protected IdamService idamService;

    @Mock
    protected ProcessHmcMessageHelper processHmcMessageHelper;

    protected ProcessHmcMessageService processHmcMessageService;

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

    abstract void givenWillReturn(CcdCaseService ccdCaseService, UpdateCcdCaseService updateCcdCaseService, Long caseId, SscsCaseDetails sscsCaseDetails, IdamService idamService) throws GetCaseException;

    abstract void callProcessEventMessage(ProcessHmcMessageService processHmcMessageService, HmcMessage hmcMessage) throws CaseException, MessageProcessingException;

    abstract void assertThatCall(UpdateCcdCaseService updateCcdCaseService, SscsCaseDetails sscsCaseDetails, DwpState dwpState);

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

        given(processHmcMessageHelper.isHearingUpdated(hmcStatus, hearingGetResponse)).willReturn(true);

        givenWillReturn(ccdCaseService, updateCcdCaseService, CASE_ID, sscsCaseDetails, idamService);

        // when
        callProcessEventMessage(processHmcMessageService, hmcMessage);

        // then
        verifyUpdateCaseDataCalledCorrectlyForHmcStatus(ccdCaseService, updateCcdCaseService, caseData, hmcStatus, hearingGetResponse);
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

        givenWillReturn(ccdCaseService, updateCcdCaseService, CASE_ID, sscsCaseDetails, idamService);

        // when
        callProcessEventMessage(processHmcMessageService, hmcMessage);

        // then
        verifyUpdateCaseDataCalledCorrectlyForHmcStatus(ccdCaseService, updateCcdCaseService, caseData, hmcStatus, hearingGetResponse);
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

        givenWillReturn(ccdCaseService, updateCcdCaseService, CASE_ID, sscsCaseDetails, idamService);

        given(hearingUpdateService.resolveDwpState(LISTED))
            .willReturn(DwpState.HEARING_DATE_ISSUED);

        // when
        callProcessEventMessage(processHmcMessageService, hmcMessage);

        // then
        assertThatCall(updateCcdCaseService, sscsCaseDetails, DwpState.HEARING_DATE_ISSUED);
    }

    @Test
    void testNoStatusShouldNotUpdateDwpStateForCaseData() throws Exception {
        // given
        hearingGetResponse.getRequestDetails().setStatus(LISTED);
        hearingGetResponse.getHearingResponse().setListingStatus(FIXED);
        hmcMessage.getHearingUpdate().setHmcStatus(LISTED);

        given(hmcHearingApiService.getHearingRequest(HEARING_ID))
            .willReturn(hearingGetResponse);

        givenWillReturn(ccdCaseService, updateCcdCaseService, CASE_ID, sscsCaseDetails, idamService);

        // when
        callProcessEventMessage(processHmcMessageService, hmcMessage);

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

        given(processHmcMessageHelper.stateNotHandled(any(), any())).willReturn(true);

        // when
        callProcessEventMessage(processHmcMessageService, hmcMessage);

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

        given(processHmcMessageHelper.stateNotHandled(LISTED, hearingGetResponse)).willReturn(true);

        // when
        callProcessEventMessage(processHmcMessageService, hmcMessage);

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

        givenWillReturn(ccdCaseService, updateCcdCaseService, CASE_ID, sscsCaseDetails, idamService);

        // when
        callProcessEventMessage(processHmcMessageService, hmcMessage);

        // then
        verifyUpdateCaseDataCalledCorrectlyForHmcStatus(ccdCaseService, updateCcdCaseService, caseData, status, hearingGetResponse);
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

        givenWillReturn(ccdCaseService, updateCcdCaseService, CASE_ID, sscsCaseDetails, idamService);

        // when
        callProcessEventMessage(processHmcMessageService, hmcMessage);

        // then
        verifyUpdateCaseDataCalledCorrectlyForHmcStatus(ccdCaseService, updateCcdCaseService, caseData, CANCELLED, hearingGetResponse);
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

        givenWillReturn(ccdCaseService, updateCcdCaseService, CASE_ID, sscsCaseDetails, idamService);

        // when
        callProcessEventMessage(processHmcMessageService, hmcMessage);

        // then
        verifyUpdateCaseDataCalledCorrectlyForHmcStatus(ccdCaseService, updateCcdCaseService, caseData, CANCELLED, hearingGetResponse);
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

        givenWillReturn(ccdCaseService, updateCcdCaseService, CASE_ID, sscsCaseDetails, idamService);

        // when
        callProcessEventMessage(processHmcMessageService, hmcMessage);

        // then
        verifyUpdateCaseDataCalledCorrectlyForHmcStatus(ccdCaseService, updateCcdCaseService, caseData, CANCELLED, hearingGetResponse);

    }

    @DisplayName("When HmcStatus is Exception updateFailed and updateCaseData are called")
    @Test
    void testUpdateCancelledInvalidStatusNullReason() throws Exception {
        // given
        hearingGetResponse.getRequestDetails().setStatus(EXCEPTION);
        hmcMessage.getHearingUpdate().setHmcStatus(EXCEPTION);

        given(hmcHearingApiService.getHearingRequest(HEARING_ID))
                .willReturn(hearingGetResponse);

        givenWillReturn(ccdCaseService, updateCcdCaseService, CASE_ID, sscsCaseDetails, idamService);

        // when
        callProcessEventMessage(processHmcMessageService, hmcMessage);

        // then
        verifyUpdateCaseDataCalledCorrectlyForHmcStatus(ccdCaseService, updateCcdCaseService, caseData, EXCEPTION, hearingGetResponse);
    }

    @DisplayName("When HmcStatus is Exception updateFailed and updateCaseData are called")
    @Test
    void testHmcStatusWithNoEventMapperShouldNotUpdateCaseData() throws Exception {
        // given
        hearingGetResponse.getRequestDetails().setStatus(HEARING_REQUESTED);
        hmcMessage.getHearingUpdate().setHmcStatus(HEARING_REQUESTED);

        given(hmcHearingApiService.getHearingRequest(HEARING_ID))
            .willReturn(hearingGetResponse);

        given(processHmcMessageHelper.stateNotHandled(HEARING_REQUESTED, hearingGetResponse)).willReturn(true);

        // when
        callProcessEventMessage(processHmcMessageService, hmcMessage);

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

        given(processHmcMessageHelper.stateNotHandled(value, hearingGetResponse)).willReturn(true);

        // when
        callProcessEventMessage(processHmcMessageService, hmcMessage);

        // then
        verify(ccdCaseService, never()).updateCaseData(any(),any(),any(),any());
    }

    abstract void verifyUpdateCaseDataCalledCorrectlyForHmcStatus(CcdCaseService ccdCaseService, UpdateCcdCaseService updateCcdCaseService,
                                                                  SscsCaseData caseData, HmcStatus hmcStatus, HearingGetResponse hearingGetResponse) throws UpdateCaseException;

}
