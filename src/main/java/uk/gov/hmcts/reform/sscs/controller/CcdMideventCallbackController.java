package uk.gov.hmcts.reform.sscs.controller;

import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.ResponseEntity.ok;
import static uk.gov.hmcts.reform.sscs.service.AuthorisationService.SERVICE_AUTHORISATION_HEADER;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase.AdjournCasePreviewService;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionService;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;

@RestController
@Slf4j
public class CcdMideventCallbackController {

    private final AuthorisationService authorisationService;
    private final SscsCaseCallbackDeserializer deserializer;
    private final WriteFinalDecisionPreviewDecisionService writeFinalDecisionPreviewDecisionService;
    private final AdjournCasePreviewService adjournCasePreviewService;
    private final VenueDataLoader venueDataLoader;

    @Autowired
    public CcdMideventCallbackController(AuthorisationService authorisationService, SscsCaseCallbackDeserializer deserializer,
                                         WriteFinalDecisionPreviewDecisionService writeFinalDecisionPreviewDecisionService,
                                            AdjournCasePreviewService adjournCasePreviewService, VenueDataLoader venueDataLoader) {
        this.authorisationService = authorisationService;
        this.deserializer = deserializer;
        this.writeFinalDecisionPreviewDecisionService = writeFinalDecisionPreviewDecisionService;
        this.adjournCasePreviewService = adjournCasePreviewService;
        this.venueDataLoader = venueDataLoader;
    }

    @PostMapping(path = "/ccdMidEventAdjournCasePopulateVenueDropdown")
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdMidEventAdjournCasePopulateVenueDropdown(
        @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
        @RequestHeader(AUTHORIZATION) String userAuthorisation,
        @RequestBody String message) {
        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        log.info("About to start ccdMidEventPreviewFinalDecision callback `{}` received for Case ID `{}`", callback.getEvent(),
            callback.getCaseDetails().getId());

        authorisationService.authorise(serviceAuthHeader);

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        List<DynamicListItem> itemList = venueDataLoader.getVenueDetailsMap()
            .entrySet().stream()
            .filter(e -> isRegionalProcessingCentreMatch(e.getValue().getRegionalProcessingCentre(),
                sscsCaseData.getRegionalProcessingCenter().getName()))
            .map(e -> new DynamicListItem(e.getKey(),
                getVenueDisplayString(e.getValue(), false))).collect(Collectors.toList());

        List<DynamicListItem> otherRPCitemList = venueDataLoader.getVenueDetailsMap()
            .entrySet().stream()
            .filter(e -> !isRegionalProcessingCentreMatch(e.getValue().getRegionalProcessingCentre(),
                sscsCaseData.getRegionalProcessingCenter().getName()))
            .map(e -> new DynamicListItem(e.getKey(),
                getVenueDisplayString(e.getValue(), true))).collect(Collectors.toList());

        Collections.sort(itemList, (d1, d2) -> d1.getLabel().compareTo(d2.getLabel()));

        Collections.sort(otherRPCitemList, (d1, d2) -> d1.getLabel().compareTo(d2.getLabel()));

        List<DynamicListItem> fullList = new ArrayList<>();
        fullList.addAll(itemList);
        fullList.addAll(otherRPCitemList);

        DynamicList list = new DynamicList(new DynamicListItem("", ""), fullList);

        sscsCaseData.setAdjournCaseNextHearingVenueSelected(list);

        return ok(preSubmitCallbackResponse);
    }

    private String getVenueDisplayString(VenueDetails venueDetails, boolean prefix) {
        return (prefix ? (getRPC(venueDetails) + " - " ) : "") + venueDetails.getVenName() + ", "
            + venueDetails.getVenAddressLine1() + ", "
            + venueDetails.getVenAddressLine2() + ", "
            + venueDetails.getVenAddressTown() + ", "
            + venueDetails.getVenAddressCounty() + ", "
            + venueDetails.getVenAddressPostcode();
    }

    private boolean isRegionalProcessingCentreMatch(String value, String rpc) {
        return value.substring(5).equalsIgnoreCase(rpc);
    }

    private String getRPC(VenueDetails vd) {
        return vd.getRegionalProcessingCentre().substring(5);
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
}
