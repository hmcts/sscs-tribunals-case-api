package uk.gov.hmcts.reform.sscs.ccd.presubmit.abatecase;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
public class TestEventAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.TEST_EVENT;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem("reviewByTcw", "Review by TCW"));
        listOptions.add(new DynamicListItem("reviewByJudge", "Review by Judge"));

        List<DynamicListItem> listOptions2 = new ArrayList<>();
        listOptions2.add(new DynamicListItem("reviewByTcw2", "Review by TCW2"));
        listOptions2.add(new DynamicListItem("reviewByJudge2", "Review by Judge2"));

        sscsCaseData.setTestField(TestField.builder().testFieldDl(new DynamicList(new DynamicListItem("", ""), listOptions)).build());
        sscsCaseData.setTestFieldTop(new DynamicList(new DynamicListItem("", ""), listOptions2));

        return preSubmitCallbackResponse;
    }
}
