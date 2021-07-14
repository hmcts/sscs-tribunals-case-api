package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.WITHDRAWAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;


@SpringBootTest
@AutoConfigureMockMvc
public class AdminAppealWithdrawnHandlerIt extends AbstractEventIt {


    @Before
    public void setup() throws IOException {
        setup("callback/adminAppealWithdrawn.json");
    }

    @Test
    public void callToAboutToSubmit_willAddWithdrawalDocumentToSscsDocuments_andSetDwpState() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(4, result.getData().getSscsDocument().size());
        assertEquals("withdrawalRequest", result.getData().getSscsDocument().get(3).getValue().getDocumentType());
        assertEquals(WITHDRAWAL_RECEIVED.getId(), result.getData().getDwpState());
    }


}


