package uk.gov.hmcts.reform.sscs.service;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.sscs.config.FeignClientConfig;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;

@SuppressWarnings({"PMD.UseObjectForClearerAPI"})
@FeignClient(name = "hmc-hearing", url = "${hmc.url}", configuration = FeignClientConfig.class)
public interface HmcHearingsApi {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    String HEARINGS_ENDPOINT = "/hearings";
    String ID = "id";

    @GetMapping(HEARINGS_ENDPOINT + "/{caseId}")
    HearingsGetResponse getHearingsRequest(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
            @RequestHeader(required = false) String hmctsDeploymentId,
            @PathVariable String caseId,
            @RequestParam(name = "status", required = false) HmcStatus hmcStatus
    );
}
