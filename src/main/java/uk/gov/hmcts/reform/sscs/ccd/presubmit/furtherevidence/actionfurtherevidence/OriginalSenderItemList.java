package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DWP_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.HMCTS_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.JOINT_PARTY_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;

import lombok.Getter;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;

@Getter
public enum OriginalSenderItemList {
    APPELLANT("appellant", "Appellant (or Appointee)", "Appellant evidence", APPELLANT_EVIDENCE),
    REPRESENTATIVE("representative", "Representative", "Representative evidence", REPRESENTATIVE_EVIDENCE),
    DWP("dwp", "DWP", "DWP evidence", DWP_EVIDENCE),
    JOINT_PARTY("jointParty", "Joint party", "Joint party evidence", JOINT_PARTY_EVIDENCE),
    HMCTS("hmcts", "HMCTS", "HMCTS evidence", HMCTS_EVIDENCE);

    private final String documentFooter;
    private final String code;
    private final String label;
    private final DocumentType documentType;

    OriginalSenderItemList(String code, String label, String documentFooter, DocumentType documentType) {
        this.code = code;
        this.label = label;
        this.documentFooter = documentFooter;
        this.documentType = documentType;
    }
}