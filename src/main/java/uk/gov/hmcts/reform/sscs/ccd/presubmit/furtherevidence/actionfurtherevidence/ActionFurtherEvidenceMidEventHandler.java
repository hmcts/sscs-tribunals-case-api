package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.ActionFurtherEvidenceAboutToSubmitHandler.checkWarningsAndErrors;
import static uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState.PASSWORD_ENCRYPTED;
import static uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState.UNREADABLE;

import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Component
@Slf4j
public class ActionFurtherEvidenceMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;

    @Autowired
    public ActionFurtherEvidenceMidEventHandler(FooterService footerService) {
        this.footerService = footerService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
            && callback.getEvent() == EventType.ACTION_FURTHER_EVIDENCE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse =
            new PreSubmitCallbackResponse<>(sscsCaseData);

        buildSscsDocumentFromScan(sscsCaseData, caseDetails.getId(), callback.isIgnoreWarnings(),
            preSubmitCallbackResponse);

        return preSubmitCallbackResponse;
    }

    private void buildSscsDocumentFromScan(SscsCaseData sscsCaseData, long caseId, Boolean ignoreWarnings,
                                           PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        if (isEmpty(sscsCaseData.getScannedDocuments())) {
            preSubmitCallbackResponse.addError("Please add a scanned document");
            return;
        }
        List<PdfReadable> pdfReadableErrorList = getPdfReadableErrorList(sscsCaseData);

        if (pdfReadableErrorList.isEmpty()) {
            checkForWarningsAndErrorsOnScannedDocuments(sscsCaseData, ignoreWarnings, preSubmitCallbackResponse);
        }

        addErrorsIfPdfsHaveErrors(caseId, preSubmitCallbackResponse, pdfReadableErrorList);

    }

    private void addErrorsIfPdfsHaveErrors(long caseId, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, List<PdfReadable> pdfReadableErrorList) {
        addErrorsIfAnyPdfsAreUnreadable(caseId, preSubmitCallbackResponse, pdfReadableErrorList);
        addErrorsIfAnyPdfsAreEncrypted(caseId, preSubmitCallbackResponse, pdfReadableErrorList);
    }

    private void addErrorsIfAnyPdfsAreEncrypted(long caseId, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, List<PdfReadable> pdfReadableErrorList) {
        List<String> encryptedPdfLinks = getPdfLinksWithError(pdfReadableErrorList, PASSWORD_ENCRYPTED);
        if (isNotEmpty(encryptedPdfLinks)) {
            addPdfEncryptedError(caseId, preSubmitCallbackResponse, encryptedPdfLinks);
        }
    }

    private void addErrorsIfAnyPdfsAreUnreadable(long caseId, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, List<PdfReadable> pdfReadableErrorList) {
        List<String> unreadablePdfLinks = getPdfLinksWithError(pdfReadableErrorList, UNREADABLE);
        if (isNotEmpty(unreadablePdfLinks)) {
            addPdfUnreadableError(caseId, preSubmitCallbackResponse, unreadablePdfLinks);
        }
    }

    @NotNull
    private List<String> getPdfLinksWithError(List<PdfReadable> pdfReadableErrorList, PdfState pdfState) {
        return pdfReadableErrorList.stream()
                .filter(pdfReadable -> pdfReadable.getPdfState().equals(pdfState))
                .map(PdfReadable::getFilename)
                .collect(toUnmodifiableList());
    }

    private void checkForWarningsAndErrorsOnScannedDocuments(SscsCaseData sscsCaseData, Boolean ignoreWarnings, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        emptyIfNull(sscsCaseData.getScannedDocuments())
                .forEach(scannedDocument -> checkWarningsAndErrors(
                        sscsCaseData,
                        scannedDocument,
                        sscsCaseData.getCcdCaseId(),
                        ignoreWarnings,
                        preSubmitCallbackResponse));
    }

    @NotNull
    private List<PdfReadable> getPdfReadableErrorList(SscsCaseData sscsCaseData) {
        return emptyIfNull(sscsCaseData.getScannedDocuments()).stream()
                .filter(Objects::nonNull)
                .filter(doc -> doc.getValue() != null)
                .filter(doc -> doc.getValue().getUrl() != null)
                .filter(doc -> doc.getValue().getUrl().getDocumentUrl() != null)
                .map(doc -> new PdfReadable(doc.getValue().getFileName(), footerService.isReadablePdf(doc.getValue().getUrl().getDocumentUrl())))
                .filter(pdfReadable -> pdfReadable.getPdfState().equals(UNREADABLE) || pdfReadable.getPdfState().equals(PASSWORD_ENCRYPTED))
                .collect(toUnmodifiableList());
    }

    private void addPdfEncryptedError(long caseId, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, List<String> encryptedPdfLinks) {
        preSubmitCallbackResponse.addError(
            "The below PDF document(s) cannot be password protected, please correct this");
        addFileErrors(encryptedPdfLinks, preSubmitCallbackResponse);
        log.error("{} – {} failed due to encrypted PDF(s)\n{}", caseId, EventType.ACTION_FURTHER_EVIDENCE,
            getFormattedFileUrl(encryptedPdfLinks));
    }

    private void addPdfUnreadableError(long caseId, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, List<String> unreadablePdfLinks) {
        preSubmitCallbackResponse
            .addError("The below PDF document(s) are not readable, please correct this");
        addFileErrors(unreadablePdfLinks, preSubmitCallbackResponse);
        log.error("{} – {} failed due to broken PDF(s)\n{}", caseId, EventType.ACTION_FURTHER_EVIDENCE,
            getFormattedFileUrl(unreadablePdfLinks));
    }

    private String getFormattedFileUrl(List<String> errors) {
        StringBuilder fileUrls = new StringBuilder("");
        errors.forEach(
            error -> fileUrls.append(error).append("\n")
        );
        fileUrls.delete(fileUrls.lastIndexOf("\n"), fileUrls.length());
        return fileUrls.toString();
    }

    private void addFileErrors(List<String> errors, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        for (String error : errors) {
            preSubmitCallbackResponse.addError(error);
        }
    }

    @Getter
    @AllArgsConstructor
    private static class PdfReadable {
        private final String filename;
        private final PdfState pdfState;

    }
}
