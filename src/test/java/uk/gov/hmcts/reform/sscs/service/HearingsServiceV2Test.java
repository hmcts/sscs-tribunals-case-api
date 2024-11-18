package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CASE_UPDATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.ADJOURN_CREATE_HEARING;
import static uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason.SETTLED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.ExhaustedRetryException;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseManagementLocation;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.exception.UnhandleableHearingStateException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingCancelRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.service.hearings.AdjournCreateHearingCaseUpdater;
import uk.gov.hmcts.reform.sscs.service.hearings.CreateHearingCaseUpdater;
import uk.gov.hmcts.reform.sscs.service.hearings.UpdateHearingCaseUpdater;

@ExtendWith(MockitoExtension.class)
class HearingsServiceV2Test {

    @Mock
    private HmcHearingApiService hmcHearingApiService;
    @Mock
    private CcdCaseService ccdCaseService;
    @Mock
    private CreateHearingCaseUpdater createHearingCaseUpdater;
    @Mock
    private AdjournCreateHearingCaseUpdater adjournCreateHearingCaseUpdater;
    @Mock
    private UpdateHearingCaseUpdater updateHearingCaseUpdater;
    private HearingsServiceV2 hearingsService;

    private static final long CASE_ID = 1625080769409918L;
    private static final String HEARING_REQUEST_ID = "12345";
    private static final String BENEFIT_CODE = "002";
    private static final String ISSUE_CODE = "DD";
    private static final String PROCESSING_VENUE = "Processing Venue";

    @Captor
    private ArgumentCaptor<HearingCancelRequestPayload> hearingCancelRequestPayloadArgumentCaptor;

    @BeforeEach
    void setup() {
        hearingsService = new HearingsServiceV2(
            hmcHearingApiService,
            ccdCaseService,
            createHearingCaseUpdater,
            adjournCreateHearingCaseUpdater,
            updateHearingCaseUpdater
        );
    }

    @Test
    void processHearingRequestThrowsExceptionWhenHearingStateIsNull() {
        HearingRequest hearingRequest = HearingRequest.internalBuilder()
            .ccdCaseId(String.valueOf(CASE_ID))
            .hearingRoute(HearingRoute.LIST_ASSIST)
            .build();

        UnhandleableHearingStateException exceptionThrown = assertThrows(
            UnhandleableHearingStateException.class,
            () -> hearingsService.processHearingRequest(hearingRequest)
        );
        assertNotNull(exceptionThrown.getMessage());
    }

    @DisplayName("When wrapper with a valid Hearing State is given addHearingResponse should run without error")
    @ParameterizedTest
    @EnumSource(
        value = HearingState.class,
        names = {"UPDATED_CASE","PARTY_NOTIFIED"})
    void processHearingRequestShouldNotUpdateCaseForUnsupportedHearingsStates(HearingState hearingState) {
        final HearingRequest hearingRequest = HearingRequest.internalBuilder()
            .hearingRoute(HearingRoute.LIST_ASSIST)
            .ccdCaseId(String.valueOf(CASE_ID))
            .hearingState(hearingState).build();

        assertThatNoException().isThrownBy(
            () -> hearingsService.processHearingRequest(hearingRequest));

        verifyNoInteractions(createHearingCaseUpdater, adjournCreateHearingCaseUpdater, updateHearingCaseUpdater, ccdCaseService);
    }

    @Test
    void processHearingRequestForAdjournAndCreateHearing() throws Exception {
        final HearingRequest hearingRequest = HearingRequest.internalBuilder()
            .hearingRoute(HearingRoute.LIST_ASSIST)
            .ccdCaseId(String.valueOf(CASE_ID))
            .hearingState(HearingState.ADJOURN_CREATE_HEARING).build();

        hearingsService.processHearingRequest(hearingRequest);
        verify(adjournCreateHearingCaseUpdater).createHearingAndUpdateCase(hearingRequest);
        verifyNoInteractions(updateHearingCaseUpdater, ccdCaseService);
    }

    @Test
    void processHearingRequestForCreateHearing() throws Exception {
        final HearingRequest hearingRequest = HearingRequest.internalBuilder()
            .hearingRoute(HearingRoute.LIST_ASSIST)
            .ccdCaseId(String.valueOf(CASE_ID))
            .hearingState(HearingState.CREATE_HEARING).build();

        hearingsService.processHearingRequest(hearingRequest);

        verify(createHearingCaseUpdater).createHearingAndUpdateCase(hearingRequest);
        verifyNoInteractions(updateHearingCaseUpdater, ccdCaseService);
    }

    @Test
    void processHearingRequestForUpdateHearing() throws Exception {
        final HearingRequest hearingRequest = HearingRequest.internalBuilder()
            .hearingRoute(HearingRoute.LIST_ASSIST)
            .ccdCaseId(String.valueOf(CASE_ID))
            .hearingState(HearingState.UPDATE_HEARING).build();

        hearingsService.processHearingRequest(hearingRequest);
        verify(updateHearingCaseUpdater).updateHearingAndCase(hearingRequest);
        verifyNoInteractions(createHearingCaseUpdater, adjournCreateHearingCaseUpdater, ccdCaseService);
    }

    @Test
    void processHearingRequestForCancelHearing() throws Exception {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .ccdCaseId(String.valueOf(CASE_ID))
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .caseManagementLocation(CaseManagementLocation.builder().build())
            .appeal(Appeal.builder().build())
            .hearings(new ArrayList<>(Collections.singletonList(Hearing.builder()
                                                                    .value(HearingDetails.builder()
                                                                               .hearingId(HEARING_REQUEST_ID)
                                                                               .versionNumber(1L)
                                                                               .build())
                                                                    .build())))
            .processingVenue(PROCESSING_VENUE)
            .build();

        when(ccdCaseService.getStartEventResponse(CASE_ID, CASE_UPDATED))
            .thenReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        final HearingRequest hearingRequest = HearingRequest.internalBuilder()
            .hearingRoute(HearingRoute.LIST_ASSIST)
            .ccdCaseId(String.valueOf(CASE_ID))
            .hearingState(HearingState.CANCEL_HEARING).build();

        hearingsService.processHearingRequest(hearingRequest);
        verify(ccdCaseService).getStartEventResponse(CASE_ID, CASE_UPDATED);
        verify(hmcHearingApiService).sendCancelHearingRequest(
            hearingCancelRequestPayloadArgumentCaptor.capture(), eq(HEARING_REQUEST_ID));
        assertThat(hearingCancelRequestPayloadArgumentCaptor.getValue().getCancellationReasonCodes())
                .isNull();
        verifyNoInteractions(createHearingCaseUpdater, adjournCreateHearingCaseUpdater, updateHearingCaseUpdater);
    }

    @Test
    void processHearingRequestForCancelHearingWithCancelHearingReasons() throws Exception {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .ccdCaseId(String.valueOf(CASE_ID))
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .caseManagementLocation(CaseManagementLocation.builder().build())
            .appeal(Appeal.builder().build())
            .hearings(new ArrayList<>(Collections.singletonList(Hearing.builder()
                                                                    .value(HearingDetails.builder()
                                                                               .hearingId(HEARING_REQUEST_ID)
                                                                               .versionNumber(1L)
                                                                               .build())
                                                                    .build())))
            .processingVenue(PROCESSING_VENUE)
            .build();

        when(ccdCaseService.getStartEventResponse(CASE_ID, CASE_UPDATED))
            .thenReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        final HearingRequest hearingRequest = HearingRequest.internalBuilder()
            .hearingRoute(HearingRoute.LIST_ASSIST)
            .ccdCaseId(String.valueOf(CASE_ID))
            .cancellationReason(SETTLED)
            .hearingState(HearingState.CANCEL_HEARING).build();

        hearingsService.processHearingRequest(hearingRequest);
        verify(ccdCaseService).getStartEventResponse(CASE_ID, CASE_UPDATED);
        verify(hmcHearingApiService).sendCancelHearingRequest(
                hearingCancelRequestPayloadArgumentCaptor.capture(), eq(HEARING_REQUEST_ID));
        assertThat(hearingCancelRequestPayloadArgumentCaptor.getValue().getCancellationReasonCodes())
                .isEqualTo(List.of(SETTLED));
        verifyNoInteractions(createHearingCaseUpdater, adjournCreateHearingCaseUpdater, updateHearingCaseUpdater);
    }

    @Test
    void shouldThrowExhaustedRetryException() {
        HearingWrapper wrapper = HearingWrapper.builder()
            .caseData(SscsCaseData.builder()
                          .ccdCaseId("404")
                          .build())
            .hearingState(ADJOURN_CREATE_HEARING)
            .build();

        assertThatExceptionOfType(ExhaustedRetryException.class).isThrownBy(
                () -> hearingsService.hearingResponseUpdateRecover(
                    new UpdateCaseException("Retry exhausted"),
                    wrapper,
                    HmcUpdateResponse.builder()
                        .hearingRequestId(404L)
                        .build()))
            .withMessageContaining("Cancellation request Response received, rethrowing exception");
    }


}
