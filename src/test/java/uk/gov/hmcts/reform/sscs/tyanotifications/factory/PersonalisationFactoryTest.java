package uk.gov.hmcts.reform.sscs.tyanotifications.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.DO_NOT_SEND;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.personalisation.Personalisation;
import uk.gov.hmcts.reform.sscs.tyanotifications.personalisation.SubscriptionPersonalisation;
import uk.gov.hmcts.reform.sscs.tyanotifications.personalisation.SyaAppealCreatedAndReceivedPersonalisation;
import uk.gov.hmcts.reform.sscs.tyanotifications.personalisation.WithRepresentativePersonalisation;

@RunWith(JUnitParamsRunner.class)
public class PersonalisationFactoryTest {

    @Mock
    private SubscriptionPersonalisation subscriptionPersonalisation;

    @Mock
    private Personalisation personalisation;

    @Mock
    private WithRepresentativePersonalisation withRepresentativePersonalisation;

    @Mock
    private SyaAppealCreatedAndReceivedPersonalisation syaAppealCreatedAndReceivedPersonalisation;

    @InjectMocks
    private PersonalisationFactory factory;

    @BeforeEach
    public void setup() {
        openMocks(this);
    }

    @Test
    public void createPersonalisationWhenNotificationApplied() {
        Personalisation result = factory.apply(DO_NOT_SEND);
        assertEquals(personalisation, result);
    }

    @Test
    public void createSubscriptionPersonalisationWhenSubscriptionUpdatedNotificationApplied() {
        Personalisation result = factory.apply(SUBSCRIPTION_UPDATED);
        assertEquals(subscriptionPersonalisation, result);
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"APPEAL_LAPSED", "APPEAL_LAPSED", "DWP_APPEAL_LAPSED",
        "HMCTS_APPEAL_LAPSED", "APPEAL_WITHDRAWN", "APPEAL_DORMANT",
        "ADJOURNED", "POSTPONEMENT", "HEARING_BOOKED", "EVIDENCE_REMINDER",
        "HEARING_REMINDER", "DWP_RESPONSE_RECEIVED", "DWP_UPLOAD_RESPONSE",
        "DIRECTION_ISSUED", "DECISION_ISSUED", "DIRECTION_ISSUED_WELSH", "DECISION_ISSUED_WELSH",
        "PROCESS_AUDIO_VIDEO", "PROCESS_AUDIO_VIDEO_WELSH", "ACTION_POSTPONEMENT_REQUEST", "ACTION_POSTPONEMENT_REQUEST_WELSH",
        "ISSUE_FINAL_DECISION", "ISSUE_FINAL_DECISION_WELSH", "ISSUE_ADJOURNMENT_NOTICE", "STRUCK_OUT", "NON_COMPLIANT",
        "DEATH_OF_APPELLANT", "PROVIDE_APPOINTEE_DETAILS"})
    public void createRepsPersonalisationWhenNotificationApplied(NotificationEventType eventType) {
        Personalisation result = factory.apply(eventType);
        assertEquals(withRepresentativePersonalisation, result);
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"SYA_APPEAL_CREATED", "VALID_APPEAL_CREATED", "APPEAL_RECEIVED",
        "CASE_UPDATED"})
    public void createSyaAppealCreatedPersonalisationWhenNotificationApplied(NotificationEventType eventType) {
        Personalisation result = factory.apply(eventType);
        assertEquals(syaAppealCreatedAndReceivedPersonalisation, result);
    }

    @Test
    public void shouldReturnNullWhenNotificationTypeIsNull() {
        Personalisation personalisation = factory.apply(null);
        assertNull(personalisation);
    }

}
