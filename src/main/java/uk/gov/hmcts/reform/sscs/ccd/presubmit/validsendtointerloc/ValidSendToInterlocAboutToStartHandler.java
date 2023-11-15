package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.*;
import static uk.gov.hmcts.reform.sscs.util.PartiesOnCaseUtil.getPartiesOnCase;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class ValidSendToInterlocAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean postponementsFeature;
    private final boolean postHearingsB;

    public ValidSendToInterlocAboutToStartHandler(@Value("${feature.postponements.enabled}")  boolean postponementsFeature,
                                                  @Value("${feature.postHearingsB.enabled}")  boolean postHearingsB) {
        this.postponementsFeature = postponementsFeature;
        this.postHearingsB = postHearingsB;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
            && (callback.getEvent() == EventType.VALID_SEND_TO_INTERLOC
                || callback.getEvent() == EventType.ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        setSelectWhoReviewsCase(sscsCaseData);
        setOriginalSenderDropdown(sscsCaseData);

        if (postHearingsB) {
            sscsCaseData.setPrePostHearing(null);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setSelectWhoReviewsCase(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(REVIEW_BY_TCW.getId(), REVIEW_BY_TCW.getLabel()));
        listOptions.add(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()));

        if (postponementsFeature) {
            listOptions.add(new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(), POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()));
        }

        sscsCaseData.setSelectWhoReviewsCase(new DynamicList(new DynamicListItem("", ""), listOptions));
    }

    private void setOriginalSenderDropdown(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = getPartiesOnCase(sscsCaseData);

        sscsCaseData.setOriginalSender(new DynamicList(listOptions.get(0), listOptions));
    }
}
