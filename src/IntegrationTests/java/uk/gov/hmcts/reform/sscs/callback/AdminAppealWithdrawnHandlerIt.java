package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.WITHDRAWAL_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.WITHDRAWAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;


@SpringBootTest
@AutoConfigureMockMvc
public class AdminAppealWithdrawnHandlerIt extends AbstractEventIt {

    @MockBean
    private IdamClient idamClient;

    @Before
    public void setup() throws IOException {
        given(idamClient.getUserInfo(anyString())).willReturn(UserInfo.builder()
                .givenName("Jason").familyName("Hart").build());

        setup("callback/adminAppealWithdrawn.json");
    }

    @Test
    public void callToAboutToSubmit_willAddWithdrawalDocumentToSscsDocuments_andSetDwpState() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(4, result.getData().getSscsDocument().size());
        assertEquals(WITHDRAWAL_REQUEST.getValue(), result.getData().getSscsDocument().get(3).getValue().getDocumentType());
        assertEquals(WITHDRAWAL_RECEIVED, result.getData().getDwpState());
        assertEquals(1, result.getData().getAppealNotePad().getNotesCollection().size());
        assertEquals("withdrawal note added", result.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDetail());
    }


}


