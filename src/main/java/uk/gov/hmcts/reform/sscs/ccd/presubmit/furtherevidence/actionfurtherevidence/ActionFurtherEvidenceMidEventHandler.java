package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.ActionFurtherEvidenceAboutToSubmitHandler.checkWarningsAndErrors;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.exceptions.PdfPasswordException;

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

        if (isNotEmpty(sscsCaseData.getScannedDocuments())) {
            List<String> unreadablePdfLinks = new ArrayList<>();
            List<String> encryptedPdfLinks = new ArrayList<>();
            for (ScannedDocument scannedDocument : sscsCaseData.getScannedDocuments()) {
                if (scannedDocument != null && scannedDocument.getValue() != null) {
                    if (scannedDocument.getValue().getUrl() != null
                        && scannedDocument.getValue().getUrl().getDocumentUrl() != null) {
                        try {
                            footerService.isReadablePdf(scannedDocument.getValue().getUrl().getDocumentUrl());
                        } catch (PdfPasswordException e) {
                            encryptedPdfLinks.add(scannedDocument.getValue().getFileName());
                        } catch (Exception ioE) {
                            unreadablePdfLinks.add(scannedDocument.getValue().getFileName());
                        }
                    }
                    if (isEmpty(unreadablePdfLinks) && isEmpty(encryptedPdfLinks)) {
                        checkWarningsAndErrors(sscsCaseData, scannedDocument, sscsCaseData.getCcdCaseId(),
                            ignoreWarnings,
                            preSubmitCallbackResponse);
                    }
                }
            }

            if (isNotEmpty(unreadablePdfLinks)) {
                preSubmitCallbackResponse
                    .addError("The below PDF document(s) are not readable, please correct this");
                addFileErrors(unreadablePdfLinks, preSubmitCallbackResponse);
                log.error("{} – {} failed due to broken PDF(s)\n{}", caseId, EventType.ACTION_FURTHER_EVIDENCE,
                    getFormattedFileUrl(unreadablePdfLinks));
            }
            if (isNotEmpty(encryptedPdfLinks)) {
                preSubmitCallbackResponse.addError(
                    "The below PDF document(s) cannot be password protected, please correct this");
                addFileErrors(encryptedPdfLinks, preSubmitCallbackResponse);
                log.error("{} – {} failed due to encrypted PDF(s)\n{}", caseId, EventType.ACTION_FURTHER_EVIDENCE,
                    getFormattedFileUrl(encryptedPdfLinks));
            }
        } else {
            preSubmitCallbackResponse.addError("Please add a scanned document");
        }
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

}
