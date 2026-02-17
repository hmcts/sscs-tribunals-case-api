package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuehearingenquiryform;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.HashSet;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentSelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class IssueHearingEnquiryFormMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT) && callback.getEvent() == EventType.ISSUE_HEARING_ENQUIRY_FORM;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
        String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        checkOtherPartiesSelectionContainsNoDuplicates(sscsCaseData, response);
        checkDocumentsContainsNoDuplicates(sscsCaseData, response);

        return response;
    }

    private static void checkOtherPartiesSelectionContainsNoDuplicates(SscsCaseData sscsCaseData,
        PreSubmitCallbackResponse<SscsCaseData> response) {
        if (sscsCaseData.getSendToOtherParties() == YES) {
            List<String> selectedOtherPartyIds = sscsCaseData.getOtherPartySelection().stream().map(CcdValue::getValue)
                .map(OtherPartySelectionDetails::getOtherPartiesList).map(DynamicList::getValue).map(DynamicListItem::getCode)
                .toList();

            boolean hasDuplicateOtherPartySelections = selectedOtherPartyIds.size() != new HashSet<>(
                selectedOtherPartyIds).size();

            if (hasDuplicateOtherPartySelections) {
                response.addError("Other parties cannot be selected more than once");
            }

        }
    }

    private void checkDocumentsContainsNoDuplicates(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {

        if (CollectionUtils.isNotEmpty(sscsCaseData.getDocumentSelection())) {


            List<String> list = sscsCaseData.getDocumentSelection().stream().map(CcdValue::getValue)
                .map(DocumentSelectionDetails::getDocumentsList).map(DynamicList::getValue).map(DynamicListItem::getCode)
                .toList();

            boolean hasDuplicateOtherPartySelections = list.size() != new HashSet<>(
                list).size();

            if (hasDuplicateOtherPartySelections) {
                response.addError("The same document cannot be selected more than once");
            }
        }
    }

}
