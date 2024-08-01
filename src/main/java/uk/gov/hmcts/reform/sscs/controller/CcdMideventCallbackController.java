package uk.gov.hmcts.reform.sscs.controller;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.ResponseEntity.ok;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADMIN_ACTION_CORRECTION;
import static uk.gov.hmcts.reform.sscs.service.AuthorisationService.SERVICE_AUTHORISATION_HEADER;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.isDateInTheFuture;

import com.opencsv.CSVReader;
import java.io.InputStreamReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase.AdjournCaseCcdService;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase.AdjournCaseMidEventValidationService;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase.AdjournCasePreviewService;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionBenefitTypeHelper;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceBase;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.admin.RestoreCasesService2;
import uk.gov.hmcts.reform.sscs.service.admin.RestoreCasesStatus;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@RestController
@Slf4j
public class CcdMideventCallbackController {

    private final AuthorisationService authorisationService;
    private final SscsCaseCallbackDeserializer deserializer;
    private final DecisionNoticeService decisionNoticeService;
    private final AdjournCasePreviewService adjournCasePreviewService;
    private final AdjournCaseCcdService adjournCaseCcdService;
    private final RestoreCasesService2 restoreCasesService2;

    private final AdjournCaseMidEventValidationService adjournCaseMidEventValidationService;

    @Value("${feature.postHearings.enabled}")
    private boolean isPostHearingsEnabled;
    @Value("${feature.postHearingsB.enabled}")
    private boolean isPostHearingsBEnabled;

    @Autowired
    public CcdMideventCallbackController(AuthorisationService authorisationService,
                                         SscsCaseCallbackDeserializer deserializer,
                                         DecisionNoticeService decisionNoticeService,
                                         AdjournCasePreviewService adjournCasePreviewService,
                                         AdjournCaseCcdService adjournCaseCcdService,
                                         RestoreCasesService2 restoreCasesService2, AdjournCaseMidEventValidationService adjournCaseMidEventValidationService) {
        this.authorisationService = authorisationService;
        this.deserializer = deserializer;
        this.decisionNoticeService = decisionNoticeService;
        this.adjournCasePreviewService = adjournCasePreviewService;
        this.adjournCaseCcdService = adjournCaseCcdService;
        this.restoreCasesService2 = restoreCasesService2;
        this.adjournCaseMidEventValidationService = adjournCaseMidEventValidationService;
    }

    @PostMapping(path = "/ccdMidEventAdjournCasePopulateVenueDropdown", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdMidEventAdjournCasePopulateVenueDropdown(
        @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
        @RequestHeader(AUTHORIZATION) String userAuthorisation,
        @RequestBody String message) {
        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        log.info("About to start ccdMidEventAdjournCasePopulateVenueDropdown callback `{}` received for Case ID `{}`", callback.getEvent(),
            callback.getCaseDetails().getId());

        authorisationService.authorise(serviceAuthHeader);

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        sscsCaseData.getAdjournment().setNextHearingVenueSelected(adjournCaseCcdService.getVenueDynamicListForRpcName(
            sscsCaseData.getRegionalProcessingCenter().getName()));

        return ok(preSubmitCallbackResponse);
    }

    @PostMapping(path = "/ccdMidEventPreviewFinalDecision", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdMidEventPreviewFinalDecision(
        @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
        @RequestHeader(AUTHORIZATION) String userAuthorisation,
        @RequestBody String message) {
        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        EventType event = callback.getEvent();
        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        log.info("About to start ccdMidEventPreviewFinalDecision callback `{}` received for Case ID `{}`", event,
            caseDetails.getId());

        authorisationService.authorise(serviceAuthHeader);
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        String benefitType = WriteFinalDecisionBenefitTypeHelper.getBenefitType(sscsCaseData);

        if (isPostHearingsEnabled
                && ADMIN_ACTION_CORRECTION.equals(event)
                && SscsUtil.isOriginalDecisionNoticeUploaded(sscsCaseData)) {
            log.info("Final decision generated by upload, not creating preview document for case ID {}", caseDetails.getId());
            sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(null);

            PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
            return ok(preSubmitCallbackResponse);
        }

        if (isNull(benefitType)) {
            PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
            preSubmitCallbackResponse.addError("Unexpected error - benefit type is null");
            return ok(preSubmitCallbackResponse);
        }

        SscsUtil.setCorrectionInProgress(caseDetails, isPostHearingsEnabled);

        WriteFinalDecisionPreviewDecisionServiceBase writeFinalDecisionPreviewDecisionService = decisionNoticeService.getPreviewService(benefitType);
        DocumentType docType = SscsUtil.getWriteFinalDecisionDocumentType(sscsCaseData, event, isPostHearingsEnabled);

        boolean showIssueDate = ADMIN_ACTION_CORRECTION.equals(event);

        return ok(writeFinalDecisionPreviewDecisionService.preview(callback, docType, userAuthorisation, showIssueDate, isPostHearingsEnabled, isPostHearingsBEnabled));
    }

    @PostMapping(path = "/ccdMidEventPreviewAdjournCase", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdMidEventPreviewAdjournCase(
        @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
        @RequestHeader(AUTHORIZATION) String userAuthorisation,
        @RequestBody String message) {
        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        log.info("About to start ccdMidEventPreviewAdjournCase callback `{}` received for Case ID `{}`", callback.getEvent(),
            callback.getCaseDetails().getId());

        authorisationService.authorise(serviceAuthHeader);

        return ok(adjournCasePreviewService.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, userAuthorisation, false));
    }

    @PostMapping(path = "/ccdMidEventAdjournCaseDirectionDueDate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdMidEventAdjournDirectionDueDate(
            @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody String message) {
        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        log.info("About to start ccdMidEventAdjournDirectionDueDate callback `{}` received for Case ID `{}`", callback.getEvent(),
                callback.getCaseDetails().getId());
        authorisationService.authorise(serviceAuthHeader);
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
        adjournCaseMidEventValidationService.validateSscsCaseDataConstraints(caseData, preSubmitCallbackResponse);
        try {
            adjournCaseMidEventValidationService.checkDirectionsDueDateInvalid(caseData);
            Boolean isDueDateInvalid =  nonNull(caseData.getAdjournment().getDirectionsDueDate())
                    && !isDateInTheFuture(caseData.getAdjournment().getDirectionsDueDate());
            if (isDueDateInvalid) {
                preSubmitCallbackResponse.addError("Directions due date must be in the future");
            }
        } catch (IllegalStateException e) {
            log.error(e.getMessage() + ". Something has gone wrong for caseId: ", caseData.getCcdCaseId());
            preSubmitCallbackResponse.addError(e.getMessage());
        }
        return  ok(preSubmitCallbackResponse);
    }

    @PostMapping(path = "/ccdMidEventAdjournCaseNextHearing", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdMidEventAdjournCaseNextHearing(
            @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody String message) {
        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        log.info("About to start ccdMidEventAdjournCaseNextHearing callback `{}` received for Case ID `{}`", callback.getEvent(),
                callback.getCaseDetails().getId());
        authorisationService.authorise(serviceAuthHeader);
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
        adjournCaseMidEventValidationService.validateSscsCaseDataConstraints(caseData, preSubmitCallbackResponse);
        try {
            if (adjournCaseMidEventValidationService.adjournCaseNextHearingDateOrPeriodIsProvideDate(caseData)
                    && adjournCaseMidEventValidationService.adjournCaseNextHearingDateTypeIsFirstAvailableDateAfter(caseData)
                    && adjournCaseMidEventValidationService.isNextHearingFirstAvailableDateAfterDateInvalid(caseData)) {
                preSubmitCallbackResponse.addError("'First available date after' date cannot be in the past");
            }
        } catch (IllegalStateException e) {
            log.error(e.getMessage() + ". Something has gone wrong for caseId: ", caseData.getCcdCaseId());
            preSubmitCallbackResponse.addError(e.getMessage());
        }
        return ok(preSubmitCallbackResponse);
    }

    @PostMapping(path = "/ccdMidEventAdminRestoreCases", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdMidEventAdminRestoreCases(
        @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
        @RequestHeader(AUTHORIZATION) String userAuthorisation,
        @RequestBody String message) {

        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        log.info("About to start ccdMidEventAdminRestoreCases callback `{}` received for Case ID `{}`", callback.getEvent(),
            callback.getCaseDetails().getId());

        authorisationService.authorise(serviceAuthHeader);

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());

        try {

            String fileName = restoreCasesService2.getRestoreCaseFileName(message);

            ClassPathResource classPathResource = new ClassPathResource("csv/" + fileName);
            CSVReader reader = new CSVReader(new InputStreamReader(classPathResource.getInputStream()));

            RestoreCasesStatus status = restoreCasesService2.restoreCases(reader);

            if (!status.isCompleted()) {
                preSubmitCallbackResponse.addError(status.toString());
            } else {
                preSubmitCallbackResponse.addWarning(status.toString());
                preSubmitCallbackResponse.addWarning("Completed - no more cases");
            }
        } catch (Exception e) {
            preSubmitCallbackResponse.addError(e.getMessage());
        }
        return ok(preSubmitCallbackResponse);
    }
}
