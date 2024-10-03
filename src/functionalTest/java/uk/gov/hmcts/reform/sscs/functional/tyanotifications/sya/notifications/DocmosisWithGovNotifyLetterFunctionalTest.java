package uk.gov.hmcts.reform.sscs.functional.tyanotifications.sya.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.DOCMOSIS_LETTERS;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.functional.tyanotifications.AbstractFunctionalTest;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

@RunWith(JUnitParamsRunner.class)
public class DocmosisWithGovNotifyLetterFunctionalTest extends AbstractFunctionalTest {

    public static final String EXPECTED_LETTER_SUBJECT = "Pre-compiled PDF";
    //TODO: Add callback jsons for these letters to test them functionally
    private static final Set<NotificationEventType> DOCMOSIS_LETTERS_WITH_NO_TEST_CALLBACK = EnumSet.of(
        ACTION_HEARING_RECORDING_REQUEST,
        ACTION_POSTPONEMENT_REQUEST_WELSH,
        ADMIN_APPEAL_WITHDRAWN,
        APPEAL_LAPSED,
        APPEAL_WITHDRAWN,
        DECISION_ISSUED_WELSH,
        DIRECTION_ISSUED_WELSH,
        DWP_APPEAL_LAPSED,
        DWP_UPLOAD_RESPONSE,
        HMCTS_APPEAL_LAPSED,
        JOINT_PARTY_ADDED,
        POSTPONEMENT,
        RESEND_APPEAL_CREATED,
        VALID_APPEAL_CREATED
    );

    public DocmosisWithGovNotifyLetterFunctionalTest() {
        super(30);
    }

    // TODO: SSCS-11436
    @Test
    @Ignore
    @Parameters(method = "eventTypes")
    public void shouldSendDocmosisLetters(NotificationEventType notificationEventType)
        throws IOException, NotificationClientException {
        simulateCcdCallback(notificationEventType);

        List<Notification> notifications = fetchLetters();
        assertThat(notifications)
            .extracting(Notification::getSubject)
            .allSatisfy(subject ->
                assertThat(subject)
                    .isPresent()
                    .hasValue(EXPECTED_LETTER_SUBJECT)
            );
    }

    @Test
    @Parameters(method = "expectedNumberOfLetters")
    public void shouldSendCorrectNumberOfDocmosisLetters(NotificationEventType notificationEventType,
                                                         int expectedNumberOfLetters)
        throws IOException, NotificationClientException {
        simulateCcdCallback(notificationEventType);

        List<Notification> notifications = fetchLetters();
        assertThat(notifications)
            .hasSize(expectedNumberOfLetters)
            .extracting(Notification::getSubject)
            .allSatisfy(subject ->
                assertThat(subject)
                    .isPresent()
                    .hasValue(EXPECTED_LETTER_SUBJECT)
            );
    }

    private Object[] eventTypes() {
        Set<NotificationEventType> docmosisLetters = new HashSet<>(DOCMOSIS_LETTERS);
        docmosisLetters.removeAll(DOCMOSIS_LETTERS_WITH_NO_TEST_CALLBACK);
        return docmosisLetters.toArray();
    }

    private Object[] expectedNumberOfLetters() {
        int expectedNumberOfLettersIsOne = 1;
        int expectedNumberOfLettersIsTwo = 2;
        int expectedNumberOfLettersIsThree = 3;
        int expectedNumberOfLettersIsFour = 4;
        return new Object[]{
            new Object[]{PROCESS_AUDIO_VIDEO, expectedNumberOfLettersIsThree},
            new Object[]{PROCESS_AUDIO_VIDEO_WELSH, expectedNumberOfLettersIsThree},
            new Object[]{REQUEST_FOR_INFORMATION, expectedNumberOfLettersIsOne},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE, expectedNumberOfLettersIsThree},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE_WELSH, expectedNumberOfLettersIsThree},
            new Object[]{STRUCK_OUT, expectedNumberOfLettersIsTwo},
            new Object[]{ISSUE_FINAL_DECISION, expectedNumberOfLettersIsTwo},
            new Object[]{ISSUE_FINAL_DECISION_WELSH, expectedNumberOfLettersIsFour},
            new Object[]{DECISION_ISSUED, expectedNumberOfLettersIsTwo},
            new Object[]{DIRECTION_ISSUED, expectedNumberOfLettersIsTwo},
            new Object[]{APPEAL_RECEIVED, expectedNumberOfLettersIsTwo},
            new Object[]{REVIEW_CONFIDENTIALITY_REQUEST, expectedNumberOfLettersIsOne},
            new Object[]{NON_COMPLIANT, expectedNumberOfLettersIsTwo},
            new Object[]{DRAFT_TO_NON_COMPLIANT, expectedNumberOfLettersIsTwo},
            new Object[]{ACTION_POSTPONEMENT_REQUEST, expectedNumberOfLettersIsTwo},
            new Object[]{DEATH_OF_APPELLANT, expectedNumberOfLettersIsTwo},
            new Object[]{PROVIDE_APPOINTEE_DETAILS, expectedNumberOfLettersIsTwo},
            new Object[]{UPDATE_OTHER_PARTY_DATA, expectedNumberOfLettersIsOne}
        };
    }
}
