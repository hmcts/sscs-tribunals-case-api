package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Component
@Slf4j
public class ActionFurtherEvidenceAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private static final String FURTHER_EVIDENCE_RECEIVED = "furtherEvidenceReceived";
    private static final String COVERSHEET = "coversheet";

    private PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse;
    private final FooterService footerService;

    @Autowired
    public ActionFurtherEvidenceAboutToSubmitHandler(FooterService footerService) {
        this.footerService = footerService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.ACTION_FURTHER_EVIDENCE
            && caseData.getFurtherEvidenceAction() != null
            && caseData.getOriginalSender() != null;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (isIssueFurtherEvidenceToAllParties(callback.getCaseDetails().getCaseData().getFurtherEvidenceAction())) {
            sscsCaseData.setDwpFurtherEvidenceStates(FURTHER_EVIDENCE_RECEIVED);
        }

        buildSscsDocumentFromScan(sscsCaseData, caseDetails.getState(), callback.isIgnoreWarnings());

        return preSubmitCallbackResponse;
    }

    private boolean isIssueFurtherEvidenceToAllParties(DynamicList furtherEvidenceActionList) {
        if (furtherEvidenceActionList != null && furtherEvidenceActionList.getValue() != null
                && isNotBlank(furtherEvidenceActionList.getValue().getCode())) {
            return furtherEvidenceActionList.getValue().getCode().equals(ISSUE_FURTHER_EVIDENCE.getCode());
        }
        return false;
    }

    private void buildSscsDocumentFromScan(SscsCaseData sscsCaseData, State caseState, Boolean ignoreWarnings) {

        if (sscsCaseData.getScannedDocuments() != null) {
            for (ScannedDocument scannedDocument : sscsCaseData.getScannedDocuments()) {
                if (scannedDocument != null && scannedDocument.getValue() != null) {

                    checkWarningsAndErrors(scannedDocument, sscsCaseData.getCcdCaseId(), ignoreWarnings);

                    List<SscsDocument> documents = new ArrayList<>();

                    if (!equalsIgnoreCase(scannedDocument.getValue().getType(), COVERSHEET)) {
                        SscsDocument sscsDocument = buildSscsDocument(sscsCaseData, scannedDocument, caseState);
                        documents.add(sscsDocument);
                    }
                    if (sscsCaseData.getSscsDocument() != null) {
                        documents.addAll(sscsCaseData.getSscsDocument());
                    }

                    if (documents.size() > 0) {
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

    private void checkWarningsAndErrors(ScannedDocument scannedDocument, String caseId, boolean ignoreWarnings) {
        if (scannedDocument.getValue().getUrl() == null) {
            preSubmitCallbackResponse.addError("No document URL so could not process");
        }

        if (scannedDocument.getValue().getFileName() == null) {
            preSubmitCallbackResponse.addError("No document file name so could not process");
        }

        if (equalsIgnoreCase(scannedDocument.getValue().getType(), COVERSHEET)) {
            if (!ignoreWarnings) {
                preSubmitCallbackResponse.addWarning("Coversheet will be ignored, are you happy to proceed?");
            } else {
                log.info("Coversheet not moved over for {}} - {}}", caseId, scannedDocument.getValue().getUrl().getDocumentUrl());
            }
        }
    }

    private SscsDocument buildSscsDocument(SscsCaseData sscsCaseData, ScannedDocument scannedDocument, State caseState) {

        String scannedDate = null;
        if (scannedDocument.getValue().getScannedDate() != null) {
            scannedDate = LocalDateTime.parse(scannedDocument.getValue().getScannedDate()).toLocalDate().format(DateTimeFormatter.ISO_DATE);
        }

        DocumentLink url = scannedDocument.getValue().getUrl();

        DocumentType documentType = getSubtype(sscsCaseData.getFurtherEvidenceAction().getValue().getCode(),
                sscsCaseData.getOriginalSender().getValue().getCode());

        String bundleAddition = null;
        if (caseState != null && isIssueFurtherEvidenceToAllParties(sscsCaseData.getFurtherEvidenceAction())
                && (caseState.equals(State.DORMANT_APPEAL_STATE)
                || caseState.equals(State.RESPONSE_RECEIVED)
                || caseState.equals(State.READY_TO_LIST))) {
            log.info("adding footer appendix document link: {} and caseId {}", url, sscsCaseData.getCcdCaseId());

            String originalSenderCode = sscsCaseData.getOriginalSender().getValue().getCode();
            String documentFooterText = OriginalSenderItemList.APPELLANT.getCode().equals(originalSenderCode) ? "Appellant evidence" : "Representative evidence";

            bundleAddition = footerService.getNextBundleAddition(sscsCaseData.getSscsDocument());

            url = footerService.addFooter(url, documentFooterText, bundleAddition);
        }

        String fileName = buildAdditionFileName(documentType, bundleAddition, scannedDocument.getValue().getScannedDate());

        return SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType(documentType.getValue())
                .documentFileName(fileName)
                .bundleAddition(bundleAddition)
                .documentLink(url)
                .documentDateAdded(scannedDate)
                .controlNumber(scannedDocument.getValue().getControlNumber())
                .evidenceIssued("No")
                .build()).build();
    }

    private String buildAdditionFileName(DocumentType documentType, String bundleAddition, String scannedDate) {
        String bundleText = "";
        if (bundleAddition != null) {
            bundleText = "Addition " + bundleAddition + " - ";
        }
        scannedDate = scannedDate != null ? LocalDateTime.parse(scannedDate).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        String label = documentType.getLabel() != null ? documentType.getLabel() : documentType.getValue();

        return bundleText + label + " received on " + scannedDate;
    }

    private DocumentType getSubtype(String furtherEvidenceActionItemCode, String originalSenderCode) {
        if (OTHER_DOCUMENT_MANUAL.getCode().equals(furtherEvidenceActionItemCode)) {
            return OTHER_DOCUMENT;
        }
        if (OriginalSenderItemList.APPELLANT.getCode().equals(originalSenderCode)) {
            return APPELLANT_EVIDENCE;
        }
        if (OriginalSenderItemList.REPRESENTATIVE.getCode().equals(originalSenderCode)) {
            return REPRESENTATIVE_EVIDENCE;
        }
        if (OriginalSenderItemList.DWP.getCode().equals(originalSenderCode)) {
            return DWP_EVIDENCE;
        }
        throw new IllegalStateException("document Type could not be worked out");
    }

}
