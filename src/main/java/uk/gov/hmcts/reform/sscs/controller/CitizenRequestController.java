package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.notFound;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.model.tya.HearingRecordingResponse;
import uk.gov.hmcts.reform.sscs.service.citizenrequest.CitizenRequestService;

@Slf4j
@RestController
@RequestMapping("/api/request")
public class CitizenRequestController {

    private final CitizenRequestService citizenRequestService;

    public CitizenRequestController(CitizenRequestService citizenRequestService) {
        this.citizenRequestService = citizenRequestService;
    }

    @Operation(summary = "getHearingRecording", description = "Returns hearing recordings given the CCD case id")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Hearing Recordings", content = {
            @Content(schema = @Schema(implementation = HearingRecordingResponse.class))}),
        @ApiResponse(responseCode = "404", description = "No online hearing found with online hearing id")
    })
    @GetMapping(value = "/{identifier}/hearingrecording", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<HearingRecordingResponse> getHearingRecording(@RequestHeader(AUTHORIZATION) String authorisation,
                                                                        @PathVariable("identifier") String identifier) {
        return citizenRequestService.findHearingRecordings(identifier, authorisation)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @Operation(summary = "submitHearingRecordingRequest",
        description = "Request hearing recordings of given hearing ids of the CCD case")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Hearing recordings requested"),
        @ApiResponse(responseCode = "404", description = "No online hearing found with online hearing ids")
    })
    @PostMapping(value = "/{identifier}/recordingrequest",
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity submitHearingRecordingRequest(@RequestHeader(AUTHORIZATION) String authorisation,
                                                        @PathVariable("identifier") String identifier,
                                                        @RequestParam("hearingIds") List<String> hearingIds) {
        boolean requested = citizenRequestService.requestHearingRecordings(identifier, hearingIds, authorisation);
        return requested ? ResponseEntity.noContent().build() : notFound().build();
    }
}
