package uk.gov.hmcts.reform.sscs.ccd.presubmit.dormant;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CONFIRM_LAPSED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DORMANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.WITHDRAWN;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.TaskManagementApiService;

@Slf4j
@Service
@RequiredArgsConstructor
public class DormantEventsSubmittedHandler  implements PreSubmitCallbackHandler<SscsCaseData> {

    private final TaskManagementApiService taskManagementApiService;

    @Value("${feature.work-allocation.enabled}")
    private final boolean isWorkAllocationEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
                && (callback.getEvent() == WITHDRAWN
                || callback.getEvent() == DORMANT
                || callback.getEvent() == CONFIRM_LAPSED
            );
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        final String caseId = String.valueOf(callback.getCaseDetails().getId());

        log.info("Handling {} Case Submitted callback for case id: {}", callback.getEvent().getCcdType(), caseId);

        if (isWorkAllocationEnabled) {
            taskManagementApiService.cancelTasksByTaskProperties(caseId, "ftaCommunicationId");
        }

        return new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
    }
}
