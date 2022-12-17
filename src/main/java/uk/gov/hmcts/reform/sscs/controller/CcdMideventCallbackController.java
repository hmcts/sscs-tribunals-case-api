package uk.gov.hmcts.reform.sscs.controller;

import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.ResponseEntity.ok;
import static uk.gov.hmcts.reform.sscs.service.AuthorisationService.SERVICE_AUTHORISATION_HEADER;

import com.opencsv.CSVReader;
import java.io.InputStreamReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase.AdjournCaseCcdService;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase.AdjournCasePreviewService;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionBenefitTypeHelper;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceBase;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.admin.RestoreCasesService2;
import uk.gov.hmcts.reform.sscs.service.admin.RestoreCasesStatus;

@RestController
@Slf4j
public class CcdMideventCallbackController {

    private final AuthorisationService authorisationService;
    private final SscsCaseCallbackDeserializer deserializer;
    private final DecisionNoticeService decisionNoticeService;
    private final AdjournCasePreviewService adjournCasePreviewService;
    private final AdjournCaseCcdService adjournCaseCcdService;
    private final RestoreCasesService2 restoreCasesService2;

    @Autowired
    public CcdMideventCallbackController(AuthorisationService authorisationService,
                                         SscsCaseCallbackDeserializer deserializer,
                                         DecisionNoticeService decisionNoticeService,
                                         AdjournCasePreviewService adjournCasePreviewService,
                                         AdjournCaseCcdService adjournCaseCcdService,
                                         RestoreCasesService2 restoreCasesService2) {
        this.authorisationService = authorisationService;
        this.deserializer = deserializer;
        this.decisionNoticeService = decisionNoticeService;
        this.adjournCasePreviewService = adjournCasePreviewService;
        this.adjournCaseCcdService = adjournCaseCcdService;
        this.restoreCasesService2 = restoreCasesService2;
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

    @PostMapping(path = "/ccdMidEventAdjournCasePopulatePanelMemberDropdown", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdMidEventAdjournCasePopulatePanelMemberDropdown(
        @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
        @RequestHeader(AUTHORIZATION) String userAuthorisation,
        @RequestBody String message) {
        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        log.info("About to start ccdMidEventAdjournCasePopulatePanelMemberDropdown callback `{}`"
                + "received for Case ID `{}`", callback.getEvent(),
            callback.getCaseDetails().getId());

        authorisationService.authorise(serviceAuthHeader);

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        sscsCaseData.getAdjournment().setDisabilityQualifiedPanelMemberName(adjournCaseCcdService.getPanelMembers());
        sscsCaseData.getAdjournment().setMedicallyQualifiedPanelMemberName(adjournCaseCcdService.getPanelMembers());
        sscsCaseData.getAdjournment().setOtherPanelMemberName(adjournCaseCcdService.getPanelMembers());

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        return ok(preSubmitCallbackResponse);
    }

    @PostMapping(path = "/ccdMidEventPreviewFinalDecision", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdMidEventPreviewFinalDecision(
        @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
        @RequestHeader(AUTHORIZATION) String userAuthorisation,
        @RequestBody String message) {
        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        log.info("About to start ccdMidEventPreviewFinalDecision callback `{}` received for Case ID `{}`", callback.getEvent(),
            callback.getCaseDetails().getId());

        authorisationService.authorise(serviceAuthHeader);

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        String benefitType = WriteFinalDecisionBenefitTypeHelper.getBenefitType(sscsCaseData);

        if (benefitType == null) {
            PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
            preSubmitCallbackResponse.addError("Unexpected error - benefit type is null");
            return ok(preSubmitCallbackResponse);
        }

        WriteFinalDecisionPreviewDecisionServiceBase writeFinalDecisionPreviewDecisionService = decisionNoticeService.getPreviewService(benefitType);

        return ok(writeFinalDecisionPreviewDecisionService.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, userAuthorisation, false));
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
