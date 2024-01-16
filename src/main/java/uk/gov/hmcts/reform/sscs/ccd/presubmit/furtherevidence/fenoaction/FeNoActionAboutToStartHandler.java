package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.fenoaction;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class FeNoActionAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType != null && callback != null && callbackType.equals(CallbackType.ABOUT_TO_START)
            && callback.getEvent().equals(EventType.FE_NO_ACTION);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        if (!DwpState.FE_RECEIVED.equals(caseData.getDwpState())) {
            PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);
            response.addError("The dwp state value has to be 'FE received' in order to run this event");
            return response;
        }
        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(DwpState.FE_ACTIONED_NR.getCcdDefinition(), DwpState.FE_ACTIONED_NR.getDescription()));
        listOptions.add(new DynamicListItem(DwpState.FE_ACTIONED_NA.getCcdDefinition(), DwpState.FE_ACTIONED_NA.getDescription()));
        caseData.setDwpStateFeNoAction(new DynamicList(listOptions.get(0), listOptions));
        return new PreSubmitCallbackResponse<>(caseData);
    }
}
