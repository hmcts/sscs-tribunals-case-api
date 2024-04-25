package uk.gov.hmcts.reform.sscs.functional.tyanotifications.sya.notifications;

import static org.apache.commons.lang3.StringUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.functional.tyanotifications.AbstractFunctionalTest;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

public class JointPartyFunctionalTest extends AbstractFunctionalTest {
    private static final String NO_HEARING_TYPE = null;
    private static final String ORAL = "oral";
    private static final String PAPER = "paper";

    /* // SSCS-11586

    @Value("${notification.english.appealLapsed.joint_party.emailId}")
    private String appealLapsedJointPartyEmailId;
    @Value("${notification.english.appealLapsed.joint_party.smsId}")
    private String appealLapsedJointPartySmsId;
    @Value("${notification.english.hearingAdjourned.joint_party.emailId}")
    private String hearingAdjournedJointPartyEmailId;
    @Value("${notification.english.hearingAdjourned.joint_party.smsId}")
    private String hearingAdjournedJointPartySmsId;
    @Value("${notification.english.listAssist.oral.hearingPostponed.joint_party.emailId}")
    private String hearingPostponedJointPartyEmailId;
    @Value("${notification.english.oral.evidenceReminder.joint_party.emailId}")
    private String oralEvidenceReminderJointPartyEmailId;
    @Value("${notification.english.oral.evidenceReminder.joint_party.smsId}")
    private String oralEvidenceReminderJointPartySmsId;
    @Value("${notification.english.paper.evidenceReminder.joint_party.emailId}")
    private String paperEvidenceReminderJointPartyEmailId;
    @Value("${notification.english.paper.evidenceReminder.joint_party.smsId}")
    private String paperEvidenceReminderJointPartySmsId;
    @Value("${notification.english.oral.evidenceReceived.joint_party.emailId}")
    private String oralEvidenceReceivedJointPartyEmailId;
    @Value("${notification.english.oral.evidenceReceived.joint_party.smsId}")
    private String oralEvidenceReceivedJointPartySmsId;
    @Value("${notification.english.listAssist.oral.hearingBooked.joint_party.emailId}")
    private String hearingBookedJointPartyEmailId;
    @Value("${notification.english.listAssist.oral.hearingBooked.joint_party.smsId}")
    private String hearingBookedJointPartySmsId;
    @Value("${notification.english.listAssist.oral.hearingReminder.joint_party.emailId}")
    private String hearingReminderJointPartyEmailId;
    @Value("${notification.english.listAssist.oral.hearingReminder.joint_party.smsId}")
    private String hearingReminderJointPartySmsId;
    @Value("${notification.english.appealWithdrawn.joint_party.emailId}")
    private String appealWithdrawnJointPartyEmailId;
    @Value("${notification.english.appealWithdrawn.joint_party.smsId}")
    private String appealWithdrawnJointPartySmsId;
    @Value("${notification.english.oral.appealDormant.joint_party.emailId}")
    private String oralAppealDormantJointPartyEmailId;
    @Value("${notification.english.paper.appealDormant.joint_party.emailId}")
    private String paperAppealDormantJointPartyEmailId;
    @Value("${notification.english.paper.appealDormant.joint_party.smsId}")
    private String paperAppealDormantJointPartySmsId;
    @Value("${notification.english.oral.dwpUploadResponse.joint_party.emailId}")
    private String oralDwpUploadResponseJointPartyEmailId;
    @Value("${notification.english.oral.dwpUploadResponse.joint_party.smsId}")
    private String oralDwpUploadResponseJointPartySmsId;
    @Value("${notification.english.paper.dwpUploadResponse.joint_party.emailId}")
    private String paperDwpUploadResponseJointPartyEmailId;
    @Value("${notification.english.paper.dwpUploadResponse.joint_party.smsId}")
    private String paperDwpUploadResponseJointPartySmsId;
    @Value("${notification.english.reviewConfidentialityRequest.joint_party.docmosisId}")
    private String reviewConfidentialityRequestJointPartyLetterId;

    */

    public JointPartyFunctionalTest() {
        super(30);
    }

    // TODO: SSCS-11436
    @Test
    @Ignore
    @Parameters(method = "eventTypeAndSubscriptions")
    public void givenEventAndJointPartySubscription_shouldSendNotificationToJointParty(
        NotificationEventType notificationEventType, @Nullable String hearingType,
        int expectedNumberOfLetters, boolean isDocmosisLetter)
        throws Exception {
        //Given
        final String jointPartyEmailId = getFieldValue(hearingType, notificationEventType, "JointPartyEmailId");
        final String jointPartySmsId = getFieldValue(hearingType, notificationEventType, "JointPartySmsId");

        simulateCcdCallback(notificationEventType,
            "tyanotifications/jointParty/" + ((hearingType == null) ? EMPTY : (hearingType + "-")) + notificationEventType.getId() + "Callback.json");

        List<String> expectedIds = new ArrayList<>();

        if (jointPartyEmailId != null) {
            expectedIds.add(jointPartyEmailId);
        }

        if (jointPartySmsId != null) {
            expectedIds.add(jointPartySmsId);
        }

        List<Notification> notifications = tryFetchNotificationsForTestCase(expectedIds.toArray(new String[expectedIds.size()]));

        assertEquals(expectedIds.size(), notifications.size());

        String jointPartyName = "Joint Party";
        assertNotificationBodyContains(notifications, jointPartyEmailId, jointPartyName);
        assertNotificationBodyContains(notifications, jointPartySmsId);


        if (expectedNumberOfLetters > 0) {
            List<Notification> notificationLetters = fetchLetters();
            assertEquals(expectedNumberOfLetters, notificationLetters.size());
            if (!isDocmosisLetter) {
                Optional<Notification> notificationOptional =
                    notificationLetters.stream().filter(notification ->
                        notification.getLine1().map(f -> f.contains(jointPartyName)).orElse(false)).findFirst();
                assertTrue(notificationOptional.isPresent());
                assertTrue(notificationOptional.get().getBody().contains("Dear " + jointPartyName));
            } else {
                notificationLetters.forEach(n -> assertEquals("Pre-compiled PDF", n.getSubject().orElse("Unknown Subject")));
            }
        }
    }

    @Test
    public void sendsDirectionIssuedProvideInformationLetterToAppellantRepresentativeAndJointParty() throws IOException, NotificationClientException {

        NotificationEventType notificationEventType = NotificationEventType.DIRECTION_ISSUED;

        simulateCcdCallback(notificationEventType,
            notificationEventType.getId() + "ProvideInformationCallback.json");

        List<Notification> notifications = fetchLetters();

        assertEquals(3, notifications.size());
        assertEquals("Pre-compiled PDF", notifications.get(0).getSubject().orElse("Unknown Subject"));
        assertEquals("Pre-compiled PDF", notifications.get(1).getSubject().orElse("Unknown Subject"));
        assertEquals("Pre-compiled PDF", notifications.get(2).getSubject().orElse("Unknown Subject"));
    }

    private String getFieldValue(String hearingType, NotificationEventType notificationEventType, String fieldName) throws Exception {
        String fieldValue;
        try {
            Field field = this.getClass().getDeclaredField(
                defaultIfBlank(hearingType, EMPTY)
                    + ((hearingType == null) ? notificationEventType.getId() : capitalize(notificationEventType.getId()))
                    + fieldName);
            field.setAccessible(true);
            fieldValue = (String) field.get(this);
        } catch (NoSuchFieldException e) {
            fieldValue = null;
        }
        return fieldValue;
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] eventTypeAndSubscriptions() {
        final int expectedNumberOfLettersIsThree = 3;
        final int expectedNumberOfLettersIsTwo = 2;
        final int expectedNumberOfLettersIsOne = 1;
        final int expectedNumberOfLettersIsZero = 0;
        final boolean isDocmosisLetterTrue = true;
        final boolean isDocmosisLetterFalse = false;
        return new Object[]{
            new Object[]{APPEAL_LAPSED, NO_HEARING_TYPE, expectedNumberOfLettersIsThree, isDocmosisLetterTrue},
            new Object[]{APPEAL_DORMANT, ORAL, expectedNumberOfLettersIsZero, isDocmosisLetterFalse},
            new Object[]{APPEAL_DORMANT, PAPER, expectedNumberOfLettersIsZero, isDocmosisLetterFalse},
            new Object[]{ADJOURNED, NO_HEARING_TYPE, expectedNumberOfLettersIsZero, isDocmosisLetterFalse},
            new Object[]{POSTPONEMENT, NO_HEARING_TYPE, expectedNumberOfLettersIsZero, isDocmosisLetterFalse},
            new Object[]{EVIDENCE_REMINDER, ORAL, expectedNumberOfLettersIsZero, isDocmosisLetterFalse},
            new Object[]{HEARING_BOOKED, NO_HEARING_TYPE, expectedNumberOfLettersIsZero, isDocmosisLetterFalse},
            new Object[]{HEARING_REMINDER, NO_HEARING_TYPE, expectedNumberOfLettersIsZero, isDocmosisLetterFalse},
            new Object[]{EVIDENCE_RECEIVED, ORAL, expectedNumberOfLettersIsZero, isDocmosisLetterFalse},
            new Object[]{EVIDENCE_REMINDER, PAPER, expectedNumberOfLettersIsZero, isDocmosisLetterFalse},
            new Object[]{DWP_UPLOAD_RESPONSE, ORAL, expectedNumberOfLettersIsTwo, isDocmosisLetterTrue},
            new Object[]{STRUCK_OUT, PAPER, expectedNumberOfLettersIsTwo, isDocmosisLetterTrue},
            new Object[]{APPEAL_WITHDRAWN, NO_HEARING_TYPE, expectedNumberOfLettersIsTwo, isDocmosisLetterTrue},
            new Object[]{DIRECTION_ISSUED, NO_HEARING_TYPE, expectedNumberOfLettersIsTwo, isDocmosisLetterTrue},
            new Object[]{REVIEW_CONFIDENTIALITY_REQUEST, NO_HEARING_TYPE, expectedNumberOfLettersIsOne, isDocmosisLetterTrue}
        };
    }
}
