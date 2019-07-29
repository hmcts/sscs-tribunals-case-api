package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.APPELLANT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.REPRESENTATIVE;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
public class HandleEvidenceEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse;

    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.ACTION_FURTHER_EVIDENCE
            && caseData.getFurtherEvidenceAction() != null
            && caseData.getOriginalSender() != null;
    }

    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        buildSscsDocumentFromScan(sscsCaseData);

        return preSubmitCallbackResponse;
    }

    private void buildSscsDocumentFromScan(SscsCaseData sscsCaseData) {

        if (sscsCaseData.getScannedDocuments() != null) {
            for (ScannedDocument scannedDocument : sscsCaseData.getScannedDocuments()) {
                if (scannedDocument != null && scannedDocument.getValue() != null) {

                    if (scannedDocument.getValue().getUrl() == null) {
                        preSubmitCallbackResponse.addError("No document URL so could not process");
                    }

                    if (scannedDocument.getValue().getFileName() == null) {
                        preSubmitCallbackResponse.addError("No document file name so could not process");
                    }

                    List<SscsDocument> documents = new ArrayList<>();
                    if (sscsCaseData.getSscsDocument() != null) {
                        documents = sscsCaseData.getSscsDocument();
                    }

                    SscsDocument sscsDocument = buildSscsDocument(sscsCaseData, scannedDocument);
                    documents.add(sscsDocument);
                    sscsCaseData.setSscsDocument(documents);
                    sscsCaseData.setEvidenceHandled(workOutEvidenceHandled(sscsCaseData.getEvidenceHandled(),
                        sscsDocument.getValue().getDocumentType()));
                }
            }
        } else {
            preSubmitCallbackResponse.addError("No further evidence to process");
        }

        sscsCaseData.setScannedDocuments(null);

    }

    private String workOutEvidenceHandled(String evidenceHandled, String documentType) {
        if ((StringUtils.isBlank(evidenceHandled) || "No".equalsIgnoreCase(evidenceHandled))
            && (documentType.equals(APPELLANT_EVIDENCE.getValue())
            || documentType.equals(REPRESENTATIVE_EVIDENCE.getValue()))) {
            return "Yes";
        }
        return evidenceHandled;
    }

    private SscsDocument buildSscsDocument(SscsCaseData sscsCaseData, ScannedDocument scannedDocument) {
        String controlNumber = scannedDocument.getValue().getControlNumber();
        String fileName = scannedDocument.getValue().getFileName();

        String scannedDate = null;
        if (scannedDocument.getValue().getScannedDate() != null) {
            LocalDateTime localDate = LocalDateTime.parse(scannedDocument.getValue().getScannedDate());
            scannedDate = localDate.format(DateTimeFormatter.ISO_DATE);
        }

        DocumentLink url = scannedDocument.getValue().getUrl();

        return SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentType(getSubtype(sscsCaseData.getFurtherEvidenceAction().getValue().getCode(),
                sscsCaseData.getOriginalSender().getValue().getCode()))
            .documentFileName(fileName)
            .documentLink(url)
            .documentDateAdded(scannedDate)
            .controlNumber(controlNumber)
            .evidenceIssued("No")
            .build()).build();
    }

    private String getSubtype(String furtherEvidenceActionItemCode, String originalSenderCode) {
        if (OTHER_DOCUMENT_MANUAL.getCode().equals(furtherEvidenceActionItemCode)) {
            return OTHER_DOCUMENT.getValue();
        }
        if (APPELLANT.getCode().equals(originalSenderCode)) {
            return APPELLANT_EVIDENCE.getValue();
        }
        if (REPRESENTATIVE.getCode().equals(originalSenderCode)) {
            return REPRESENTATIVE_EVIDENCE.getValue();
        }
        throw new IllegalStateException("document Type could not be worked out");
    }

}
