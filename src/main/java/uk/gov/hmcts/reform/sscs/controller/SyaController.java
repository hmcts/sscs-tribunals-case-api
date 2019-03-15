package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.status;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.service.SubmitAppealService;

@RestController
@Slf4j
public class SyaController {

    private final SubmitAppealService submitAppealService;

    @Autowired
    SyaController(SubmitAppealService submitAppealService) {
        this.submitAppealService = submitAppealService;
    }

    @ApiOperation(value = "submitAppeal",
            notes = "Creates a case from the SYA details",
            response = String.class, responseContainer = "Appeal details")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Submitted appeal successfully",
            response = String.class)})
    @PostMapping(value = "/appeals", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createAppeals(@RequestBody SyaCaseWrapper syaCaseWrapper) {
        log.info("Appeal with Nino - {} and benefit type {} received", syaCaseWrapper.getAppellant().getNino(),
                syaCaseWrapper.getBenefitType().getCode());
        Long caseId = submitAppealService.submitAppeal(syaCaseWrapper);
        log.info("Case {} with Nino - {} and benefit type - {} processed successfully",
                caseId,
                syaCaseWrapper.getAppellant().getNino(),
                syaCaseWrapper.getBenefitType().getCode());

        return status(201).build();
    }


    @ApiOperation(value = "submitDraftAppeal", notes = "Creates a draft case from the SYA details",
            response = String.class, responseContainer = "Appeal details")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Submitted draft appeal successfully",
            response = String.class)})
    @PostMapping(value = "/drafts", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> createDraftAppeal(@RequestBody SyaCaseWrapper syaCaseWrapper) {
        Preconditions.checkNotNull(syaCaseWrapper);
        Long caseId = submitAppealService.submitDraftAppeal(syaCaseWrapper);
        log.info("Draft case {} processed successfully", caseId);
        return status(HttpStatus.CREATED).body(caseId);
    }
}
