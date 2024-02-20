package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.status;

import com.google.common.base.Preconditions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(summary = "submitAppeal", description = "Creates a case from the SYA details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Submitted appeal successfully", content = {
            @Content(schema = @Schema(implementation = String.class))})})
    @PostMapping(value = "/appeals", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createAppeals(@RequestHeader(value = AUTHORIZATION, required = false)
                                                    String authorisation, @RequestBody SyaCaseWrapper syaCaseWrapper) {

        if (syaCaseWrapper.getAppellant() == null || syaCaseWrapper.getAppellant().getNino() == null
                || syaCaseWrapper.getBenefitType() == null || syaCaseWrapper.getBenefitType().getCode() == null) {
            logBadRequest(syaCaseWrapper);
        }
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

    static void logBadRequest(SyaCaseWrapper syaCaseWrapper) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SYA data for bad request: ");
        if (syaCaseWrapper.getBenefitType() != null) {
            stringBuilder.append(" Benefit code ").append(syaCaseWrapper.getBenefitType().getCode());
            stringBuilder.append(" Benefit description ").append(syaCaseWrapper.getBenefitType().getDescription());
        }
        if (syaCaseWrapper.getAppellant() != null && syaCaseWrapper.getAppellant().getNino() != null) {
            stringBuilder.append(" Nino ").append(syaCaseWrapper.getAppellant().getNino());
        }
        if (syaCaseWrapper.getCcdCaseId() != null) {
            stringBuilder.append(" CCD ID ").append(syaCaseWrapper.getCcdCaseId());
        }
        if (syaCaseWrapper.getAppellant() != null && syaCaseWrapper.getAppellant().getTitle() != null) {
            stringBuilder.append(" Appellant title ").append(syaCaseWrapper.getAppellant().getTitle());
        }
        if (syaCaseWrapper.getAppellant() != null && syaCaseWrapper.getAppellant().getLastName() != null) {
            stringBuilder.append(" Has last name? ").append(syaCaseWrapper.getAppellant().getLastName() != null);
        }
        if (syaCaseWrapper.getReasonsForAppealing() != null && syaCaseWrapper.getReasonsForAppealing().getReasons() != null) {
            stringBuilder.append(" Has entered reasons ").append(syaCaseWrapper.getReasonsForAppealing().getReasons().size());
        }
        log.info(stringBuilder.toString());
    }

    @Operation(summary = "getDraftAppeals", description = "Get all draft appeals")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns all draft appeals data if it exists.", content = {
            @Content(schema = @Schema(implementation = SessionDraft.class))}),
        @ApiResponse(responseCode = "404", description = "The user does not have any draft appeal."),
        @ApiResponse(responseCode = "500", description = "Most probably the user is unauthorised.")
    })
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


    @Operation(summary = "getDraftAppeal", description = "Get a draft appeal")
    @ApiResponses(value =
        {@ApiResponse(responseCode = "200", description = "Returns a draft appeal data if it exists.", content = {
            @Content(schema = @Schema(implementation = SessionDraft.class))
        }),
            @ApiResponse(responseCode = "204", description = "The user does not have any draft appeal."),
            @ApiResponse(responseCode = "500", description = "Most probably the user is unauthorised.")})
    @GetMapping(value = "/drafts", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<SessionDraft> getDraftAppeal(@RequestHeader(AUTHORIZATION) String authorisation) {
        Preconditions.checkNotNull(authorisation);

        Optional<SessionDraft> draftAppeal = submitAppealService.getDraftAppeal(authorisation);
        if (!draftAppeal.isPresent()) {
            log.info("Did not find any draft appeals for the requested user.");
        }
        return draftAppeal.map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "submitDraftAppeal", description = "Creates a draft appeal")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Submitted draft appeal successfully", content = {
            @Content(schema = @Schema(implementation = Draft.class))})})
    @PutMapping(value = "/drafts", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Draft> createDraftAppeal(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestBody SyaCaseWrapper syaCaseWrapper,
        @RequestParam(required = false) String forceCreate) {

        if (!isValidCreate(syaCaseWrapper, authorisation)) {
            log.info("Cannot proceed with create draft because the {} data is missing", getMissingDataInfo(syaCaseWrapper, authorisation));
            return ResponseEntity.noContent().build();
        }

        Boolean forceCreateDraft;
        if (forceCreate != null && forceCreate.equals("true")) {
            forceCreateDraft = true;
        } else {
            forceCreateDraft = false;
        }

        log.info("createDraftAppeal {} {}", forceCreateDraft, syaCaseWrapper.getCcdCaseId());

        Optional<SaveCaseResult> submitDraftResult = submitAppealService.submitDraftAppeal(authorisation, syaCaseWrapper, forceCreateDraft);
        return submitDraftResult.map(this::returnCreateOrOkDraftResponse).orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "updateDraftAppeal", description = "Updates a draft appeal")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Updated draft appeal successfully", content = {
            @Content(schema = @Schema(implementation = Draft.class))})})
    @PostMapping(value = "/drafts", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Draft> updateDraftAppeal(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestBody SyaCaseWrapper syaCaseWrapper) {

        if (!isValidUpdate(syaCaseWrapper, authorisation)) {
            log.info("Cannot proceed with update draft because the {} data is missing", getMissingDataInfo(syaCaseWrapper, authorisation));
            return ResponseEntity.noContent().build();
        }

        Optional<SaveCaseResult> submitDraftResult = submitAppealService.updateDraftAppeal(authorisation, syaCaseWrapper);
        return submitDraftResult.map(this::returnCreateOrOkDraftResponse).orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "archiveDraftAppeal", description = "Archives a draft appeal")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Updated draft appeal successfully", content = {
            @Content(schema = @Schema(implementation = Draft.class))})})
    @DeleteMapping (value = "/drafts/{id}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Draft> archiveDraftAppeal(
            @RequestHeader(AUTHORIZATION) String authorisation,
            @RequestBody SyaCaseWrapper syaCaseWrapper,
            @PathVariable("id") long ccdCaseId) {

        if (!isValidArchive(syaCaseWrapper, authorisation)) {
            log.info("Cannot proceed with archive draft because the {} data is missing", getMissingDataInfo(syaCaseWrapper, authorisation));
            return ResponseEntity.noContent().build();
        }

        Optional<SaveCaseResult> submitDraftResult = submitAppealService.archiveDraftAppeal(authorisation, syaCaseWrapper, ccdCaseId);
        return submitDraftResult.map(this::returnCreateOrOkDraftResponse).orElse(ResponseEntity.noContent().build());
    }

    private ResponseEntity<Draft> returnCreateOrOkDraftResponse(SaveCaseResult submitDraftResult) {

        Draft draft = Draft.builder().id(submitDraftResult.getCaseDetailsId()).build();
        log.info("{} {} successfully draft", draft, submitDraftResult.getSaveCaseOperation().name());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(draft.getId()).toUri();

        if (submitDraftResult.getSaveCaseOperation().equals(SaveCaseOperation.CREATE)) {
            return created(location).body(draft);
        } else {
            return status(HttpStatus.OK).location(location).body(draft);
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

        if (syaCaseWrapper.getCcdCaseId() == null) {
            return "ccdCaseId";
        }
        return null;
    }

    private boolean isValidCreate(SyaCaseWrapper syaCaseWrapper, String authorisation) {
        return syaCaseWrapper != null && syaCaseWrapper.getBenefitType() != null
            && StringUtils.isNotBlank(syaCaseWrapper.getBenefitType().getCode())
            && StringUtils.isNotBlank(authorisation);
    }

    private boolean isValidUpdate(SyaCaseWrapper syaCaseWrapper, String authorisation) {
        return syaCaseWrapper != null && syaCaseWrapper.getBenefitType() != null && syaCaseWrapper.getCcdCaseId() != null
                && StringUtils.isNotBlank(syaCaseWrapper.getBenefitType().getCode())
                && StringUtils.isNotBlank(authorisation);
    }

    private boolean isValidArchive(SyaCaseWrapper syaCaseWrapper, String authorisation) {
        return syaCaseWrapper.getCcdCaseId() != null && StringUtils.isNotBlank(authorisation);
    }
}
