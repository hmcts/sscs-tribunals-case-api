package uk.gov.hmcts.reform.sscs.util;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FINAL_DECISION_WELSH;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

class SscsUtilTest {

    public static final String DATE_TEMPLATE = "2017-06-2%dT12:00:00";

    private SscsCaseData caseData;
    private Event issueFinalDecisionEarliest;
    private Event issueFinalDecisionLatest;
    private Event issueFinalDecisionWelshEarliest;
    private Event issueFinalDecisionWelshLatest;
    private Event evidenceReceived;
    private List<Event> eventsList;

    @BeforeEach
    void setUp() {
        issueFinalDecisionEarliest = new Event(new EventDetails(String.format(DATE_TEMPLATE, 1), "issueFinalDecision", "issueFinalDecision earliest"));
        issueFinalDecisionLatest = new Event(new EventDetails(String.format(DATE_TEMPLATE, 2), "issueFinalDecision", "issueFinalDecision latest"));
        issueFinalDecisionWelshEarliest = new Event(new EventDetails(String.format(DATE_TEMPLATE, 3), "issueFinalDecisionWelsh", "issueFinalDecisionWelsh earliest"));
        issueFinalDecisionWelshLatest = new Event(new EventDetails(String.format(DATE_TEMPLATE, 4), "issueFinalDecisionWelsh", "issueFinalDecisionWelsh latest"));
        evidenceReceived = new Event(new EventDetails(String.format(DATE_TEMPLATE, 4), "evidenceReceived", "evidence received"));

        caseData = SscsCaseData.builder().build();
    }

    @Test
    @DisplayName("Given event type exists on case data, getLatestEventOfSpecifiedTypes returns it")
    void getLatestEventOfSpecifiedTypes_returnsEvent() {
        eventsList = List.of(issueFinalDecisionEarliest);
        caseData.setEvents(eventsList);

        Optional<Event> result = SscsUtil.getLatestEventOfSpecifiedTypes(caseData, ISSUE_FINAL_DECISION);

        assertThat(result)
            .isPresent()
            .contains(issueFinalDecisionEarliest);
    }

    @Test
    @DisplayName("Given multiple issue final decision events, getLatestEventOfSpecifiedTypes returns latest")
    void getLatestEventOfSpecifiedTypes_returnsLatestEventEnglish() {
        eventsList = List.of(evidenceReceived, issueFinalDecisionEarliest, issueFinalDecisionLatest);
        caseData.setEvents(eventsList);

        Optional<Event> result = SscsUtil.getLatestEventOfSpecifiedTypes(caseData, ISSUE_FINAL_DECISION, ISSUE_FINAL_DECISION_WELSH);

        assertThat(result)
            .isPresent()
            .contains(issueFinalDecisionLatest);
    }

    @Test
    @DisplayName("Given multiple issue final decision Welsh events, getLatestEventOfSpecifiedTypes returns latest")
    void getLatestEventOfSpecifiedTypes_returnsLatestEventWelsh() {
        eventsList = List.of(evidenceReceived, issueFinalDecisionWelshEarliest, issueFinalDecisionWelshLatest);
        caseData.setEvents(eventsList);

        Optional<Event> result = SscsUtil.getLatestEventOfSpecifiedTypes(caseData, ISSUE_FINAL_DECISION, ISSUE_FINAL_DECISION_WELSH);

        assertThat(result)
            .isPresent()
            .contains(issueFinalDecisionWelshLatest);
    }

    @Test
    @DisplayName("Given multiple issue final decision English and Welsh events, getLatestEventOfSpecifiedTypes returns latest")
    void getLatestEventOfSpecifiedTypes_returnsLatestEvent() {
        eventsList = List.of(issueFinalDecisionWelshEarliest, evidenceReceived, issueFinalDecisionWelshLatest,
            issueFinalDecisionEarliest, issueFinalDecisionLatest);
        caseData.setEvents(eventsList);

        Optional<Event> result = SscsUtil.getLatestEventOfSpecifiedTypes(caseData, ISSUE_FINAL_DECISION, ISSUE_FINAL_DECISION_WELSH);

        assertThat(result)
            .isPresent()
            .contains(issueFinalDecisionWelshLatest);
    }

    @Test
    @DisplayName("Given issue final decision event is not in case data events, getLatestEventOfSpecifiedTypes returns empty Optional")
    void getLatestEventOfSpecifiedTypes_returnsEmptyOptional() {
        eventsList = List.of(evidenceReceived);
        caseData.setEvents(eventsList);

        Optional<Event> result = SscsUtil.getLatestEventOfSpecifiedTypes(caseData, ISSUE_FINAL_DECISION, ISSUE_FINAL_DECISION_WELSH);

        assertThat(result).isNotPresent();
    }
}
