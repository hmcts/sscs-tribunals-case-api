package uk.gov.hmcts.reform.sscs.ccd.presubmit.fenoaction;

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
            && callback.getEvent().equals(EventType.FE_NO_ACTION)
            && DwpState.FE_RECEIVED.getValue().equals(callback.getCaseDetails().getCaseData().getDwpState());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(DwpState.FE_ACTIONED_NR.getValue(), DwpState.FE_ACTIONED_NR.getLabel()));
        listOptions.add(new DynamicListItem(DwpState.FE_ACTIONED_NA.getValue(), DwpState.FE_ACTIONED_NA.getLabel()));
        callback.getCaseDetails().getCaseData()
            .setDwpStateFeNoAction(new DynamicList(listOptions.get(0), listOptions));
        return new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
    }
}
