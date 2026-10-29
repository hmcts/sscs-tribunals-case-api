package uk.gov.hmcts.reform.sscs.scheduled;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Component
@Slf4j
@RequiredArgsConstructor
public class MigrateCasesTask implements Runnable {

    private final IdamService idamService;
    private final UpdateCcdCaseService updateCcdCaseService;
    @Value("${job.resendHearingBookedReferences}")
    private final String caseReferences;


    @Override
    public void run() {
        log.info("Migrate cases scheduled task started");
        Arrays.stream(caseReferences.split(",")).map(Long::valueOf).forEach(ref -> {
            updateCcdCaseService.triggerCaseEventV2(ref, EventType.RESEND_HEARING_BOOKED.getCcdType(),
                    "Resend hearing booked notification", "", idamService.getIdamTokens());
            log.info("Updating case using triggerCaseEventV2 for event: {}, event description: {}, caseId: {}",
                    EventType.RESEND_HEARING_BOOKED.getCcdType(), "Resend hearing booked notification", ref);
        });

    }
}
