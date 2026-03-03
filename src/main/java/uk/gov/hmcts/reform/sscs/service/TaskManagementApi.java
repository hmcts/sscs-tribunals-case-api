package uk.gov.hmcts.reform.sscs.service;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import javax.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.sscs.model.task.management.GetTasksResponse;
import uk.gov.hmcts.reform.sscs.model.task.management.TaskRequestPayload;

@FeignClient(
        name = "wa",
        url = "${task-management.api.url}"
)
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public interface TaskManagementApi {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    String AUTHORIZATION = "Authorization";

    @PostMapping(value = "/task",
            consumes = APPLICATION_JSON_VALUE)
    GetTasksResponse getTasksByCaseId(
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
            @RequestHeader(AUTHORIZATION) String authorization,
            @Valid @RequestBody TaskRequestPayload taskRequestPayload
    );

    @PostMapping(
            value = "/task/{task-id}/cancel",
            consumes = APPLICATION_JSON_VALUE
    )
    void cancelTask(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                    @RequestHeader(AUTHORIZATION) String authorisation,
                    @PathVariable("task-id") String id);
}
