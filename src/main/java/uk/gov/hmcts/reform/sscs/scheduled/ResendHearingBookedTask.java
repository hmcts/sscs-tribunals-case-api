package uk.gov.hmcts.reform.sscs.scheduled;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "job.resendHearingBooked.enabled", havingValue = "true")
public class ResendHearingBookedTask implements Runnable {

    private final IdamService idamService;
    private final UpdateCcdCaseService updateCcdCaseService;
    @Value("${job.resendHearingBooked.references}")
    private final String caseReferences;
    @Getter
    private final List<Long> migratedCases = new ArrayList<>();
    @Getter
    private final List<Long> failedCases = new ArrayList<>();
    private static final String LOG_STRING = """
        \n-----------------------------------------
        Resend Hearing Booked Job Completed
        -----------------------------------------
        Migrated cases: {}
        Failed/Skipped Migrated cases: {}
        -----------------------------------------
        """;

    @Override
    public void run() {
        log.info("Migrate cases scheduled task started");
        Arrays.stream(caseReferences.split(",")).map(Long::valueOf).forEach(ref -> {
            try {
                log.info("Updating case using triggerCaseEventV2 for event: {}, event description: {}, caseId: {}",
                        EventType.RESEND_HEARING_BOOKED.getCcdType(), "Resend hearing booked notification", ref);
                updateCcdCaseService.triggerCaseEventV2(ref, EventType.RESEND_HEARING_BOOKED.getCcdType(),
                        "Resend hearing booked notification", "", idamService.getIdamTokens());
                migratedCases.add(ref);
            } catch (Exception e) {
                log.error("Updating case failed for caseID: {}, event: {}",
                         ref, EventType.RESEND_HEARING_BOOKED.getCcdType());
                failedCases.add(ref);
            }
        });

        log.info(LOG_STRING,  getMigratedCases(), getFailedCases());
    }
}
