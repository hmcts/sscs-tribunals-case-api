package uk.gov.hmcts.reform.sscs.notifications.bulkprint.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.thirdparty.docmosis.domain.Pdf;

public interface PrintService {
    Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, SscsCaseData sscsCaseData, FurtherEvidenceLetterType letterType, EventType event, String recipient);

    Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, SscsCaseData sscsCaseData, String recipient);
}
