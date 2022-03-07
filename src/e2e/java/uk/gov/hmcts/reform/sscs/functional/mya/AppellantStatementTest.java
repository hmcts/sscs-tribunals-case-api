package uk.gov.hmcts.reform.sscs.functional.mya;

import java.io.IOException;
import org.junit.Test;

public class AppellantStatementTest extends BaseFunctionTest {

    @Test
    public void canUploadAnAppellantStatementForDigital() throws IOException, InterruptedException {
        CreatedCcdCase createdCase = createCase();
        Thread.sleep(5000L);
        sscsMyaBackendRequests.uploadAppellantStatement(createdCase.getCaseId(), "statement");
    }
}
