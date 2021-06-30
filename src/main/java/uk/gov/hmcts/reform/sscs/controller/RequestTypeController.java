package uk.gov.hmcts.reform.sscs.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.sscs.model.tya.HearingRecordingResponse;
import uk.gov.hmcts.reform.sscs.service.requesttype.RequestTypeService;


import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@RequestMapping("/api/request")
public class RequestTypeController {

    private final RequestTypeService requestTypeService;

    public RequestTypeController(RequestTypeService requestTypeService) {
        this.requestTypeService = requestTypeService;
    }

    @ApiOperation(value = "getHearingRecording",
            notes = "Returns hearing recordings given the CCD case id",
            response = HearingRecordingResponse.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Hearing Recordings",
            response = HearingRecordingResponse.class)})
    @GetMapping(value = "/{identifier}/hearingrecording", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<HearingRecordingResponse> getHearingRecording(@PathVariable("identifier") String identifier) {
        return ok(requestTypeService.findHearingRecordings(identifier));
    }


    @ApiOperation(value = "submitHearingRecordingRequest",
            notes = "Request hearing recordings of given hearing ids of the CCD case")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Hearing recordings requested"),
            @ApiResponse(code = 404, message = "No online hearing found with online hearing ids")
    })
    @PostMapping(value = "/{identifier}/recordingrequest",
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity submitHearingRecordingRequest(@PathVariable("identifier") String identifier,
                                                        @RequestParam("hearingIds") List<String> hearingIds) {
        boolean requested = requestTypeService.requestHearingRecordings(identifier, hearingIds);
        return requested ? ResponseEntity.noContent().build() : notFound().build();
    }
}
