package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@Service
public class ActionFurtherEvidenceDropdownHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.issue_further_evidence}")
    private Boolean issueFurtherEvidenceFeature;

    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.ACTION_FURTHER_EVIDENCE;
    }

    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
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
        List<DynamicListItem> listCostOptions  = new ArrayList<>();

        if (issueFurtherEvidenceFeature) {
            listCostOptions.add(new DynamicListItem("issueFurtherEvidence", "Issue further evidence to all parties"));
        }
        listCostOptions.add(new DynamicListItem("otherDocumentManual", "Other document type - action manually"));

        if (sscsCaseData.getInterlocReviewState() != null) {
            listCostOptions.add(new DynamicListItem("informationReceivedForInterloc", "Information received for interlocutory review"));
        }

        sscsCaseData.setFurtherEvidenceAction(new DynamicList(listCostOptions.get(0), listCostOptions));
    }

    private void setOriginalSenderDropdown(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listCostOptions  = new ArrayList<>();

        listCostOptions.add(new DynamicListItem("appellant", "Appellant (or Appointee)"));

        if (sscsCaseData.getAppeal().getRep() != null && sscsCaseData.getAppeal().getRep().getHasRepresentative().equalsIgnoreCase("yes")) {
            listCostOptions.add(new DynamicListItem("representative", "Representative"));
        }

        sscsCaseData.setOriginalSender(new DynamicList(listCostOptions.get(0), listCostOptions));

    }
}
