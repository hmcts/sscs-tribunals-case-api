package uk.gov.hmcts.reform.sscs.ccd.presubmit.reissuedocument;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;

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
public class ReissueDocumentAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.REISSUE_DOCUMENT;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        List<DynamicListItem> dropdownList = getDocumentDropdown(sscsCaseData);

        if (CollectionUtils.isEmpty(dropdownList)) {
            response.addError("There are no documents in this appeal available to reissue.");
        } else {
            sscsCaseData.setReissueFurtherEvidenceDocument(new DynamicList(dropdownList.get(0), dropdownList));
            sscsCaseData.setResendToAppellant(null);
            sscsCaseData.setResendToRepresentative(null);
        }

        return response;
    }

    private List<DynamicListItem> getDocumentDropdown(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listCostOptions = new ArrayList<>();

        if (null != sscsCaseData.getSscsDocument()) {
            if (sscsCaseData.getSscsDocument().stream()
                    .anyMatch(doc -> doc.getValue().getDocumentType().equals(DECISION_NOTICE.getValue()))) {
                listCostOptions.add(new DynamicListItem(EventType.DECISION_ISSUED.getCcdType(), DECISION_NOTICE.getValue()));
            }
            if (sscsCaseData.getSscsDocument().stream()
                    .anyMatch(doc -> doc.getValue().getDocumentType().equals(DIRECTION_NOTICE.getValue()))) {
                listCostOptions.add(new DynamicListItem(EventType.DIRECTION_ISSUED.getCcdType(), DIRECTION_NOTICE.getLabel()));
            }
            if (sscsCaseData.getSscsDocument().stream()
                    .anyMatch(doc -> doc.getValue().getDocumentType().equals(FINAL_DECISION_NOTICE.getValue()))) {
                listCostOptions.add(new DynamicListItem(EventType.ISSUE_FINAL_DECISION.getCcdType(), FINAL_DECISION_NOTICE.getLabel()));
            }
            if (sscsCaseData.getSscsDocument().stream()
                .anyMatch(doc -> doc.getValue().getDocumentType().equals(ADJOURNMENT_NOTICE.getValue()))) {
                listCostOptions.add(new DynamicListItem(EventType.ISSUE_ADJOURNMENT_NOTICE.getCcdType(), ADJOURNMENT_NOTICE.getLabel()));
            }
        }

        return listCostOptions;

    }
}
