package uk.gov.hmcts.reform.sscs.tyanotifications.factory;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED;

import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.personalisation.*;

@Component
public class PersonalisationFactory implements Function<NotificationEventType, Personalisation> {

    @Autowired
    private SyaAppealCreatedAndReceivedPersonalisation syaAppealCreatedAndReceivedPersonalisation;

    @Autowired
    private WithRepresentativePersonalisation withRepresentativePersonalisation;

    @Autowired
    private SubscriptionPersonalisation subscriptionPersonalisation;

    @Autowired
    private ActionFurtherEvidencePersonalisation actionFurtherEvidencePersonalisation;

    @Autowired
    private Personalisation personalisation;

    @Override
    public Personalisation apply(NotificationEventType notificationType) {
        if (isNull(notificationType)) {
            return null;
        }

        if (EVENTS_FOR_SYA_PERSONALISATION.contains(notificationType)) {
            return syaAppealCreatedAndReceivedPersonalisation;
        } else if (EVENTS_FOR_REPRESENTATIVE_PERSONALISATION.contains(notificationType)) {
            return withRepresentativePersonalisation;
        } else if (SUBSCRIPTION_UPDATED.equals(notificationType)) {
            return subscriptionPersonalisation;
        } else if (EVENTS_FOR_ACTION_FURTHER_EVIDENCE.contains(notificationType)) {
            return actionFurtherEvidencePersonalisation;
        } else {
            return this.personalisation;
        }
    }
}
