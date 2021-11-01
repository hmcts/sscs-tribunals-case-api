package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.DocumentType.REQUEST_FOR_HEARING_RECORDING;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;

@RunWith(JUnitParamsRunner.class)
public class UploadDocumentFurtherEvidenceAboutToSubmitHandlerTest extends BaseHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String UPLOAD_DOCUMENT_FE_CALLBACK_JSON = "uploaddocument/uploadDocumentFECallback.json";
    private static final String UPLOAD_AUDIO_VIDEO_DOCUMENT_FE_CALLBACK_JSON = "uploaddocument/uploadAudioVideoDocumentFECallback.json";

    UploadDocumentFurtherEvidenceAboutToSubmitHandler handler;

    @Before
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        handler = new UploadDocumentFurtherEvidenceAboutToSubmitHandler();

        super.setUp();
    }

    @Test
    public void givenAValidHandleAndEventType_thenReturnTrue()
        throws IOException {
        Callback<SscsCaseData> actualCallback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE, "withDwp",
            "representativeEvidence", "appellantEvidence", UPLOAD_DOCUMENT_FE_CALLBACK_JSON);
        boolean actualResult = handler.canHandle(ABOUT_TO_SUBMIT, actualCallback);

        assertTrue(actualResult);

    }

    @Test
    public void handleHappyPathWhenAllFieldsAreProvided() throws IOException {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,"appealCreated",
            "representativeEvidence", "appellantEvidence", UPLOAD_DOCUMENT_FE_CALLBACK_JSON);

        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(ABOUT_TO_SUBMIT, callback,
            USER_AUTHORISATION);

        assertThatJson(actualCaseData).isEqualTo(getExpectedResponse());
        assertNull(actualCaseData.getData().getDwpState());
        assertNull(actualCaseData.getData().getDraftSscsFurtherEvidenceDocument());
    }

    @Test
    public void handleHappyPathWhenAudioVideoAndPdfFileUploaded() throws IOException {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,"appealCreated",
                DocumentType.OTHER_EVIDENCE.getId(), DocumentType.APPELLANT_EVIDENCE.getId(), UPLOAD_AUDIO_VIDEO_DOCUMENT_FE_CALLBACK_JSON);

        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(ABOUT_TO_SUBMIT, callback,
                USER_AUTHORISATION);

        assertEquals(1, actualCaseData.getData().getScannedDocuments().size());
        assertEquals("reps-some-name.pdf", actualCaseData.getData().getScannedDocuments().get(0).getValue().getFileName());
        assertEquals("other", actualCaseData.getData().getScannedDocuments().get(0).getValue().getType());
        assertEquals(1, actualCaseData.getData().getAudioVideoEvidence().size());
        assertEquals("appellant-some-name.mp3", actualCaseData.getData().getAudioVideoEvidence().get(0).getValue().getFileName());
        assertEquals(UploadParty.CTSC, actualCaseData.getData().getAudioVideoEvidence().get(0).getValue().getPartyUploaded());
        assertEquals(InterlocReviewState.REVIEW_BY_TCW.getId(), actualCaseData.getData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE.getId(), actualCaseData.getData().getInterlocReferralReason());
        assertNull(actualCaseData.getData().getDwpState());
        assertNull(actualCaseData.getData().getDraftSscsFurtherEvidenceDocument());
        assertEquals(YesNo.YES, actualCaseData.getData().getHasUnprocessedAudioVideoEvidence());
    }

    @Test
    public void givenAudioVideoEvidenceWithNoDocumentFileName_thenDefaultToTheNameOfFile() throws IOException {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,"appealCreated",
                DocumentType.OTHER_EVIDENCE.getId(), DocumentType.APPELLANT_EVIDENCE.getId(), UPLOAD_AUDIO_VIDEO_DOCUMENT_FE_CALLBACK_JSON);

        callback.getCaseDetails().getCaseData().getDraftSscsFurtherEvidenceDocument().get(1).getValue().setDocumentFileName(null);

        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(ABOUT_TO_SUBMIT, callback,
                USER_AUTHORISATION);

        assertEquals(1, actualCaseData.getData().getScannedDocuments().size());
        assertEquals("reps-some-name.pdf", actualCaseData.getData().getScannedDocuments().get(0).getValue().getFileName());
        assertEquals("other", actualCaseData.getData().getScannedDocuments().get(0).getValue().getType());
        assertEquals(1, actualCaseData.getData().getAudioVideoEvidence().size());
        assertEquals("file-name-appellant-some-name.mp3", actualCaseData.getData().getAudioVideoEvidence().get(0).getValue().getFileName());
        assertEquals(UploadParty.CTSC, actualCaseData.getData().getAudioVideoEvidence().get(0).getValue().getPartyUploaded());
        assertEquals(InterlocReviewState.REVIEW_BY_TCW.getId(), actualCaseData.getData().getInterlocReviewState());
        assertNull(actualCaseData.getData().getDwpState());
        assertNull(actualCaseData.getData().getDraftSscsFurtherEvidenceDocument());
    }

    @Test
    public void givenUploadDocumentFurtherEvidenceAndInterlocReviewStateAlreadyReviewByJudge_thenLeaveAsReviewByJudge() throws IOException {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,"appealCreated",
                DocumentType.OTHER_EVIDENCE.getId(), DocumentType.APPELLANT_EVIDENCE.getId(), UPLOAD_AUDIO_VIDEO_DOCUMENT_FE_CALLBACK_JSON);

        callback.getCaseDetails().getCaseData().setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE.getId());

        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(ABOUT_TO_SUBMIT, callback,
                USER_AUTHORISATION);

        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), actualCaseData.getData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE.getId(), actualCaseData.getData().getInterlocReferralReason());
    }

    private String getExpectedResponse() throws IOException {
        String expectedResponse = fetchData("uploaddocument/" + "expectedUploadDocumentFECallbackResponse.json");
        return expectedResponse.replace("DOCUMENT_DATE_ADDED_PLACEHOLDER", LocalDate.now().atStartOfDay().toString());
    }

    @Test
    public void givenHearingRecordingRequest_thenMoveToRequestHearingsCollection() throws IOException {

        Callback<SscsCaseData> actualCallback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE, "withDwp",
                REQUEST_FOR_HEARING_RECORDING.getId(), APPELLANT_EVIDENCE.getId(), UPLOAD_DOCUMENT_FE_CALLBACK_JSON);

        actualCallback.getCaseDetails().getCaseData().getSscsHearingRecordingCaseData().setRequestingParty(new DynamicList("appellant"));
        actualCallback.getCaseDetails().getCaseData().getSscsHearingRecordingCaseData().setSscsHearingRecordings(Arrays.asList(SscsHearingRecording.builder()
                .value(SscsHearingRecordingDetails.builder().hearingId("an_id11").build()).build()));

        actualCallback.getCaseDetails().getCaseData().getSscsHearingRecordingCaseData().setRequestableHearingDetails(new DynamicList("an_id11"));

        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(ABOUT_TO_SUBMIT, actualCallback, USER_AUTHORISATION);

        assertEquals(1, actualCaseData.getData().getSscsHearingRecordingCaseData().getRequestedHearings().size());
        assertEquals("reps-some-name.pdf", actualCaseData.getData().getSscsHearingRecordingCaseData().getRequestedHearings().get(0).getValue().getRequestDocument().getDocumentFilename());

        assertEquals(1, actualCaseData.getData().getScannedDocuments().size());
        assertEquals("appellant-some-name.pdf", actualCaseData.getData().getScannedDocuments().get(0).getValue().getFileName());

        HearingRecordingRequest hearingRecordingRequest = actualCaseData.getData().getSscsHearingRecordingCaseData().getRequestedHearings().get(0);
        assertEquals("appellant", hearingRecordingRequest.getValue().getRequestingParty());
        assertEquals(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), hearingRecordingRequest.getValue().getDateRequested());

        assertEquals(YesNo.YES, actualCaseData.getData().getSscsHearingRecordingCaseData().getHearingRecordingRequestOutstanding());
    }

    @Test
    public void givenHearingRecordingRequestAlreadyExistsAndANewHearingRequestIsAdded_thenAddToRequestHearingsCollection() throws IOException {

        Callback<SscsCaseData> actualCallback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE, "withDwp",
                REQUEST_FOR_HEARING_RECORDING.getId(), APPELLANT_EVIDENCE.getId(), UPLOAD_DOCUMENT_FE_CALLBACK_JSON);

        actualCallback.getCaseDetails().getCaseData().getSscsHearingRecordingCaseData().setRequestingParty(new DynamicList("appellant"));
        actualCallback.getCaseDetails().getCaseData().getSscsHearingRecordingCaseData().setSscsHearingRecordings(Arrays.asList(SscsHearingRecording.builder()
                .value(SscsHearingRecordingDetails.builder().hearingId("an_id11").build()).build()));

        List<HearingRecordingRequest> hearingRecordingRequests = new ArrayList<>();
        hearingRecordingRequests.add(HearingRecordingRequest.builder().build());

        actualCallback.getCaseDetails().getCaseData().getSscsHearingRecordingCaseData().setRequestedHearings(hearingRecordingRequests);
        actualCallback.getCaseDetails().getCaseData().getSscsHearingRecordingCaseData().setRequestableHearingDetails(new DynamicList("an_id11"));

        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(ABOUT_TO_SUBMIT, actualCallback, USER_AUTHORISATION);

        assertEquals(2, actualCaseData.getData().getSscsHearingRecordingCaseData().getRequestedHearings().size());
        assertEquals("reps-some-name.pdf", actualCaseData.getData().getSscsHearingRecordingCaseData().getRequestedHearings().get(1).getValue().getRequestDocument().getDocumentFilename());

        assertEquals(1, actualCaseData.getData().getScannedDocuments().size());
        assertEquals("appellant-some-name.pdf", actualCaseData.getData().getScannedDocuments().get(0).getValue().getFileName());

        HearingRecordingRequest hearingRecordingRequest = actualCaseData.getData().getSscsHearingRecordingCaseData().getRequestedHearings().get(1);
        assertEquals("appellant", hearingRecordingRequest.getValue().getRequestingParty());
        assertEquals(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), hearingRecordingRequest.getValue().getDateRequested());
        assertEquals("an_id11", hearingRecordingRequest.getValue().getSscsHearingRecording().getHearingId());

        assertEquals(YesNo.YES, actualCaseData.getData().getSscsHearingRecordingCaseData().getHearingRecordingRequestOutstanding());
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({
        "ABOUT_TO_START,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp",
        "ABOUT_TO_SUBMIT,null,withDwp",
        "null,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp"
    })
    public void handleCornerCaseScenarios(@Nullable CallbackType callbackType, @Nullable EventType eventType,
                                          @Nullable String state)
        throws IOException {
        handler.handle(callbackType, buildTestCallbackGivenData(eventType, state,
            "representativeEvidence", "appellantEvidence",
            UPLOAD_DOCUMENT_FE_CALLBACK_JSON), USER_AUTHORISATION);
    }
}
