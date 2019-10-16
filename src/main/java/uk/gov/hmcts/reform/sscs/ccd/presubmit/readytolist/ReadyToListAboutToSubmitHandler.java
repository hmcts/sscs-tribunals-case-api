package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class ReadyToListAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.READY_TO_LIST;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        log.info(String.format("createdInGapsFrom is %s for caseId %s",
                sscsCaseData.getCreatedInGapsFrom(), sscsCaseData.getCcdCaseId()));

        if (sscsCaseData.getCreatedInGapsFrom() == null
                || StringUtils.equalsIgnoreCase(sscsCaseData.getCreatedInGapsFrom(), State.VALID_APPEAL.getId())) {
            callbackResponse.addError("Case already created in GAPS at valid appeal.");
            log.warn(String.format("Case already created in GAPS at valid appeal for caseId %s.", sscsCaseData.getCcdCaseId()));
        }
        return callbackResponse;
    }
}
