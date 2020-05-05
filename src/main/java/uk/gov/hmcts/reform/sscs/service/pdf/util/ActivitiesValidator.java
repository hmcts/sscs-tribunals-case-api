package uk.gov.hmcts.reform.sscs.service.pdf.util;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.domain.wrapper.Activities;
import uk.gov.hmcts.reform.sscs.domain.wrapper.Activity;
import uk.gov.hmcts.reform.sscs.domain.wrapper.OnlineHearing;
import uk.gov.hmcts.reform.sscs.util.I18nBuilder;

@Component
public class ActivitiesValidator {
    private final Map i18n;

    public ActivitiesValidator(I18nBuilder i18nBuilder) throws IOException {
        this.i18n = i18nBuilder.build();
    }

    public void validateWeHaveMappingForActivities(OnlineHearing onlineHearing) {
        Activities activities = onlineHearing.getDecision().getActivities();
        Map activitiesMap = (Map) (((Map) i18n.get("tribunalView")).get("activities"));
        validate(activities.getDailyLiving(), (Map)(activitiesMap.get("dailyLiving")));
        validate(activities.getMobility(), (Map)(activitiesMap.get("mobility")));
    }

    private void validate(List<Activity> dailyLiving, Map activitiesMap) {
        dailyLiving.forEach(activity -> {
            Map activityMap = (Map)(activitiesMap.get(activity.getActivity()));
            if (activityMap == null) {
                throw new IllegalArgumentException("Cannot find mapping for activity [" + activity.getActivity() + "]");
            }
            Object score = ((Map)activityMap.get("scores")).get(activity.getSelectionKey());
            if (score == null) {
                throw new IllegalArgumentException("Cannot find score for activity [" + activity.getActivity() + "] and key [" + activity.getSelectionKey() + "]");
            }
        });
    }
}
