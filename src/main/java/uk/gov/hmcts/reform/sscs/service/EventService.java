package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.model.NotificationEventType.CREATE_APPEAL_PDF;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.NotificationEventType;

@Service
@Slf4j
public class EventService {

    private final SscsPdfService sscsPdfService;
    private final IdamService idamService;

    @Autowired
    EventService(SscsPdfService sscsPdfService, IdamService idamService) {
        this.sscsPdfService = sscsPdfService;
        this.idamService = idamService;
    }

    @Async
    public void submitEvent(NotificationEventType eventType, SscsCaseData caseData) {
        performAction(eventType, caseData);
    }

    public boolean performAction(NotificationEventType actionType, SscsCaseData caseData) {

        if (CREATE_APPEAL_PDF == actionType) {
            createAppealPdf(caseData);
            return true;
        }

        return false;
    }

    private void createAppealPdf(SscsCaseData caseData) {
        IdamTokens idamTokens = idamService.getIdamTokens();
        sscsPdfService.generateAndSendPdf(caseData, Long.parseLong(caseData.getCcdCaseId()), idamTokens);
    }

}
