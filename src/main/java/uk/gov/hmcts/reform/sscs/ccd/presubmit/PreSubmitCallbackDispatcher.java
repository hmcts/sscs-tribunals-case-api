package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.requireNonNull;

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

    public PreSubmitCallbackDispatcher(List<PreSubmitCallbackHandler<T>> callbackHandlers) {
        requireNonNull(callbackHandlers, "callbackHandlers must not be null");
        this.callbackHandlers = callbackHandlers;
    }

    public PreSubmitCallbackResponse<T> handle(CallbackType callbackType, Callback<T> callback, String userAuth) {
        requireNonNull(callback, "callback must not be null");

        List<PreSubmitCallbackHandler<T>> eligibleHandlers = new ArrayList<>();
        PreSubmitCallbackResponse<T> aggregateResponse =
                new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());

        for (PreSubmitCallbackHandler<T> callbackHandler : callbackHandlers) {
            if (callbackHandler.canHandle(callbackType, callback)) {
                eligibleHandlers.add(callbackHandler);

                PreSubmitCallbackResponse<T> handlerResponse =
                        callbackHandler.handle(callbackType, getUpdatedCallback(aggregateResponse, callback), userAuth);

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

    private Callback<T> getUpdatedCallback(PreSubmitCallbackResponse<T> aggregateResponse,
                                                     Callback<T> callback) {
        var updatedCaseDetails = new CaseDetails<>(
                callback.getCaseDetails().getId(),
                callback.getCaseDetails().getJurisdiction(),
                callback.getCaseDetails().getState(),
                aggregateResponse.getData(),
                callback.getCaseDetails().getCreatedDate(),
                callback.getCaseDetails().getCaseTypeId()
        );
        return new Callback<>(
                updatedCaseDetails, callback.getCaseDetailsBefore(), callback.getEvent(), callback.isIgnoreWarnings()
        );
    }
}
