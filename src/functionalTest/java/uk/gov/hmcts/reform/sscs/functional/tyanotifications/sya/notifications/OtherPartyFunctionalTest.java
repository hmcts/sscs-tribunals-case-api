package uk.gov.hmcts.reform.sscs.functional.tyanotifications.sya.notifications;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.functional.tyanotifications.AbstractFunctionalTest;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.service.notify.Notification;

public class OtherPartyFunctionalTest extends AbstractFunctionalTest {

    private SscsCaseData sscsCaseData;
    @Value("${notification.english.oral.dwpUploadResponse.other_party.smsId}")
    private String oralDwpUploadResponseJointPartySmsId;
    @Value("${notification.english.oral.dwpUploadResponse.other_party.emailId}")
    private String oralDwpUploadResponseOtherPartyEmailId;
    @Value("${notification.english.hearingAdjourned.other_party.emailId}")
    private String hearingAdjournedOtherPartyEmailTemplateId;
    @Value("${notification.english.hearingAdjourned.other_party.smsId}")
    private String hearingAdjournedOtherPartySmsTemplateId;
    @Value("${notification.english.listAssist.oral.hearingPostponed.other_party.emailId}")
    private String hearingPostponedOtherPartyEmailTemplateId;
    @Value("${notification.english.appealLapsed.other_party.emailId}")
    private String appealLapsedOtherPartyEmailTemplateId;
    @Value("${notification.english.appealLapsed.other_party.smsId}")
    private String appealLapsedOtherPartySmsTemplateId;
    @Value("${notification.english.appealWithdrawn.other_party.emailId}")
    private String appealWithdrawnOtherPartyEmailTemplateId;
    @Value("${notification.english.appealWithdrawn.other_party.smsId}")
    private String appealWithdrawnOtherPartySmsTemplateId;
    @Value("${notification.english.oral.evidenceReminder.other_party.emailId}")
    private String evidenceReminderOtherPartyEmailTemplateId;
    @Value("${notification.english.oral.evidenceReminder.other_party.smsId}")
    private String evidenceReminderOtherPartySmsTemplateId;

    public OtherPartyFunctionalTest() {
        super(30);
    }


    @Test
    @Ignore
    @Parameters({"oral-,DWP_UPLOAD_RESPONSE, oralDwpUploadResponseJointPartySmsId, oralDwpUploadResponseOtherPartyEmailId"})
    public void willSendDwpUploadResponse(@Nullable String prefix, NotificationEventType notificationEventType, String... fieldNames) throws Exception {

        simulateCcdCallback(notificationEventType,
            "tyanotifications/otherparty/" + prefix + notificationEventType.getId() + "Callback.json");

        List<Notification> notifications = fetchLetters();
        assertEquals(2, notifications.size());
        notifications.forEach(n -> assertEquals("Pre-compiled PDF", n.getSubject().orElse("Unknown Subject")));
        tryFetchNotificationsForTestCase(getFieldValue(fieldNames));
    }

    private String[] getFieldValue(String... fieldNames) {
        return Arrays.stream(fieldNames)
            .map(this::getValue)
            .toArray(String[]::new);
    }

    @Test
    @Ignore
    @Parameters({
        "ADJOURNED, 0, hearingAdjournedOtherPartyEmailTemplateId, hearingAdjournedOtherPartySmsTemplateId",
        "POSTPONEMENT, 0, hearingPostponedOtherPartyEmailTemplateId",
        "APPEAL_LAPSED, 2, appealLapsedOtherPartyEmailTemplateId, appealLapsedOtherPartySmsTemplateId",
        "APPEAL_WITHDRAWN, 2, appealWithdrawnOtherPartyEmailTemplateId, appealWithdrawnOtherPartySmsTemplateId",
        "STRUCK_OUT, 2,",
        "DECISION_ISSUED, 2,",
        "DIRECTION_ISSUED, 2,",
        "ISSUE_ADJOURNMENT_NOTICE, 2,",
        "ISSUE_FINAL_DECISION, 2,",
        "EVIDENCE_REMINDER, 0, evidenceReminderOtherPartyEmailTemplateId, evidenceReminderOtherPartySmsTemplateId"
    })
    public void willSendEventNotification(NotificationEventType notificationEventType, int expectedNumberOfLetters, @Nullable String... fieldNames) throws Exception {

        simulateCcdCallback(notificationEventType,
            "tyanotifications/otherparty/oral-eventTypeCallback.json");

        List<Notification> notifications = fetchLetters();

        assertEquals(expectedNumberOfLetters, notifications.size());
        notifications.forEach(n -> assertEquals("Pre-compiled PDF", n.getSubject().orElse("Unknown Subject")));
        if (fieldNames != null && !fieldNames[0].equals("")) {
            tryFetchNotificationsForTestCase(getFieldValue(fieldNames));
        }
    }

    private String getValue(String fieldName) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (String) field.get(this);
        } catch (Exception e) {
            return null;
        }
    }
}
