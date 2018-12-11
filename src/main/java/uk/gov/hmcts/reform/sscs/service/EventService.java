package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.model.NotificationEventType.CREATE_APPEAL_PDF;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.NotificationEventType;

@Service
@Slf4j
public class EventService {

    private final SscsPdfService sscsPdfService;
    private final CcdService ccdService;
    private final ThreadLocal<IdamTokens> idamTokens;

    @Autowired
    EventService(SscsPdfService sscsPdfService,
                 IdamService idamService,
                 CcdService ccdService) {
        this.sscsPdfService = sscsPdfService;
        this.ccdService = ccdService;
        this.idamTokens = ThreadLocal.withInitial(idamService::getIdamTokens);
    }

    @Async
    public void submitEvent(NotificationEventType eventType, SscsCaseData caseData) {
        handleEvent(eventType, caseData);
    }

    public boolean handleEvent(NotificationEventType eventType, SscsCaseData caseData) {

        idamTokens.remove();

        if (CREATE_APPEAL_PDF == eventType) {
            return createAppealPdf(Long.parseLong(caseData.getCcdCaseId()));
        }

        return false;
    }

    private boolean createAppealPdf(long caseId) {
        SscsCaseDetails caseDetails = ccdService.getByCaseId(caseId, idamTokens.get());
        if (caseDetails != null) {
            sscsPdfService.generateAndSendPdf(caseDetails.getData(), caseId, idamTokens.get());
            return true;
        }
        return false;
    }

}
