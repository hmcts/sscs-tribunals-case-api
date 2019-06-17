package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@Component
public class HandleEvidenceEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public boolean canHandle(Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");

        return callback.getEvent() == EventType.HANDLE_EVIDENCE;
    }

    public PreSubmitCallbackResponse<SscsCaseData> handle(Callback<SscsCaseData> callback) {
        if (!canHandle(callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        buildSscsDocumentFromScan(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void buildSscsDocumentFromScan(SscsCaseData sscsCaseData) {

        if (sscsCaseData.getScannedDocuments() != null) {
            for (ScannedDocument scannedDocument : sscsCaseData.getScannedDocuments()) {
                if (scannedDocument != null && scannedDocument.getValue() != null) {
                    String controlNumber = scannedDocument.getValue().getControlNumber();
                    String fileName = scannedDocument.getValue().getFileName();
                    String subtype = scannedDocument.getValue().getSubtype() != null ? scannedDocument.getValue().getSubtype() : "appellantEvidence";

                    LocalDateTime localDate = LocalDateTime.parse(scannedDocument.getValue().getScannedDate());

                    String scannedDate = localDate.format(DateTimeFormatter.ISO_DATE);
                    DocumentLink url = scannedDocument.getValue().getUrl();

                    SscsDocument doc = SscsDocument.builder().value(SscsDocumentDetails.builder()
                            .documentType(subtype)
                            .documentFileName(fileName)
                            .documentLink(url)
                            .documentDateAdded(scannedDate)
                            .controlNumber(controlNumber)
                            .build()).build();

                    List<SscsDocument> documents = new ArrayList<>();
                    if (sscsCaseData.getSscsDocument() != null) {
                        documents = sscsCaseData.getSscsDocument();
                    }
                    documents.add(doc);
                    sscsCaseData.setSscsDocument(documents);
                }
            }
        }
        sscsCaseData.setScannedDocuments(null);

    }

}
