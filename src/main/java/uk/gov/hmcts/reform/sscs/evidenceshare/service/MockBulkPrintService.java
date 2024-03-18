package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.service.CcdNotificationsPdfService;


@Service("bulkPrintService")
@ConditionalOnProperty(prefix = "send-letter", name = "url", havingValue = "false")
public class MockBulkPrintService implements PrintService {
    private static final Logger logger = LoggerFactory.getLogger(MockBulkPrintService.class);

    private final CcdNotificationsPdfService ccdNotificationsPdfService;

    private final BulkPrintServiceHelper bulkPrintServiceHelper;

    public MockBulkPrintService(CcdNotificationsPdfService ccdNotificationsPdfService,
                                BulkPrintServiceHelper bulkPrintServiceHelper) {
        this.ccdNotificationsPdfService = ccdNotificationsPdfService;
        this.bulkPrintServiceHelper = bulkPrintServiceHelper;
    }

    public Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, SscsCaseData sscsCaseData, String recipient) {
        logger.info("No bulk print operation needs to be performed as 'Bulk print url' is switched off.");
        return Optional.of(UUID.fromString("abc123ca-c336-11e9-9cb5-123456789abc"));
    }


    public Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, SscsCaseData sscsCaseData, FurtherEvidenceLetterType letterType, EventType event, String recipient) {
        if (bulkPrintServiceHelper.sendForReasonableAdjustment(sscsCaseData, letterType)) {
            logger.info("Sending to bulk print service {} reasonable adjustments enabled {}", sscsCaseData.getCcdCaseId());
            bulkPrintServiceHelper.saveAsReasonableAdjustment(sscsCaseData, pdfs, letterType);
        } else {
            logger.info("No bulk print operation needs to be performed as 'Bulk print url' is switched off.");
        }

        return Optional.empty();
    }
}
