package uk.gov.hmcts.reform.sscs.model.service.hearingvalues;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PartyFlagsMap {

    DISABLED_ACCESS("21", "Step free / wheelchair access", "6"),
    SIGN_LANGUAGE_TYPE("44", "Sign Language Interpreter", "10"),
    HEARING_LOOP("45", "Hearing loop (hearing enhancement system)", "11"),
    IS_CONFIDENTIAL_CASE("53", "Confidential address", "2"),
    DWP_UCB("56", "Unacceptable customer behaviour", "2"),
    DWP_PHME("63", "Potentially harmful medical evidence", "1"),
    URGENT_CASE("67", "Urgent flag", "1"),
    LANGUAGE_INTERPRETER_FLAG("70", "Language Interpreter", "2");

    private final String flagId;
    private final String flagDescription;
    private final String parentId;
}
