package uk.gov.hmcts.reform.sscs.tyanotifications.config;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppealHearingType.ORAL;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppealHearingType.PAPER;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.EVENT_TYPES_FOR_BUNDLED_LETTER;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;

import java.util.*;
import junitparams.Parameters;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.Template;

public class BundledNotificationsConfigTestIt extends AbstractNotificationConfigTest {
    private static final Set<NotificationEventType> BUNDLED_LETTER_EVENT_TYPES_IGNORED = EnumSet.of(
        ACTION_POSTPONEMENT_REQUEST,
        ACTION_POSTPONEMENT_REQUEST_WELSH,
        ADMIN_CORRECTION_HEADER,
        CORRECTION_GRANTED,
        CORRECTION_REFUSED,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        ISSUE_ADJOURNMENT_NOTICE,
        ISSUE_ADJOURNMENT_NOTICE_WELSH
    );
    private static final Set<AppealHearingType> APPEAL_HEARING_TYPES = Set.of(PAPER, ORAL);
    private static final Set<HearingRoute> HEARING_ROUTE_TYPES = Set.of(HearingRoute.GAPS, HearingRoute.LIST_ASSIST);

    @Test
    @Parameters(method = "bundledLetterTemplateNames")
    public void given_bundledLetters_should_notHaveTemplate(NotificationEventType eventType) {
        List<Template> templates = getTemplates(eventType);

        assertThat(templates)
            .isNotEmpty()
            .allSatisfy(template -> {
                assertThat(template.getEmailTemplateId()).isNull();
                assertThat(template.getSmsTemplateId()).isEmpty();
                assertThat(template.getLetterTemplateId()).isNull();
                assertThat(template.getDocmosisTemplateId()).isNull();
            });
    }

    @NotNull
    private List<Template> getTemplates(NotificationEventType eventType) {
        List<Template> templates = new ArrayList<>();
        for (SubscriptionType subscriptionType : SubscriptionType.values()) {
            for (AppealHearingType appealHearingType : APPEAL_HEARING_TYPES) {
                for (HearingRoute hearingRoute : HEARING_ROUTE_TYPES) {
                    templates.add(getTemplate(eventType, subscriptionType, appealHearingType, hearingRoute, true, null));
                    templates.add(getTemplate(eventType, subscriptionType, appealHearingType, hearingRoute, false, null));
                }
            }
        }
        return templates;
    }

    private Object[] bundledLetterTemplateNames() {
        Set<NotificationEventType> bundledLetterEventTypes = new HashSet<>(EVENT_TYPES_FOR_BUNDLED_LETTER);
        bundledLetterEventTypes.removeAll(BUNDLED_LETTER_EVENT_TYPES_IGNORED);
        return bundledLetterEventTypes.toArray();
    }
}
