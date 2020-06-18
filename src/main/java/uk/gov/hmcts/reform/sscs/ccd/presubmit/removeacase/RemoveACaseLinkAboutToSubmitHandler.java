package uk.gov.hmcts.reform.sscs.ccd.presubmit.removeacase;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class RemoveACaseLinkAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.REMOVE_A_CASE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        final List<CaseLink> linkedCaseBefore = ofNullable(callback.getCaseDetailsBefore().orElse(caseDetails).getCaseData().getLinkedCase()).orElse(emptyList());
        final List<CaseLink> linkedCasesAfter = ofNullable(sscsCaseData.getLinkedCase()).orElse(emptyList());

        final PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (linkedCaseBefore.size() == 0) {
            preSubmitCallbackResponse.addError("There are no case links to remove.");
        } else if (linkedCasesAfter.containsAll(linkedCaseBefore)) {
            preSubmitCallbackResponse.addError("No case links have been selected to remove from the case.");
        }
        if (linkedCasesAfter.stream().filter(f -> !linkedCaseBefore.contains(f)).toArray().length > 0) {
            preSubmitCallbackResponse.addError("Cannot add a case link.");
        }
        if (preSubmitCallbackResponse.getErrors().isEmpty()) {
            sscsCaseData.setLinkedCase(linkedCasesAfter);
        }

        return preSubmitCallbackResponse;
    }

}
