package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class InterlocServiceTest {

    @Test
    public void setsCorrectInterlocSecondaryStatus() {
        InterlocService interlocService = new InterlocService();

        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .build();
        EventType eventType = EventType.TCW_DIRECTION_ISSUED;

        SscsCaseData caseData = interlocService.setInterlocSecondaryState(eventType, sscsCaseData);

        assertThat(caseData.getInterlocSecondaryState(), is("awaitingInformation"));
    }

    @Test
    public void resetsInterlocSecondaryStatus() {
        InterlocService interlocService = new InterlocService();

        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .interlocSecondaryState("someValue")
                .build();
        EventType eventType = EventType.JUDGE_DECISION_APPEAL_TO_PROCEED;

        SscsCaseData caseData = interlocService.setInterlocSecondaryState(eventType, sscsCaseData);

        assertThat(caseData.getInterlocSecondaryState(), is(nullValue()));
    }

    @Test
    public void doesNotChangeInterlocSecondaryStateIfNoMapping() {
        InterlocService interlocService = new InterlocService();

        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .interlocSecondaryState("someValue")
                .build();
        EventType eventType = EventType.CASE_UPDATED;

        SscsCaseData caseData = interlocService.setInterlocSecondaryState(eventType, sscsCaseData);

        assertThat(caseData.getInterlocSecondaryState(), is("someValue"));
    }
}