package uk.gov.hmcts.reform.sscs.functional.tyanotifications.sya.notifications;

import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;

import java.lang.reflect.Field;
import java.util.List;
import junitparams.Parameters;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.functional.tyanotifications.AbstractFunctionalTest;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.service.notify.Notification;

public class WithRepresentativePersonalisationTest extends AbstractFunctionalTest {
    @Value("${notification.english.appealLapsed.appellant.emailId}")
    private String appealLapsedAppellantEmailId;
    @Value("${notification.english.appealLapsed.appellant.smsId}")
    private String appealLapsedAppellantSmsId;
    @Value("${notification.english.appealLapsed.representative.emailId}")
    private String appealLapsedRepsEmailId;
    @Value("${notification.english.appealLapsed.representative.smsId}")
    private String appealLapsedRepsSmsId;
    @Value("${notification.english.appealWithdrawn.appellant.emailId}")
    private String appealWithdrawnAppellantEmailId;
    @Value("${notification.english.appealWithdrawn.appellant.smsId}")
    private String appealWithdrawnAppellantSmsId;
    @Value("${notification.english.appealWithdrawn.representative.emailId}")
    private String appealWithdrawnRepsEmailId;
    @Value("${notification.english.appealWithdrawn.representative.smsId}")
    private String appealWithdrawnRepsSmsId;
    /*
    // SSCS-11586
    @Value("${notification.english.listAssist.oral.hearingBooked.appellant.emailId}")
    private String hearingBookedAppellantEmailId;
    @Value("${notification.english.listAssist.oral.hearingBooked.appellant.smsId}")
    private String hearingBookedAppellantSmsId;
    @Value("${notification.english.listAssist.oral.hearingBooked.representative.emailId}")
    private String hearingBookedRepsEmailId;
    @Value("${notification.english.listAssist.oral.hearingBooked.representative.smsId}")
    private String hearingBookedRepsSmsId;

     */
    @Value("${notification.english.appealCreated.appellant.emailId}")
    private String appealCreatedAppellantEmailId;
    @Value("${notification.english.appealCreated.appellant.smsId}")
    private String appealCreatedAppellantSmsId;
    @Value("${notification.english.appealCreated.representative.emailId}")
    private String appealCreatedRepsEmailId;
    @Value("${notification.english.appealCreated.representative.smsId}")
    private String appealCreatedRepsSmsId;
    @Value("${notification.english.paper.appealDormant.appellant.emailId}")
    private String appealDormantAppellantEmailId;
    @Value("${notification.english.paper.appealDormant.appellant.smsId}")
    private String appealDormantAppellantSmsId;
    @Value("${notification.english.paper.appealDormant.representative.emailId}")
    private String appealDormantRepsEmailId;
    @Value("${notification.english.paper.appealDormant.representative.smsId}")
    private String appealDormantRepsSmsId;

    @Value("${notification.english.hearingAdjourned.appellant.emailId}")
    private String hearingAdjournedAppellantEmailId;
    @Value("${notification.english.hearingAdjourned.appellant.smsId}")
    private String hearingAdjournedAppellantSmsId;
    @Value("${notification.english.hearingAdjourned.representative.emailId}")
    private String hearingAdjournedRepsEmailId;
    @Value("${notification.english.hearingAdjourned.representative.smsId}")
    private String hearingAdjournedRepsSmsId;
    @Value("${notification.english.appealReceived.appellant.emailId}")
    private String appealReceivedAppellantEmailId;
    @Value("${notification.english.appealReceived.appellant.smsId}")
    private String appealReceivedAppellantSmsId;
    @Value("${notification.english.appealReceived.representative.emailId}")
    private String appealReceivedRepsEmailId;
    @Value("${notification.english.appealReceived.representative.smsId}")
    private String appealReceivedRepsSmsId;
    @Value("${notification.english.listAssist.oral.hearingPostponed.appellant.emailId}")
    private String hearingPostponedAppellantEmailId;
    @Value("${notification.english.listAssist.oral.hearingPostponed.representative.emailId}")
    private String hearingPostponedRepsEmailId;
    @Value("${notification.english.validAppealCreated.appellant.emailId}")
    private String validAppealCreatedAppellantEmailId;
    @Value("${notification.english.validAppealCreated.appellant.smsId}")
    private String validAppealCreatedAppellantSmsId;
    @Value("${notification.english.validAppealCreated.representative.emailId}")
    private String validAppealCreatedRepsEmailId;
    @Value("${notification.english.validAppealCreated.representative.smsId}")
    private String validAppealCreatedRepsSmsId;

    public WithRepresentativePersonalisationTest() {
        super(30);
    }

    @Test
    @Parameters(method = "eventTypeAndSubscriptions")
    public void givenEventAndRepsSubscription_shouldSendNotificationToReps(NotificationEventType notificationEventType)
        throws Exception {
        //Given
        final String repsEmailId = getFieldValue(notificationEventType, "RepsEmailId");
        final String repsSmsId = getFieldValue(notificationEventType, "RepsSmsId");

        simulateCcdCallback(notificationEventType,
            "tyanotifications/representative/" + notificationEventType.getId() + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(repsEmailId, repsSmsId);

        String representativeName = "Harry Potter";
        assertNotificationBodyContains(notifications, repsEmailId, representativeName);
        assertNotificationBodyContains(notifications, repsSmsId);
    }

    @Test
    public void givenHearingPostponedEventAndRepsSubscription_shouldSendEmailOnlyNotificationToReps()
        throws Exception {

        final String repsEmailId = getFieldValue(POSTPONEMENT, "RepsEmailId");

        simulateCcdCallback(POSTPONEMENT,
            "tyanotifications/representative/" + POSTPONEMENT.getId()
                + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(repsEmailId);

        String representativeName = "Harry Potter";
        assertNotificationBodyContains(notifications, repsEmailId, representativeName);
    }

    private String getFieldValue(NotificationEventType notificationEventType, String fieldName) throws Exception {
        String fieldValue;
        try {
            Field field = this.getClass().getDeclaredField(notificationEventType.getId() + fieldName);
            field.setAccessible(true);
            fieldValue = (String) field.get(this);
        } catch (NoSuchFieldException e) {
            fieldValue = null;
        }
        return fieldValue;
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] eventTypeAndSubscriptions() {
        return new Object[]{
            new Object[]{APPEAL_LAPSED},
            new Object[]{APPEAL_WITHDRAWN},
            new Object[]{SYA_APPEAL_CREATED},
            new Object[]{APPEAL_DORMANT},
            new Object[]{ADJOURNED},
            new Object[]{APPEAL_RECEIVED},
            // Put back when covid19 feature turned off: new Object[]{HEARING_BOOKED},
            new Object[]{VALID_APPEAL_CREATED}
        };
    }
}
