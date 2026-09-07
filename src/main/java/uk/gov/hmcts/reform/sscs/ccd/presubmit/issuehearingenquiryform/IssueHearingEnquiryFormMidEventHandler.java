package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuehearingenquiryform;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.util.SelectionValidator.documentSelectionContainsDuplicates;
import static uk.gov.hmcts.reform.sscs.ccd.util.SelectionValidator.otherPartySelectionContainsDuplicates;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class IssueHearingEnquiryFormMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
            && callback.getEvent() == EventType.ISSUE_HEARING_ENQUIRY_FORM;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
        String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        final PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (otherPartySelectionContainsDuplicates(sscsCaseData.getOtherPartySelection())) {
            response.addError("Other parties cannot be selected more than once");
        }
        if (documentSelectionContainsDuplicates(sscsCaseData.getDocumentSelection())) {
            response.addError("The same document cannot be selected more than once");
        }
        return response;
    }

}
