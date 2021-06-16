package uk.gov.hmcts.reform.sscs.ccd.presubmit.validappeal;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.ResponseEventsAboutToSubmit;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;

@Component
@Slf4j
public class ValidateAppealAboutToSubmitHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private final ServiceRequestExecutor serviceRequestExecutor;
    private final String bulkScanEndpoint;

    public ValidateAppealAboutToSubmitHandler(ServiceRequestExecutor serviceRequestExecutor,
                                              @Value("${bulk_scan.url}") String bulkScanUrl,
                                              @Value("${bulk_scan.validateEndpoint}") String validateEndpoint) {
        this.serviceRequestExecutor = serviceRequestExecutor;
        this.bulkScanEndpoint = String.format("%s%s", trimToEmpty(bulkScanUrl), trimToEmpty(validateEndpoint));
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.VALID_APPEAL
                && nonNull(callback.getCaseDetails())
                && nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        //Set digital flag on legacy cases
        if ((sscsCaseData.getCreatedInGapsFrom() == null
                || VALID_APPEAL.getId().equalsIgnoreCase(sscsCaseData.getCreatedInGapsFrom()))) {
            sscsCaseData.setCreatedInGapsFrom(READY_TO_LIST.getId());
        }

        return serviceRequestExecutor.post(callback, bulkScanEndpoint);
    }

}
