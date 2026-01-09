package uk.gov.hmcts.reform.sscs.functional.utilities.idam;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.sscs.functional.utilities.idam.model.CreateUserResponse;
import uk.gov.hmcts.reform.sscs.functional.utilities.idam.model.UserRecord;

@FeignClient(
    name = "idam-api",
    url = "${idam.api.url}"
)
public interface IdamClient {

    @PostMapping(
        value = "/testing-support/accounts",
        consumes = APPLICATION_JSON_VALUE
    )
    CreateUserResponse createTestUser(@RequestBody UserRecord userRecord);

    @GetMapping(value = "/testing-support/accounts")
    UserRecord getUserDetails(@RequestParam("email") String email);

}