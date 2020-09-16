package uk.gov.hmcts.reform.sscs.controller;

import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.ResponseEntity.ok;
import static uk.gov.hmcts.reform.sscs.service.AuthorisationService.SERVICE_AUTHORISATION_HEADER;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase.AdjournCaseCcdService;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase.AdjournCasePreviewService;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionService;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.admin.RestoreCasesService;
import uk.gov.hmcts.reform.sscs.service.admin.RestoreCasesStatus;

@RestController
@Slf4j
public class CcdMideventCallbackController {

    private final AuthorisationService authorisationService;
    private final SscsCaseCallbackDeserializer deserializer;
    private final WriteFinalDecisionPreviewDecisionService writeFinalDecisionPreviewDecisionService;
    private final AdjournCasePreviewService adjournCasePreviewService;
    private final AdjournCaseCcdService adjournCaseCcdService;
    private final RestoreCasesService restoreCasesService;

    @Autowired
    public CcdMideventCallbackController(AuthorisationService authorisationService, SscsCaseCallbackDeserializer deserializer,
                                         WriteFinalDecisionPreviewDecisionService writeFinalDecisionPreviewDecisionService,
                                            AdjournCasePreviewService adjournCasePreviewService, AdjournCaseCcdService adjournCaseCcdService,
                                            RestoreCasesService restoreCasesService) {
        this.authorisationService = authorisationService;
        this.deserializer = deserializer;
        this.writeFinalDecisionPreviewDecisionService = writeFinalDecisionPreviewDecisionService;
        this.adjournCasePreviewService = adjournCasePreviewService;
        this.adjournCaseCcdService = adjournCaseCcdService;
        this.restoreCasesService = restoreCasesService;
    }

    @PostMapping(path = "/ccdMidEventAdjournCasePopulateVenueDropdown")
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

        sscsCaseData.setAdjournCaseNextHearingVenueSelected(adjournCaseCcdService.getVenueDynamicListForRpcName(
            sscsCaseData.getRegionalProcessingCenter().getName()));

        return ok(preSubmitCallbackResponse);
    }

    @PostMapping(path = "/ccdMidEventPreviewFinalDecision")
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdMidEventPreviewFinalDecision(
        @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
        @RequestHeader(AUTHORIZATION) String userAuthorisation,
        @RequestBody String message) {
        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        log.info("About to start ccdMidEventPreviewFinalDecision callback `{}` received for Case ID `{}`", callback.getEvent(),
            callback.getCaseDetails().getId());

        authorisationService.authorise(serviceAuthHeader);

        return ok(writeFinalDecisionPreviewDecisionService.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, userAuthorisation, false));
    }

    @PostMapping(path = "/ccdMidEventPreviewAdjournCase")
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

    @PostMapping(path = "/ccdMidEventAdminRestoreCases")
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

            String date = restoreCasesService.getRestoreCasesDate(message);

            RestoreCasesStatus status =
                restoreCasesService.restoreNextBatchOfCases(date);

            if (!status.isCompleted()) {
                if (!status.isOk()) {
                    preSubmitCallbackResponse.addError(status.toString());
                } else {
                    preSubmitCallbackResponse.addWarning(status.toString());
                }
            } else {
                preSubmitCallbackResponse.addWarning("Completed - no more cases");
            }
        } catch (Exception e) {
            preSubmitCallbackResponse.addError(e.getMessage());
        }
        return ok(preSubmitCallbackResponse);
    }
}
