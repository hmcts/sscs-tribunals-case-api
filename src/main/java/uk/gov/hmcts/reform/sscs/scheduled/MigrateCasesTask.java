package uk.gov.hmcts.reform.sscs.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MigrateCasesTask implements Runnable {


    @Override
    public void run() {
        log.info("Migrate cases scheduled task started");
    }
}
