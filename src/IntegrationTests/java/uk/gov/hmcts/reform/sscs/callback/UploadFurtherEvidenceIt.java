package uk.gov.hmcts.reform.sscs.callback;

import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(JUnitParamsRunner.class)
public class UploadFurtherEvidenceIt extends AbstractEventIt {

    public static final String JURISDICTION = "Benefit";
    private SscsCaseData sscsCaseData;

    @Before
    public void setup() throws IOException {
        super.setup();
        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1")
                .state(State.READY_TO_LIST)
                .interlocReviewState(InterlocReviewState.REVIEW_BY_TCW.getId())
                .appeal(Appeal.builder().build()).build();
        setJson();
    }


    @Test
    @Parameters({"pdf", "PDF", "mp3", "MP3", "mp4", "MP4"})
    public void shouldMoveOneDraftUploadsToSscsDocumentsOrAudioVideoEvidence(String fileType) throws Exception {
        final List<DraftSscsDocument> draftDocs = getDraftSscsDocuments(format("document.%s", fileType));
        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocs);
        setJson();
        
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors().size(), is(0));
        assertThat(result.getData().getDraftFurtherEvidenceDocuments(), is(nullValue()));
        if (fileType.equalsIgnoreCase("pdf")) {
            assertThat(result.getData().getSscsDocument().size(), is(1));
            assertThat(result.getData().getAudioVideoEvidence(), is(nullValue()));
            assertEquals(YesNo.NO, result.getData().getHasUnprocessedAudioVideoEvidence());
        } else {
            assertThat(result.getData().getSscsDocument(), is(nullValue()));
            assertThat(result.getData().getAudioVideoEvidence().size(), is(1));
            assertThat(result.getData().getAudioVideoEvidence().get(0).getValue().getPartyUploaded(), is(UploadParty.CTSC));
            assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE.getId(), result.getData().getInterlocReferralReason());
            assertEquals(YesNo.YES, result.getData().getHasUnprocessedAudioVideoEvidence());
        }
    }

    private void setJson() throws JsonProcessingException {
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(parseLong(sscsCaseData.getCcdCaseId()), JURISDICTION,
                sscsCaseData.getState(), sscsCaseData, LocalDateTime.now());
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(),
                EventType.UPLOAD_FURTHER_EVIDENCE, false);
        json = mapper.writeValueAsString(callback);
    }


    @NotNull
    private List<DraftSscsDocument> getDraftSscsDocuments(String fileName) {
        final DraftSscsDocument doc = DraftSscsDocument.builder().value(DraftSscsDocumentDetails.builder()
                .documentFileName(fileName)
                .documentType("documentType")
                .documentLink(DocumentLink.builder().documentUrl(
                        "documentUrl").documentFilename(fileName).build())
                .build()).build();
        return singletonList(doc);
    }

}
