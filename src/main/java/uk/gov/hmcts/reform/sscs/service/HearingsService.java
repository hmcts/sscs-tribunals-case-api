package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.helper.service.HearingsServiceHelper.findHearingsWithRequestedHearingState;
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
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.exception.GetHearingException;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.exception.UnhandleableHearingStateException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMapping;
import uk.gov.hmcts.reform.sscs.helper.mapping.HearingsRequestMapping;
import uk.gov.hmcts.reform.sscs.helper.mapping.OverridesMapping;
import uk.gov.hmcts.reform.sscs.helper.service.HearingsServiceHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.model.HearingEvent;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
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
    private final CcdCaseService ccdCaseService;
    private final ReferenceDataServiceHolder refData;
    private final UpdateCcdCaseService updateCcdCaseService;
    private final IdamService idamService;
    private final HearingServiceConsumer hearingServiceConsumer;
    private final HearingsMapping hearingsMapping;
    private final OverridesMapping overridesMapping;


    // Leaving blank for now until a future change is scoped and completed, then we can add the case states back in
    private static final Long HEARING_VERSION_NUMBER = 1L;
    public static final String EXISTING_HEARING_ERROR = "There is already a hearing request in List assist, "
            + "are you sure you want to send another request? If you do proceed, then please cancel the existing hearing request first";
    public static final String REQUEST_FAILURE_WARNING = "There is a listing request in failure state, please update any missing data then send a new listing request.";

    @Retryable(
            retryFor = UpdateCaseException.class,
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

    private void createHearing(HearingWrapper wrapper) throws UpdateCaseException, ListingException {
        SscsCaseData caseData = wrapper.getCaseData();
        String caseId = caseData.getCcdCaseId();
        HearingsGetResponse hearingsGetResponse = hmcHearingApiService.getHearingsRequest(caseId, null);
        CaseHearing hearing = HearingsServiceHelper.findHearingsWithRequestedHearingState(hearingsGetResponse, null, true);
        overridesMapping.setDefaultListingValues(wrapper.getCaseData(), refData);

        if (nonNull(hearing)) {
            HearingWrapper cancellationWrapper = HearingWrapper.builder()
                    .caseData(wrapper.getCaseData())
                    .eventId(wrapper.getEventId())
                    .eventToken(wrapper.getEventToken())
                    .caseState(wrapper.getCaseState())
                    .hearingState(HearingState.CANCEL_HEARING)
                    .cancellationReasons(List.of(CancellationReason.OTHER))
                    .build();

            log.info("Cancelling existing hearing with hearing state requested or awaiting listing. "
                    + "Case ID {}, Hearing ID {}", caseId, hearing.getHearingId());
            cancelHearing(cancellationWrapper);
        }

        HmcUpdateResponse hmcUpdateResponse;
        HearingRequestPayload hearingPayload = hearingsMapping.buildHearingPayload(wrapper, refData);
        log.info("Sending Create Hearing Request for Case ID {}", caseId);
        hmcUpdateResponse = hmcHearingApiService.sendCreateHearingRequest(hearingPayload);

        var johTiers = hearingPayload.getHearingDetails().getPanelRequirements().getRoleTypes();
        log.info("Saving JOH tiers ({}) onto the case ({})", johTiers, caseId);
        wrapper.getCaseData().setPanelMemberComposition(new PanelMemberComposition(johTiers));

        log.info("Received Create Hearing Request Response for Case ID {}, Hearing State {} and Response:\n{}",
                caseId, wrapper.getHearingState().getState(), hmcUpdateResponse.toString());

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
        SscsCaseData caseData = wrapper.getCaseData();
        String caseId = caseData.getCcdCaseId();

        if (isNull(caseData.getSchedulingAndListingFields().getOverrideFields())) {
            overridesMapping.setOverrideValues(caseData, refData);
        }
        Integer duration = wrapper
                .getCaseData()
                .getSchedulingAndListingFields()
                .getOverrideFields()
                .getDuration();
        boolean isMultipleOfFive = isNull(duration) || duration % 5 == 0;
        if (!isMultipleOfFive) {
            throw new ListingException("Listing duration must be multiple of 5.0 minutes");
        }

        HearingsGetResponse hearingsGetResponse = hmcHearingApiService.getHearingsRequest(caseId, null);
        CaseHearing hearing = HearingsServiceHelper.findHearingsWithRequestedHearingState(hearingsGetResponse, null, true);
        if (nonNull(hearing)) {
            Long hmcHearingVersionId = getHearingVersionNumber(hearing);
            Hearing caseDataHearing = HearingsServiceHelper.getHearingById(hearing.getHearingId(), caseData);

            if (nonNull(caseDataHearing) && !caseDataHearing.getValue().getVersionNumber().equals(hmcHearingVersionId)) {
                log.info("Setting case {} hearing version number to {} on ccd for hearing id {}",
                        caseId, hmcHearingVersionId, hearing.getHearingId());
                caseData.getLatestHearing().getValue().setVersionNumber(hmcHearingVersionId);
            }

            log.info("Case id: {}, hmc hearing request version: {}, ccd case data hearing request version: {}",
                    caseId, hmcHearingVersionId, caseData.getLatestHearing().getValue().getVersionNumber());
        }

        HearingRequestPayload hearingPayload = hearingsMapping.buildHearingPayload(wrapper, refData);
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

    protected void hearingResponseUpdate(HearingWrapper wrapper, HmcUpdateResponse response) throws UpdateCaseException {
        SscsCaseData caseData = wrapper.getCaseData();
        Long hearingRequestId = response.getHearingRequestId();
        String caseId = caseData.getCcdCaseId();

        log.info("Updating Case with Hearing Response for Case ID {}, Hearing ID {} and Hearing State {}",
                caseId,
                hearingRequestId,
                wrapper.getHearingState().getState());

        HearingEvent event = HearingsServiceHelper.getHearingEvent(wrapper.getHearingState());
        log.info("Updating case with event {} description is {}", event, event.getDescription());

        updateCaseWithHearingResponseV2(wrapper, response, hearingRequestId, event, caseId);

        log.info("Case Updated with Hearing Response for Case ID {}, Hearing ID {}, Hearing State {} and CCD Event {}",
                caseId,
                hearingRequestId,
                wrapper.getHearingState().getState(),
                event.getEventType().getCcdType());
    }

    private void updateCaseWithHearingResponseV2(HearingWrapper wrapper, HmcUpdateResponse response,
                                                 Long hearingRequestId, HearingEvent event,
                                                 String caseId) throws UpdateCaseException {
        log.info("Updating case with hearing response using updateCaseDataV2 for event {} description {}",
                event, event.getDescription());

        try {
            Consumer<SscsCaseDetails> caseDataMutator = hearingServiceConsumer
                    .getCreateHearingCaseDetailsConsumerV2(
                            wrapper.getCaseData().getPanelMemberComposition(),
                            response, hearingRequestId, HearingState.UPDATE_HEARING.equals(wrapper.getHearingState())
                    );

            updateCcdCaseService.updateCaseV2(
                    Long.parseLong(caseId),
                    event.getEventType().getCcdType(),
                    event.getSummary(),
                    event.getDescription(),
                    idamService.getIdamTokens(),
                    caseDataMutator
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

    public PreSubmitCallbackResponse<SscsCaseData> validationCheckForListedOrExceptionHearings(SscsCaseData caseData, PreSubmitCallbackResponse<SscsCaseData> response) {
        if (HearingRoute.LIST_ASSIST == caseData.getSchedulingAndListingFields().getHearingRoute()) {
            HearingsGetResponse hearingsGetResponse = hmcHearingApiService.getHearingsRequest(caseData.getCcdCaseId(), null);
            CaseHearing listedCase = findHearingsWithRequestedHearingState(hearingsGetResponse, HmcStatus.LISTED, false);
            if (nonNull(listedCase)) {
                response.addError(EXISTING_HEARING_ERROR);
                log.error("Error on case {}: There is already a hearing request in List assist", caseData.getCcdCaseId());
            }
            CaseHearing requestFailureCase = findHearingsWithRequestedHearingState(hearingsGetResponse, HmcStatus.EXCEPTION, false);
            if (nonNull(requestFailureCase)) {
                response.addWarning(REQUEST_FAILURE_WARNING);
                log.info("Warning on case {}: There is a failed request in List assist", caseData.getCcdCaseId());
            }
        }
        return response;
    }
}
