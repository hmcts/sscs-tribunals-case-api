package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.status;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.model.Draft;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
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
        log.info("Case {} with benefit type - {} processed successfully",
            caseId,
            syaCaseWrapper.getBenefitType().getCode());

        return status(HttpStatus.CREATED).build();
    }


    @ApiOperation(value = "submitDraftAppeal", notes = "Creates a draft appeal", response = Draft.class)
    @ApiResponses(value =
        {@ApiResponse(code = 201, message = "Submitted draft appeal successfully", response = Draft.class)})
    @PutMapping(value = "/drafts", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Draft> createDraftAppeal(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestBody SyaCaseWrapper syaCaseWrapper) {
        Preconditions.checkNotNull(syaCaseWrapper);
        SaveCaseResult submitDraftResult = submitAppealService.submitDraftAppeal(authorisation, syaCaseWrapper);
        Draft draft = Draft.builder()
            .id(submitDraftResult.getCaseDetailsId())
            .build();
        log.info("{} {} successfully", draft, submitDraftResult.getSaveCaseOperation().name());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(draft.getId()).toUri();
        if (submitDraftResult.getSaveCaseOperation().equals(SaveCaseOperation.CREATE)) {
            return ResponseEntity.created(location).build();
        } else {
            return ResponseEntity.status(HttpStatus.OK).location(location).build();
        }
    }
}
