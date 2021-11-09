package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;


@SpringBootTest
@AutoConfigureMockMvc
public class UpdateOtherPartyDataIt extends AbstractEventIt {

    @Before
    public void setup() throws IOException {
        setup("callback/updateOtherPartyCallback.json");
    }

    @Test
    public void callToAboutToSubmit_willUpdateProcessingVenueWhenChanged() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(2, result.getData().getOtherParties().size());
        assertEquals("1", result.getData().getOtherParties().get(0).getValue().getId());
        assertEquals("2", result.getData().getOtherParties().get(1).getValue().getId());
    }

}


