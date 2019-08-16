package uk.gov.hmcts.reform.sscs.ccd.presubmit.reissuefurtherevidence;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

public class ReissueFurtherEvidenceAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.REISSUE_FURTHER_EVIDENCE;
    }

    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        Optional<String> selectedDocumentUrl = Optional.ofNullable(sscsCaseData.getReissueFurtherEvidenceDocument()).map(f -> f.getValue().getCode());
        ArrayList<String> errors = new ArrayList<>();
        if (!selectedDocumentUrl.isPresent()) {
            errors.add("Select a document to re-issue further evidence.");
        } else {
            Optional<SscsDocument> optionalSelectedDocument = sscsCaseData.getSscsDocument().stream().filter(f -> selectedDocumentUrl.get().equals(f.getValue().getDocumentLink().getDocumentUrl())).findFirst();
            if (!optionalSelectedDocument.isPresent()) {
                errors.add(String.format("Could not find the selected document with url '%s' to re-issue further evidence in the appeal with id '%s'.", selectedDocumentUrl.get(), sscsCaseData.getCcdCaseId()));
            } else {
                SscsDocumentDetails documentDetails = optionalSelectedDocument.get().getValue();
                documentDetails.setEvidenceIssued("No");
            }
        }
        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (CollectionUtils.isNotEmpty(errors)) {
            callbackResponse.addErrors(errors);
        }
        return callbackResponse;
    }
}
