package uk.gov.hmcts.reform.sscs.callback;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "feature.cm-other-party-confidentiality.enabled=true")
public class ConfidentialityConfirmedIt extends AbstractEventIt {

    @Before
    public void setup() throws IOException {
        setup("callback/confidentialityConfirmedMidEventMissingConfidentiality.json");
    }

    @Test
    public void callToMidEventHandler_returnsErrorWhenConfidentialityMissing() throws Exception {
        PreSubmitCallbackResponse<SscsCaseData> result = assertResponseOkAndGetResult(CallbackType.MID_EVENT);

        assertThat(result.getErrors()).contains("Confidentiality for all parties must be determined to either Yes or No.");
    }
}
