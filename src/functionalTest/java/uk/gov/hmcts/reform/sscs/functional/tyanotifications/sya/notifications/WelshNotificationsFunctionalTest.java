package uk.gov.hmcts.reform.sscs.functional.tyanotifications.sya.notifications;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import junitparams.Parameters;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.functional.tyanotifications.AbstractFunctionalTest;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

@DisabledIfEnvironmentVariable(named = "BYPASS_NOTIFICATIONS_SERVICE", matches = "false")
public class WelshNotificationsFunctionalTest extends AbstractFunctionalTest {

    private static final String AS_APPOINTEE_FOR = "You are receiving this update as the appointee for";
    private static final String DEAR_APPOINTEE_USER = "Dear Appointee User";
    private static final String TYA = "v8eg15XeZk";

    @Value("${notification.welsh.oral.appealDormant.appellant.emailId}")
    private String appealDormantOralAppellantEmailTemplateIdWelsh;

    @Value("${notification.welsh.oral.appealDormant.joint_party.emailId}")
    private String appealDormantOralJointPartyEmailTemplateIdWelsh;

    @Value("${notification.welsh.listAssist.oral.hearingPostponed.joint_party.emailId}")
    private String hearingPostponedEmailTemplateIdJointPartyWelsh;

    @Value("${notification.welsh.paper.appealDormant.appellant.emailId}")
    private String appealDormantPaperAppellantEmailTemplateIdWelsh;

    @Value("${notification.welsh.paper.appealDormant.appellant.smsId}")
    private String appealDormantPaperAppellantSmsTemplateIdWelsh;

    @Value("${notification.welsh.paper.appealDormant.joint_party.emailId}")
    private String appealDormantPaperJointPartyEmailTemplateIdWelsh;

    @Value("${notification.welsh.paper.appealDormant.joint_party.smsId}")
    private String appealDormantPaperJointPartySmsTemplateIdWelsh;

    @Value("${notification.welsh.listAssist.oral.hearingPostponed.appellant.emailId}")
    private String hearingPostponedEmailTemplateIdWelsh;

    @Value("${notification.welsh.hearingAdjourned.appellant.emailId}")
    private String hearingAdjournedEmailTemplateIdWelsh;

    @Value("${notification.welsh.hearingAdjourned.appellant.smsId}")
    private String hearingAdjournedSmsTemplateIdWelsh;

    @Value("${notification.welsh.hearingAdjourned.joint_party.emailId}")
    private String hearingAdjournedJointPartyEmailTemplateIdWelsh;

    @Value("${notification.welsh.hearingAdjourned.joint_party.smsId}")
    private String hearingAdjournedJointPartySmsTemplateIdWelsh;

    @Value("${notification.welsh.subscriptionCreated.appellant.smsId}")
    private String subscriptionCreatedSmsTemplateIdWelsh;

    @Value("${notification.welsh.subscriptionUpdated.emailId}")
    private String subscriptionUpdatedEmailTemplateIdWelsh;


    @Value("${notification.welsh.oral.evidenceReceived.appointee.emailId}")
    private String oralEvidenceReceivedEmailTemplateIdWelsh;

    @Value("${notification.welsh.oral.evidenceReceived.appointee.smsId}")
    private String oralEvidenceReceivedSmsTemplateIdWelsh;

    @Value("${notification.welsh.oral.evidenceReceived.joint_party.emailId}")
    private String oralJointPartyEvidenceReceivedEmailIdWelsh;

    @Value("${notification.welsh.oral.evidenceReceived.joint_party.smsId}")
    private String oralJointPartyEvidenceReceivedSmsIdWelsh;

    @Value("${notification.welsh.oral.evidenceReminder.appointee.emailId}")
    private String oralAppointeeEvidenceReminderEmailIdWelsh;

    @Value("${notification.welsh.oral.evidenceReminder.appointee.smsId}")
    private String oralAppointeeEvidenceReminderSmsIdWelsh;

    @Value("${notification.welsh.oral.evidenceReminder.joint_party.emailId}")
    private String oralJointPartyEvidenceReminderEmailIdWelsh;

    @Value("${notification.welsh.oral.evidenceReminder.joint_party.smsId}")
    private String oralJointPartyEvidenceReminderSmsIdWelsh;

    @Value("${notification.welsh.paper.evidenceReminder.appointee.emailId}")
    private String paperAppointeeEvidenceReminderEmailIdWelsh;

    @Value("${notification.welsh.paper.evidenceReminder.appointee.smsId}")
    private String paperAppointeeEvidenceReminderSmsIdWelsh;

    @Value("${notification.welsh.paper.evidenceReminder.joint_party.emailId}")
    private String paperJointPartyEvidenceReminderEmailIdWelsh;

    @Value("${notification.welsh.paper.evidenceReminder.joint_party.smsId}")
    private String paperJointPartyEvidenceReminderSmsIdWelsh;

    @Value("${notification.welsh.appealCreated.appellant.smsId}")
    private String appealCreatedAppellantSmsIdWelsh;

    @Value("${notification.welsh.appealCreated.appellant.emailId}")
    private String appealCreatedAppellantEmailIdWelsh;

    @Value("${notification.welsh.appealCreated.appointee.smsId}")
    private String appealCreatedAppointeeSmsIdWelsh;

    @Value("${notification.welsh.appealCreated.appointee.emailId}")
    private String appealCreatedAppointeeEmailIdWelsh;

    @Value("${notification.welsh.appealLapsed.appointee.emailId}")
    private String appealLapsedAppointeeEmailTemplateIdWelsh;

    @Value("${notification.welsh.appealLapsed.appointee.smsId}")
    private String appealLapsedAppointeeSmsTemplateIdWelsh;

    @Value("${notification.welsh.appealLapsed.joint_party.emailId}")
    private String appealLapsedJointPartyEmailTemplateIdWelsh;

    @Value("${notification.welsh.appealLapsed.joint_party.smsId}")
    private String appealLapsedJointPartySmsTemplateIdWelsh;

    @Value("${notification.welsh.appealWithdrawn.appointee.emailId}")
    private String appointeeAppealWithdrawnEmailIdWelsh;

    @Value("${notification.welsh.appealWithdrawn.appointee.smsId}")
    private String appointeeAppealWithdrawnSmsIdWelsh;

    @Value("${notification.welsh.appealWithdrawn.joint_party.emailId}")
    private String jointPartyAppealWithdrawnEmailIdWelsh;

    @Value("${notification.welsh.appealWithdrawn.joint_party.smsId}")
    private String jointPartyAppealWithdrawnSmsIdWelsh;

    /*// SSCS-11586
    @Value("${notification.welsh.listAssist.oral.hearingBooked.appointee.emailId}")
    private String appointeeHearingBookedEmailIdWelsh;

    @Value("${notification.welsh.listAssist.oral.hearingBooked.appointee.smsId}")
    private String appointeeHearingBookedSmsIdWelsh;

    @Value("${notification.welsh.listAssist.oral.hearingBooked.joint_party.emailId}")
    private String jointPartyHearingBookedEmailIdWelsh;

    @Value("${notification.welsh.listAssist.oral.hearingBooked.joint_party.smsId}")
    private String jointPartyHearingBookedSmsIdWelsh;

    */
    @Value("${notification.welsh.paper.evidenceReceived.appointee.emailId}")
    private String paperEvidenceReceivedEmailTemplateIdWelsh;

    @Value("${notification.welsh.paper.evidenceReceived.appointee.smsId}")
    private String paperEvidenceReceivedSmsTemplateIdWelsh;

    @Value("${notification.welsh.paper.evidenceReceived.joint_party.emailId}")
    private String paperJointPartyEvidenceReceivedEmailIdWelsh;

    @Value("${notification.welsh.paper.evidenceReceived.joint_party.smsId}")
    private String paperJointPartyEvidenceReceivedSmsIdWelsh;


    public WelshNotificationsFunctionalTest() {
        super(30);
    }

    @Override
    public void setup() {
        idamTokens = idamService.getIdamTokens();
        createCase(true);
    }

    @Test
    public void shouldSendEvidenceReceivedNotificationWelsh() throws NotificationClientException, IOException {
        simulateWelshCcdCallback(EVIDENCE_RECEIVED);
        tryFetchNotificationsForTestCase(
            oralEvidenceReceivedEmailTemplateIdWelsh,
            oralEvidenceReceivedSmsTemplateIdWelsh,
            oralJointPartyEvidenceReceivedEmailIdWelsh,
            oralJointPartyEvidenceReceivedSmsIdWelsh
        );
    }

    @Test
    public void shouldSendPaperEvidenceReceivedNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(EVIDENCE_RECEIVED, BASE_PATH_TYAN + "paper-" + EVIDENCE_RECEIVED.getId() + "CallbackWelsh.json");
        tryFetchNotificationsForTestCase(
            paperEvidenceReceivedEmailTemplateIdWelsh,
            paperEvidenceReceivedSmsTemplateIdWelsh,
            paperJointPartyEvidenceReceivedEmailIdWelsh,
            paperJointPartyEvidenceReceivedSmsIdWelsh);
    }


    @Test
    public void shouldSendHearingPostponedNotificationWelsh() throws NotificationClientException, IOException {
        simulateWelshCcdCallback(POSTPONEMENT);

        tryFetchNotificationsForTestCase(hearingPostponedEmailTemplateIdWelsh, hearingPostponedEmailTemplateIdJointPartyWelsh);
    }

    // TODO: SSCS-11436
    @Test
    @Ignore
    public void shouldSendHearingBookedNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(HEARING_BOOKED, "tyanotifications/appointee/" + HEARING_BOOKED.getId() + "CallbackWelsh.json");

        // SSCS-11586
        /* tryFetchNotificationsForTestCase(
                appointeeHearingBookedEmailIdWelsh,
                appointeeHearingBookedSmsIdWelsh,
                jointPartyHearingBookedEmailIdWelsh,
                jointPartyHearingBookedSmsIdWelsh);*/
    }


    @Test
    public void shouldSendHearingAdjournedNotificationWelsh() throws NotificationClientException, IOException {
        simulateWelshCcdCallback(ADJOURNED);

        tryFetchNotificationsForTestCase(
            hearingAdjournedEmailTemplateIdWelsh,
            hearingAdjournedSmsTemplateIdWelsh,
            hearingAdjournedJointPartyEmailTemplateIdWelsh,
            hearingAdjournedJointPartySmsTemplateIdWelsh
        );
    }

    @Test
    public void shouldSendSubscriptionCreatedNotificationWelsh() throws NotificationClientException, IOException {

        simulateWelshCcdCallback(SUBSCRIPTION_CREATED);

        tryFetchNotificationsForTestCase(subscriptionCreatedSmsTemplateIdWelsh);
    }


    @Test
    public void shouldSendSubscriptionUpdatedNotificationWelsh() throws NotificationClientException, IOException {
        simulateWelshCcdCallback(SUBSCRIPTION_UPDATED);

        tryFetchNotificationsForTestCase(subscriptionUpdatedEmailTemplateIdWelsh);
    }

    @Test
    public void shouldSendAppealCreatedAppellantNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(SYA_APPEAL_CREATED, BASE_PATH_TYAN + SYA_APPEAL_CREATED.getId() + "CallbackWelsh.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(appealCreatedAppellantEmailIdWelsh, appealCreatedAppellantSmsIdWelsh);

        assertNotificationBodyContains(notifications, appealCreatedAppellantEmailIdWelsh, "appeal has been received");
    }


    @Test
    public void shouldSendValidAppealCreatedAppellantNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(VALID_APPEAL_CREATED, BASE_PATH_TYAN + SYA_APPEAL_CREATED.getId() + "CallbackWelsh.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(appealCreatedAppellantEmailIdWelsh, appealCreatedAppellantSmsIdWelsh);

        assertNotificationBodyContains(notifications, appealCreatedAppellantEmailIdWelsh, "appeal has been received");
    }

    @Test
    public void shouldSendAppealCreatedAppointeeNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(SYA_APPEAL_CREATED, BASE_PATH_TYAN + SYA_APPEAL_CREATED.getId() + "AppointeeCallbackWelsh.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(appealCreatedAppointeeEmailIdWelsh, appealCreatedAppointeeSmsIdWelsh);

        assertNotificationBodyContains(notifications, appealCreatedAppointeeEmailIdWelsh, "appointee");
    }


    @Test
    public void shouldSendValidAppealCreatedAppointeeNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(VALID_APPEAL_CREATED, BASE_PATH_TYAN + SYA_APPEAL_CREATED.getId() + "AppointeeCallbackWelsh.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(appealCreatedAppointeeEmailIdWelsh, appealCreatedAppointeeSmsIdWelsh);

        assertNotificationBodyContains(notifications, appealCreatedAppointeeEmailIdWelsh, "appointee");
    }

    @Test
    public void shouldSendAppointeeEvidenceReminderForPaperCaseNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EVIDENCE_REMINDER,
            BASE_PATH_TYAN + "appointee/paper-" + EVIDENCE_REMINDER.getId() + "CallbackWelsh.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
            paperAppointeeEvidenceReminderEmailIdWelsh,
            paperAppointeeEvidenceReminderSmsIdWelsh,
            paperJointPartyEvidenceReminderEmailIdWelsh,
            paperJointPartyEvidenceReminderSmsIdWelsh
        );

        assertNotificationBodyContains(
            notifications,
            paperAppointeeEvidenceReminderEmailIdWelsh,
            DEAR_APPOINTEE_USER,
            AS_APPOINTEE_FOR,
            "/evidence/" + TYA
        );
    }

    @Test
    public void shouldSendPaperAppealDormantNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(APPEAL_DORMANT, BASE_PATH_TYAN + "paper-" + APPEAL_DORMANT.getId() + "CallbackWelsh.json");
        tryFetchNotificationsForTestCase(
            appealDormantPaperJointPartyEmailTemplateIdWelsh,
            appealDormantPaperAppellantSmsTemplateIdWelsh,
            appealDormantPaperJointPartySmsTemplateIdWelsh,
            appealDormantPaperAppellantEmailTemplateIdWelsh);
    }

    @Test
    public void shouldSendOralAppealDormantNotificationWelsh() throws NotificationClientException, IOException {
        simulateCcdCallback(APPEAL_DORMANT, BASE_PATH_TYAN + "oral-" + APPEAL_DORMANT.getId() + "CallbackWelsh.json");
        tryFetchNotificationsForTestCase(appealDormantOralJointPartyEmailTemplateIdWelsh, appealDormantOralAppellantEmailTemplateIdWelsh);
    }

    @Test
    public void shouldSendAppealLapsedNotificationToAppointeeJointPartyAndRep() throws NotificationClientException, IOException {
        simulateCcdCallback(APPEAL_LAPSED,
            "tyanotifications/appointee/" + APPEAL_LAPSED.getId() + "CallbackWelsh.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(
            appealLapsedAppointeeEmailTemplateIdWelsh,
            appealLapsedAppointeeSmsTemplateIdWelsh,
            appealLapsedJointPartyEmailTemplateIdWelsh,
            appealLapsedJointPartySmsTemplateIdWelsh
        );
        Notification emailNotification = notifications.stream().filter(f -> f.getTemplateId().toString().equals(appealLapsedAppointeeEmailTemplateIdWelsh)).collect(toList()).get(0);

        assertTrue(emailNotification.getBody().contains("Dear Appointee User"));
        assertTrue(emailNotification.getBody().contains("You are receiving this update as the appointee for"));
        Notification emailNotificationJp = notifications.stream().filter(f -> f.getTemplateId().toString().equals(appealLapsedJointPartyEmailTemplateIdWelsh)).collect(toList()).get(0);
        assertTrue(emailNotificationJp.getBody().contains("Rydym felly wedi cau’r apêl hon."));
        List<Notification> letterNotification = fetchLetters();
        assertEquals(3, letterNotification.size());
        assertEquals("Pre-compiled PDF", letterNotification.get(0).getSubject().orElse("Unknown Subject"));
        assertEquals("Pre-compiled PDF", letterNotification.get(1).getSubject().orElse("Unknown Subject"));
        assertEquals("Pre-compiled PDF", letterNotification.get(1).getSubject().orElse("Unknown Subject"));
    }

    @Test
    public void shouldSendAppointeeAppealWithdrawnNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(APPEAL_WITHDRAWN,
            "tyanotifications/appointee/" + APPEAL_WITHDRAWN.getId() + "CallbackWelsh.json");
        List<Notification> notifications = tryFetchNotificationsForTestCase(
            appointeeAppealWithdrawnEmailIdWelsh,
            appointeeAppealWithdrawnSmsIdWelsh,
            jointPartyAppealWithdrawnEmailIdWelsh,
            jointPartyAppealWithdrawnSmsIdWelsh);
        Notification appointeeEmail = notifications.stream()
            .filter(f -> f.getTemplateId().toString().equals(appointeeAppealWithdrawnEmailIdWelsh))
            .collect(toList()).get(0);
        assertTrue(appointeeEmail.getBody().contains("Annwyl Appointee User"));
        assertTrue(appointeeEmail.getBody().contains("You are receiving this update as the appointee for"));
        Notification jointPartyEmail = notifications.stream()
            .filter(f -> f.getTemplateId().toString().equals(jointPartyAppealWithdrawnEmailIdWelsh))
            .collect(toList()).get(0);
        assertTrue(jointPartyEmail.getBody().contains("Annwyl Joint Party"));
        assertTrue(jointPartyEmail.getBody().contains("Ysgrifennwyd yr e-bost hwn yn Gymraeg a Saesneg"));
        List<Notification> letters = fetchLetters();
        assertEquals(letters.size(), 3);
        for (int i = 0; i < 3; i++) {
            assertEquals(letters.get(i).toString(), "Pre-compiled PDF", letters.get(i).getSubject().orElse("Unknown Subject"));
        }
    }

    @Test
    public void shouldSendAppointeeEvidenceReminderForOralCaseNotification() throws NotificationClientException, IOException {
        simulateCcdCallback(EVIDENCE_REMINDER,
            "tyanotifications/appointee/oral-" + EVIDENCE_REMINDER.getId() + "CallbackWelsh.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
            oralAppointeeEvidenceReminderEmailIdWelsh,
            oralAppointeeEvidenceReminderSmsIdWelsh,
            oralJointPartyEvidenceReminderEmailIdWelsh,
            oralJointPartyEvidenceReminderSmsIdWelsh
        );

        assertNotificationBodyContains(
            notifications,
            oralAppointeeEvidenceReminderEmailIdWelsh,
            DEAR_APPOINTEE_USER,
            AS_APPOINTEE_FOR,
            "/evidence/" + TYA
        );
    }

    @Test
    @Parameters(method = "docmosisTestSetup")
    public void shouldSendDocmosisLettersViaGovNotify(NotificationEventType notificationEventType,
                                                      Optional<String> resourceParam,
                                                      int expectedNumberOfLetters) throws IOException, NotificationClientException {

        simulateCcdCallback(notificationEventType,
            BASE_PATH_TYAN + notificationEventType.getId() + resourceParam.orElse("") + "CallbackWelsh.json");
        List<Notification> notifications = fetchLetters();

        assertEquals(expectedNumberOfLetters, notifications.size());
        notifications.forEach(n -> assertEquals("Pre-compiled PDF", n.getSubject().orElse("Unknown Subject")));
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] docmosisTestSetup() {
        return new Object[]{
            new Object[]{NON_COMPLIANT, Optional.empty(), 2},
            new Object[]{DRAFT_TO_NON_COMPLIANT, Optional.empty(), 2},
            new Object[]{REQUEST_FOR_INFORMATION, Optional.empty(), 1},
            new Object[]{STRUCK_OUT, Optional.empty(), 3},
            new Object[]{DIRECTION_ISSUED_WELSH, Optional.of("ProvideInformation"), 3},
            new Object[]{DIRECTION_ISSUED_WELSH, Optional.of("AppealToProceed"), 3},
            new Object[]{DIRECTION_ISSUED_WELSH, Optional.of("RefuseExtension"), 3},
            new Object[]{DIRECTION_ISSUED_WELSH, Optional.of("GrantExtension"), 3},
            new Object[]{JOINT_PARTY_ADDED, Optional.empty(), 2}
        };
    }
}
