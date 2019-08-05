package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.OTHER_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.APPELLANT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.REPRESENTATIVE;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.domain.pdf.ByteArrayMultipartFile;
import uk.gov.hmcts.reform.sscs.pdf.PdfWatermarker;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

@Component
@Slf4j
public class HandleEvidenceEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private static final String FURTHER_EVIDENCE_RECEIVED = "furtherEvidenceReceived";
    private static final String DM_STORE_USER_ID = "sscs";
    private static final String COVERSHEET = "coversheet";

    private PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse;
    private final EvidenceManagementService evidenceManagementService;

    @Autowired
    public HandleEvidenceEventHandler(EvidenceManagementService evidenceManagementService) {
        this.evidenceManagementService = evidenceManagementService;
    }

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

        if (isIssueFurtherEvidenceToAllParties(callback.getCaseDetails().getCaseData().getFurtherEvidenceAction())) {
            sscsCaseData.setDwpFurtherEvidenceStates(FURTHER_EVIDENCE_RECEIVED);
        }

        buildSscsDocumentFromScan(sscsCaseData, caseDetails.getState());

        return preSubmitCallbackResponse;
    }

    private boolean isIssueFurtherEvidenceToAllParties(DynamicList furtherEvidenceActionList) {
        if (furtherEvidenceActionList != null && furtherEvidenceActionList.getValue() != null
                && isNotBlank(furtherEvidenceActionList.getValue().getCode())) {
            return furtherEvidenceActionList.getValue().getCode().equals(ISSUE_FURTHER_EVIDENCE.getCode());
        }
        return false;
    }

    private void buildSscsDocumentFromScan(SscsCaseData sscsCaseData, State caseState) {

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
                        documents.addAll(sscsCaseData.getSscsDocument());
                    }
                    if (!equalsIgnoreCase(scannedDocument.getValue().getType(), COVERSHEET)) {
                        SscsDocument sscsDocument = buildSscsDocument(sscsCaseData, scannedDocument, caseState);
                        documents.add(sscsDocument);
                        sscsCaseData.setSscsDocument(documents);
                    }
                    sscsCaseData.setEvidenceHandled("Yes");
                } else {
                    log.info("Not adding any scanned document as there aren't any or the type is a coversheet for case Id {}.", sscsCaseData.getCcdCaseId());
                }
            }
        } else {
            preSubmitCallbackResponse.addError("No further evidence to process");
        }

        sscsCaseData.setScannedDocuments(null);

    }

    private SscsDocument buildSscsDocument(SscsCaseData sscsCaseData, ScannedDocument scannedDocument, State caseState) {

        String scannedDate = null;
        if (scannedDocument.getValue().getScannedDate() != null) {
            LocalDateTime localDate = LocalDateTime.parse(scannedDocument.getValue().getScannedDate());
            scannedDate = localDate.format(DateTimeFormatter.ISO_DATE);
        }

        DocumentLink url = scannedDocument.getValue().getUrl();

        log.info("document link: " + url);

        if (caseState != null && isIssueFurtherEvidenceToAllParties(sscsCaseData.getFurtherEvidenceAction())
                && (caseState.equals(State.DORMANT_APPEAL_STATE)
                || caseState.equals(State.RESPONSE_RECEIVED)
                || caseState.equals(State.READY_FOR_HEARING))) {
            url = addFooter(sscsCaseData, url);
            log.info("footer appendix document link: " + url);
        }

        String fileName = scannedDocument.getValue().getFileName();
        String controlNumber = scannedDocument.getValue().getControlNumber();
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

    private DocumentLink addFooter(SscsCaseData sscsCaseData, DocumentLink url) {

        String originalSenderCode = sscsCaseData.getOriginalSender().getValue().getCode();
        String documentType = APPELLANT.getCode().equals(originalSenderCode) ? "Appellant evidence" : "Representative evidence";
        String bundleAddition = getNextBundleAddition(sscsCaseData.getSscsDocument());

        byte[] oldContent = toBytes(url.getDocumentUrl());
        PdfWatermarker alter = new PdfWatermarker();
        byte[] newContent;
        try {
            newContent = alter.shrinkAndWatermarkPdf(oldContent, documentType, String.format("Addition  %s", bundleAddition));
        } catch (Exception e) {
            log.error("Caught exception :" + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }

        ByteArrayMultipartFile file = ByteArrayMultipartFile.builder()
                .content(newContent)
                .name(url.getDocumentFilename())
                .contentType(APPLICATION_PDF).build();

        UploadResponse uploadResponse = evidenceManagementService.upload(singletonList(file), DM_STORE_USER_ID);
        String location = uploadResponse.getEmbedded().getDocuments().get(0).links.self.href;

        return url.toBuilder().documentUrl(location).documentBinaryUrl(location).build();
    }

    String getNextBundleAddition(List<SscsDocument> sscsDocument) {
        if (sscsDocument == null) {
            sscsDocument = new ArrayList<>();
        }
        String[] appendixArray = sscsDocument.stream().filter(s -> StringUtils.isNotEmpty(s.getValue().getAppendix())).map(s -> StringUtils.stripToEmpty(s.getValue().getAppendix())).toArray(String[]::new);
        Arrays.sort(appendixArray, (o1, o2) -> {
            if (StringUtils.isNotEmpty(o1) && StringUtils.isNotEmpty(o2) && o1.length() > 1 && o2.length() > 1) {
                Integer n1 = NumberUtils.isNumber(o1.substring(1)) ? Integer.parseInt(o1.substring(1)) : 0;
                Integer n2 = NumberUtils.isNumber(o2.substring(1)) ? Integer.parseInt(o2.substring(1)) : 0;
                return ComparatorUtils.<Integer>naturalComparator().compare(n1, n2);
            }
            return ComparatorUtils.<String>naturalComparator().compare(o1, o2);
        });
        if (appendixArray.length >  0) {
            String lastAppendix = appendixArray[appendixArray.length - 1];
            char nextChar =  (char) (StringUtils.upperCase(lastAppendix).charAt(0) + 1);
            if (nextChar > 'Z') {
                if (lastAppendix.length() == 1) {
                    return "Z1";
                } else if (NumberUtils.isNumber(lastAppendix.substring(1))) {
                    return "Z" + (Integer.valueOf(lastAppendix.substring(1)) + 1);
                }
            }
            return String.valueOf(nextChar);
        }

        return "A";
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

    private byte[] toBytes(String documentUrl) {
        return evidenceManagementService.download(
                URI.create(documentUrl),
                DM_STORE_USER_ID
        );
    }

}
