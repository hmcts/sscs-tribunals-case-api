package uk.gov.hmcts.reform.sscs.functional.mya;

import java.io.IOException;
import org.junit.Test;

public class AppellantStatementTest extends BaseFunctionTest {

    @Test
    public void canUploadAnAppellantStatementForDigital() throws IOException {
        CreatedCcdCase createdCase = createCase();

        sscsMyaBackendRequests.uploadAppellantStatement(createdCase.getCaseId(), "statement");
    }
}
