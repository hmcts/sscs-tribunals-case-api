package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.INFORMATION_RECEIVED_FOR_INTERLOC_TCW;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.APPELLANT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.DWP;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.REPRESENTATIVE;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class ActionFurtherEvidenceAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.issue_further_evidence}")
    private Boolean issueFurtherEvidenceFeature;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
            && callback.getEvent() == EventType.ACTION_FURTHER_EVIDENCE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        setFurtherEvidenceActionDropdown(sscsCaseData);
        setOriginalSenderDropdown(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setFurtherEvidenceActionDropdown(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        if (issueFurtherEvidenceFeature) {
            listOptions.add(new DynamicListItem(ISSUE_FURTHER_EVIDENCE.getCode(), ISSUE_FURTHER_EVIDENCE.getLabel()));
        }

        listOptions.add(new DynamicListItem(OTHER_DOCUMENT_MANUAL.getCode(), OTHER_DOCUMENT_MANUAL.getLabel()));

        if (StringUtils.isNotBlank(sscsCaseData.getInterlocReviewState())) {
            populateListWithItemsWhenInterlocStateIsNotBlank(listOptions);
        }

        sscsCaseData.setFurtherEvidenceAction(new DynamicList(listOptions.get(0), listOptions));
    }

    private void populateListWithItemsWhenInterlocStateIsNotBlank(List<DynamicListItem> listOptions) {
        listOptions.add(new DynamicListItem(INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE.getCode(),
            INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE.getLabel()));

        listOptions.add(new DynamicListItem(INFORMATION_RECEIVED_FOR_INTERLOC_TCW.getCode(),
            INFORMATION_RECEIVED_FOR_INTERLOC_TCW.getLabel()));

        listOptions.add(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(),
            SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel()));

        listOptions.add(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode(),
            SEND_TO_INTERLOC_REVIEW_BY_TCW.getLabel()));
    }

    private void setOriginalSenderDropdown(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        listOptions.add(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        listOptions.add(new DynamicListItem(DWP.getCode(), DWP.getLabel()));

        if (sscsCaseData.getAppeal().getRep() != null
            && equalsIgnoreCase(sscsCaseData.getAppeal().getRep().getHasRepresentative(), "yes")) {
            listOptions.add(new DynamicListItem(REPRESENTATIVE.getCode(), REPRESENTATIVE.getLabel()));
        }

        sscsCaseData.setOriginalSender(new DynamicList(listOptions.get(0), listOptions));

    }
}
