package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase.CreateCaseAboutToStartHandler.isCreateCaseStartCallback;
import static uk.gov.hmcts.reform.sscs.config.MetricsConstants.*;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;

@Slf4j
@Component
public class PreSubmitCallbackDispatcher<T extends CaseData> {

    private final List<PreSubmitCallbackHandler<T>> callbackHandlers;
    private final MeterRegistry meterRegistry;

    public PreSubmitCallbackDispatcher(List<PreSubmitCallbackHandler<T>> callbackHandlers,
                                       MeterRegistry meterRegistry) {
        requireNonNull(callbackHandlers, "callbackHandlers must not be null");
        this.callbackHandlers = callbackHandlers;
        this.meterRegistry = meterRegistry;
    }

    public PreSubmitCallbackResponse<T> handle(CallbackType callbackType, Callback<T> callback, String userAuth) {
        requireNonNull(callback, "callback must not be null");

        String eventType = callback.getEvent() != null ? callback.getEvent().getCcdType() : "unknown";
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            PreSubmitCallbackResponse<T> response = doHandle(callbackType, callback, userAuth);
            sample.stop(Timer.builder(CCD_CALLBACK_DURATION)
                .tag(TAG_EVENT_TYPE, eventType)
                .tag(TAG_OUTCOME, "success")
                .register(meterRegistry));
            return response;
        } catch (Exception e) {
            sample.stop(Timer.builder(CCD_CALLBACK_DURATION)
                .tag(TAG_EVENT_TYPE, eventType)
                .tag(TAG_OUTCOME, "error")
                .register(meterRegistry));
            meterRegistry.counter(CCD_CALLBACK_ERRORS, TAG_EVENT_TYPE, eventType).increment();
            throw e;
        }
    }

    private PreSubmitCallbackResponse<T> doHandle(CallbackType callbackType, Callback<T> callback, String userAuth) {
        List<PreSubmitCallbackHandler<T>> eligibleHandlers = new ArrayList<>();
        PreSubmitCallbackResponse<T> aggregateResponse =
                new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());

        for (PreSubmitCallbackHandler<T> callbackHandler : callbackHandlers) {
            if (callbackHandler.canHandle(callbackType, callback)) {
                eligibleHandlers.add(callbackHandler);

                var updatedCallback = getUpdatedCallback(aggregateResponse.getData(), callback, callbackType);

                PreSubmitCallbackResponse<T> handlerResponse =
                        callbackHandler.handle(callbackType, updatedCallback, userAuth);

                aggregateResponse.setData(handlerResponse.getData());
                aggregateResponse.addErrors(handlerResponse.getErrors());
                aggregateResponse.addWarnings(handlerResponse.getWarnings());
            }
        }
        if (eligibleHandlers.size() > 1) {
            log.info("{} has more than one handler {}", callback.getEvent(), eligibleHandlers);
        }
        return aggregateResponse;
    }

    private Callback<T> getUpdatedCallback(T caseData, Callback<T> callback, CallbackType callbackType) {
        boolean isCreateCaseStartCallback = isCreateCaseStartCallback(callbackType, callback.getEvent());
        var updatedCaseDetails = new CaseDetails<>(
                callback.getCaseDetails().getId(),
                callback.getCaseDetails().getJurisdiction(),
                isCreateCaseStartCallback ? null : callback.getCaseDetails().getState(),
                caseData,
                isCreateCaseStartCallback ? null : callback.getCaseDetails().getCreatedDate(),
                callback.getCaseDetails().getCaseTypeId()
        );
        var updatedCallback = new Callback<>(
                updatedCaseDetails, callback.getCaseDetailsBefore(), callback.getEvent(), callback.isIgnoreWarnings()
        );
        updatedCallback.setPageId(callback.getPageId());
        return updatedCallback;
    }
}
