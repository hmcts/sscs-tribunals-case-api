package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario.SCENARIO_5;
import static uk.gov.hmcts.reform.sscs.util.DateUtilities.today;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;

class UcTemplateContentTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final UcTemplateContent content = new UcTemplateContentUnderTest();

    @Test
    void shouldReturnDoesNotHaveLimitedCapabilityForWorkSentence() {
        String formattedSentence = content.getDoesNotHaveLimitedCapabilityForWorkSentence("John Doe");
        assertThat(formattedSentence).isEqualTo("John Doe does not have limited capability for work and cannot be treated as having limited capability for work.");
    }

    @ParameterizedTest
    @MethodSource("doesHaveLimitedCapabilityForWorkSentence")
    void shouldReturnDoesHaveLimitedCapabilityForWorkSentence(String appellantName, boolean isTreatedLimitedCapability, boolean includeWorkRelatedActivities, boolean isWorkRelatedActivitiesLimited,
                                                              boolean isWorkRelatedActivitiesToBeTreatedLimitedCapability, String expected) {
        String formattedSentence = content.getDoesHaveLimitedCapabilityForWorkSentence(
            appellantName, isTreatedLimitedCapability, includeWorkRelatedActivities, isWorkRelatedActivitiesLimited,
            isWorkRelatedActivitiesToBeTreatedLimitedCapability, LocalDate.now());
        assertThat(formattedSentence).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("doesHaveLimitedCapabilityForWorkRelatedActivitySentence")
    void shouldReturnDoesHaveLimitedCapabilityForWorkRelatedActivitySentence(String appellantName, boolean isTreatedLimitedCapability, String expected) {
        String formattedSentence = content.getLimitedCapabilityForWorkRelatedSentence(appellantName, isTreatedLimitedCapability, LocalDate.now());
        assertThat(formattedSentence).isEqualTo(expected);
    }

    private static Stream<Arguments> doesHaveLimitedCapabilityForWorkSentence() {
        return Stream.of(
            Arguments.of(
                "Joe Bloggs", true, true, true, true,
                ("Joe Bloggs is to be treated as having limited capability for work and is to be treated as having " + "limited capability for work-related activity from %s.").formatted(
                    today())),
            Arguments.of(
                "Joe Bloggs", false, true, true, true,
                ("Joe Bloggs has limited capability for work and is to be treated as having limited capability for " + "work-related activity from %s.").formatted(today())),
            Arguments.of("Joe Bloggs", true, false, true, true, "Joe Bloggs is to be treated as having limited capability for work from %s.".formatted(today())),
            Arguments.of(
                "Joe Bloggs", true, true, false, true,
                ("Joe Bloggs is to be treated as having limited capability for work and for work-related activity " + "from" + " %s.").formatted(today())),
            Arguments.of(
                "Joe Bloggs", true, true, true, false,
                ("Joe Bloggs is to be treated as having limited capability for work and has limited capability for " + "work-related activity from %s.").formatted(today())));
    }

    private static Stream<Arguments> doesHaveLimitedCapabilityForWorkRelatedActivitySentence() {
        return Stream.of(
            Arguments.of("Joe Bloggs", true, ("Joe Bloggs is to be treated as having limited capability for work-related activity from %s.").formatted(today())),
            Arguments.of("Joe Bloggs", false, "Joe Bloggs has limited capability for work-related activity from %s.".formatted(today())));
    }

    private static class UcTemplateContentUnderTest extends UcTemplateContent {
        @Override
        public UcScenario getScenario() {
            return SCENARIO_5;
        }
    }

}