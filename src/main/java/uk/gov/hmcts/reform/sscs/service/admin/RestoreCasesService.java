package uk.gov.hmcts.reform.sscs.service.admin;

import static java.lang.String.format;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import feign.FeignException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
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

    public String getRestoreCasesDate(String message) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(message);
        TextNode textNode = (TextNode)jsonNode.get("case_details").get("case_data")
            .get("restoreCasesDate");
        if (textNode == null) {
            throw new IllegalStateException("Unable to extract restoreCasesDate");
        }
        return textNode.asText();
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

    public List<SscsCaseDetails> getMatchedCases(IdamTokens idamTokens, String date) {
        List<SscsCaseDetails> matchedCases = getMatchedCasesForDate(idamTokens, date);

        // Defensively double-check that the matched cases do match the checkable criteria
        if (!validateCasesMatchStateAndDwpFurtherInfoCriteria(matchedCases)) {
            throw new IllegalStateException("Matched cases do not all match state and further info critera");
        }
        return matchedCases;
    }

    private List<SscsCaseDetails> getMatchedCasesForDate(IdamTokens idamTokens, String date) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("state", REQUIRED_PRE_STATE.getId());
        map.put("case.dwpFurtherInfo", DWP_FURTHER_INFO_REQUIRED_VALUE);
        addDateRangeCriteria(map, date);
        return ccdService.findCaseBy(map, idamTokens);
    }

    private void addDateRangeCriteria(HashMap<String, String> map, String date) {
        map.put("last_state_modified_date", date);
    }
    
    private boolean validateCasesMatchStateAndDwpFurtherInfoCriteria(List<SscsCaseDetails> cases) {
        return cases.stream().allMatch(c -> caseMatchesStateAndFurtherInfoCriteria(c));
    }
    
    private boolean caseMatchesStateAndFurtherInfoCriteria(SscsCaseDetails caseDetails) {
        return (DWP_FURTHER_INFO_REQUIRED_VALUE.equals(caseDetails.getData().getDwpFurtherInfo())
            && REQUIRED_PRE_STATE.getId().equals(caseDetails.getState())
                && REQUIRED_PRE_STATE.equals(caseDetails.getData().getState()));
    }

    private void triggerEvent(SscsCaseDetails caseDetails) {
        log.info("About to update case with readyToList event for id {}", caseDetails.getId());
        try {
            ccdService.updateCase(caseDetails.getData(), caseDetails.getId(), POST_STATE_EVENT_TYPE.getCcdType(), "Ready to list", "Ready to list event triggered", idamService.getIdamTokens());
        } catch (FeignException.UnprocessableEntity e) {
            log.error(format("readyToList event failed for caseId %s, root cause is %s", caseDetails.getId(), getRootCauseMessage(e), e));
            throw e;
        }
    }
}
