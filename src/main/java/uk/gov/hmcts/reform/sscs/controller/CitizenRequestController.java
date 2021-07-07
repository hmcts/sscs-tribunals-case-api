package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.notFound;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @ApiOperation(value = "getHearingRecording",
            notes = "Returns hearing recordings given the CCD case id",
            response = HearingRecordingResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Hearing Recordings", response = HearingRecordingResponse.class),
            @ApiResponse(code = 404, message = "No online hearing found with online hearing id")})
    @GetMapping(value = "/{identifier}/hearingrecording", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<HearingRecordingResponse> getHearingRecording(@RequestHeader(AUTHORIZATION) String authorisation,
                                                                        @PathVariable("identifier") String identifier) {
        return citizenRequestService.findHearingRecordings(identifier, authorisation)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @ApiOperation(value = "submitHearingRecordingRequest",
            notes = "Request hearing recordings of given hearing ids of the CCD case")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Hearing recordings requested"),
            @ApiResponse(code = 404, message = "No online hearing found with online hearing ids")
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
