package uk.gov.hmcts.reform.sscs.scheduled;

import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Component
@Slf4j
@RequiredArgsConstructor
public class ResendHearingBookedTask implements Runnable {

    private final IdamService idamService;
    private final UpdateCcdCaseService updateCcdCaseService;
    private final CcdService ccdService;
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
                SscsCaseDetails retrievedCase = ccdService.getByCaseId(ref, idamService.getIdamTokens());
                if (nonNull(retrievedCase)) {
                    log.info("Updating case using triggerCaseEventV2 for event: {}, event description: {}, caseId: {}",
                            EventType.RESEND_HEARING_BOOKED.getCcdType(), "Resend hearing booked notification", retrievedCase.getId());
                    updateCcdCaseService.triggerCaseEventV2(retrievedCase.getId(), EventType.RESEND_HEARING_BOOKED.getCcdType(),
                            "Resend hearing booked notification", "", idamService.getIdamTokens());
                    log.info("Case updated successfully for caseId: {}, event: {}",
                            retrievedCase.getId(), EventType.RESEND_HEARING_BOOKED.getCcdType());
                    migratedCases.add(retrievedCase.getId());
                }
            } catch (Exception e) {
                log.error("Updating case failed for caseId: {}, due to: {}", ref, e.getMessage());
                failedCases.add(ref);
            }
        });
        log.info(LOG_STRING, getMigratedCases(), getFailedCases());
    }
}
