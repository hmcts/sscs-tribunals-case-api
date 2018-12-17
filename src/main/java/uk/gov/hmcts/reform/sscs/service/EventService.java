package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.model.NotificationEventType.CREATE_APPEAL_PDF;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.NotificationEventType;

@Service
@Slf4j
public class EventService {

    private final SscsPdfService sscsPdfService;
    private final RoboticsService roboticsService;
    private final EvidenceManagementService evidenceManagementService;
    private final EmailService emailService;
    private final IdamService idamService;

    @Autowired
    EventService(SscsPdfService sscsPdfService,
                 IdamService idamService,
                 RoboticsService roboticsService,
                 EvidenceManagementService evidenceManagementService,
                 EmailService emailService) {
        this.sscsPdfService = sscsPdfService;
        this.roboticsService = roboticsService;
        this.evidenceManagementService = evidenceManagementService;
        this.emailService = emailService;
        this.idamService = idamService;
    }

    public boolean handleEvent(NotificationEventType eventType, SscsCaseData caseData) {

        if (CREATE_APPEAL_PDF == eventType) {
            createAppealPdfAndSendToRobotics(caseData);
            return true;
        }

        return false;
    }

    private void createAppealPdfAndSendToRobotics(SscsCaseData caseData) {

        if (!hasDocument(caseData)) {

            IdamTokens idamTokens = idamService.getIdamTokens();
            byte[] pdf = sscsPdfService.generateAndSendPdf(caseData, Long.parseLong(caseData.getCcdCaseId()), idamTokens);

            Map<String, byte[]> additionalEvidence = downloadEvidence(caseData);
            caseData.setEvidencePresent(additionalEvidence.size() > 0 ? "Yes" : "No");
            String postcode = caseData.getAppeal().getAppellant().getAddress().getPostcode();
            roboticsService.sendCaseToRobotics(caseData, Long.parseLong(caseData.getCcdCaseId()), postcode, pdf, additionalEvidence);
        }
    }

    private boolean hasDocument(SscsCaseData caseData) {
        String fileName = emailService.generateUniqueEmailId(caseData.getAppeal().getAppellant()) + ".pdf";
        for (SscsDocument document : caseData.getSscsDocument()) {
            if (document != null && fileName.equals(document.getValue().getDocumentFileName())) {
                return true;
            }
        }
        return false;
    }

    private Map<String, byte[]> downloadEvidence(SscsCaseData caseData) {
        Map<String, byte[]> map = new LinkedHashMap<>();
        if (caseData.getSscsDocument() != null) {
            String appealPdfName = emailService.generateUniqueEmailId(caseData.getAppeal().getAppellant()) + ".pdf";
            for (SscsDocument document : caseData.getSscsDocument()) {
                if (!appealPdfName.equals(document.getValue().getDocumentFileName())) {
                    map.put(document.getValue().getDocumentFileName(), downloadBinary(document));
                }
            }
        }
        return map;
    }

    private byte[] downloadBinary(SscsDocument document) {
        return evidenceManagementService.download(URI.create(document.getValue().getDocumentLink().getDocumentUrl()),
                SubmitAppealService.DM_STORE_USER_ID);
    }

}
