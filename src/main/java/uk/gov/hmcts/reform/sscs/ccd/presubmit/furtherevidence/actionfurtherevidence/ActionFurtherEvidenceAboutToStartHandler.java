package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.OriginalSenderItemList.*;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class ActionFurtherEvidenceAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

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

        populateListWithItems(listOptions, ISSUE_FURTHER_EVIDENCE, OTHER_DOCUMENT_MANUAL,
                INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE, INFORMATION_RECEIVED_FOR_INTERLOC_TCW,
                SEND_TO_INTERLOC_REVIEW_BY_JUDGE, SEND_TO_INTERLOC_REVIEW_BY_TCW);

        sscsCaseData.setFurtherEvidenceAction(new DynamicList(listOptions.get(0), listOptions));
    }

    private void populateListWithItems(List<DynamicListItem> listOptions,
                                       FurtherEvidenceActionDynamicListItems... items) {
        for (FurtherEvidenceActionDynamicListItems item : items) {
            listOptions.add(new DynamicListItem(item.getCode(), item.getLabel()));
        }
    }

    private void setOriginalSenderDropdown(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        listOptions.add(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        listOptions.add(new DynamicListItem(DWP.getCode(), DWP.getLabel()));
        if (sscsCaseData.isThereAJointParty()) {
            listOptions.add(new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel()));
        }

        if (sscsCaseData.getAppeal().getRep() != null
            && equalsIgnoreCase(sscsCaseData.getAppeal().getRep().getHasRepresentative(), "yes")) {
            listOptions.add(new DynamicListItem(REPRESENTATIVE.getCode(), REPRESENTATIVE.getLabel()));
        }
        listOptions.add(new DynamicListItem(HMCTS.getCode(), HMCTS.getLabel()));

        sscsCaseData.setOriginalSender(new DynamicList(listOptions.get(0), listOptions));

    }
}
