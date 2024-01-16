package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.domain.wrapper.AssociateCaseDetails;
import uk.gov.hmcts.reform.sscs.domain.wrapper.OnlineHearing;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.service.CitizenLoginService;

@Slf4j
@RestController
@RequestMapping("/api/citizen")
public class CitizenController {

    private final CitizenLoginService citizenLoginService;
    private final IdamService idamService;

    @Autowired
    public CitizenController(CitizenLoginService citizenLoginService,
                             IdamService idamService) {
        this.citizenLoginService = citizenLoginService;
        this.idamService = idamService;
    }

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Loads cases associated with a citizen",
        description = "Loads the cases that have been associated with a citizen in CCD. "
            + "Gets the user from the token in the Authorization header.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description  = "A list of the hearings associated with a citizen.")
    })
    public ResponseEntity<java.util.List<OnlineHearing>> getOnlineHearings(
        @Parameter(description = "user authorisation header", example = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdW")
        @RequestHeader(AUTHORIZATION) String authorisation) {
        return getOnlineHearingsForTyaNumber(authorisation, "");
    }

    @GetMapping(value = "{tya}", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Loads cases associated with a citizen",
        description = "Loads the cases that have been associated with a citizen in CCD. If a tya parameter is provided "
            + "then the list will be limited to the case with the tya number or be empty if the case has not "
            + "been associated with the user. Gets the user from the token in the Authorization header."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description  = "A list of the hearings associated with a citizen and tya number.")
    })
    public ResponseEntity<java.util.List<OnlineHearing>> getOnlineHearingsForTyaNumber(
        @Parameter(description = "user authorisation header", example = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdW")
        @RequestHeader(AUTHORIZATION) String authorisation,
        @Parameter(description = "tya number for an user and appeal", example = "A123-B123-c123-Dgdg")
        @PathVariable("tya") String tya) {

        List<OnlineHearing> casesForCitizen = citizenLoginService.findCasesForCitizen(getUserTokens(authorisation), tya);

        return ResponseEntity.ok(casesForCitizen);
    }

    @GetMapping(value = "/cases/active", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Loads active cases associated with a citizen",
        description = "Loads the active cases that have been associated with a citizen in CCD. If a tya parameter is provided "
            + "then the list will be limited to the case with the tya number or be empty if the case has not "
            + "been associated with the user. Gets the user from the token in the Authorization header."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "A list of the hearings associated with a citizen and tya number.")
    })
    public ResponseEntity<java.util.List<OnlineHearing>> getActiveOnlineHearings(
        @Parameter(description = "user authorisation header", example = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdW")
        @RequestHeader(AUTHORIZATION) String authorisation) {

        List<OnlineHearing> casesForCitizen = citizenLoginService.findActiveCasesForCitizen(getUserTokens(authorisation));

        return ResponseEntity.ok(casesForCitizen);
    }

    @GetMapping(value = "/cases/dormant", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Loads dormant cases associated with a citizen",
        description = "Loads the dormant cases that have been associated with a citizen in CCD. If a tya parameter is provided "
            + "then the list will be limited to the case with the tya number or be empty if the case has not "
            + "been associated with the user. Gets the user from the token in the Authorization header."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "A list of the hearings associated with a citizen and tya number.")
    })
    public ResponseEntity<java.util.List<OnlineHearing>> getDormantOnlineHearings(
        @Parameter(description = "user authorisation header", example = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdW")
        @RequestHeader(AUTHORIZATION) String authorisation) {

        List<OnlineHearing> casesForCitizen = citizenLoginService.findDormantCasesForCitizen(getUserTokens(authorisation));

        return ResponseEntity.ok(casesForCitizen);
    }

    private IdamTokens getUserTokens(String oauth2Token) {
        UserDetails userDetails = idamService.getUserDetails(oauth2Token);
        return IdamTokens.builder()
            .idamOauth2Token(oauth2Token)
            .serviceAuthorization(idamService.generateServiceAuthorization())
            .userId(userDetails.getId())
            .email(userDetails.getEmail())
            .roles(userDetails.getRoles())
            .build();
    }

    @PostMapping(value = "{tya}", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Associates a case with a citizen",
        description = "Associates a case in CCD with a citizen idam user. Checks the TYA number, email and postcode are"
            + " all match the case before associating the case with the idam user."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The citizen has been associated with the case"),
        @ApiResponse(responseCode = "403", description = "The citizen cannot be associated with the case, "
            + "either the user does not exists or the email/postcode do not match the case the tya number is for."),
    })
    public ResponseEntity<OnlineHearing> associateUserWithCase(
        @Parameter(description = "user authorisation header", example = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdW")
        @RequestHeader(AUTHORIZATION) String authorisation,
        @Parameter(description = "tya number for an user and appeal", example = "A123-B123-c123-Dgdg")
        @PathVariable("tya") String tya,
        @Parameter(description = "email address of the appellant", example = "foo@bar.com")
        @RequestBody() AssociateCaseDetails associateCaseDetails
    ) {
        Optional<OnlineHearing> onlineHearing = citizenLoginService.associateCaseToCitizen(
            getUserTokens(authorisation),
            tya,
            associateCaseDetails.getEmail(),
            associateCaseDetails.getPostcode()
        );

        return onlineHearing
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(FORBIDDEN.value()).build());
    }

    @Operation(summary = "Log time against a case for a citizen", description = "Log time against a case a case for "
        + "a citizen idam user. Checks the email is match the case before logging the mya time with the idam user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "No content, the time has been logged against the case"),
        @ApiResponse(responseCode = "403", description = "The time cannot be logged against the case, "
            + "either the user does not exists or the email do not match the case.")
    })
    @PutMapping(value = "/cases/{caseId}/log", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logUserWithCase(
        @Parameter(description = "user authorisation header", example = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdW")
        @RequestHeader(AUTHORIZATION) String authorisation,
        @Parameter(description = "case id for an user and appeal", example = "12345678")
        @PathVariable("caseId") String caseId
    ) {

        IdamTokens citizenTokens = getUserTokens(authorisation);
        citizenLoginService.findAndUpdateCaseLastLoggedIntoMya(citizenTokens, caseId);

        return ResponseEntity.noContent().build();
    }
}
