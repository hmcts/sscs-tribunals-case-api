package uk.gov.hmcts.reform.sscs.hearings.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.hearings.helper.mapping.HearingsMapping.buildHearingPayload;
import static uk.gov.hmcts.reform.sscs.hearings.helper.service.HearingsServiceHelper.getHearingId;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.hearings.exception.GetHearingException;
import uk.gov.hmcts.reform.sscs.hearings.exception.ListingException;
import uk.gov.hmcts.reform.sscs.hearings.exception.UnhandleableHearingStateException;
import uk.gov.hmcts.reform.sscs.hearings.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.hearings.helper.mapping.HearingsRequestMapping;
import uk.gov.hmcts.reform.sscs.hearings.helper.mapping.OverridesMapping;
import uk.gov.hmcts.reform.sscs.hearings.helper.service.HearingsServiceHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.hearings.model.HearingEvent;
import uk.gov.hmcts.reform.sscs.hearings.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.hearings.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.hearings.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.hearings.model.single.hearing.HearingCancelRequestPayload;
import uk.gov.hmcts.reform.sscs.hearings.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.hearings.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.hearings.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.hearings.service.exceptions.UpdateCcdCaseDetailsException;
import uk.gov.hmcts.reform.sscs.hearings.service.hearings.AdjournCreateHearingCaseUpdater;
import uk.gov.hmcts.reform.sscs.hearings.service.hearings.CreateHearingCaseUpdater;
import uk.gov.hmcts.reform.sscs.hearings.service.hearings.UpdateHearingCaseUpdater;
import uk.gov.hmcts.reform.sscs.hearings.service.holder.ReferenceDataServiceHolder;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.UnusedFormalParameter", "PMD.TooManyMethods"})
public class HearingsService {

    @Value("${retry.hearing-response-update.max-retries}")
    private static int hearingResponseUpdateMaxRetries;

    private final HmcHearingApiService hmcHearingApiService;

    private final HmcHearingsApiService hmcHearingsApiService;

    private final CcdCaseService ccdCaseService;

    private final ReferenceDataServiceHolder refData;

    private final UpdateCcdCaseService updateCcdCaseService;

    private final IdamService idamService;

    private final CreateHearingCaseUpdater createHearingCaseUpdater;
    private final AdjournCreateHearingCaseUpdater adjournCreateHearingCaseUpdater;
    private final UpdateHearingCaseUpdater updateHearingCaseUpdater;

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

        try {
            processHearingWrapper(createWrapper(hearingRequest));
        } catch (UpdateCcdCaseDetailsException e) {
            if (e.getException() instanceof UpdateCaseException) {
                throw (UpdateCaseException) e.getException();
            } else if (e.getException() instanceof ListingException) {
                throw (ListingException) e.getException();
            } else {
                throw new UpdateCaseException("Failed to update case with hearing response");
            }
        }
    }

    public void processHearingWrapper(HearingWrapper wrapper)
        throws UnhandleableHearingStateException, UpdateCcdCaseDetailsException {

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
            case ADJOURN_CREATE_HEARING -> adjournCreateHearingCaseUpdater.createHearingAndUpdateCase(wrapper);
            case CREATE_HEARING -> createHearingCaseUpdater.createHearingAndUpdateCase(wrapper);
            case UPDATE_HEARING -> updateHearingCaseUpdater.updateHearingAndCase(wrapper);
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

        OverridesMapping.setDefaultListingValues(wrapper, refData);

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
            OverridesMapping.setOverrideValues(wrapper, refData);
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

    public void hearingResponseUpdate(HearingWrapper wrapper, HmcUpdateResponse response) throws UpdateCaseException {
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
            updateCaseDataWithHearingResponse(response, hearingRequestId, wrapper.getCaseData());
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

    private void updateCaseWithHearingResponseV2(HearingWrapper wrapper, HmcUpdateResponse response, Long hearingRequestId, HearingEvent event,
                                                 String caseId) throws UpdateCaseException {
        Consumer<SscsCaseData> caseDataConsumer = sscsCaseData -> updateCaseDataWithHearingResponse(response, hearingRequestId, sscsCaseData);

        log.info("Updating case with hearing response using updateCaseDataV2 for event {} description {}",
            event, event.getDescription());

        try {
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

    private void updateCaseDataWithHearingResponse(HmcUpdateResponse response, Long hearingRequestId, SscsCaseData caseData) {
        Hearing hearing = HearingsServiceHelper.getHearingById(hearingRequestId, caseData);

        if (isNull(hearing)) {
            hearing = HearingsServiceHelper.createHearing(hearingRequestId);
            HearingsServiceHelper.addHearing(hearing, caseData);
        }

        HearingsServiceHelper.updateHearingId(hearing, response);
        HearingsServiceHelper.updateVersionNumber(hearing, response);

        if (refData.isAdjournmentFlagEnabled()
            && YesNo.isYes(caseData.getAdjournment().getAdjournmentInProgress())) {
            log.debug("Case Updated with AdjournmentInProgress to NO for Case ID {}", caseData.getCcdCaseId());
            caseData.getAdjournment().setAdjournmentInProgress(YesNo.NO);
        }
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
