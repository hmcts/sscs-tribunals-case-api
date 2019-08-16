package uk.gov.hmcts.reform.sscs.ccd.presubmit.reissuefurtherevidence;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class ReissueFurtherEvidenceAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.REISSUE_FURTHER_EVIDENCE;
    }

    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        if (CollectionUtils.isNotEmpty(sscsCaseData.getSscsDocument())) {
            setDocumentDropdown(sscsCaseData);
        }

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (CollectionUtils.isEmpty(sscsCaseData.getSscsDocument())) {
            response.addError("There are no documents in the appeal. Cannot reissue further evidence.");
        }
        return response;
    }

    private void setDocumentDropdown(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listCostOptions = new ArrayList<>();

        for (SscsDocument doc: sscsCaseData.getSscsDocument()) {
            String label = String.format("%s -  %s", doc.getValue().getDocumentFileName(), doc.getValue().getDocumentType());
            if (doc.getValue().getDocumentLink() != null) {
                listCostOptions.add(new DynamicListItem(doc.getValue().getDocumentLink().getDocumentUrl(), label));
            }
        }

        sscsCaseData.setReissueFurtherEvidenceDocument(new DynamicList(listCostOptions.get(0), listCostOptions));
    }
}
