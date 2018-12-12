package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.model.NotificationEventType.CREATE_APPEAL_PDF;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.NotificationEventType;

@Service
@Slf4j
public class EventService {

    private final SscsPdfService sscsPdfService;
    private final CcdService ccdService;
    private final RoboticsService roboticsService;
    private final EvidenceManagementService evidenceManagementService;
    private final EmailService emailService;
    private final IdamService idamService;

    @Autowired
    EventService(SscsPdfService sscsPdfService,
                 IdamService idamService,
                 RoboticsService roboticsService,
                 EvidenceManagementService evidenceManagementService,
                 EmailService emailService,
                 CcdService ccdService) {
        this.sscsPdfService = sscsPdfService;
        this.roboticsService = roboticsService;
        this.evidenceManagementService = evidenceManagementService;
        this.emailService = emailService;
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Async
    public void submitEvent(NotificationEventType eventType, SscsCaseData caseData) {
        handleEvent(eventType, caseData);
    }

    public boolean handleEvent(NotificationEventType eventType, SscsCaseData caseData) {

        if (CREATE_APPEAL_PDF == eventType) {
            return createAppealPdfAndSendToRobotics(Long.parseLong(caseData.getCcdCaseId()));
        }

        return false;
    }

    private boolean createAppealPdfAndSendToRobotics(long caseId) {

        IdamTokens idamTokens = idamService.getIdamTokens();
        SscsCaseDetails caseDetails = ccdService.getByCaseId(caseId, idamTokens);

        if (caseDetails != null) {
            SscsCaseData caseData = caseDetails.getData();
            caseData.setEvidencePresent(hasEvidence(caseData));

            String postcode = caseData.getAppeal().getAppellant().getAddress().getPostcode();

            byte[] pdf = sscsPdfService.generateAndSendPdf(caseDetails.getData(), caseId, idamTokens);

            Map<String, byte[]> additionalEvidence = downloadEvidence(caseData);

            roboticsService.sendCaseToRobotics(caseData, caseId, postcode, pdf, additionalEvidence);
            return true;
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
