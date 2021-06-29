package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason.PHME_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.io.IOException;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(JUnitParamsRunner.class)
public class DwpUploadResponseIt extends AbstractEventIt {


    @Before
    public void setup() throws IOException {
        setup("callback/dwpUploadResponse.json");
    }

    @Test
    public void callToAboutToSubmit_willAddUcCaseCodeIfCaseIsUC() throws Exception {
        json = json.replace("BENEFIT_CODE_PLACEHOLDER", "UC");
        json = json.replace("BENEFIT_DESCRIPTION_PLACEHOLDER", "Universal Credit");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals("US", result.getData().getIssueCode());
        assertEquals("001", result.getData().getBenefitCode());
        assertEquals("001US", result.getData().getCaseCode());
        assertEquals(DwpState.RESPONSE_SUBMITTED_DWP.getId(), result.getData().getDwpState());
        assertNull(result.getData().getInterlocReviewState());
    }

    @Test
    public void callToAboutToSubmit_willMoveDocumentsToSscsDocumentsCollection() throws Exception {
        json = json.replace("BENEFIT_CODE_PLACEHOLDER", "PIP");
        json = json.replace("BENEFIT_DESCRIPTION_PLACEHOLDER", "Personal Independence Payment");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals("CC", result.getData().getIssueCode());
        assertEquals("003", result.getData().getBenefitCode());
        assertEquals("003CC", result.getData().getCaseCode());
        assertEquals(2, result.getData().getSscsDocument().size());
        assertEquals("sscs1", result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals("appellantEvidence", result.getData().getSscsDocument().get(1).getValue().getDocumentType());
        assertNull(result.getData().getInterlocReviewState());
    }

    @Test
    public void callToAboutToSubmit_willSetPhmeRequestIfPhmeIsSelected() throws Exception {
        setup("callback/dwpUploadResponsePhme.json");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(PHME_REQUEST.getId(), result.getData().getInterlocReferralReason());
        assertEquals(REVIEW_BY_JUDGE.getId(), result.getData().getInterlocReviewState());
    }

}
