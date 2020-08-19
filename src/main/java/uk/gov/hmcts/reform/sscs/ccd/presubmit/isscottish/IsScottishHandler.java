package uk.gov.hmcts.reform.sscs.ccd.presubmit.isscottish;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class IsScottishHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() != EventType.CREATE_BUNDLE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        String isScotCase = isScottishCase(caseData.getRegionalProcessingCenter(), caseData);

        caseData.setIsScottishCase(isScotCase);

        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        return sscsCaseDataPreSubmitCallbackResponse;
    }

    public static String isScottishCase(RegionalProcessingCenter rpc, SscsCaseData caseData) {

        if (isNull(rpc) || isNull(rpc.getName())) {
            log.info("Setting isScottishCase field to No for empty RPC for case " + caseData.getCcdCaseId());
            return "No";
        } else {
            String isScotCase = rpc.getName().equalsIgnoreCase("GLASGOW") ? "Yes" : "No";
            log.info("Setting isScottishCase field to " + isScotCase + " for RPC " + rpc.getName() + " for case " + caseData.getCcdCaseId());
            return isScotCase;
        }
    }
}
