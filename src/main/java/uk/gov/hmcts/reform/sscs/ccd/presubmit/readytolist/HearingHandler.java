package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

@Slf4j
public enum HearingHandler {
    GAPS {
        @Override
        public PreSubmitCallbackResponse<SscsCaseData> handle(SscsCaseData sscsCaseData, boolean gapsSwitchOverFeature) {
            if (gapsSwitchOverFeature){
                sscsCaseData.setHearingRoute(HearingRoute.GAPS);
                sscsCaseData.setHearingState(HearingState.HEARING_CREATED);
            }
            PreSubmitCallbackResponse<uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
            log.info(String.format("createdInGapsFrom is %s for caseId %s", sscsCaseData.getCreatedInGapsFrom(), sscsCaseData.getCcdCaseId()));
            if (sscsCaseData.getCreatedInGapsFrom() == null
                || StringUtils.equalsIgnoreCase(sscsCaseData.getCreatedInGapsFrom(), State.VALID_APPEAL.getId())) {
                callbackResponse.addError("Case already created in GAPS at valid appeal.");
                log.warn(String.format("Case already created in GAPS at valid appeal for caseId %s.", sscsCaseData.getCcdCaseId()));
            }
            return callbackResponse;
        }
    },
    LIST_ASSIST {
        @Override
        public PreSubmitCallbackResponse<SscsCaseData> handle(SscsCaseData sscsCaseData, boolean gapsSwitchOverFeature) {
            if (gapsSwitchOverFeature) {
                sscsCaseData.setHearingRoute(HearingRoute.LIST_ASSIST);
                sscsCaseData.setHearingState(HearingState.HEARING_CREATED);
            }
            return new PreSubmitCallbackResponse<>(sscsCaseData);

        }
    };

    public abstract PreSubmitCallbackResponse<SscsCaseData> handle(SscsCaseData caseData, boolean gapsSwitchOverFeature);
}
