package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuehearingenquiryform;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

        final PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        checkOtherPartiesSelectionContainsNoDuplicates(sscsCaseData, response);
        checkDocumentsContainsNoDuplicates(sscsCaseData, response);

        return response;
    }

    private static void checkOtherPartiesSelectionContainsNoDuplicates(SscsCaseData sscsCaseData,
        PreSubmitCallbackResponse<SscsCaseData> response) {

        if (isNotEmpty(sscsCaseData.getOtherPartySelection())) {
            final List<String> selectedOtherPartyIds = sscsCaseData.getOtherPartySelection().stream()
                .map(CcdValue::getValue)
                .map(OtherPartySelectionDetails::getOtherPartiesList)
                .filter(Objects::nonNull)
                .map(DynamicList::getValue)
                .filter(Objects::nonNull)
                .map(DynamicListItem::getCode)
                .toList();

            if (selectedOtherPartyIds.size() != new HashSet<>(selectedOtherPartyIds).size()) {
                response.addError("Other parties cannot be selected more than once");
            }
        }
    }

    private static void checkDocumentsContainsNoDuplicates(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {

        if (isNotEmpty(sscsCaseData.getDocumentSelection())) {
            final List<String> documentCodes = sscsCaseData.getDocumentSelection().stream()
                .map(CcdValue::getValue)
                .map(DocumentSelectionDetails::getDocumentsList)
                .filter(Objects::nonNull)
                .map(DynamicList::getValue)
                .filter(Objects::nonNull)
                .map(DynamicListItem::getCode)
                .toList();

            if (documentCodes.size() != new HashSet<>(documentCodes).size()) {
                response.addError("The same document cannot be selected more than once");
            }
        }
    }

}
