package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
public class HandleEvidenceEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.ACTION_FURTHER_EVIDENCE;
    }

    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        if (sscsCaseData.getScannedDocuments() == null) {
            PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse =
                new PreSubmitCallbackResponse<>(sscsCaseData);
            preSubmitCallbackResponse.addError("No further evidence to process");
            return preSubmitCallbackResponse;
        }

        buildSscsDocumentFromScan(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void buildSscsDocumentFromScan(SscsCaseData sscsCaseData) {

        if (sscsCaseData.getScannedDocuments() != null) {
            for (ScannedDocument scannedDocument : sscsCaseData.getScannedDocuments()) {
                if (scannedDocument != null && scannedDocument.getValue() != null) {
                    List<SscsDocument> documents = new ArrayList<>();
                    if (sscsCaseData.getSscsDocument() != null) {
                        documents = sscsCaseData.getSscsDocument();
                    }
                    documents.add(buildSscsDocument(sscsCaseData, scannedDocument));
                    sscsCaseData.setSscsDocument(documents);
                }
            }
        }
        sscsCaseData.setScannedDocuments(null);

    }

    private SscsDocument buildSscsDocument(SscsCaseData sscsCaseData, ScannedDocument scannedDocument) {
        String controlNumber = scannedDocument.getValue().getControlNumber();
        String fileName = scannedDocument.getValue().getFileName();

        LocalDateTime localDate = LocalDateTime.parse(scannedDocument.getValue().getScannedDate());

        String scannedDate = localDate.format(DateTimeFormatter.ISO_DATE);
        DocumentLink url = scannedDocument.getValue().getUrl();

        return SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentType(getSubtype(sscsCaseData.getFurtherEvidenceAction().getValue().getCode(),
                sscsCaseData.getOriginalSender().getValue().getCode()))
            .documentFileName(fileName)
            .documentLink(url)
            .documentDateAdded(scannedDate)
            .controlNumber(controlNumber)
            .build()).build();
    }

    private String getSubtype(String code, String originalSender) {
        if (OTHER_DOCUMENT_MANUAL.getCode().equals(code)) {
            return DocumentType.OTHER_DOCUMENT.getValue();
        }
        if (ISSUE_FURTHER_EVIDENCE.getCode().equals(code) && "appellant".equals(originalSender)) {
            return "appellantEvidence";
        }
        throw new RuntimeException("document Type could not be worked out");
    }

}
