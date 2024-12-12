package uk.gov.hmcts.reform.sscs.jms.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.exception.TribunalsEventProcessingException;
import uk.gov.hmcts.reform.sscs.exception.UnhandleableHearingStateException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.service.CcdCaseService;
import uk.gov.hmcts.reform.sscs.service.HearingsService;

@ExtendWith(MockitoExtension.class)
class TribunalsHearingsEventTopicListenerTest {

    @InjectMocks
    private TribunalsHearingsEventQueueListener tribunalsHearingsEventQueueListener;

    @Mock
    private HearingsService hearingsService;

    @Mock
    private CcdCaseService ccdCaseService;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;

    private static final String CASE_ID = "1001";

    @Test
    @DisplayName("When a valid request comes in make sure processHearingRequest is hit")
    void whenAValidRequestComesIn_makeSureProcessHearingRequestIsHit() throws Exception {

        ReflectionTestUtils.setField(tribunalsHearingsEventQueueListener, "isByPassHearingServiceEnabled", true);
        HearingRequest hearingRequest = createHearingRequest();

        tribunalsHearingsEventQueueListener.handleIncomingMessage(hearingRequest);

        verify(hearingsService, times(1)).processHearingRequest((hearingRequest));
    }

    @ParameterizedTest
    @DisplayName("When an invalid request comes in make sure exception is thrown")
    @MethodSource("throwableParameters")
    void whenAnInvalidRequestComesIn_makeSureExceptionIsThrown(Class<? extends Throwable> throwable) throws Exception {
        ReflectionTestUtils.setField(tribunalsHearingsEventQueueListener, "isByPassHearingServiceEnabled", true);
        HearingRequest hearingRequest = new HearingRequest();

        doThrow(throwable).when(hearingsService).processHearingRequest(hearingRequest);

        assertThrows(TribunalsEventProcessingException.class, () -> tribunalsHearingsEventQueueListener.handleIncomingMessage(hearingRequest));
    }

    private static Stream<Arguments> throwableParameters() {
        return Stream.of(
            Arguments.of(UnhandleableHearingStateException.class),
            Arguments.of(UpdateCaseException.class)
        );
    }

    @Test
    @DisplayName("When an null request comes in make sure exception is thrown")
    void whenAnNullRequestComesIn_makeSureExceptionIsThrown() {
        ReflectionTestUtils.setField(tribunalsHearingsEventQueueListener, "isByPassHearingServiceEnabled", true);
        assertThrows(TribunalsEventProcessingException.class, () -> tribunalsHearingsEventQueueListener.handleIncomingMessage(null));
    }

    private HearingRequest createHearingRequest() {
        return HearingRequest.builder(CASE_ID)
            .hearingRoute(HearingRoute.LIST_ASSIST)
            .hearingState(HearingState.CREATE_HEARING)
            .build();
    }

    @Test
    @DisplayName("When a listing exception is thrown, catch the error and update the state")
    void whenListingExceptionThrown_UpdateCaseDataState() throws Exception {
        ReflectionTestUtils.setField(tribunalsHearingsEventQueueListener, "hearingsCaseUpdateV2Enabled", false);
        ReflectionTestUtils.setField(tribunalsHearingsEventQueueListener, "isByPassHearingServiceEnabled", true);
        SscsCaseData caseData = SscsCaseData.builder().build();
        SscsCaseDetails caseDetails = SscsCaseDetails.builder().data(caseData).build();
        HearingRequest hearingRequest = createHearingRequest();

        doThrow(ListingException.class)
            .when(hearingsService)
            .processHearingRequest(hearingRequest);

        when(ccdCaseService.getCaseDetails(CASE_ID)).thenReturn(caseDetails);
        when(ccdCaseService.updateCaseData(caseData,
                                           EventType.LISTING_ERROR,
                                           null,
                                           null)).thenReturn(caseDetails);

        assertDoesNotThrow(() -> tribunalsHearingsEventQueueListener.handleIncomingMessage(hearingRequest));

        verifyNoInteractions(updateCcdCaseService);
    }

    @Test
    @DisplayName("When a listing exception is thrown, with hearingsCaseUpdateV2Enabled, catch the error and update the state")
    void whenListingExceptionThrownWithV2Enabled_UpdateCaseDataState() throws Exception {
        ReflectionTestUtils.setField(tribunalsHearingsEventQueueListener, "hearingsCaseUpdateV2Enabled", true);
        ReflectionTestUtils.setField(tribunalsHearingsEventQueueListener, "isByPassHearingServiceEnabled", true);
        SscsCaseData caseData = SscsCaseData.builder().build();
        SscsCaseDetails caseDetails = SscsCaseDetails.builder().data(caseData).build();
        HearingRequest hearingRequest = createHearingRequest();

        doThrow(ListingException.class)
                .when(hearingsService)
                .processHearingRequest(hearingRequest);

        when(updateCcdCaseService.triggerCaseEventV2(
                1001L,
                EventType.LISTING_ERROR.getCcdType(),
                null,
                null,
                idamService.getIdamTokens()
        )).thenReturn(caseDetails);

        verifyNoInteractions(ccdCaseService);
        assertDoesNotThrow(() -> tribunalsHearingsEventQueueListener.handleIncomingMessage(hearingRequest));
    }
}
