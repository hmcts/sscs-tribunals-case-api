package uk.gov.hmcts.reform.sscs.callback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason.PHE_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_TCW;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.HearingsService;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(JUnitParamsRunner.class)
public class DwpUploadResponseIt extends AbstractEventIt {

    @MockitoBean
    private IdamClient idamClient;

    @MockitoBean
    private UserInfo userInfo;

    @MockitoBean
    private HearingsService hearingsService;

    @Before
    public void setup() throws IOException {
        setup("callback/dwpUploadResponse.json");
    }

    @Test
    public void callToAboutToSubmit_willAddUcCaseCodeIfCaseIsUC_AndAddAudioVideoEvidenceWithoutRip1Doc() throws Exception {
        json = json.replace("BENEFIT_CODE_PLACEHOLDER", "UC");
        json = json.replace("BENEFIT_DESCRIPTION_PLACEHOLDER", "Universal Credit");


        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getData().getIssueCode()).isEqualTo("US");
        assertThat(result.getData().getBenefitCode()).isEqualTo("001");
        assertThat(result.getData().getCaseCode()).isEqualTo("001US");
        assertThat(result.getData().getSscsDocument()).hasSize(2);
        assertThat(result.getData().getSscsDocument().getFirst().getValue().getDocumentType()).isEqualTo("sscs1");
        assertThat(result.getData().getSscsDocument().getLast().getValue().getDocumentType()).isEqualTo("appellantEvidence");
        assertThat(result.getData().getSscsDocument().get(1).getValue().getPartyUploaded()).isNull();
        assertThat(result.getData().getDwpState()).isEqualTo(DwpState.RESPONSE_SUBMITTED_DWP);
        assertThat(result.getData().getAudioVideoEvidence()).hasSize(1);
        assertThat(result.getData().getAudioVideoEvidence().getLast().getValue().getRip1Document()).isNull();
        assertThat(result.getData().getInterlocReviewState()).isEqualTo(REVIEW_BY_TCW);
        assertThat(result.getData().getInterlocReferralReason()).isEqualTo(REVIEW_AUDIO_VIDEO_EVIDENCE);

    }

    @Test
    public void callToAboutToSubmit_willMoveDocumentsToSscsDocumentsCollection_AndAddAudioVideoEvidenceWithoutRip1Doc() throws Exception {
        json = json.replace("BENEFIT_CODE_PLACEHOLDER", "PIP");
        json = json.replace("BENEFIT_DESCRIPTION_PLACEHOLDER", "Personal Independence Payment");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getData().getIssueCode()).isEqualTo("CC");
        assertThat(result.getData().getBenefitCode()).isEqualTo("003");
        assertThat(result.getData().getCaseCode()).isEqualTo("003CC");
        assertThat(result.getData().getSscsDocument()).hasSize(2);
        assertThat(result.getData().getSscsDocument().getFirst().getValue().getDocumentType()).isEqualTo("sscs1");
        assertThat(result.getData().getSscsDocument().getLast().getValue().getDocumentType()).isEqualTo("appellantEvidence");
        assertThat(result.getData().getAudioVideoEvidence()).hasSize(1);
        assertThat(result.getData().getAudioVideoEvidence().getLast().getValue().getRip1Document()).isNull();
        assertThat(result.getData().getInterlocReviewState()).isEqualTo(REVIEW_BY_TCW);
        assertThat(result.getData().getInterlocReferralReason()).isEqualTo(REVIEW_AUDIO_VIDEO_EVIDENCE);
    }

    @Test
    public void callToAboutToSubmit_willSetPheRequestIfPhmeIsSelected_AndAddAudioVideoEvidenceWithRip1Doc() throws Exception {
        setup("callback/dwpUploadResponsePhe.json");

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);
        json = json.replace("BENEFIT_CODE_PLACEHOLDER", "PIP");
        json = json.replace("BENEFIT_DESCRIPTION_PLACEHOLDER", "Personal Independence Payment");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getData().getAudioVideoEvidence()).hasSize(2);
        assertThat(result.getData().getAudioVideoEvidence().get(0).getValue().getRip1Document()).isNotNull();
        assertThat(result.getData().getAudioVideoEvidence().get(1).getValue().getRip1Document()).isNotNull();
        assertThat(result.getData().getInterlocReferralReason()).isEqualTo(PHE_REQUEST);
        assertThat(result.getData().getInterlocReviewState()).isNull();
    }

    @Test
    public void callToAboutToSubmit_willErrorAsNoDwpResponseDocumentUploaded() throws Exception {
        setup("callback/dwpUploadResponseError.json");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors()).hasSize(1);
    }

}
