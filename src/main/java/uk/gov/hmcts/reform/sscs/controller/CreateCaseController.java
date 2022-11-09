package uk.gov.hmcts.reform.sscs.controller;

import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static uk.gov.hmcts.reform.sscs.controller.SyaController.logBadRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.exception.CreateCaseException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.SubmitAppealService;

@RestController
@ConditionalOnProperty("create_ccd_endpoint")
@Slf4j
public class CreateCaseController {

    private final SubmitAppealService submitAppealService;
    private final CcdService ccdService;
    private final IdamService idamService;

    public CreateCaseController(
        @Autowired SubmitAppealService submitAppealService,
        @Autowired CcdService ccdService,
        @Autowired IdamService idamService
    ) {
        this.submitAppealService = submitAppealService;
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Operation(summary = "Create a case",
        description = "Creates a case in CCD with an online panel which can the be used for a online hearing. This "
            + "endpoint is just for test and should only be present in test environments. The email address "
            + "used will need to be unique for all other cases in CCD with an online panel if we want to load "
            + "it for the MYA process."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Case has been created")
    })
    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping(value = "/api/case", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> createCase(
        @Parameter(description = "email address of the appellant must be unique in CCD", example = "foo@bar.com", required = true)
        @RequestParam("email")String email,
        @Parameter(description = "mobile number of appellant. Optional if not set will not subscribe for sms.")
        @RequestParam(value = "mobile", required = false) String mobile,
        @RequestParam(value = "hearingType", defaultValue = "oral") String hearingType
    ) throws URISyntaxException {
        SscsCaseDetails caseDetails = ccdService.createCase(
            createSscsCase(email, mobile, hearingType),
            EventType.CREATE_TEST_CASE.getCcdType(),
            "SSCS - create test case event", "Created SSCS",
            idamService.getIdamTokens()
        );

        HashMap<String, String> body = new HashMap<>();
        body.put("id", caseDetails.getId().toString());
        body.put("case_reference", caseDetails.getData().getCaseReference());
        body.put("appellant_tya", caseDetails.getData().getSubscriptions().getAppellantSubscription().getTya());
        if (caseDetails.getData().getSubscriptions().getJointPartySubscription() != null) {
            body.put("joint_party_tya", caseDetails.getData().getSubscriptions().getJointPartySubscription().getTya());
        }
        if (caseDetails.getData().getSubscriptions().getRepresentativeSubscription() != null) {
            body.put("representative_tya", caseDetails.getData().getSubscriptions().getRepresentativeSubscription().getTya());
        }
        return ResponseEntity.created(new URI("case/someId")).body(body);
    }

    private SscsCaseData createSscsCase(String email, String mobile, String hearingType) {
        SscsCaseData sscsCaseData;
        try (InputStream caseStream = getClass().getClassLoader().getResourceAsStream("json/ccd_case.json")) {
            String caseAsString = new BufferedReader(new InputStreamReader(caseStream)).lines().collect(joining("\n"));
            sscsCaseData = new ObjectMapper().readValue(caseAsString, SscsCaseData.class);

            Event events = Event.builder()
                .value(EventDetails.builder()
                    .type("appealCreated")
                    .description("Some Events")
                    .date("2017-05-24T14:01:18.243")
                    .build())
                .build();

            sscsCaseData = sscsCaseData.toBuilder()
                .events(Collections.singletonList(events))
                .caseReference("SC285/17/" + new SecureRandom().nextInt(90000) + 10000)
                .subscriptions(Subscriptions.builder()
                    .appellantSubscription(Subscription.builder()
                        .email(email)
                        .mobile(mobile)
                        .subscribeEmail("yes")
                        .subscribeSms((mobile != null) ? "yes" : "no")
                        .tya(UUID.randomUUID().toString())
                        .build())
                    .jointPartySubscription(Subscription.builder()
                        .email(email)
                        .mobile(mobile)
                        .subscribeEmail("yes")
                        .subscribeSms((mobile != null) ? "yes" : "no")
                        .tya(UUID.randomUUID().toString())
                        .build())
                    .representativeSubscription(Subscription.builder()
                        .email(email)
                        .mobile(mobile)
                        .subscribeEmail("yes")
                        .subscribeSms((mobile != null) ? "yes" : "no")
                        .tya(UUID.randomUUID().toString())
                        .build())
                    .build())
                .build();
            sscsCaseData.getAppeal().setHearingType(hearingType);
        } catch (IOException e) {
            throw new CreateCaseException(e);
        }

        return sscsCaseData;
    }

    @Operation(summary = "submitTestAppeal",
        description = "Creates a case from the SYA details - Used for tests. Changes the mrn date and nino to a random value")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Submitted test appeal successfully", content = {
            @Content(schema = @Schema(implementation = String.class))})
    })
    @PostMapping(value = "/api/appeals", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createAppeals(@RequestHeader(value = AUTHORIZATION, required = false)
                                                        String authorisation, @RequestBody SyaCaseWrapper syaCaseWrapper) {

        if (syaCaseWrapper.getAppellant() == null
            || syaCaseWrapper.getBenefitType() == null
            || syaCaseWrapper.getBenefitType().getCode() == null) {
            logBadRequest(syaCaseWrapper);
        }
        syaCaseWrapper.getAppellant().setNino(getRandomNino());
        syaCaseWrapper.getMrn().setDate(getRandomMrnDate());
        log.info("Appeal with Nino - {} and benefit type {} received", syaCaseWrapper.getAppellant().getNino(),
            syaCaseWrapper.getBenefitType().getCode());
        Long caseId = submitAppealService.submitAppeal(syaCaseWrapper, authorisation);

        log.info("Case {} with benefit type - {} processed successfully",
            caseId,
            syaCaseWrapper.getBenefitType().getCode());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(caseId).toUri();

        log.info(location.toString());
        return created(location).build();
    }

    @SuppressWarnings("squid:S2245")
    public String getRandomNino() {
        return RandomStringUtils.random(9, true, true).toUpperCase();
    }

    public LocalDate getRandomMrnDate() {
        long minDay = LocalDate.now().minusDays(1).toEpochDay();
        long maxDay = LocalDate.now().minusDays(28).toEpochDay();
        @SuppressWarnings("squid:S2245")
        long randomDay = ThreadLocalRandom.current().nextLong(maxDay, minDay);
        return LocalDate.ofEpochDay(randomDay);
    }
}
