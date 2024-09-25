package uk.gov.hmcts.reform.sscs.tyanotifications.personalisation;

import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.*;

import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationUtils;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.SendNotificationHelper;


@Component
public class WithRepresentativePersonalisation extends Personalisation<CcdNotificationWrapper> {

    @Override
    protected Map<String, Object> create(NotificationSscsCaseDataWrapper responseWrapper, SubscriptionWithType subscriptionWithType) {
        Map<String, Object> personalisation = super.create(responseWrapper, subscriptionWithType);
        SscsCaseData ccdResponse = responseWrapper.getNewSscsCaseData();

        setRepresentativeName(personalisation, ccdResponse);

        return personalisation;
    }

    public Map<String, Object> setRepresentativeName(Map<String, Object> personalisation, SscsCaseData sscsCaseData) {
        if (NotificationUtils.hasRepresentative(sscsCaseData.getAppeal())) {
            personalisation.put(REPRESENTATIVE_NAME,
                SendNotificationHelper.getRepSalutation(sscsCaseData.getAppeal().getRep(), true));
        }
        overrideRepNameIfNotificationIsForAnOtherParty(personalisation);

        return personalisation;
    }

    private void overrideRepNameIfNotificationIsForAnOtherParty(final Map<String, Object> personalisation) {
        //REPRESENTATIVE_NAME tag in templates is reused to send notification to other parties,
        //so that we don't need to refactor all the notification templates to accommodate other parties
        if (personalisation.get(OTHER_PARTY) != null) {
            personalisation.put(REPRESENTATIVE_NAME, personalisation.get(OTHER_PARTY));
            personalisation.put(IS_OTHER_PARTY, "Yes");
        }
    }

}
