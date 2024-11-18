package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.helper.service.HearingsServiceHelper.getHearingId;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.exception.UnhandleableHearingStateException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.helper.mapping.HearingsRequestMapping;
import uk.gov.hmcts.reform.sscs.helper.service.HearingsServiceHelper;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingCancelRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.hearings.AdjournCreateHearingCaseUpdater;
import uk.gov.hmcts.reform.sscs.service.hearings.CreateHearingCaseUpdater;
import uk.gov.hmcts.reform.sscs.service.hearings.UpdateHearingCaseUpdater;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "feature.hearings-case-updateV2.enabled", havingValue = "true")
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.UnusedFormalParameter", "PMD.TooManyMethods"})
public class HearingsServiceV2 implements HearingsService {
    @Value("${retry.hearing-response-update.max-retries}")
    private static int hearingResponseUpdateMaxRetries;

    private final HmcHearingApiService hmcHearingApiService;

    private final CcdCaseService ccdCaseService;

    private final CreateHearingCaseUpdater createHearingCaseUpdater;
    private final AdjournCreateHearingCaseUpdater adjournCreateHearingCaseUpdater;
    private final UpdateHearingCaseUpdater updateHearingCaseUpdater;

    // Leaving blank for now until a future change is scoped and completed, then we can add the case states back in
    // Add validation when adding invalid case states
    public static final List<State> INVALID_CASE_STATES = List.of();

    @Retryable(
        value = UpdateCaseException.class,
        maxAttemptsExpression = "${retry.hearing-response-update.max-retries}",
        backoff = @Backoff(delayExpression = "${retry.hearing-response-update.backoff}"))
    @Override
    public void processHearingRequest(HearingRequest hearingRequest) throws UnhandleableHearingStateException,
        UpdateCaseException, ListingException {
        log.info("Processing Hearing Request for Case ID {}, Hearing State {} and Route {} and Cancellation Reason {}",
            hearingRequest.getCcdCaseId(),
            hearingRequest.getHearingState(),
            hearingRequest.getHearingRoute(),
            hearingRequest.getCancellationReason());

        validateHearingState(hearingRequest);
        process(hearingRequest);
    }

    private static void validateHearingState(HearingRequest hearingRequest) throws UnhandleableHearingStateException {
        if (isNull(hearingRequest.getHearingState())) {
            UnhandleableHearingStateException err = new UnhandleableHearingStateException();
            log.error(err.getMessage(), err);
            throw err;
        }
    }

    private void process(HearingRequest hearingRequest)
        throws UnhandleableHearingStateException, UpdateCaseException, ListingException {

        String caseId = hearingRequest.getCcdCaseId();
        log.info("Processing Hearing Request for Case ID {} and Hearing State {}",
                 caseId,
                 hearingRequest.getHearingState().getState());

        switch (hearingRequest.getHearingState()) {
            case ADJOURN_CREATE_HEARING -> adjournCreateHearingCaseUpdater.createHearingAndUpdateCase(hearingRequest);
            case CREATE_HEARING -> createHearingCaseUpdater.createHearingAndUpdateCase(hearingRequest);
            case UPDATE_HEARING -> updateHearingCaseUpdater.updateHearingAndCase(hearingRequest);
            case UPDATED_CASE -> log.info(
                "Updated case API not supported. Case ID {}",
                caseId
            );
            case CANCEL_HEARING -> cancelHearing(createWrapper(hearingRequest));
            case PARTY_NOTIFIED -> log.info(
                "Parties notified API not supported. Case ID {}",
                caseId
            );
            default -> {
                UnhandleableHearingStateException err = new UnhandleableHearingStateException(hearingRequest.getHearingState());
                log.error(err.getMessage(), err);
                throw err;
            }
        }
    }

    private void cancelHearing(HearingWrapper wrapper) {
        String hearingId = getHearingId(wrapper);
        HearingCancelRequestPayload hearingPayload = HearingsRequestMapping.buildCancelHearingPayload(wrapper);
        HmcUpdateResponse response = hmcHearingApiService.sendCancelHearingRequest(hearingPayload, hearingId);

        log.debug("Received Cancel Hearing Request Response for Case ID {}, Hearing State {} and Response:\n{}",
            wrapper.getCaseData().getCcdCaseId(),
            wrapper.getHearingState().getState(),
            response);
        // TODO process hearing response
    }

    @Recover
    public void hearingResponseUpdateRecover(UpdateCaseException exception, HearingWrapper wrapper, HmcUpdateResponse response) {
        log.info(
            "Updating Case with Hearing Response has failed {} times, rethrowing exception, for Case ID {}, Hearing ID {} and Hearing State {} with the exception: {}",
            hearingResponseUpdateMaxRetries,
            wrapper.getCaseData().getCcdCaseId(),
            response.getHearingRequestId(),
            wrapper.getHearingState().getState(),
            exception.toString());

        throw new ExhaustedRetryException("Cancellation request Response received, rethrowing exception", exception);
    }

    private HearingWrapper createWrapper(HearingRequest hearingRequest) {
        List<CancellationReason> cancellationReasons = null;

        if (hearingRequest.getCancellationReason() != null) {
            cancellationReasons = List.of(hearingRequest.getCancellationReason());
        }

        EventType eventType = HearingsServiceHelper.getCcdEvent(hearingRequest.getHearingState());
        log.info("Getting case details with event {} {}", eventType, eventType.getCcdType());
        SscsCaseDetails sscsCaseDetails = ccdCaseService.getStartEventResponse(Long.parseLong(hearingRequest.getCcdCaseId()), eventType);

        return HearingWrapper.builder()
            .caseData(sscsCaseDetails.getData())
            .eventId(sscsCaseDetails.getEventId())
            .eventToken(sscsCaseDetails.getEventToken())
            .caseState(State.getById(sscsCaseDetails.getState()))
            .hearingState(hearingRequest.getHearingState())
            .cancellationReasons(cancellationReasons)
            .build();
    }

}
