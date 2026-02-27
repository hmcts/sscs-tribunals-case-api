package uk.gov.hmcts.reform.sscs.ccd.presubmit.awaitotherpartydata;

import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class AwaitOtherPartyDataAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final boolean cmOtherPartyConfidentialityEnabled;

    public AwaitOtherPartyDataAboutToSubmitHandler(
        @Value("${feature.cm-other-party-confidentiality.enabled}") boolean cmOtherPartyConfidentialityEnabled) {
        this.cmOtherPartyConfidentialityEnabled = cmOtherPartyConfidentialityEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return cmOtherPartyConfidentialityEnabled && callbackType == CallbackType.ABOUT_TO_SUBMIT;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
        String userAuthorisation) {

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }


        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        log.error("******* State: {}, Event: {} *******", sscsCaseData.getState(), callback.getCaseDetails().getState());


        State currentState = callback.getCaseDetails().getState();

        if ((sscsCaseData.isBenefitType(CHILD_SUPPORT) && currentState == State.AWAIT_OTHER_PARTY_DATA)
            || (sscsCaseData.isBenefitType(UC) && currentState == State.WITH_DWP)) {
            sscsCaseData.getExtendedSscsCaseData().setEnableAddOtherPartyData(YesNo.YES);
        } else {
            sscsCaseData.getExtendedSscsCaseData().setEnableAddOtherPartyData(YesNo.NO);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

}
