package uk.gov.hmcts.reform.sscs.callback;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;


@SpringBootTest
@AutoConfigureMockMvc
public class AdminAppealWithdrawnHandlerIt extends AbstractEventIt {

    @MockitoBean
    private IdamClient idamClient;

    @Override
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

        assertThat(result.getData().getSscsDocument()).hasSize(4);
        assertThat(result.getData().getSscsDocument().getFirst().getValue().getDocumentType()).isEqualTo(WITHDRAWAL_REQUEST.getValue());
        assertThat(result.getData().getDwpState()).isEqualTo(WITHDRAWAL_RECEIVED);
        assertThat(result.getData().getAppealNotePad().getNotesCollection()).hasSize(1);
        assertThat(result.getData().getAppealNotePad().getNotesCollection().getFirst().getValue().getNoteDetail()).isEqualTo("withdrawal note added");
    }


}


