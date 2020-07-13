package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@SpringBootTest
@AutoConfigureMockMvc
public class AdjournCaseIt extends AbstractEventIt {

    @Test
    public void callToMidEventCallback_whenPathIsYesNoYes_willValidateTheData_WhenDueDateInPast() throws Exception {
        setup();
        setJsonAndReplace("callback/adjournCaseYesNoYes.json", "DIRECTIONS_DUE_DATE_PLACEHOLDER", "2019-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("Directions due date must be in the future", result.getErrors().toArray()[0]);
    }

    @Test
    public void callToMidEventCallback_whenPathIsYesNoYes_willValidateTheData_WhenDueDateInFuture() throws Exception {
        setup();
        setJsonAndReplace("callback/adjournCaseYesNoYes.json", "DIRECTIONS_DUE_DATE_PLACEHOLDER", LocalDate.now().plus(1, ChronoUnit.DAYS).toString());

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void callToMidEventCallback_whenPathIsYesNoNo_willValidateTheNextHearingDate_WhenDateInFuture() throws Exception {
        setup();
        setJsonAndReplace("callback/adjournCaseYesNoNo.json", "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER", LocalDate.now().plus(1, ChronoUnit.DAYS).toString());

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void callToMidEventCallback_whenPathIsYesNoNo_willValidateTheNextHearingDate_WhenDateInPast() throws Exception {
        setup();
        setJsonAndReplace("callback/adjournCaseYesNoNo.json", "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER", LocalDate.now().plus(-1, ChronoUnit.DAYS).toString());

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("Specified date cannot be in the past", result.getErrors().toArray()[0]);
    }

}
