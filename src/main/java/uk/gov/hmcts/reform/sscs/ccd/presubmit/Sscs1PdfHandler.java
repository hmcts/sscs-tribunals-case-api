package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.helper.EmailHelper;
import uk.gov.hmcts.reform.sscs.service.SscsPdfService;

@Component
@Slf4j
public class Sscs1PdfHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final SscsPdfService sscsPdfService;
    private final EmailHelper emailHelper;

    @Autowired
    public Sscs1PdfHandler(SscsPdfService sscsPdfService, EmailHelper emailHelper) {
        this.sscsPdfService = sscsPdfService;
        this.emailHelper = emailHelper;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        boolean canHandle = callbackType == CallbackType.ABOUT_TO_SUBMIT
                && (!"Paper".equalsIgnoreCase(callback.getCaseDetails().getCaseData().getAppeal().getReceivedVia())
                && (callback.getEvent() == EventType.VALID_APPEAL_CREATED
                || callback.getEvent() == EventType.NON_COMPLIANT
                || callback.getEvent() == EventType.INCOMPLETE_APPLICATION_RECEIVED)
                || callback.getEvent() == EventType.CREATE_APPEAL_PDF);
        return canHandle;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData caseData = caseDetails.getCaseData();
        log.info("Handling create appeal pdf event for case [" + caseData.getCcdCaseId() + "]");

        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        createAppealPdf(caseData);

        return sscsCaseDataPreSubmitCallbackResponse;
    }

    private void createAppealPdf(SscsCaseData caseData) {
        String fileName = emailHelper.generateUniqueEmailId(caseData.getAppeal().getAppellant()) + ".pdf";

        boolean hasPdf = hasPdfDocument(caseData, fileName);

        log.info("Does case have pdf {} for caseId {}", hasPdf, caseData.getCcdCaseId());
        if (!hasPdf) {
            log.info("Existing pdf document not found, start generating pdf for caseId {}", caseData.getCcdCaseId());

            sscsPdfService.generateAndSendPdf(caseData, Long.parseLong(caseData.getCcdCaseId()),"sscs1", fileName);
        }
    }

    private boolean hasPdfDocument(SscsCaseData caseData, String fileName) {
        log.info("Case does have document {} and Pdf file name to check {} for caseId {}",
                !CollectionUtils.isEmpty(caseData.getSscsDocument()), fileName, caseData.getCcdCaseId());

        if (caseData.getSscsDocument() != null) {
            for (SscsDocument document : caseData.getSscsDocument()) {
                log.info("Existing document {} for case {} ",
                        document != null ? document.getValue().getDocumentFileName() : null,
                        caseData.getCcdCaseId());
                if (document != null && fileName.equalsIgnoreCase(document.getValue().getDocumentFileName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
