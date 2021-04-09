package uk.gov.hmcts.reform.sscs.ccd.presubmit.isscottish;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.List;
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

    List<EventType> permittedEvents = Arrays.asList(EventType.VALID_APPEAL_CREATED,
            EventType.INCOMPLETE_APPLICATION_RECEIVED, EventType.NON_COMPLIANT,
            EventType.DRAFT_TO_INCOMPLETE_APPLICATION, EventType.DRAFT_TO_NON_COMPLIANT,
            EventType.DRAFT_TO_VALID_APPEAL_CREATED);

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && permittedEvents.contains(callback.getEvent());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        String isScotCase = isScottishCase(caseData.getRegionalProcessingCenter(), caseData);

        if (! isScotCase.equals(caseData.getIsScottishCase())) {
            log.info("Setting isScottishCase field to " + isScotCase + " for case " + caseData.getCcdCaseId());
            caseData.setIsScottishCase(isScotCase);
        } else {
            log.info("Keeping isScottishCase field as " + isScotCase + " for case " + caseData.getCcdCaseId());
        }

        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        return sscsCaseDataPreSubmitCallbackResponse;
    }

    public static String isScottishCase(RegionalProcessingCenter rpc, SscsCaseData caseData) {

        if (isNull(rpc) || isNull(rpc.getName())) {
            log.info("Calculated isScottishCase field to No for empty RPC for case " + caseData.getCcdCaseId());
            return "No";
        } else {
            String isScotCase = rpc.getName().equalsIgnoreCase("GLASGOW") ? "Yes" : "No";
            log.info("Calculated isScottishCase field to " + isScotCase + " for RPC " + rpc.getName() + " for case " + caseData.getCcdCaseId());
            return isScotCase;
        }
    }
}
