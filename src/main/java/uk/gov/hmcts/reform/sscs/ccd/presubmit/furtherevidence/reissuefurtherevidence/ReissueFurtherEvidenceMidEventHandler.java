package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.reissuefurtherevidence;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DWP_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.OriginalSenderItemList.APPELLANT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.OriginalSenderItemList.DWP;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.OriginalSenderItemList.REPRESENTATIVE;

import java.util.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.OriginalSenderItemList;

@Service
public class ReissueFurtherEvidenceMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static Map<String, OriginalSenderItemList> documentTypeToOriginalSender = new HashMap<>();

    static {
        documentTypeToOriginalSender.put(APPELLANT_EVIDENCE.getValue(), APPELLANT);
        documentTypeToOriginalSender.put(REPRESENTATIVE_EVIDENCE.getValue(), REPRESENTATIVE);
        documentTypeToOriginalSender.put(DWP_EVIDENCE.getValue(), DWP);
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null.");
        requireNonNull(callbackType, "callbacktype must not be null.");

        return callbackType.equals(CallbackType.MID_EVENT)
                && callback.getEvent() == EventType.REISSUE_FURTHER_EVIDENCE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        ArrayList<String> errors = new ArrayList<>();
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        Optional<String> selectedDocumentUrl = Optional.ofNullable(sscsCaseData.getReissueFurtherEvidenceDocument()).map(f -> Optional.ofNullable(f.getValue()).map(DynamicListItem::getCode).orElse(""));
        if (!selectedDocumentUrl.isPresent() || selectedDocumentUrl.map(StringUtils::isBlank).orElse(true)) {
            errors.add("Select a document to re-issue further evidence.");
        } else {
            Optional<SscsDocument> optionalSelectedDocument = sscsCaseData.getSscsDocument().stream().filter(f -> selectedDocumentUrl.get().equals(f.getValue().getDocumentLink().getDocumentUrl())).findFirst();
            if (!optionalSelectedDocument.isPresent()) {
                errors.add(String.format("Could not find the selected document with url '%s' to re-issue further evidence in the appeal with id '%s'.", selectedDocumentUrl.get(), sscsCaseData.getCcdCaseId()));
            } else {
                List<DynamicListItem> listCostOptions = new ArrayList<>();
                listCostOptions.add(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
                if (sscsCaseData.getAppeal().getRep() != null
                        && equalsIgnoreCase(sscsCaseData.getAppeal().getRep().getHasRepresentative(), "yes")) {
                    listCostOptions.add(new DynamicListItem(REPRESENTATIVE.getCode(), REPRESENTATIVE.getLabel()));
                }
                listCostOptions.add(new DynamicListItem(DWP.getCode(), DWP.getLabel()));
                SscsDocumentDetails documentDetails = optionalSelectedDocument.get().getValue();
                OriginalSenderItemList originalSender = Optional.ofNullable(documentTypeToOriginalSender.get(documentDetails.getDocumentType())).orElse(APPELLANT);
                DynamicListItem value = new DynamicListItem(originalSender.getCode(), originalSender.getLabel());
                sscsCaseData.setOriginalSender(new DynamicList(value, listCostOptions));
            }
        }
        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (CollectionUtils.isNotEmpty(errors)) {
            callbackResponse.addErrors(errors);
        }

        return callbackResponse;
    }

}
