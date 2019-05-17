package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SEND_TO_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SENT_TO_DWP;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class SendToDwpService {

    private final CcdService ccdService;

    @Value("${feature.send_to_dwp}")
    private Boolean sendToDwpFeature;

    @Autowired
    SendToDwpService(CcdService ccdService) {
        this.ccdService = ccdService;
    }

    public void sendToDwp(SscsCaseData caseData, Long caseId, IdamTokens idamTokens) {
        if (sendToDwpFeature) {
            String eventType;
            String summary;
            String description;
            if (caseData.getAppeal().getReceivedVia().equalsIgnoreCase("Paper")) {
                eventType = SEND_TO_DWP.getCcdType();
                summary = "Send to DWP";
                description = "Send to DWP event has been triggered from Tribunals service";
            } else {
                eventType = SENT_TO_DWP.getCcdType();
                summary = "Sent to DWP";
                description = "Case has been sent to the DWP by Robotics";
                caseData.setDateSentToDwp(LocalDate.now().toString());
            }
            log.info("About to update case with {} event for id {}", eventType, caseId);
            ccdService.updateCase(caseData, caseId, eventType, summary, description, idamTokens);
            log.info("Case updated with {} event for id {}", eventType, caseId);
        }
    }
}
