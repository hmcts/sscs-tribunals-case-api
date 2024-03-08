package uk.gov.hmcts.reform.sscs.service.evidenceshare;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.evidenceshare.FurtherEvidenceLetterType;

public interface PrintService {
    Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, SscsCaseData sscsCaseData, FurtherEvidenceLetterType letterType, EventType event, String recipient);

    Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, SscsCaseData sscsCaseData, String recipient);
}
