package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;

@Service
@Slf4j
public class CreateBundleSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final ServiceRequestExecutor serviceRequestExecutor;

    private final String bundleUrl;

    private static final  String CREATE_BUNDLE_ENDPOINT = "/api/new-bundle";

    @Autowired
    public CreateBundleSubmittedHandler(ServiceRequestExecutor serviceRequestExecutor,
                                        @Value("${bundle.url}") String bundleUrl) {
        this.serviceRequestExecutor = serviceRequestExecutor;
        this.bundleUrl = bundleUrl;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
                && callback.getEvent() == EventType.CREATE_BUNDLE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final PreSubmitCallbackResponse<SscsCaseData> response =
                serviceRequestExecutor.post(callback, bundleUrl + CREATE_BUNDLE_ENDPOINT);
        if (response.getErrors().size() > 0) {
            log.info("bundling returned an error");
        }
        if (response.getWarnings().size() > 0) {
            log.info("bundling returned a warning");
        }
        return response;
    }

}
