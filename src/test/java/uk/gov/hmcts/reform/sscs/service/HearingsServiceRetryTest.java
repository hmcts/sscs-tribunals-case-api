package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.CREATE_HEARING;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseManagementLocation;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingSubtype;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMapping;
import uk.gov.hmcts.reform.sscs.helper.mapping.OverridesMapping;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.model.HearingEvent;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingCancelRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@EnableRetry
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {HearingsService.class})
@TestPropertySource(properties = {"retry.hearing-response-update.backoff=100"})
class HearingsServiceRetryTest {

    private static final long HEARING_REQUEST_ID = 12345;
    private static final long VERSION = 1;
    private static final long CASE_ID = 1625080769409918L;
    private static final String BENEFIT_CODE = "002";
    private static final String ISSUE_CODE = "DD";
    private static final String EVENT_ID = "EVENT_ID";
    private static final String EVENT_TOKEN = "EVENT_TOKEN";

    @MockitoBean
    private HmcHearingApiService hmcHearingApiService;
    @MockitoBean
    private CcdCaseService ccdCaseService;
    @MockitoBean
    private ReferenceDataServiceHolder refData;
    @MockitoBean
    private HearingDurationsService hearingDurations;
    @MockitoBean
    private IdamService idamService;
    @MockitoBean
    private UpdateCcdCaseService updateCcdCaseService;
    @MockitoBean
    private HearingServiceConsumer hearingServiceConsumer;
    @MockitoBean
    private Consumer<SscsCaseDetails> sscsCaseDetailsConsumer;
    @MockitoBean
    private HearingsMapping hearingsMapping;
    @MockitoBean
    private PanelCompositionService panelCompositionService;
    @MockitoBean
    private OverridesMapping overridesMapping;

    @Mock
    private Consumer<SscsCaseData> sscsCaseDataConsumer;

    @Autowired
    private HearingsService hearingsService;

    private HearingWrapper wrapper;
    private SscsCaseDetails caseDetails;

    @BeforeEach
    void setup() {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId(String.valueOf(CASE_ID))
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .caseManagementLocation(CaseManagementLocation.builder().build())
            .appeal(Appeal.builder()
                        .rep(Representative.builder().hasRepresentative("No").build())
                        .hearingOptions(HearingOptions.builder().wantsToAttend("yes").build())
                        .hearingType("test")
                        .hearingSubtype(HearingSubtype.builder().wantsHearingTypeFaceToFace("yes").build())
                        .appellant(Appellant.builder()
                                       .name(Name.builder().build())
                                       .build())
                        .build())
            .build();

        wrapper = HearingWrapper.builder()
            .hearingState(CREATE_HEARING)
            .caseData(caseData)
            .caseData(caseData)
            .build();

        caseDetails = SscsCaseDetails.builder()
            .data(caseData)
            .eventId(EVENT_ID)
            .eventToken(EVENT_TOKEN)
            .build();

        when(hearingServiceConsumer.getCreateHearingCaseDetailsConsumerV2(any(), any(), any(), anyBoolean()))
                .thenReturn(sscsCaseDetailsConsumer);
        when(hearingServiceConsumer.getCreateHearingCaseDataConsumer(any(), any())).thenReturn(sscsCaseDataConsumer);

    }

    @DisplayName("When wrapper with a valid HearingResponse is given updateHearingResponse should return updated valid HearingResponse")
    @ParameterizedTest
    @CsvSource(value = {
        "CREATE_HEARING,CREATE_HEARING",
        "UPDATED_CASE,UPDATED_CASE",
    }, nullValues = {"null"})
    void updateHearingResponse(HearingState state, HearingEvent event) throws UpdateCaseException {
        given(ccdCaseService.getStartEventResponse(eq(CASE_ID), any(EventType.class))).willReturn(caseDetails);

        given(ccdCaseService.updateCaseData(
            any(SscsCaseData.class),
            any(EventType.class),
            anyString(), anyString()
        ))
            .willReturn(caseDetails);

        wrapper.setHearingState(state);

        HmcUpdateResponse response = HmcUpdateResponse.builder()
            .versionNumber(VERSION)
            .hearingRequestId(HEARING_REQUEST_ID)
            .build();
        assertThatNoException()
            .isThrownBy(() -> hearingsService.hearingResponseUpdate(wrapper, response));

        verify(updateCcdCaseService).updateCaseV2(
                eq(CASE_ID),
                eq(event.getEventType().getCcdType()),
                eq(event.getSummary()),
                eq(event.getDescription()),
                any(),
                any()
        );
    }

    @Disabled
    @DisplayName("When ccdCaseService throws UpdateCaseException, hearingResponseUpdate should retry "
        + "and if then succeeds, throw no error not send additional messages to hmc")
    @Test
    void updateHearingResponse() throws UpdateCaseException {
        given(ccdCaseService.getStartEventResponse(eq(CASE_ID), any(EventType.class))).willReturn(caseDetails);
        given(ccdCaseService.updateCaseData(any(SscsCaseData.class), eq(wrapper), any(HearingEvent.class)))
                .willThrow(UpdateCaseException.class)
                .willReturn(caseDetails);
        given(refData.getHearingDurations()).willReturn(hearingDurations);

        HmcUpdateResponse hmcUpdateResponse =
                HmcUpdateResponse.builder().hearingRequestId(HEARING_REQUEST_ID).versionNumber(1L).build();
        given(hmcHearingApiService.sendCancelHearingRequest(any(HearingCancelRequestPayload.class), anyString()))
            .willReturn(hmcUpdateResponse);

        var hearingRequest = HearingRequest.builder(String.valueOf(CASE_ID))
                .hearingState(HearingState.UPDATE_HEARING).hearingRoute(HearingRoute.LIST_ASSIST).build();

        assertThatNoException()
            .isThrownBy(() -> hearingsService.processHearingRequest(hearingRequest));
        verify(ccdCaseService, times(2))
                .updateCaseData(any(SscsCaseData.class), eq(wrapper), any(HearingEvent.class));
        verifyNoMoreInteractions(hmcHearingApiService);
    }

    @Disabled
    @DisplayName("When ccdCaseService throws UpdateCaseException, hearingResponseUpdate should retry three times "
        + " and if it continues to fail then cancel the hearing and throw a ExhaustedRetryException Exception")
    @Test
    void updateHearingResponseFailure() throws UpdateCaseException {
        given(ccdCaseService.updateCaseData(
            any(SscsCaseData.class),
            any(EventType.class),
            anyString(),
            anyString()
        ))
            .willThrow(UpdateCaseException.class);

        HmcUpdateResponse hmcUpdateResponse = HmcUpdateResponse.builder()
            .hearingRequestId(HEARING_REQUEST_ID)
            .versionNumber(1L)
            .build();

        given(hmcHearingApiService.sendCancelHearingRequest(
            any(HearingCancelRequestPayload.class),
            anyString()
        ))
            .willReturn(hmcUpdateResponse);

        var hearingRequest = HearingRequest.builder(String.valueOf(CASE_ID))
            .hearingRoute(HearingRoute.LIST_ASSIST)
            .cancellationReason(CancellationReason.FEE_NOT_PAID)
            .build();

        assertThatExceptionOfType(ExhaustedRetryException.class)
            .isThrownBy(() -> hearingsService.processHearingRequest(hearingRequest));

        verify(ccdCaseService, times(3))
            .updateCaseData(
                any(SscsCaseData.class),
                eq(wrapper),
                any(HearingEvent.class)
            );
    }
}
