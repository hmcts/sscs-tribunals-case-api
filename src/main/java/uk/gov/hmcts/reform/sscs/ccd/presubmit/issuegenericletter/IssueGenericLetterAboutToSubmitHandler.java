package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuegenericletter;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentSelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class IssueGenericLetterAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.ISSUE_GENERIC_LETTER;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        if (!isEmpty(sscsCaseData.getDocumentSelection())) {
            removeDocumentsDuplicates(sscsCaseData);
        }

        if (!isEmpty(sscsCaseData.getOtherPartySelection())) {
            removeOtherPartyDuplicates(sscsCaseData);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void removeOtherPartyDuplicates(SscsCaseData sscsCaseData) {
        var list = new ArrayList<CcdValue<OtherPartySelectionDetails>>();

        var duplicates = new ArrayList<>();

        for (CcdValue<OtherPartySelectionDetails> otherParty : sscsCaseData.getOtherPartySelection()) {
            var otherPartySelection = otherParty.getValue().getOtherPartiesList();
            var test = otherPartySelection.getValue();

            if (!duplicates.contains(test)) {
                list.add(otherParty);
                duplicates.add(test);
            }
        }

        sscsCaseData.setOtherPartySelection(list);
    }

    private void removeDocumentsDuplicates(SscsCaseData sscsCaseData) {
        var list = new ArrayList<CcdValue<DocumentSelectionDetails>>();

        var duplicates = new ArrayList<>();

        for (CcdValue<DocumentSelectionDetails> document : sscsCaseData.getDocumentSelection()) {
            var documentSelection = document.getValue().getDocumentsList();
            var test = documentSelection.getValue();

            if (!duplicates.contains(test)) {
                list.add(document);
                duplicates.add(test);
            }
        }

        sscsCaseData.setDocumentSelection(list);
    }
}
