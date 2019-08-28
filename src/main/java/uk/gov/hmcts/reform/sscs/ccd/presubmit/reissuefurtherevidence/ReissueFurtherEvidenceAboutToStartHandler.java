package uk.gov.hmcts.reform.sscs.ccd.presubmit.reissuefurtherevidence;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DWP_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class ReissueFurtherEvidenceAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.REISSUE_FURTHER_EVIDENCE;
    }

    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        ArrayList<SscsDocument> availableDocumentsToReIssue =
                Optional.ofNullable(sscsCaseData.getSscsDocument()).map(Collection::stream)
                        .orElse(Stream.empty()).filter(f ->
                        APPELLANT_EVIDENCE.getValue().equals(f.getValue().getDocumentType())
                                || REPRESENTATIVE_EVIDENCE.getValue().equals(f.getValue().getDocumentType())
                                || DWP_EVIDENCE.getValue().equals(f.getValue().getDocumentType())
        ).collect(Collectors.toCollection(ArrayList::new));

        if (CollectionUtils.isNotEmpty(availableDocumentsToReIssue)) {
            setDocumentDropdown(sscsCaseData, availableDocumentsToReIssue);
            sscsCaseData.setResendToAppellant(null);
            sscsCaseData.setResendToRepresentative(null);
            sscsCaseData.setResendToDwp(null);
            sscsCaseData.setOriginalSender(null);
        }

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (CollectionUtils.isEmpty(availableDocumentsToReIssue)) {
            response.addError("There are no evidence documents in the appeal. Cannot reissue further evidence.");
        }
        return response;
    }

    private void setDocumentDropdown(SscsCaseData sscsCaseData, List<SscsDocument> availableDocumentsToReIssue) {
        List<DynamicListItem> listCostOptions = new ArrayList<>();

        for (SscsDocument doc: availableDocumentsToReIssue) {
            String label = String.format("%s -  %s", doc.getValue().getDocumentFileName(), userFriendlyName(doc.getValue().getDocumentType()));
            if (doc.getValue().getDocumentLink() != null) {
                listCostOptions.add(new DynamicListItem(doc.getValue().getDocumentLink().getDocumentUrl(), label));
            }
        }

        sscsCaseData.setReissueFurtherEvidenceDocument(new DynamicList(listCostOptions.get(0), listCostOptions));
    }

    private String userFriendlyName(String documentType) {
        return StringUtils.capitalize(StringUtils.join(Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(documentType)).map(StringUtils::uncapitalize).toArray(String[]::new), " "));
    }

}
