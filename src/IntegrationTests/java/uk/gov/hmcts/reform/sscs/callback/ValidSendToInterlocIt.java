package uk.gov.hmcts.reform.sscs.callback;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.io.IOException;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase;

@SpringBootTest
@AutoConfigureMockMvc
public class ValidSendToInterlocIt extends AbstractEventIt {

    @Before
    public void setup() throws IOException {
        setup("callback/validSendToInterloc.json");
    }

    @Test
    public void callToAboutToStart_willPopulateWhoReviewsCase() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        DynamicList expected = new DynamicList(
            new DynamicListItem("", ""),
            Arrays.asList(new DynamicListItem(SelectWhoReviewsCase.REVIEW_BY_TCW.getId(), SelectWhoReviewsCase.REVIEW_BY_TCW.getLabel()),
                new DynamicListItem(SelectWhoReviewsCase.REVIEW_BY_JUDGE.getId(), SelectWhoReviewsCase.REVIEW_BY_JUDGE.getLabel()),
                new DynamicListItem(SelectWhoReviewsCase.POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(), SelectWhoReviewsCase.POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()))
        );
        assertThat(result.getData().getSelectWhoReviewsCase()).isEqualTo(expected);
    }

    @Test
    public void callToAboutToSubmit_willPopulateWhoReviewsCase() throws Exception {
        String json = getJson("callback/validSendToInterlocSelectedWhoToReviewCase.json");
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getData().getSelectWhoReviewsCase()).isNull();
        assertThat(result.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.REVIEW_BY_TCW);
    }

    @Test
    public void callToAboutToSubmitPostponementRequest_willPopulateWhoReviewsCase() throws Exception {
        String json = getJson("callback/validSendToInterlocSelectedWhoToReviewCasePostponementRequest.json");
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getData().getSelectWhoReviewsCase()).isNull();
        assertThat(result.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.REVIEW_BY_TCW);
        assertThat(result.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST);
        assertThat(result.getData().getSscsDocument().getFirst().getValue().getDocumentType()).isEqualTo(DocumentType.POSTPONEMENT_REQUEST.getValue());
    }

}


