package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.status;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.Draft;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
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
    public ResponseEntity<String> createAppeals(@RequestHeader(value = AUTHORIZATION, required = false)
                                                    String authorisation, @RequestBody SyaCaseWrapper syaCaseWrapper) {

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

    @ApiOperation(value = "getDraftAppeals", notes = "Get all draft appeals", response = Draft.class)
    @ApiResponses(value =
            {@ApiResponse(code = 200, message = "Returns all draft appeals data if it exists.", response = SessionDraft.class),
                    @ApiResponse(code = 404, message = "The user does not have any draft appeal."),
                    @ApiResponse(code = 500, message = "Most probably the user is unauthorised.")})
    @GetMapping(value = "/drafts/all", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SessionDraft>> getDraftAppeals(@RequestHeader(AUTHORIZATION) String authorisation) {
        Preconditions.checkNotNull(authorisation);

        List<SessionDraft> draftAppeals = submitAppealService.getDraftAppeals(authorisation);

        if (draftAppeals.isEmpty()) {
            log.info("Did not find any draft appeals for the requested user.");
            return ResponseEntity.noContent().build();
        } else {
            log.info("Found {} draft appeals", draftAppeals.size());
            return ResponseEntity.ok().body(draftAppeals);
        }
    }


    @ApiOperation(value = "getDraftAppeal", notes = "Get a draft appeal", response = Draft.class)
    @ApiResponses(value =
        {@ApiResponse(code = 200, message = "Returns a draft appeal data if it exists.", response = SessionDraft.class),
            @ApiResponse(code = 204, message = "The user does not have any draft appeal."),
            @ApiResponse(code = 500, message = "Most probably the user is unauthorised.")})
    @GetMapping(value = "/drafts", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<SessionDraft> getDraftAppeal(@RequestHeader(AUTHORIZATION) String authorisation) {
        Preconditions.checkNotNull(authorisation);

        Optional<SessionDraft> draftAppeal = submitAppealService.getDraftAppeal(authorisation);
        if (!draftAppeal.isPresent()) {
            log.info("Did not find any draft appeals for the requested user.");
        }
        return draftAppeal.map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @ApiOperation(value = "submitDraftAppeal", notes = "Creates a draft appeal", response = Draft.class)
    @ApiResponses(value =
        {@ApiResponse(code = 201, message = "Submitted draft appeal successfully", response = Draft.class)})
    @PutMapping(value = "/drafts", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Draft> createDraftAppeal(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestBody SyaCaseWrapper syaCaseWrapper) {
        if (!isValid(syaCaseWrapper, authorisation)) {
            log.info("Cannot proceed because the {} data is missing", getMissingDataInfo(syaCaseWrapper, authorisation));
            return ResponseEntity.noContent().build();
        }
        Optional<SaveCaseResult> submitDraftResult = submitAppealService.submitDraftAppeal(authorisation, syaCaseWrapper);
        return submitDraftResult.map(this::returnCreateOrUpdateDraftResponse).orElse(ResponseEntity.noContent().build());
    }

    private ResponseEntity<Draft> returnCreateOrUpdateDraftResponse(SaveCaseResult submitDraftResult) {
        Draft draft = Draft.builder().id(submitDraftResult.getCaseDetailsId()).build();
        log.info("{} {} successfully", draft, submitDraftResult.getSaveCaseOperation().name());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(draft.getId()).toUri();
        if (submitDraftResult.getSaveCaseOperation().equals(SaveCaseOperation.CREATE)) {
            return created(location).build();
        } else {
            return status(HttpStatus.OK).location(location).build();
        }
    }

    private String getMissingDataInfo(SyaCaseWrapper syaCaseWrapper, String authorisation) {
        if (StringUtils.isBlank(authorisation)) {
            return "authorization token";
        }
        if (syaCaseWrapper == null || syaCaseWrapper.getBenefitType() == null
            || StringUtils.isBlank(syaCaseWrapper.getBenefitType().getCode())) {
            return "benefit code";
        }
        return null;
    }

    private boolean isValid(SyaCaseWrapper syaCaseWrapper, String authorisation) {
        return syaCaseWrapper != null && syaCaseWrapper.getBenefitType() != null
            && StringUtils.isNotBlank(syaCaseWrapper.getBenefitType().getCode())
            && StringUtils.isNotBlank(authorisation);
    }

}
