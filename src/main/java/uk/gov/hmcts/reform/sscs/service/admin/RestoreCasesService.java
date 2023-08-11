package uk.gov.hmcts.reform.sscs.service.admin;

import static java.lang.String.format;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static uk.gov.hmcts.reform.sscs.ccd.service.SscsQueryBuilder.findCaseByResponseReceivedStateAndNoDwpFurtherInfoAndLastModifiedDateQuery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import feign.FeignException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Slf4j
@Service
public class RestoreCasesService {

    private final CcdService ccdService;

    private final IdamService idamService;
    
    private final ObjectMapper objectMapper;

    private static final String DWP_FURTHER_INFO_REQUIRED_VALUE = "No";
    private static final State REQUIRED_PRE_STATE = State.RESPONSE_RECEIVED;
    private static final EventType POST_STATE_EVENT_TYPE = EventType.READY_TO_LIST;

    @Autowired
    public RestoreCasesService(CcdService ccdService,
        IdamService idamService, ObjectMapper objectMapper) {
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.objectMapper = objectMapper;
    }

    private JsonNode getFrom(JsonNode node, String propertyName) {
        JsonNode jsonNode = node.get(propertyName);
        if (jsonNode == null) {
            throw new IllegalStateException("Unable to extract restoreCasesDate");
        }
        return jsonNode;
    }

    public String getRestoreCasesDate(String message) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(message);
        if (jsonNode == null) {
            throw new IllegalStateException("Unable to extract restoreCasesDate");
        }
        JsonNode caseDetailsNode = getFrom(jsonNode, "case_details");
        JsonNode caseDataNode = getFrom(caseDetailsNode, "case_data");
        TextNode restoreCasesDateNode = (TextNode)getFrom(caseDataNode, "restoreCasesDate");
        return restoreCasesDateNode.asText();
    }

    public RestoreCasesStatus restoreNextBatchOfCases(String date) {
        List<Long> successIds = new ArrayList<>();
        List<Long> failureIds = new ArrayList<>();
        int processedCount = 0;

        List<SscsCaseDetails> matchedCases = getMatchedCases(idamService.getIdamTokens(), date);

        log.info("About to submit " + matchedCases.size() + " cases to the queue to be restored");

        for (SscsCaseDetails caseDetails : matchedCases) {

            try {
                triggerEvent(caseDetails);
                log.info("Succeeded in adding ready to list event to the queue for id {}", caseDetails.getId());
                successIds.add(caseDetails.getId());
            } catch (Exception e) {
                log.error(format("Failed to add ready to list event to the queue for id %s", caseDetails.getId()), e);
                failureIds.add(caseDetails.getId());
            }
            processedCount++;
        }
        log.info("Submitted {} to the queue to be restored", successIds.size());
        return new RestoreCasesStatus(processedCount, successIds.size(), failureIds, matchedCases.isEmpty());
    }

    private List<SscsCaseDetails> getMatchedCases(IdamTokens idamTokens, String date) {
        List<SscsCaseDetails> matchedCases = getMatchedCasesForDate(idamTokens, date);

        // Filter to only digital cases - this is to prevent known problems adding to the queue
        // for non-digital cases that will feedback irrelevant errors
        List<SscsCaseDetails> filteredMatchedCases = matchedCases.stream().filter(this::isDigitalCase).toList();
        if (filteredMatchedCases.size() < matchedCases.size()) {
            Set<Long> matchedIds = matchedCases.stream().map(SscsCaseDetails::getId).collect(Collectors.toSet());
            Set<Long> filteredMatchedIds = filteredMatchedCases.stream().map(SscsCaseDetails::getId).collect(Collectors.toSet());
            List<Long> skippedIds = matchedIds.stream().filter(id -> !filteredMatchedIds.contains(id)).toList();
            log.warn("Some cases were returned by the query which are non-digital - skipping the following ids {}", skippedIds);
        }

        // Defensively double-check that the matched cases do match the checkable criteria
        if (!validateCasesMatchStateAndDwpFurtherInfoCriteria(filteredMatchedCases)) {
            throw new IllegalStateException("Matched cases do not all match state and further info critera");
        }
        return filteredMatchedCases;
    }

    private boolean isDigitalCase(SscsCaseDetails caseDetails) {

        SscsCaseData sscsCaseData = caseDetails.getData();
        if (sscsCaseData != null) {
            return sscsCaseData.getCreatedInGapsFrom() != null
                && StringUtils.equalsIgnoreCase(sscsCaseData.getCreatedInGapsFrom(), State.READY_TO_LIST.getId());
        }
        return false;
    }
    
    private List<SscsCaseDetails> getMatchedCasesForDate(IdamTokens idamTokens, String date) {
        SearchSourceBuilder searchBuilder = findCaseByResponseReceivedStateAndNoDwpFurtherInfoAndLastModifiedDateQuery(date);

        return ccdService.findCaseByQuery(searchBuilder, idamTokens);
    }

    private boolean validateCasesMatchStateAndDwpFurtherInfoCriteria(List<SscsCaseDetails> cases) {
        return cases.stream().allMatch(this::caseMatchesStateAndFurtherInfoCriteria);
    }
    
    private boolean caseMatchesStateAndFurtherInfoCriteria(SscsCaseDetails caseDetails) {
        boolean matchesCriteria = (DWP_FURTHER_INFO_REQUIRED_VALUE.equals(caseDetails.getData().getDwpFurtherInfo())
            && REQUIRED_PRE_STATE.getId().equals(caseDetails.getState()));
        if (!matchesCriteria) {
            log.error(format("Matched case with id %s has state of %s and dwpFurtherInfo of %s which is inconsistent with search",
                caseDetails.getId(), caseDetails.getState(), caseDetails.getData().getDwpFurtherInfo()));
        }
        return matchesCriteria;
    }

    private void triggerEvent(SscsCaseDetails caseDetails) {
        log.info("About to update case with {} event for id {}", POST_STATE_EVENT_TYPE, caseDetails.getId());
        try {
            ccdService.updateCase(caseDetails.getData(), caseDetails.getId(), POST_STATE_EVENT_TYPE.getCcdType(), "Ready to list", "Ready to list event triggered", idamService.getIdamTokens());
        } catch (FeignException.UnprocessableEntity e) {
            log.error(format("%s event failed for caseId %s, root cause is %s", POST_STATE_EVENT_TYPE, caseDetails.getId(), getRootCauseMessage(e)), e);
            throw e;
        }
    }
}
