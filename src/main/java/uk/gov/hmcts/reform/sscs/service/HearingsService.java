package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMapping.buildHearingPayload;
import static uk.gov.hmcts.reform.sscs.helper.service.HearingsServiceHelper.getHearingId;

import feign.FeignException;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.exception.GetHearingException;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.exception.UnhandleableHearingStateException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.helper.mapping.HearingsRequestMapping;
import uk.gov.hmcts.reform.sscs.helper.mapping.OverridesMapping;
import uk.gov.hmcts.reform.sscs.helper.service.HearingsServiceHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.model.HearingEvent;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingCancelRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.UnusedFormalParameter", "PMD.TooManyMethods", "AvoidThrowingRawExceptionTypes"})
public class HearingsService {

    @Value("${retry.hearing-response-update.max-retries}")
    private static int hearingResponseUpdateMaxRetries;

    private final HmcHearingApiService hmcHearingApiService;

    private final HmcHearingsApiService hmcHearingsApiService;

    private final CcdCaseService ccdCaseService;

    private final ReferenceDataServiceHolder refData;

    private final UpdateCcdCaseService updateCcdCaseService;

    private final IdamService idamService;

    private final HearingServiceConsumer hearingServiceConsumer;

    @Value("${feature.hearings-case-updateV2.enabled:false}")
    private boolean hearingsCaseUpdateV2Enabled;
    // Leaving blank for now until a future change is scoped and completed, then we can add the case states back in
    public static final List<State> INVALID_CASE_STATES = List.of();
    private static final Long HEARING_VERSION_NUMBER = 1L;

    @Retryable(
            value = UpdateCaseException.class,
            maxAttemptsExpression = "${retry.hearing-response-update.max-retries}",
            backoff = @Backoff(delayExpression = "${retry.hearing-response-update.backoff}"))
    public void processHearingRequest(HearingRequest hearingRequest) throws UnhandleableHearingStateException,
            UpdateCaseException, ListingException {
        log.info("Processing Hearing Request for Case ID {}, Hearing State {} and Route {} and Cancellation Reason {}",
                hearingRequest.getCcdCaseId(),
                hearingRequest.getHearingState(),
                hearingRequest.getHearingRoute(),
                hearingRequest.getCancellationReason());

        processHearingWrapper(createWrapper(hearingRequest));
    }

    public void processHearingWrapper(HearingWrapper wrapper)
        throws UnhandleableHearingStateException, UpdateCaseException, ListingException {

        String caseId = wrapper.getCaseData().getCcdCaseId();
        log.info("Processing Hearing Wrapper for Case ID {}, Case State {} and Hearing State {}",
                 caseId,
                 wrapper.getCaseState().toString(),
                 wrapper.getHearingState().getState());

        if (caseStatusInvalid(wrapper)) {
            log.info("Case is in an invalid state for a hearing request. No requests sent to the HMC. Case ID {} and Case State {}",
                     caseId,
                     wrapper.getCaseState().toString());
            return;
        }

        switch (wrapper.getHearingState()) {
            case ADJOURN_CREATE_HEARING -> {
                wrapper.getCaseData().getAdjournment().setAdjournmentInProgress(YesNo.YES);
                wrapper.setHearingState(HearingState.CREATE_HEARING);
                createHearing(wrapper);
            }
            case CREATE_HEARING -> createHearing(wrapper);
            case UPDATE_HEARING -> updateHearing(wrapper);
            case UPDATED_CASE -> log.info(
                "Updated case API not supported. Case ID {}",
                caseId
            );
            case CANCEL_HEARING -> cancelHearing(wrapper);
            case PARTY_NOTIFIED -> log.info(
                "Parties notified API not supported. Case ID {}",
                caseId
            );
            default -> {
                UnhandleableHearingStateException err = new UnhandleableHearingStateException(wrapper.getHearingState());
                log.error(err.getMessage(), err);
                throw err;
            }
        }
    }

    private boolean caseStatusInvalid(HearingWrapper wrapper) {
        return INVALID_CASE_STATES.contains(wrapper.getCaseState());
    }

    private void createHearing(HearingWrapper wrapper) throws UpdateCaseException, ListingException {
        SscsCaseData caseData = wrapper.getCaseData();

        String caseId = caseData.getCcdCaseId();
        HearingsGetResponse hearingsGetResponse = hmcHearingsApiService.getHearingsRequest(caseId, null);
        CaseHearing hearing = HearingsServiceHelper.findExistingRequestedHearings(hearingsGetResponse);
        HmcUpdateResponse hmcUpdateResponse;

        OverridesMapping.setDefaultListingValues(wrapper.getCaseData(), refData);

        if (isNull(hearing)) {
            HearingRequestPayload hearingPayload = buildHearingPayload(wrapper, refData);
            log.debug("Sending Create Hearing Request for Case ID {}", caseId);
            hmcUpdateResponse = hmcHearingApiService.sendCreateHearingRequest(hearingPayload);

            log.debug("Received Create Hearing Request Response for Case ID {}, Hearing State {} and Response:\n{}",
                caseId,
                wrapper.getHearingState().getState(),
                hmcUpdateResponse.toString());
        } else {
            hmcUpdateResponse = HmcUpdateResponse.builder()
                .hearingRequestId(hearing.getHearingId())
                .versionNumber(getHearingVersionNumber(hearing))
                .status(hearing.getHmcStatus())
                .build();

            log.debug("Existing hearing found, skipping Create Hearing Request for Case ID {}, Hearing State {}, Hearing version {} and "
                    + "Hearing Id {}",
                caseId,
                hearing.getHmcStatus(),
                hearing.getRequestVersion(),
                hearing.getHearingId());
        }

        hearingResponseUpdate(wrapper, hmcUpdateResponse);
    }

    private Long getHearingVersionNumber(CaseHearing hearing) {
        try {
            HearingGetResponse response = hmcHearingApiService.getHearingRequest(hearing.getHearingId().toString());
            return response.getRequestDetails().getVersionNumber();
        } catch (GetHearingException e) {
            log.debug("Hearing with id {} doesn't exist", hearing.getHearingId());
        }

        return HEARING_VERSION_NUMBER;
    }

    private void updateHearing(HearingWrapper wrapper) throws UpdateCaseException, ListingException {
        if (isNull(wrapper.getCaseData().getSchedulingAndListingFields().getOverrideFields())) {
            OverridesMapping.setOverrideValues(wrapper.getCaseData(), refData);
        }
        Integer duration = wrapper
            .getCaseData()
            .getSchedulingAndListingFields()
            .getOverrideFields()
            .getDuration();
        boolean isMultipleOfFive = duration % 5 == 0;
        if (!isMultipleOfFive) {
            throw new ListingException("Listing duration must be multiple of 5.0 minutes");
        }

        HearingRequestPayload hearingPayload = buildHearingPayload(wrapper, refData);
        String hearingId = getHearingId(wrapper);
        log.debug("Sending Update Hearing Request for Case ID {}", wrapper.getCaseData().getCcdCaseId());
        HmcUpdateResponse response = hmcHearingApiService.sendUpdateHearingRequest(hearingPayload, hearingId);

        log.debug("Received Update Hearing Request Response for Case ID {}, Hearing State {} and Response:\n{}",
                wrapper.getCaseData().getCcdCaseId(),
                wrapper.getHearingState().getState(),
                response);

        hearingResponseUpdate(wrapper, response);
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

    protected void hearingResponseUpdate(HearingWrapper wrapper, HmcUpdateResponse response) throws UpdateCaseException, ListingException {
        SscsCaseData caseData = wrapper.getCaseData();
        Long hearingRequestId = response.getHearingRequestId();
        String caseId = caseData.getCcdCaseId();

        log.info("Updating Case with Hearing Response for Case ID {}, Hearing ID {} and Hearing State {}",
            caseId,
            hearingRequestId,
            wrapper.getHearingState().getState());

        HearingEvent event = HearingsServiceHelper.getHearingEvent(wrapper.getHearingState());
        log.info("Updating case with event {} description is {}", event, event.getDescription());

        if (hearingsCaseUpdateV2Enabled) {
            updateCaseWithHearingResponseV2(wrapper, response, hearingRequestId, event, caseId);
        } else {
            hearingServiceConsumer.getCreateHearingCaseDataConsumer(response, hearingRequestId).accept(wrapper.getCaseData());

            var details = ccdCaseService.updateCaseData(caseData, wrapper, event);

            if (nonNull(details)) {
                log.info("Case update details CCD state {}  event id: {} event token: {} callbackresponsestatus: {} caseid {}",
                         details.getState(),
                         details.getEventId(),
                         details.getEventToken(),
                         details.getCallbackResponseStatus(),
                         details.getCaseTypeId()
                );
            }
        }

        log.info("Case Updated with Hearing Response for Case ID {}, Hearing ID {}, Hearing State {} and CCD Event {}",
            caseId,
            hearingRequestId,
            wrapper.getHearingState().getState(),
            event.getEventType().getCcdType());
    }

    private void updateCaseWithHearingResponseV2(HearingWrapper wrapper, HmcUpdateResponse response, Long hearingRequestId, HearingEvent event, String caseId) throws UpdateCaseException, ListingException {
        log.info("Updating case with hearing response using updateCaseDataV2 for event {} description {}",
                 event, event.getDescription());

        try {
            Consumer<SscsCaseDetails> caseDataConsumer = hearingServiceConsumer.getCreateHearingCaseDetailsConsumerV2(response, hearingRequestId);

            updateCcdCaseService.updateCaseV2(
                Long.parseLong(caseId),
                event.getEventType().getCcdType(),
                event.getSummary(),
                event.getDescription(),
                idamService.getIdamTokens(),
                caseDataConsumer
            );
            log.info("Case Updated using updateCaseDataV2 with Hearing Response for Case ID {}, Hearing ID {}, Hearing State {} and CCD Event {}",
                     caseId,
                     hearingRequestId,
                     wrapper.getHearingState().getState(),
                     event.getEventType().getCcdType());

        } catch (FeignException e) {
            UpdateCaseException exc = new UpdateCaseException(
                String.format("The case with Case id: %s could not be updated using updateCaseV2 with status %s, %s",
                              caseId, e.status(), e));
            log.error(exc.getMessage(), exc);
            throw exc;
        }

    }

    @Recover
    public void hearingResponseUpdateRecover(UpdateCaseException exception, HearingWrapper wrapper, HmcUpdateResponse response) {
        log.info("Updating Case with Hearing Response has failed {} times, rethrowing exception, for Case ID {}, Hearing ID {} and Hearing State {} with the exception: {}",
            hearingResponseUpdateMaxRetries,
            wrapper.getCaseData().getCcdCaseId(),
            response.getHearingRequestId(),
            wrapper.getHearingState().getState(),
            exception.toString());

        throw new ExhaustedRetryException("Cancellation request Response received, rethrowing exception", exception);
    }

    private HearingWrapper createWrapper(HearingRequest hearingRequest) throws UnhandleableHearingStateException {
        if (isNull(hearingRequest.getHearingState())) {
            UnhandleableHearingStateException err = new UnhandleableHearingStateException();
            log.error(err.getMessage(), err);
            throw err;
        }

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
