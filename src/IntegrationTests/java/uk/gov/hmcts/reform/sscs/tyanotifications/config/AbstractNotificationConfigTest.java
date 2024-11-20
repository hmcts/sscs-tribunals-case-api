package uk.gov.hmcts.reform.sscs.tyanotifications.config;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import junitparams.JUnitParamsRunner;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;


@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_es_it.properties")
public abstract class AbstractNotificationConfigTest {
    public static final String SUBSCRIPTION_TEMPLATE_NAME_TEMPLATE = "%s.%s";

    @Autowired
    protected NotificationConfig notificationConfig;

    protected String getTemplateName(NotificationEventType notificationEventType, SubscriptionType subscriptionType) {
        return SUBSCRIPTION_TEMPLATE_NAME_TEMPLATE.formatted(
            notificationEventType.getId(),
            subscriptionType.name().toLowerCase());
    }

    protected String getTemplateName(NotificationEventType notificationEventType) {
        return notificationEventType.getId();
    }

    protected Template getTemplate(NotificationEventType eventType, SubscriptionType subscriptionType,
                                   AppealHearingType appealHearingType, HearingRoute hearingRoute, boolean welsh, String createdInGapsFrom) {
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(SscsCaseData.builder()
                .languagePreferenceWelsh(welsh ? YES.getValue() : null)
                .appeal(Appeal.builder()
                    .hearingType(appealHearingType.name())
                    .build())
                .schedulingAndListingFields(SchedulingAndListingFields.builder()
                    .hearingRoute(hearingRoute)
                    .build())
                .build())
            .build();

        CcdNotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);

        String template = getTemplateName(eventType);
        if (nonNull(subscriptionType)) {
            template = getTemplateName(eventType, subscriptionType);
        }

        return notificationConfig.getTemplate(template, template, template, template, PIP, wrapper, createdInGapsFrom);
    }
}
