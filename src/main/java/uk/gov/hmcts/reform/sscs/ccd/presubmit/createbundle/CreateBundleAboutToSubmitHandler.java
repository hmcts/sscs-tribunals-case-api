package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.bundling.BundlingHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.WorkAllocationFields;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreateBundleAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Autowired
    private final BundlingHandler bundlingHandler;

    @Autowired
    private final WorkAllocationService workAllocationService;

    @Value("${feature.work-allocation.enabled}")
    private final boolean workAllocationFeature;


    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.CREATE_BUNDLE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        WorkAllocationFields workAllocationFields = workAllocationFeature
                ? workAllocationService.updateAssignedCaseRoles(callback.getCaseDetails())
                : callback.getCaseDetails().getCaseData().getWorkAllocationFields();

        PreSubmitCallbackResponse<SscsCaseData> response = bundlingHandler.handle(callback);

        response.getData().setWorkAllocationFields(workAllocationFields);

        return response;
    }
}
