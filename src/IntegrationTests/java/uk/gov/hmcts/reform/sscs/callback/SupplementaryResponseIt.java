package uk.gov.hmcts.reform.sscs.callback;

import static java.lang.Long.parseLong;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_TCW;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.supplementaryresponse.SupplementaryResponseAboutToSubmitHandler;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(JUnitParamsRunner.class)
public class SupplementaryResponseIt extends AbstractEventIt {

    private static final String JURISDICTION = "Benefit";
    public static final String SUP_RESPONSE_DOC_PDF_NAME = "supResponseDoc.pdf";
    public static final String SUP_RESPONSE_DOC_MP3_NAME = "supResponseDoc.mp3";
    public static final String SUP_RESPONSE_OTHER_DOC_PDF_NAME = "other" + SUP_RESPONSE_DOC_PDF_NAME;
    private SscsCaseData sscsCaseData;

    @Before
    public void setup() throws IOException {
        super.setup();
        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1")
                .state(State.WITH_DWP)
                .appeal(Appeal.builder().build()).build();
        setJson();
    }

    @Test
    public void shouldReturnAnErrorWhenNoResponseDocumentAdded() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors().size(), is(1));
        assertThat(result.getErrors().iterator().next(),
                is(SupplementaryResponseAboutToSubmitHandler.SUPPLEMENTARY_RESPONSE_DOCUMENT_CANNOT_BE_EMPTY));
    }

    @Test
    public void shouldReturnScannedDocumentWhenResponseDocumentAdded() throws Exception {

        sscsCaseData.setDwpSupplementaryResponseDoc(getGenericDwpResponseDocument(SUP_RESPONSE_DOC_PDF_NAME));
        setJson();

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getData().getEvidenceHandled(), is(YesNo.NO.getValue()));
        assertThat(result.getData().getDwpState(), is(DwpState.SUPPLEMENTARY_RESPONSE));
        assertThat(result.getData().getScannedDocuments().size(), is(1));
        assertThat(result.getData().getScannedDocuments().get(0).getValue().getFileName(), is(SUP_RESPONSE_DOC_PDF_NAME));
    }

    @Test
    public void shouldReturnScannedDocumentsWhenResponseAndOtherDocumentAdded() throws Exception {

        sscsCaseData.setDwpSupplementaryResponseDoc(getGenericDwpResponseDocument(SUP_RESPONSE_DOC_PDF_NAME));
        sscsCaseData.setDwpOtherDoc(getGenericDwpResponseDocument(SUP_RESPONSE_OTHER_DOC_PDF_NAME));
        setJson();

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getData().getEvidenceHandled(), is(YesNo.NO.getValue()));
        assertThat(result.getData().getDwpState(), is(DwpState.SUPPLEMENTARY_RESPONSE));
        assertThat(result.getData().getScannedDocuments().size(), is(2));
        assertThat(result.getData().getScannedDocuments().stream()
                .anyMatch(sd -> sd.getValue().getFileName().equals(SUP_RESPONSE_DOC_PDF_NAME)), is(true));
        assertThat(result.getData().getScannedDocuments().stream()
                .anyMatch(sd -> sd.getValue().getFileName().equals("other" + SUP_RESPONSE_DOC_PDF_NAME)), is(true));
    }

    @Test
    public void shouldReturnScannedDocumentsAndReviewStateAndReferralReasonWhenResponseAndOtherAvDocumentAddedAndNotReviewedByJudge() throws Exception {

        sscsCaseData.setDwpSupplementaryResponseDoc(getGenericDwpResponseDocument(SUP_RESPONSE_DOC_PDF_NAME));
        sscsCaseData.setDwpOtherDoc(getGenericDwpResponseDocument(SUP_RESPONSE_DOC_MP3_NAME));
        sscsCaseData.setInterlocReviewState(InterlocReviewState.NONE);
        setJson();

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getData().getEvidenceHandled(), is(YesNo.NO.getValue()));
        assertThat(result.getData().getDwpState(), is(DwpState.SUPPLEMENTARY_RESPONSE));
        assertThat(result.getData().getScannedDocuments().size(), is(1));
        assertThat(result.getData().getScannedDocuments().get(0).getValue().getFileName(), is(SUP_RESPONSE_DOC_PDF_NAME));
        assertThat(result.getData().getAudioVideoEvidence().size(), is(1));
        assertThat(result.getData().getAudioVideoEvidence().get(0).getValue().getFileName(), is(SUP_RESPONSE_DOC_MP3_NAME));
        assertThat(result.getData().getInterlocReferralReason(), is(REVIEW_AUDIO_VIDEO_EVIDENCE));
        assertThat(result.getData().getInterlocReviewState(), is(REVIEW_BY_TCW));
    }

    @Test
    public void shouldReturnScannedDocumentsAndReferralReasonWhenResponseAndOtherAvDocumentAddedAndReviewedByJudge() throws Exception {

        sscsCaseData.setDwpSupplementaryResponseDoc(getGenericDwpResponseDocument(SUP_RESPONSE_DOC_PDF_NAME));
        sscsCaseData.setDwpOtherDoc(getGenericDwpResponseDocument(SUP_RESPONSE_DOC_MP3_NAME));
        sscsCaseData.setInterlocReviewState(REVIEW_BY_JUDGE);
        setJson();

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getData().getEvidenceHandled(), is(YesNo.NO.getValue()));
        assertThat(result.getData().getDwpState(), is(DwpState.SUPPLEMENTARY_RESPONSE));
        assertThat(result.getData().getScannedDocuments().size(), is(1));
        assertThat(result.getData().getScannedDocuments().get(0).getValue().getFileName(), is(SUP_RESPONSE_DOC_PDF_NAME));
        assertThat(result.getData().getAudioVideoEvidence().size(), is(1));
        assertThat(result.getData().getAudioVideoEvidence().get(0).getValue().getFileName(), is(SUP_RESPONSE_DOC_MP3_NAME));
        assertThat(result.getData().getInterlocReferralReason(), is(REVIEW_AUDIO_VIDEO_EVIDENCE));
    }

    @NotNull
    private DwpResponseDocument getGenericDwpResponseDocument(String fileName) {
        return DwpResponseDocument.builder()
                .documentFileName(fileName)
                .documentLink(DocumentLink.builder().documentUrl(
                        "documentUrl").documentFilename(fileName).build())
                .build();
    }

    private void setJson() throws JsonProcessingException {
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(parseLong(sscsCaseData.getCcdCaseId()), JURISDICTION,
                sscsCaseData.getState(), sscsCaseData, LocalDateTime.now(), "Benefit");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(),
                EventType.DWP_SUPPLEMENTARY_RESPONSE, false);
        json = mapper.writeValueAsString(callback);
    }

}
