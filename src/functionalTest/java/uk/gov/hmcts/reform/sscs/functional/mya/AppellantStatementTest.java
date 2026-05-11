package uk.gov.hmcts.reform.sscs.functional.mya;

import org.junit.jupiter.api.Test;

public class AppellantStatementTest extends BaseFunctionTest {

    @Test
    public void canUploadAnAppellantStatementForDigital() throws Exception {
        CreatedCcdCase createdCase = createCase();
        Thread.sleep(5000L);
        sscsMyaBackendRequests.uploadAppellantStatement(createdCase.getCaseId(), "statement");
    }
}
