package uk.gov.hmcts.reform.sscs.ccd.presubmit.reissuefurtherevidence;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.APPELLANT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.DWP;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.REPRESENTATIVE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class ReissueFurtherEvidenceAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static Map<String, String> originalSenderToDocumentType = new HashMap<>();

    static {
        originalSenderToDocumentType.put(APPELLANT.getCode(), APPELLANT_EVIDENCE.getValue());
        originalSenderToDocumentType.put(REPRESENTATIVE.getCode(), REPRESENTATIVE_EVIDENCE.getValue());
        originalSenderToDocumentType.put(DWP.getCode(), DWP_EVIDENCE.getValue());
    }

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
                String documentType = originalSenderToDocumentType.get(
                        Optional.ofNullable(sscsCaseData.getOriginalSender())
                                .map(f -> Optional.ofNullable(f.getValue())
                                        .map(DynamicListItem::getCode))
                                .orElse(Optional.of(""))
                                .orElse(""));
                if (StringUtils.isNotBlank(documentType)) {
                    documentDetails.setDocumentType(documentType);
                } else {
                    errors.add(String.format("Cannot work out the original sender from the selected '%s'.", documentType));
                }
            }
        }
        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (CollectionUtils.isNotEmpty(errors)) {
            callbackResponse.addErrors(errors);
        }
        return callbackResponse;
    }
}
