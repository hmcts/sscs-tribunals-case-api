package uk.gov.hmcts.reform.sscs.callback;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.DocumentType.*;

import java.io.IOException;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(JUnitParamsRunner.class)
public class UploadDocumentFurtherEvidenceIt extends AbstractEventIt {

    private SscsCaseData sscsCaseData;

    @Before
    public void setup() throws IOException {
        super.setup();
        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1")
                .state(State.WITH_DWP)
                .interlocReviewState(InterlocReviewState.REVIEW_BY_TCW.getId())
                .appeal(Appeal.builder()
                        .mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build())
                        .build()).build();
        setJson(sscsCaseData, UPLOAD_DOCUMENT_FURTHER_EVIDENCE);
    }


    @Test
    public void midEventGivenOneHearingRecordingRequestDocumentIsUploaded_thenShowHearingsPageAndSetPartiesListForAppellantAndRep() throws Exception {
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative("yes").build());
        SscsFurtherEvidenceDoc furtherEvidenceDoc = SscsFurtherEvidenceDoc.builder().value(SscsFurtherEvidenceDocDetails.builder().documentType(REQUEST_FOR_HEARING_RECORDING.getId()).build()).build();
        sscsCaseData.setDraftSscsFurtherEvidenceDocument(List.of(furtherEvidenceDoc));
        setJson(sscsCaseData, UPLOAD_DOCUMENT_FURTHER_EVIDENCE);

        PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(MID_EVENT);

        assertThat(response.getData().getSscsHearingRecordingCaseData().getShowRequestingPartyPage(), is(YesNo.YES));
        assertEquals(2, response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().size());
        assertEquals("appellant", response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().get(0).getCode());
        assertEquals("representative", response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().get(1).getCode());
    }

    @Test
    @Parameters({"audio.mp3","video.mp4"})
    public void aboutToSubmitHandleHappyPathWhenAudioVideoFileUploaded(String fileName) throws Exception {
        DocumentLink documentLink = DocumentLink.builder().documentFilename(fileName).documentBinaryUrl("http://doc/222").documentUrl("http://doc/skw").build();
        SscsFurtherEvidenceDoc representativeEvidenceDoc = SscsFurtherEvidenceDoc.builder().value(SscsFurtherEvidenceDocDetails.builder().documentFileName(fileName).documentLink(documentLink).documentType(APPELLANT_EVIDENCE.getId()).build()).build();
        sscsCaseData.setDraftSscsFurtherEvidenceDocument(List.of(representativeEvidenceDoc));


        setJson(sscsCaseData, UPLOAD_DOCUMENT_FURTHER_EVIDENCE);

        PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(ABOUT_TO_SUBMIT);


        assertEquals(1, response.getData().getAudioVideoEvidence().size());
        assertEquals(fileName, response.getData().getAudioVideoEvidence().get(0).getValue().getFileName());
        assertEquals(UploadParty.CTSC, response.getData().getAudioVideoEvidence().get(0).getValue().getPartyUploaded());
        assertEquals(InterlocReviewState.REVIEW_BY_TCW.getId(), response.getData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE.getId(), response.getData().getInterlocReferralReason());
        assertNull(response.getData().getDwpState());
        assertNull(response.getData().getDraftSscsFurtherEvidenceDocument());
        assertEquals(YesNo.YES, response.getData().getHasUnprocessedAudioVideoEvidence());
    }


}
