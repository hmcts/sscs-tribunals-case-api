package uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.DO_NOT_SEND;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.OTHER_PARTY_ADDED_TO_APPEAL;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.UPDATE_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.checkEvent;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

class NotificationEventTypeTest {

    @Test
    void checkEvent_eventsWeDontHandle_returnsFalse() {
        assertThat(checkEvent("answers_submitted")).isFalse();
    }

    @Test
    void checkEvent_eventsWeDoHandle_returnsTrue() {
        final List<String> events = List.of("confirmLapsed", "subscriptionCreated", "hearingReminder", "validAppealCreated",
            "actionHearingRecordingRequest");
        events.forEach(event -> assertThat(checkEvent(event)).isTrue());
    }

    @Test
    void checkEvent_otherPartyAddedToAppeal_returnsFalse() {
        assertThat(checkEvent("otherPartyAddedToAppeal")).isFalse();
    }

    @Test
    void getId_doNotSend_returnsEmptyString() {
        assertThat(DO_NOT_SEND.getId()).isEmpty();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("notificationEventTypeIdMappings")
    void getNotificationByEvent_validId_returnsCorrectEnum(final String id, final NotificationEventType expected) {
        assertThat(NotificationEventType.getNotificationByEvent(id)).isEqualTo(expected);
    }

    @Test
    void getNotificationByEvent_unknownId_returnsDoNotSend() {
        assertThat(NotificationEventType.getNotificationByEvent("unknownEvent")).isEqualTo(DO_NOT_SEND);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("notificationEventTypeMappings")
    void getNotificationByCcdEvent_matchFound_returnsExpectedEnum(final EventType eventType,
        final NotificationEventType expected) {
        assertThat(NotificationEventType.getNotificationByCcdEvent(eventType)).isEqualTo(expected);
    }

    @Test
    void getNotificationByCcdEvent_multipleEnumsShareSameEventType_returnsFirstDeclared() {
        assertThat(NotificationEventType.getNotificationByCcdEvent(EventType.UPDATE_OTHER_PARTY_DATA))
            .isEqualTo(UPDATE_OTHER_PARTY_DATA)
            .isNotEqualTo(OTHER_PARTY_ADDED_TO_APPEAL);
    }

    @Test
    void getNotificationByEvent_multipleEnumsShareSameEventId_returnsFirstDeclared() {
        assertThat(NotificationEventType.getNotificationByEvent("updateOtherPartyData"))
            .isEqualTo(UPDATE_OTHER_PARTY_DATA)
            .isNotEqualTo(OTHER_PARTY_ADDED_TO_APPEAL);
    }

    @Test
    void getNotificationByCcdEvent_noMatch_returnsDoNotSend() {
        assertThat(NotificationEventType.getNotificationByCcdEvent(ISSUE_FURTHER_EVIDENCE)).isEqualTo(DO_NOT_SEND);
    }

    @ParameterizedTest
    @MethodSource("eventTypeConfigurationProvider")
    void eventType_configuration_hasCorrectValues(NotificationEventType eventType, boolean sendForOralCase,
        boolean sendForPaperCase, Boolean sendForCohCase, boolean allowOutOfHours, boolean isReminder, boolean toBeDelayed,
        Long delayInSeconds) {
        assertSoftly(softly -> {
            softly.assertThat(eventType.isSendForOralCase()).isEqualTo(sendForOralCase);
            softly.assertThat(eventType.isSendForPaperCase()).isEqualTo(sendForPaperCase);
            if (sendForCohCase != null) {
                softly.assertThat(eventType.isSendForCohCase()).isEqualTo(sendForCohCase);
            }
            softly.assertThat(eventType.isAllowOutOfHours()).isEqualTo(allowOutOfHours);
            softly.assertThat(eventType.isReminder()).isEqualTo(isReminder);
            softly.assertThat(eventType.isToBeDelayed()).isEqualTo(toBeDelayed);
            if (delayInSeconds != null) {
                softly.assertThat(eventType.getDelayInSeconds()).isEqualTo(delayInSeconds);
            }
        });
    }

    private static Stream<Arguments> eventTypeConfigurationProvider() {
        return Stream.of(Arguments.of(OTHER_PARTY_ADDED_TO_APPEAL, false, false, false, true, false, false, null),
            Arguments.of(APPEAL_RECEIVED, true, true, null, false, false, true, 300L));
    }

    @Test
    void otherPartyAddedToAppeal_getId_returnsSameIdAsUpdateOtherPartyData() {
        assertThat(OTHER_PARTY_ADDED_TO_APPEAL.getId()).isEqualTo(UPDATE_OTHER_PARTY_DATA.getId());
    }

    private static Stream<Arguments> notificationEventTypeIdMappings() {
        return Arrays.stream(NotificationEventType.values())
            .filter(n -> n != OTHER_PARTY_ADDED_TO_APPEAL)
            .map(n -> Arguments.of(n.getId(), n));
    }

    private static Stream<Arguments> notificationEventTypeMappings() {
        return Arrays
            .stream(NotificationEventType.values())
            .filter(n -> n.getEvent() != null)
            .collect(groupingBy(NotificationEventType::getEvent, LinkedHashMap::new, toList()))
            .entrySet()
            .stream()
            .map(entry -> Arguments.of(entry.getKey(), entry.getValue().getFirst()));
    }
}