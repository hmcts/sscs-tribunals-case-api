package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.EmailService;
import uk.gov.hmcts.reform.sscs.service.SscsPdfService;

@Component
@Slf4j
public class RecreateAppealPdfHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final SscsPdfService sscsPdfService;
    private final EmailService emailService;
    private final IdamService idamService;

    @Autowired
    public RecreateAppealPdfHandler(SscsPdfService sscsPdfService, EmailService emailService, IdamService idamService) {
        this.sscsPdfService = sscsPdfService;
        this.emailService = emailService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        boolean canHandle = callbackType == CallbackType.SUBMITTED
                && callback.getEvent() == EventType.CREATE_APPEAL_PDF;
        return canHandle;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData caseData = caseDetails.getCaseData();
        log.info("Handling create appeal pdf event for case [" + caseData.getCcdCaseId() + "]");

        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        createAppealPdfAndSendToRobotics(caseData);

        return sscsCaseDataPreSubmitCallbackResponse;
    }


    private void createAppealPdfAndSendToRobotics(SscsCaseData caseData) {
        //FIXME: This code should be refactored to use the PDF generation for SYA
        boolean hasPdf = hasPdfDocument(caseData);

        log.info("Does case have pdf {}", hasPdf);
        if (!hasPdf) {
            log.info("Existing pdf document not found, start generating pdf ");
            updateAppointeeNullIfNotPresent(caseData);
            caseData.setEvidencePresent(hasEvidence(caseData));

            IdamTokens idamTokens = idamService.getIdamTokens();

            sscsPdfService.generateAndSendPdf(caseData, Long.parseLong(caseData.getCcdCaseId()),
                    idamTokens,"sscs1");
        }
    }

    private void updateAppointeeNullIfNotPresent(SscsCaseData caseData) {
        if (caseData != null && caseData.getAppeal() != null && caseData.getAppeal().getAppellant() != null) {
            Appointee appointee = caseData.getAppeal().getAppellant().getAppointee();
            if (appointee != null && appointee.getName() == null) {
                caseData.getAppeal().getAppellant().setAppointee(null);
            }
        }
    }

    private boolean hasPdfDocument(SscsCaseData caseData) {
        String fileName = emailService.generateUniqueEmailId(caseData.getAppeal().getAppellant()) + ".pdf";
        log.info("Case does have document {} and Pdf file name to check {} ",
                !CollectionUtils.isEmpty(caseData.getSscsDocument()), fileName);

        for (SscsDocument document : caseData.getSscsDocument()) {
            log.info("Existing document {} for case {} ",
                    document != null ? document.getValue().getDocumentFileName() : null,
                    caseData.getCcdCaseId());
            if (document != null && fileName.equalsIgnoreCase(document.getValue().getDocumentFileName())) {
                return true;
            }
        }
        return false;
    }

    private String hasEvidence(SscsCaseData caseData) {
        String fileName = emailService.generateUniqueEmailId(caseData.getAppeal().getAppellant()) + ".pdf";
        if (caseData.getSscsDocument() != null) {
            for (SscsDocument document : caseData.getSscsDocument()) {
                if (document != null && !fileName.equals(document.getValue().getDocumentFileName())) {
                    return "Yes";
                }
            }
        }
        return "No";
    }
}
