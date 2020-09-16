package uk.gov.hmcts.reform.sscs.service.admin;

import static java.lang.String.format;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_RESPOND;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.RESPONSE_RECEIVED;


import feign.FeignException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    public static final String LAST_STATE_MODIFIED_VALUE_START = "2020-08-28";
    public static final String LAST_STATE_MODIFIED_VALUE_END = "2020-09-10";
    public static final int MAX_CASES_PER_BATCH = 25;

    @Autowired
    public RestoreCasesService(CcdService ccdService,
        IdamService idamService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    private List<String> getDateRange(String start, String end) {
        List<String> dates = new ArrayList<>();
        LocalDate endDate = LocalDate.parse(LAST_STATE_MODIFIED_VALUE_END);

        LocalDate date = LocalDate.parse(LAST_STATE_MODIFIED_VALUE_START);
        while (!date.isAfter(endDate)) {
            dates.add(date.toString());
        }
        return dates;
    }

    public RestoreCasesStatus restoreNextBatchOfCases() {
        List<Long> successIds = new ArrayList<>();
        List<Long> failureIds = new ArrayList<>();
        int processedCount = 0;

        List<SscsCaseDetails> matchedCases = getMatchedCases(idamService.getIdamTokens());

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

    public List<SscsCaseDetails> getMatchedCases(IdamTokens idamTokens) {
        List<SscsCaseDetails> matchedCases = getMatchedCasesForDateRange(idamTokens, LAST_STATE_MODIFIED_VALUE_START, LAST_STATE_MODIFIED_VALUE_END);

        // Defensively double-check that the matched cases do match the checkable criteria
        if (!validateCasesMatchStateAndDwpFurtherInfoCriteria(matchedCases)) {
            throw new IllegalStateException("Matched cases do not all match state and further info critera");
        }
        return matchedCases;
    }

    private List<SscsCaseDetails> getMatchedCasesForDateRange(IdamTokens idamTokens, String startDate, String endDate) {
        List<SscsCaseDetails> matchedCases = new ArrayList<>();
        // We are making 14 queries here - do we need to introduce a delay to rate limit ?
        for (String date : getDateRange(LAST_STATE_MODIFIED_VALUE_START, LAST_STATE_MODIFIED_VALUE_END)) {
            matchedCases.addAll(getMatchedCasesForDate(idamTokens, date));
        }
        // Shuffle the cases and truncate to MAX_CASES_PER_BATCH
        // This is so that if there are any problematic cases that we cannot process
        // we have a good chance of selecting other cases
        Collections.shuffle(matchedCases);
        if (matchedCases.size() > MAX_CASES_PER_BATCH) {
            matchedCases = matchedCases.subList(0, MAX_CASES_PER_BATCH);
        }
        return matchedCases;
    }

    private List<SscsCaseDetails> getMatchedCasesForDate(IdamTokens idamTokens, String date) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("state", RESPONSE_RECEIVED.getId());
        map.put("case.dwpFurtherInfo", "No");
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
        return "No".equals(caseDetails.getData().getDwpFurtherInfo()) 
            && "responseReceived".equals(caseDetails.getState())
                && State.RESPONSE_RECEIVED.equals(caseDetails.getState());
    }

    private void triggerEvent(SscsCaseDetails caseDetails) {
        log.info("About to update case with readyToList event for id {}", caseDetails.getId());
        try {

            ccdService.updateCase(caseDetails.getData(), caseDetails.getId(), READY_TO_LIST.getCcdType(), "Ready to list", "Ready to list event triggered", idamService.getIdamTokens());
        } catch (FeignException.UnprocessableEntity e) {
            e.printStackTrace();
            log.error(format("readyToList event failed for caseId %s, root cause is %s", caseDetails.getId(), getRootCauseMessage(e), e));
            throw e;
        }
    }
}
