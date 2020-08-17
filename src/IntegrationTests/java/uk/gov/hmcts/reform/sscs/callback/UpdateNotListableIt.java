package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@SpringBootTest
@AutoConfigureMockMvc
public class UpdateNotListableIt extends AbstractEventIt {

    @Test
    public void callToMidEventCallback_willValidateTheData_WhenDueDateInFuture() throws Exception {
        setup();
        setJsonAndReplace("callback/updateNotListableCallback.json", "DIRECTIONS_DUE_DATE_PLACEHOLDER", LocalDate.now().plus(1, ChronoUnit.DAYS).toString());

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void callToAboutToSubmitHandler_willWriteDueDateToDirectionsDueDate() throws Exception {
        setup();
        String tomorrowDate =  LocalDate.now().plus(1, ChronoUnit.DAYS).toString();
        setJsonAndReplace("callback/updateNotListableCallback.json", "DIRECTIONS_DUE_DATE_PLACEHOLDER", tomorrowDate);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(tomorrowDate, result.getData().getDirectionDueDate());
    }

}
