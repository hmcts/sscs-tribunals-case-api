package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.fenoaction;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.DwpState;
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
        if (!DwpState.FE_RECEIVED.getId().equals(caseData.getDwpState())) {
            PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);
            response.addError("The dwp state value has to be 'FE received' in order to run this event");
            return response;
        }
        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(DwpState.FE_ACTIONED_NR.getId(), DwpState.FE_ACTIONED_NR.getLabel()));
        listOptions.add(new DynamicListItem(DwpState.FE_ACTIONED_NA.getId(), DwpState.FE_ACTIONED_NA.getLabel()));
        caseData.setDwpStateFeNoAction(new DynamicList(listOptions.get(0), listOptions));
        return new PreSubmitCallbackResponse<>(caseData);
    }
}
