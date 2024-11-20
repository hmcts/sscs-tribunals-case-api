package uk.gov.hmcts.reform.sscs.tyanotifications.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

public class IntegrationTestHelper {

    private IntegrationTestHelper() {

    }

    public static MockHttpServletRequestBuilder getRequestWithAuthHeader(String json, String url) {

        return getRequestWithoutAuthHeader(json, url)
                .header(AuthorisationService.SERVICE_AUTHORISATION_HEADER, "some-auth-header");
    }

    public static MockHttpServletRequestBuilder getRequestWithAuthHeader(String json) {

        return getRequestWithoutAuthHeader(json)
            .header(AuthorisationService.SERVICE_AUTHORISATION_HEADER, "some-auth-header");
    }

    public static MockHttpServletRequestBuilder getRequestWithoutAuthHeader(String json) {

        return getRequestWithoutAuthHeader(json, "/sendNotification");
    }

    private static MockHttpServletRequestBuilder getRequestWithoutAuthHeader(String json, String url) {

        return post(url)
                .contentType(APPLICATION_JSON)
                .content(json);
    }

    public static void assertHttpStatus(HttpServletResponse response, HttpStatus status) {
        assertThat(response.getStatus()).isEqualTo(status.value());
    }

    public static String updateEmbeddedJson(String json, Object value, String... keys) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        Map map = objectMapper.readValue(json, Map.class);
        Map t = map;
        for (int i = 0; i < keys.length - 1; i++) {
            t = (Map) t.get(keys[i]);
        }

        t.put(keys[keys.length - 1], value);

        return objectMapper.writeValueAsString(map);
    }

    public static void assertScheduledJobCount(
        Scheduler quartzScheduler,
        String message,
        int expectedValue
    ) {
        try {

            Set<JobKey> jobKeys =
                quartzScheduler
                    .getJobKeys(GroupMatcher.anyGroup());

            String observedGroups =
                jobKeys
                    .stream()
                    .map(jobKey -> jobKey.getGroup())
                    .collect(Collectors.joining(", "));

            assertTrue(
                jobKeys.size() == expectedValue,
                message + " (" + expectedValue + " != " + jobKeys.size() + ") [" + observedGroups + "]"
            );

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertScheduledJobCount(
        Scheduler quartzScheduler,
        String message,
        String groupMatch,
        int expectedValue
    ) {
        try {

            Set<JobKey> jobKeys =
                quartzScheduler
                    .getJobKeys(GroupMatcher.jobGroupContains(groupMatch));

            String observedGroups =
                jobKeys
                    .stream()
                    .map(jobKey -> jobKey.getGroup())
                    .collect(Collectors.joining(", "));

            assertTrue(
                jobKeys.size() == expectedValue,
                message + " (" + expectedValue + " != " + jobKeys.size() + ") [" + observedGroups + "]"
            );

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertScheduledJobTriggerAt(
        Scheduler quartzScheduler,
        String message,
        String groupMatch,
        String expectedTriggerAt
    ) {
        try {

            Set<JobKey> jobKeys =
                quartzScheduler.getJobKeys(GroupMatcher.jobGroupContains(groupMatch));

            if (jobKeys.isEmpty()) {
                assertTrue(false, message + " -- job group match not found");
            }

            List<String> triggersAt =
                quartzScheduler
                    .getJobKeys(GroupMatcher.jobGroupContains(groupMatch))
                    .stream()
                    .flatMap(jobKey -> {
                        try {
                            return quartzScheduler.getTriggersOfJob(jobKey).stream();
                        } catch (SchedulerException ignore) {
                            return Collections.<Trigger>emptyList().stream();
                        }
                    })
                    .map(trigger -> trigger.getStartTime().toInstant().toString())
                    .collect(Collectors.toList());

            assertTrue(
                triggersAt.contains(expectedTriggerAt),
                message + " -- " + expectedTriggerAt + " not found in collection [" + String.join(", ", triggersAt) + "]"
            );

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

}
