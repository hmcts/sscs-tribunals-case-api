package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.service.CcdNotificationsPdfService;

@Service
@Slf4j
public class CcdNotificationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d MMM y HH:mm");
    private static final ZoneId ZONE_ID_LONDON = ZoneId.of("Europe/London");

    private static final String SENDER_TYPE = "Bulk Print";

    private final CcdNotificationsPdfService ccdNotificationsPdfService;

    public CcdNotificationService(CcdNotificationsPdfService ccdNotificationsPdfService) {
        this.ccdNotificationsPdfService = ccdNotificationsPdfService;
    }

    public void storeNotificationLetterIntoCcd(EventType eventType, byte[] pdfLetter,
                                               Long ccdCaseId, String recipient) {
        var correspondence = buildCorrespondence(eventType, recipient);
        ccdNotificationsPdfService.mergeLetterCorrespondenceIntoCcd(pdfLetter, ccdCaseId, correspondence, SENDER_TYPE);
    }

    Correspondence buildCorrespondence(EventType eventType, String recipient) {
        var correspondenceDetails = CorrespondenceDetails.builder()
            .correspondenceType(CorrespondenceType.Letter)
            .eventType(eventType.getCcdType())
            .to(recipient)
            .sentOn(LocalDateTime.now(ZONE_ID_LONDON).format(DATE_TIME_FORMATTER))
            .build();

        return Correspondence.builder()
            .value(correspondenceDetails)
            .build();
    }
}
