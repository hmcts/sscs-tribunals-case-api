package uk.gov.hmcts.reform.sscs.evidenceshare.domain;

import lombok.Getter;

@Getter
public enum FurtherEvidenceLetterType {

    APPELLANT_LETTER("appellantLetter"),
    REPRESENTATIVE_LETTER("representativeLetter"),
    DWP_LETTER("dwpLetter"),
    JOINT_PARTY_LETTER("jointPartyLetter"),
    OTHER_PARTY_LETTER("otherPartyLetter"),
    OTHER_PARTY_REP_LETTER("otherPartyRepLetter");

    private final String value;

    FurtherEvidenceLetterType(String value) {
        this.value = value;
    }
}
