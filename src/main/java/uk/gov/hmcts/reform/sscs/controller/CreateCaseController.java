package uk.gov.hmcts.reform.sscs.controller;

import static java.util.stream.Collectors.joining;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.exception.CreateCaseException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@RestController
@ConditionalOnProperty("create_ccd_endpoint")
public class CreateCaseController {

    private final CcdService ccdService;
    private final IdamService idamService;

    public CreateCaseController(
            @Autowired CcdService ccdService,
            @Autowired IdamService idamService
    ) {
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @ApiOperation(value = "Create a case",
            notes = "Creates a case in CCD with an online panel which can the be used for a online hearing. This "
                    + "endpoint is just for test and should only be present in test environments. The email address "
                    + "used will need to be unique for all other cases in CCD with an online panel if we want to load "
                    + "it for the MYA process."
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Case has been created")
    })
    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping(value = "/api/case", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> createCase(
            @ApiParam(value = "email address of the appellant must be unique in CCD", example = "foo@bar.com", required = true)
            @RequestParam("email")String email,
            @ApiParam(value = "mobile number of appellant. Optional if not set will not subscribe for sms.")
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
                    .subscriptions(
                            Subscriptions.builder()
                                    .appellantSubscription(
                                            Subscription.builder()
                                                    .email(email)
                                                    .mobile(mobile)
                                                    .subscribeEmail("yes")
                                                    .subscribeSms((mobile != null) ? "yes" : "no")
                                                    .tya(UUID.randomUUID().toString())
                                                    .build()
                                    )
                                    .jointPartySubscription(
                                            Subscription.builder()
                                                    .email(email)
                                                    .mobile(mobile)
                                                    .subscribeEmail("yes")
                                                    .subscribeSms((mobile != null) ? "yes" : "no")
                                                    .tya(UUID.randomUUID().toString())
                                                    .build()
                                    )
                                    .representativeSubscription(
                                            Subscription.builder()
                                                    .email(email)
                                                    .mobile(mobile)
                                                    .subscribeEmail("yes")
                                                    .subscribeSms((mobile != null) ? "yes" : "no")
                                                    .tya(UUID.randomUUID().toString())
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();
            sscsCaseData.getAppeal().setHearingType(hearingType);
        } catch (IOException e) {
            throw new CreateCaseException(e);
        }

        return sscsCaseData;
    }
}
