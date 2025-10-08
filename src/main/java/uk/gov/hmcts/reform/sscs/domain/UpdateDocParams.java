package uk.gov.hmcts.reform.sscs.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;

@Data
@Builder
public class UpdateDocParams {
    String fileName;
    byte[] pdf;
    Long caseId;
    SscsCaseData caseData;
    String documentType;
    SscsDocumentTranslationStatus documentTranslationStatus;
    String otherPartyId;
    String otherPartyName;





}
