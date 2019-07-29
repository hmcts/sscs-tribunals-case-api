package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.DocumentType.OTHER_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.APPELLANT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.REPRESENTATIVE;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class HandleEvidenceEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final String COVERSHEET = "coversheet";

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
                    if (!equalsIgnoreCase(scannedDocument.getValue().getType(), COVERSHEET)) {
                        SscsDocument sscsDocument = buildSscsDocument(sscsCaseData, scannedDocument);
                        documents.add(sscsDocument);
                        sscsCaseData.setSscsDocument(documents);
                    }
                    sscsCaseData.setEvidenceHandled("Yes");
                } else {
                    log.info("Not adding any scanned document as there aren't any or the type is a coversheet for case Id {}.", sscsCaseData.getCcdCaseId());
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
