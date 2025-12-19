package uk.gov.hmcts.reform.sscs.service;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.gov.hmcts.reform.sscs.domain.CamundaTask;

@FeignClient(
        name = "camunda",
        url = "https://wa-task-management-api-sscs-tribunals-api-pr-4961.preview.platform.hmcts.net"
)
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public interface WaTaskManagementApi {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @PostMapping(value = "/task",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    @ResponseBody
    List<CamundaTask> getTasksByTaskVariables(
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestParam(value = "jurisdiction", defaultValue = "SSCS", required = false) String jurisdiction,
            @RequestParam(value = "caseId", defaultValue = "1234", required = false) String caseId,
            @RequestParam(value = "sortBy", defaultValue = "created", required = false) String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = "desc", required = false) String sortOrder
    );

    @PostMapping(
            value = "/task/{task-id}/cancel",
            consumes = APPLICATION_JSON_VALUE
    )
    void cancelTask(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                    @RequestHeader(AUTHORIZATION) String authorisation,
                    @PathVariable("task-id") String id);
}
