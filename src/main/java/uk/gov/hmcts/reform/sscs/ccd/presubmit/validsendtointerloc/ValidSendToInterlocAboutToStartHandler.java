package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class ValidSendToInterlocAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final Boolean validSendToInterlocFeature;

    public ValidSendToInterlocAboutToStartHandler(@Value("${feature.valid_send_to_interloc}") Boolean validSendToInterlocFeature) {
        this.validSendToInterlocFeature = validSendToInterlocFeature;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return validSendToInterlocFeature && callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.VALID_SENT_TO_INTERLOC;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        setSelectWhoReviewsCase(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setSelectWhoReviewsCase(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem("reviewByTcw", "Review by TCW"));
        listOptions.add(new DynamicListItem("reviewByJudge", "Review by Judge"));

        sscsCaseData.setSelectWhoReviewsCase(new DynamicList(new DynamicListItem("", ""), listOptions));
    }

}
