package uk.gov.hmcts.reform.sscs.service;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.gov.hmcts.reform.sscs.domain.CamundaTask;

@FeignClient(
        name = "camunda",
        url = "https://camunda-sscs-tribunals-api-pr-4986.preview.platform.hmcts.net/engine-rest"
)
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public interface WaTaskManagementApi {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    String AUTHORIZATION = "Authorization";

    @PostMapping(value = "/task",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    @ResponseBody
    List<CamundaTask> getTasksByTaskVariables(
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
            @RequestBody Map<String, Object> body
    );

    @PostMapping(
            value = "/task/{task-id}/cancel",
            consumes = APPLICATION_JSON_VALUE
    )
    void cancelTask(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                    @RequestHeader(AUTHORIZATION) String authorisation,
                    @PathVariable("task-id") String id);
}
