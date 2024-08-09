package uk.gov.hmcts.reform.sscs.service;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.sscs.config.FeignClientConfig;
import uk.gov.hmcts.reform.sscs.model.partiesnotified.HmcPartiesNotifiedResponse;

@FeignClient(name = "hmc-hearing", url = "${hmc.url}", configuration = FeignClientConfig.class)
public interface HmcHearingPartiesNotifiedApi {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    String PARTIES_NOTIFIED_ENDPOINT = "/partiesNotified";
    String ID = "id";

    @GetMapping(value = PARTIES_NOTIFIED_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE)
    HmcPartiesNotifiedResponse getPartiesNotifiedRequest(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
            @RequestParam(ID) String id
    );

}
