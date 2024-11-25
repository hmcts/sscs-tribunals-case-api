package uk.gov.hmcts.reform.sscs.functional.tyanotifications.handlers;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.ADMIN_APPEAL_WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.CASE_UPDATED;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceType;
import uk.gov.hmcts.reform.sscs.functional.tyanotifications.AbstractFunctionalTest;
import uk.gov.hmcts.reform.sscs.functional.tyanotifications.Retry;
import uk.gov.service.notify.Notification;

public class AdminAppealWithdrawnNotificationsTest extends AbstractFunctionalTest {

    public AdminAppealWithdrawnNotificationsTest() {
        super(30);
    }

    @Rule
    public Retry retry = new Retry(0);

    @Rule
    public Timeout globalTimeout = Timeout.seconds(600);

    @Before
    public void setUp() {
        initialiseCcdCase();
    }

    @Test
    @Parameters({
        "Appellant, 8620e023-f663-477e-a771-9cfad50ee30f, 446c7b23-7342-42e1-adff-b4c367e951cb, 1, 1, 1, true",
        "Appointee, 8620e023-f663-477e-a771-9cfad50ee30f, 446c7b23-7342-42e1-adff-b4c367e951cb, 1, 1, 1, false",
        "Reps, e29a2275-553f-4e70-97f4-2994c095f281, f59440ee-19ca-4d47-a702-13e9cecaccbd, 1, 1, 2, false"
    })
    public void givenCallbackWithSubscriptions_shouldSendEmailSmsAndLetterNotifications(
        String subscription,
        String emailId,
        String smsId,
        int expectedNumEmailNotifications,
        int expectedNumSmsNotifications,
        int expectedNumLetters, boolean checkFromCorrespondence) throws Exception {

        simulateCcdCallback(ADMIN_APPEAL_WITHDRAWN, "tyanotifications/handlers/" + ADMIN_APPEAL_WITHDRAWN.getId() + subscription
            + "Callback.json");

        delayInSeconds(5);
        List<Notification> notifications = tryFetchNotificationsForTestCase(emailId, smsId);

        assertEquals(expectedNumEmailNotifications, getNumberOfNotificationsForGivenEmailOrSmsTemplateId(notifications, emailId));
        assertEquals(expectedNumSmsNotifications, getNumberOfNotificationsForGivenEmailOrSmsTemplateId(notifications, smsId));

        if(checkFromCorrespondence) {
            assertTrue(fetchLettersFromCase(expectedNumLetters, subscription));
        }
        else {
            List<Notification> letterNotifications = fetchLetters();
            assertThat(letterNotifications).hasSize(expectedNumLetters);
        }

    }

    private boolean fetchLettersFromCase(int expectedNumLetters, String subscription) {
        int fetchCount = 0;
        do {
            if (getNumberOfLetterCorrespondence(subscription) == expectedNumLetters) {
                return true;
            }
            delayInSeconds(10);
        } while (true);
    }

    private void initialiseCcdCase() {
        caseData.setCorrespondence(null);
        caseData.setSubscriptions(null);
        ccdService.updateCase(caseData, caseId, CASE_UPDATED.getId(), "create by Test",
            "Notification Service updated case", idamTokens);
    }

    private long getNumberOfNotificationsForGivenEmailOrSmsTemplateId(List<Notification> notifications, String emailId) {
        return notifications.stream()
            .filter(notification -> notification.getTemplateId().equals(UUID.fromString(emailId)))
            .count();
    }

    private long getNumberOfLetterCorrespondence(String subscription) {
        List<Correspondence> correspondence = ccdService
            .getByCaseId(caseId, idamTokens).getData().getCorrespondence();
        if (correspondence == null) {
            return 0;
        }
        return correspondence.stream()
            .filter(c -> c.getValue().getCorrespondenceType() == CorrespondenceType.Letter)
            .filter(c -> c.getValue().getDocumentLink().getDocumentFilename().contains(ADMIN_APPEAL_WITHDRAWN.getId()))
            .filter(c -> isAMatch(subscription, c.getValue().getTo()))
            .count();
    }

    private boolean isAMatch(String subscription, String text) {
        Pattern pattern = Pattern.compile(setRegExpBasedOnSubscription(subscription), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

    private String setRegExpBasedOnSubscription(String subscription) {
        String regexp = subscription;
        if ("Reps".equals(subscription)) {
            regexp = "(\\bAppellant\\b|\\bReps\\b)";
        }
        return regexp;
    }

}
