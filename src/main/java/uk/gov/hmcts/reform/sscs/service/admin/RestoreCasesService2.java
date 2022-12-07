package uk.gov.hmcts.reform.sscs.service.admin;

import static java.lang.String.format;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.isscottish.IsScottishHandler.isScottishCase;
import static uk.gov.hmcts.reform.sscs.exception.BenefitMappingException.createException;
import static uk.gov.hmcts.reform.sscs.service.CaseCodeService.*;
import static uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService.getFirstHalfOfPostcode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import feign.FeignException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@Slf4j
@Service
public class RestoreCasesService2 {

    private final CcdService ccdService;

    private final IdamService idamService;
    private final ObjectMapper objectMapper;
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final AirLookupService airLookupService;

    @Autowired
    public RestoreCasesService2(CcdService ccdService,
                                IdamService idamService,
                                ObjectMapper objectMapper,
                                RegionalProcessingCenterService regionalProcessingCenterService,
                                AirLookupService airLookupService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.objectMapper = objectMapper;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.airLookupService = airLookupService;
    }

    public String getRestoreCaseFileName(String message) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(message);
        if (jsonNode == null) {
            throw new IllegalStateException("Unable to extract restoreCaseFileName");
        }
        JsonNode caseDetailsNode = getFrom(jsonNode, "case_details");
        JsonNode caseDataNode = getFrom(caseDetailsNode, "case_data");
        TextNode restoreCasesDateNode = (TextNode) getFrom(caseDataNode, "restoreCaseFileName");
        return restoreCasesDateNode.asText();
    }

    private JsonNode getFrom(JsonNode node, String propertyName) {
        JsonNode jsonNode = node.get(propertyName);
        if (jsonNode == null) {
            throw new IllegalStateException("Unable to extract " + propertyName);
        }
        return jsonNode;
    }

    public RestoreCasesStatus restoreCases(CSVReader reader) {
        List<String> caseIdsToRestore = getCaseIdsToRestoreFromCsv(reader);
        IdamTokens idamTokens = idamService.getIdamTokens();

        List<Long> successIds = new ArrayList<>();
        List<Long> failureIds = new ArrayList<>();
        int processedCount = 0;

        for (String caseId : caseIdsToRestore) {
            SscsCaseDetails sscsCaseDetails = findCase(caseId, idamTokens);
            try {
                updateCase(sscsCaseDetails, idamTokens);
                log.info("Succeeded in restoring missing case details for id {}", sscsCaseDetails.getId());
                successIds.add(sscsCaseDetails.getId());
            } catch (Exception e) {
                log.error(format("Failed to restore case data for id %s", caseId), e);
                failureIds.add(Long.valueOf(caseId));
            }
            processedCount++;
        }

        log.info("{} cases have been successful out of {}", successIds.size(), processedCount);

        return new RestoreCasesStatus(processedCount, successIds.size(), failureIds, false);
    }

    private List<String> getCaseIdsToRestoreFromCsv(CSVReader reader) {
        List<String> allCasesToRestore = new ArrayList<>();

        try {
            reader.readNext();

            List<String[]> linesList = reader.readAll();
            linesList.forEach(line ->
                allCasesToRestore.add(line[0])
            );

        } catch (IOException e) {
            log.error("IOException from RestoreCasesService2: ", e);
        } catch (CsvException e) {
            log.error("CsvException from RestoreCasesService2: ", e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                log.error("IOException from RestoreCasesService2 in finally: ", e);
            }
        }
        log.info("RestoreCasesService2: Found " + allCasesToRestore.size() + " cases to restore. Processing now...");
        return allCasesToRestore;
    }

    public SscsCaseDetails findCase(String caseId, IdamTokens idamTokens) {
        return ccdService.getByCaseId(Long.valueOf(caseId), idamTokens);
    }

    public void updateCase(SscsCaseDetails sscsCaseDetails, IdamTokens idamTokens) {
        SscsCaseData caseData = sscsCaseDetails.getData();

        caseData.setCreatedInGapsFrom("readyToList");

        String postCode = caseData.getAppeal().getAppellant().getAddress().getPostcode();
        String firstHalfOfPostcode = getFirstHalfOfPostcode(postCode);
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByPostcode(firstHalfOfPostcode);

        if (rpc != null) {
            caseData.setRegion(rpc.getName());
            caseData.setRegionalProcessingCenter(rpc);
        }

        caseData.setProcessingVenue(airLookupService.lookupAirVenueNameByPostCode(postCode, caseData.getAppeal().getBenefitType()));

        String benefitCode = generateBenefitCode(caseData.getAppeal().getBenefitType().getCode(), "")
                .orElseThrow(() -> createException(caseData.getAppeal().getBenefitType().getCode()));
        String issueCode = generateIssueCode();
        String caseCode = generateCaseCode(benefitCode, issueCode);

        caseData.setBenefitCode(benefitCode);
        caseData.setIssueCode(issueCode);
        caseData.setCaseCode(caseCode);

        if (sscsCaseDetails.getState().equals(State.WITH_DWP.getId())) {
            caseData.setDwpState(DwpState.UNREGISTERED);
        }

        String isScotCase = isScottishCase(caseData.getRegionalProcessingCenter(), caseData);

        if (!isScotCase.equals(caseData.getIsScottishCase())) {
            caseData.setIsScottishCase(isScotCase);
        }

        caseData.getAppeal().setSigner(caseData.getAppeal().getAppellant().getName().getFullNameNoTitle());

        triggerEvent(sscsCaseDetails, idamTokens);
    }

    private void triggerEvent(SscsCaseDetails caseDetails, IdamTokens idamTokens) {
        EventType eventToTrigger = caseDetails.getState().equals(State.WITH_DWP.getId()) ?  EventType.APPEAL_RECEIVED : EventType.UPDATE_CASE_ONLY;

        log.info("About to update case with {} event for id {}", eventToTrigger, caseDetails.getId());
        try {
            ccdService.updateCase(caseDetails.getData(), caseDetails.getId(), eventToTrigger.getCcdType(), "Restore case details", "Automatically restore missing case details", idamTokens);

            log.info("Case updated with {} event for id {}", eventToTrigger, caseDetails.getId());

        } catch (FeignException.UnprocessableEntity e) {
            log.error(format("%s event failed for caseId %s, root cause is %s", eventToTrigger, caseDetails.getId(), getRootCauseMessage(e)), e);
            throw e;
        }
    }
}
