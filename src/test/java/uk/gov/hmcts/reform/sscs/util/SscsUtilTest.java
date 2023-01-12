package uk.gov.hmcts.reform.sscs.util;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FINAL_DECISION_WELSH;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

class SscsUtilTest {

    private SscsCaseData caseData;
    private Event issueFinalDecisionEarliest;
    private Event issueFinalDecisionLatest;
    private Event issueFinalDecisionWelshEarliest;
    private Event issueFinalDecisionWelshLatest;
    private Event evidenceReceived;
    private List<Event> eventsList;

    @BeforeEach
    void setUp() {
        final String date1 = String.format("2017-06-2%dT12:00:00", 1);
        issueFinalDecisionEarliest = new Event(new EventDetails(date1, "issueFinalDecision", "issueFinalDecision earliest"));
        final String date2 = String.format("2017-06-2%dT12:00:00", 2);
        issueFinalDecisionLatest = new Event(new EventDetails(date2, "issueFinalDecision", "issueFinalDecision latest"));
        final String date3 = String.format("2017-06-2%dT12:00:00", 3);
        issueFinalDecisionWelshEarliest = new Event(new EventDetails(date3, "issueFinalDecisionWelsh", "issueFinalDecisionWelsh earliest"));
        final String date4 = String.format("2017-06-2%dT12:00:00", 4);
        issueFinalDecisionWelshLatest = new Event(new EventDetails(date4, "issueFinalDecisionWelsh", "issueFinalDecisionWelsh latest"));
        evidenceReceived = new Event(new EventDetails(date4, "evidenceReceived", "evidence received"));

        caseData = SscsCaseData.builder().build();
    }

    @Test
    @DisplayName("Given multiple issue final decision events, getLatestEventOfSpecifiedType returns latest")
    void getLatestEventOfSpecifiedType_returnsLatestEventEnglish() {
        eventsList = List.of(issueFinalDecisionEarliest, evidenceReceived, issueFinalDecisionLatest);
        caseData.setEvents(eventsList);

        Event result = SscsUtil.getLatestEventOfSpecifiedType(caseData, ISSUE_FINAL_DECISION);

        assertThat(result).isEqualTo(issueFinalDecisionLatest);
    }

    @Test
    @DisplayName("Given multiple issue final decision Welsh events, getLatestEventOfSpecifiedType returns latest")
    void getLatestEventOfSpecifiedType_returnsLatestEventWelsh() {
        eventsList = List.of(issueFinalDecisionWelshEarliest, evidenceReceived, issueFinalDecisionWelshLatest);
        caseData.setEvents(eventsList);

        Event result = SscsUtil.getLatestEventOfSpecifiedType(caseData, ISSUE_FINAL_DECISION_WELSH);

        assertThat(result).isEqualTo(issueFinalDecisionWelshLatest);
    }

    @Test
    @DisplayName("Given multiple issue final decision English and Welsh events, getLatestIssueFinalDecision returns latest")
    void getLatestIssueFinalDecision_returnsLatestEnglishOrWelsh() {
        eventsList = List.of(issueFinalDecisionWelshEarliest, issueFinalDecisionEarliest, evidenceReceived, issueFinalDecisionWelshLatest, issueFinalDecisionLatest);
        caseData.setEvents(eventsList);

        Event result = SscsUtil.getLatestIssueFinalDecision(caseData);

        assertThat(result).isEqualTo(issueFinalDecisionWelshLatest);
    }

    @ParameterizedTest
    @DisplayName("Given issue final decision event is not in case data events, searching for them using getLatestEventOfSpecifiedType returns null")
    @EnumSource(value = EventType.class, names = {"ISSUE_FINAL_DECISION", "ISSUE_FINAL_DECISION_WELSH"})
    void getLatestEventOfSpecifiedType_returnsNull(EventType eventType) {
        eventsList = List.of(evidenceReceived);
        caseData.setEvents(eventsList);

        Event result = SscsUtil.getLatestEventOfSpecifiedType(caseData, eventType);

        assertThat(result).isNull();
    }
}
