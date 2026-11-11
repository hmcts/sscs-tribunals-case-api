
package uk.gov.hmcts.reform.sscs.jms.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.ExhaustedRetryException;

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
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.service.CcdCaseService;
import uk.gov.hmcts.reform.sscs.service.HearingsService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingRequestHandler;

@ExtendWith(MockitoExtension.class)
class HearingRequestHandlerTest {

    private static final String CASE_ID = "1001";

    @InjectMocks
    private HearingRequestHandler hearingRequestHandler;

    @Mock
    private HearingsService hearingsService;

    @Mock
    private CcdCaseService ccdCaseService;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;

    @Test
    @DisplayName("A valid request is processed successfully")
    void whenAValidRequestComesIn_thenProcessHearingRequestIsCalled() throws Exception {
        HearingRequest hearingRequest = createHearingRequest();

        hearingRequestHandler.handleHearingRequest(hearingRequest);

        verify(hearingsService, times(1)).processHearingRequest(hearingRequest);
    }

    @ParameterizedTest
    @ValueSource(classes = {
            UnhandleableHearingStateException.class,
            UpdateCaseException.class
    })
    @DisplayName("Invalid hearing request exceptions are wrapped")
    void whenAnInvalidRequestComesIn_thenExceptionIsThrown(
            Class<? extends Throwable> throwable
    ) throws Exception {
        HearingRequest hearingRequest = new HearingRequest();

        doThrow(throwable)
                .when(hearingsService)
                .processHearingRequest(hearingRequest);

        assertThatThrownBy(() ->
                hearingRequestHandler.handleHearingRequest(hearingRequest)
        ).isInstanceOf(TribunalsEventProcessingException.class);
    }

    @Test
    @DisplayName("A null request results in a processing exception")
    void whenANullRequestComesIn_thenExceptionIsThrown() {
        assertThatThrownBy(() ->
                hearingRequestHandler.handleHearingRequest(null)
        )
                .isInstanceOf(TribunalsEventProcessingException.class)
                .hasMessageContaining("message did not match format");
    }

    @Test
    @DisplayName("A listing exception with V2 enabled updates the case state")
    void whenListingExceptionThrownWithV2Enabled_thenUpdateCaseDataState() throws Exception {
        SscsCaseData caseData = SscsCaseData.builder().build();
        SscsCaseDetails caseDetails = SscsCaseDetails.builder()
                .data(caseData)
                .build();
        HearingRequest hearingRequest = createHearingRequest();
        IdamTokens idamTokens = IdamTokens.builder().build();

        doThrow(ListingException.class)
                .when(hearingsService)
                .processHearingRequest(hearingRequest);

        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        when(updateCcdCaseService.triggerCaseEventV2(
                1001L,
                EventType.LISTING_ERROR.getCcdType(),
                null,
                null,
                idamTokens
        )).thenReturn(caseDetails);

        assertThatCode(() ->
                hearingRequestHandler.handleHearingRequest(hearingRequest)
        ).doesNotThrowAnyException();

        verify(updateCcdCaseService).triggerCaseEventV2(
                eq(1001L),
                eq(EventType.LISTING_ERROR.getCcdType()),
                isNull(),
                isNull(),
                eq(idamTokens)
        );

        verifyNoInteractions(ccdCaseService);
    }

    @Test
    @DisplayName("An exhausted retry caused by a listing error triggers the CCD listing error event")
    void exhaustedRetryWithListingExceptionTriggersListingErrorEvent() throws Exception {
        HearingRequest hearingRequest = createHearingRequest();
        IdamTokens idamTokens = IdamTokens.builder().build();
        ListingException listingException =
                new ListingException("Hearing duration is required");

        doThrow(new ExhaustedRetryException(
                "Hearing request retries exhausted",
                listingException
        ))
                .when(hearingsService)
                .processHearingRequest(hearingRequest);

        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        assertThatCode(() ->
                hearingRequestHandler.handleHearingRequest(hearingRequest)
        ).doesNotThrowAnyException();

        verify(updateCcdCaseService).triggerCaseEventV2(
                eq(1001L),
                eq(EventType.LISTING_ERROR.getCcdType()),
                eq("Listing Exception"),
                eq("Hearing duration is required"),
                eq(idamTokens)
        );
    }

    @Test
    @DisplayName("An exhausted retry caused by an unexpected error is propagated")
    void exhaustedRetryWithUnexpectedCauseIsPropagated() throws Exception {
        HearingRequest hearingRequest = createHearingRequest();
        RuntimeException cause = new RuntimeException("HMC unavailable");

        doThrow(new ExhaustedRetryException(
                "Hearing request retries exhausted",
                cause
        ))
                .when(hearingsService)
                .processHearingRequest(hearingRequest);

        assertThatThrownBy(() ->
                hearingRequestHandler.handleHearingRequest(hearingRequest)
        )
                .isInstanceOf(TribunalsEventProcessingException.class)
                .hasCause(cause);

        verifyNoInteractions(updateCcdCaseService);
    }

    private HearingRequest createHearingRequest() {
        return HearingRequest.builder(CASE_ID)
                .hearingRoute(HearingRoute.LIST_ASSIST)
                .hearingState(HearingState.CREATE_HEARING)
                .build();
    }
}
