package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.DocumentType.REQUEST_FOR_HEARING_RECORDING;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecordingRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFurtherEvidenceDoc;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFurtherEvidenceDocDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsHearingRecording;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsHearingRecordingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.WorkAllocationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.util.AddedDocumentsUtil;

@RunWith(JUnitParamsRunner.class)
public class UploadDocumentFurtherEvidenceAboutToSubmitHandlerTest extends BaseHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String UPLOAD_DOCUMENT_FE_CALLBACK_JSON = "uploaddocument/uploadDocumentFECallback.json";
    private static final String UPLOAD_AUDIO_VIDEO_DOCUMENT_FE_CALLBACK_JSON = "uploaddocument/uploadAudioVideoDocumentFECallback.json";

    private UploadDocumentFurtherEvidenceAboutToSubmitHandler handler;

    private AddedDocumentsUtil addedDocumentsUtil;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        addedDocumentsUtil = new AddedDocumentsUtil(false);

        MockitoAnnotations.openMocks(this);
        handler = new UploadDocumentFurtherEvidenceAboutToSubmitHandler(addedDocumentsUtil);
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE);
        sscsCaseData = SscsCaseData.builder().state(State.VALID_APPEAL)
            .interlocReviewState(InterlocReviewState.REVIEW_BY_TCW)
            .appeal(Appeal.builder()
                .build())
            .build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

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

        assertThatJson(actualCaseData)
            .whenIgnoringPaths(
                "data.jointPartyId",
                "data.appeal.appellant.appointee.id",
                "data.appeal.appellant.id",
                "data.appeal.rep.id",
                "data.appeal.hearingOptions",
                "data.correction",
                "data.correctionBodyContent",
                "data.bodyContent",
                "data.correctionGenerateNotice",
                "data.generateNotice",
                "data.dateAdded",
                "data.directionNoticeContent",
                "data.libertyToApply",
                "data.libertyToApplyBodyContent",
                "data.libertyToApplyGenerateNotice",
                "data.permissionToAppeal",
                "data.postHearingRequestType",
                "data.postHearingReviewType",
                "data.previewDocument",
                "data.setAside",
                "data.signedBy",
                "data.signedRole",
                "data.statementOfReasons",
                "data.statementOfReasonsBodyContent",
                "data.statementOfReasonsGenerateNotice",
                "data.workBasketHearingDateIssued")
            .isEqualTo(getExpectedResponse());
        assertNull(actualCaseData.getData().getDwpState());
        assertNull(actualCaseData.getData().getDraftSscsFurtherEvidenceDocument());
    }

    @Test
    @Parameters({"audio.mp3","video.mp4"})
    public void handleHappyPathWhenAudioVideoFileUploaded(String fileName) throws IOException {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,
                "withDwp",
                "representativeEvidence", "appellantEvidence",
                UPLOAD_DOCUMENT_FE_CALLBACK_JSON);
        List<SscsFurtherEvidenceDoc> draftDocuments = Collections.singletonList(SscsFurtherEvidenceDoc.builder()
                .value(SscsFurtherEvidenceDocDetails.builder()
                        .documentFileName(fileName)
                        .documentType("representativeEvidence")
                        .documentLink(DocumentLink.builder()
                                .documentUrl("http://dm-store:5005/documents/abe3b75a-7a72-4e68-b136-4349b7d4f655")
                                .documentFilename(fileName).build())
                        .build())
                .build());

        callback.getCaseDetails().getCaseData().setDraftSscsFurtherEvidenceDocument(draftDocuments);

        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(ABOUT_TO_SUBMIT, callback,
                USER_AUTHORISATION);

        assertEquals(1, actualCaseData.getData().getAudioVideoEvidence().size());
        assertEquals(fileName, actualCaseData.getData().getAudioVideoEvidence().get(0).getValue().getFileName());
        assertEquals(UploadParty.CTSC, actualCaseData.getData().getAudioVideoEvidence().get(0).getValue().getPartyUploaded());
        assertEquals("Representative", actualCaseData.getData().getAudioVideoEvidence().get(0).getValue().getOriginalPartySender());
        assertEquals(InterlocReviewState.REVIEW_BY_TCW, actualCaseData.getData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE, actualCaseData.getData().getInterlocReferralReason());
        assertNull(actualCaseData.getData().getDwpState());
        assertNull(actualCaseData.getData().getDraftSscsFurtherEvidenceDocument());
        assertEquals(YesNo.YES, actualCaseData.getData().getHasUnprocessedAudioVideoEvidence());
    }

    @Test
    @Parameters({"audio.mp3","video.mp4"})
    public void errorThrownWhenAudioVideoDocumentHasIncorrectDocumentType(String fileName) throws IOException {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,
                "withDwp",
                "representativeEvidence", "appellantEvidence",
                UPLOAD_DOCUMENT_FE_CALLBACK_JSON);
        List<SscsFurtherEvidenceDoc> draftDocuments = Collections.singletonList(SscsFurtherEvidenceDoc.builder()
                .value(SscsFurtherEvidenceDocDetails.builder()
                        .documentFileName(fileName)
                        .documentType("incorrectType")
                        .documentLink(DocumentLink.builder()
                                .documentUrl("http://dm-store:5005/documents/abe3b75a-7a72-4e68-b136-4349b7d4f655")
                                .documentFilename(fileName).build())
                        .build())
                .build());

        callback.getCaseDetails().getCaseData().setDraftSscsFurtherEvidenceDocument(draftDocuments);

        PreSubmitCallbackResponse<SscsCaseData> actualResponse = handler.handle(ABOUT_TO_SUBMIT, callback,
                USER_AUTHORISATION);

        long numberOfExpectedError = actualResponse.getErrors().stream()
                .filter(error -> error.equalsIgnoreCase("Type not accepted for AV evidence. Select a Type for the party that originally submitted the audio/video evidence"))
                .count();
        assertEquals(1, numberOfExpectedError);
    }

    @Test
    public void givenAMixtureOfAudioVideoAndDocumentEvidence_onlyAudioVideoShouldBeInsertedIntoAddedDocuments()
        throws JsonProcessingException {
        handler = new UploadDocumentFurtherEvidenceAboutToSubmitHandler(new AddedDocumentsUtil(true));

        List<SscsFurtherEvidenceDoc> furtherEvidenceDocs = new ArrayList<>();
        furtherEvidenceDocs.add(SscsFurtherEvidenceDoc.builder()
            .value(SscsFurtherEvidenceDocDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("testurl/video")
                    .documentFilename("test.mp4").build())
                .documentType("representativeEvidence")
                .documentFileName("test.mp4")
                .build())
            .build());

        furtherEvidenceDocs.add(SscsFurtherEvidenceDoc.builder()
            .value(SscsFurtherEvidenceDocDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("testurl/audio")
                    .documentFilename("test1.mp3").build())
                .documentType("finalDecisionNotice")
                .documentFileName("test1.mp3")
                .build())
            .build());

        furtherEvidenceDocs.add(SscsFurtherEvidenceDoc.builder()
            .value(SscsFurtherEvidenceDocDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("testurl/doc")
                    .documentFilename("test4.pdf").build())
                .documentType("adjournmentNotice")
                .documentFileName("test4.pdf")
                .build())
            .build());

        sscsCaseData.setDraftSscsFurtherEvidenceDocument(furtherEvidenceDocs);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Map<String, Integer> addedDocuments = new ObjectMapper().readerFor(Map.class)
            .readValue(response.getData().getWorkAllocationFields().getAddedDocuments());

        org.assertj.core.api.Assertions.assertThat(addedDocuments)
            .as("Only audio video evidence should be added into added documents for this event type.")
            .containsOnly(org.assertj.core.api.Assertions.entry("audioDocument", 1),
                org.assertj.core.api.Assertions.entry("videoDocument", 1));
    }

    @Test
    public void givenAudioVideoAndDocumentEvidenceWithoutDocumentFileName_shouldResolveFromDocumentLink()
        throws JsonProcessingException {
        handler = new UploadDocumentFurtherEvidenceAboutToSubmitHandler(new AddedDocumentsUtil(true));

        List<SscsFurtherEvidenceDoc> furtherEvidenceDocs = new ArrayList<>();
        furtherEvidenceDocs.add(SscsFurtherEvidenceDoc.builder()
            .value(SscsFurtherEvidenceDocDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("testurl/video")
                    .documentFilename("test.mp4").build())
                .documentType("representativeEvidence")
                .build())
            .build());

        sscsCaseData.setDraftSscsFurtherEvidenceDocument(furtherEvidenceDocs);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Map<String, Integer> addedDocuments = new ObjectMapper().readerFor(Map.class)
            .readValue(response.getData().getWorkAllocationFields().getAddedDocuments());

        org.assertj.core.api.Assertions.assertThat(addedDocuments)
            .as("Without a given document file name the file type should be resolveable from the document link.")
            .containsOnly(org.assertj.core.api.Assertions.entry("videoDocument", 1));
    }

    @Test
    public void givenAudioVideoEvidenceHandledMultipleTimes_shouldInsertMostRecentIntoAddedDocuments()
        throws JsonProcessingException {
        handler = new UploadDocumentFurtherEvidenceAboutToSubmitHandler(new AddedDocumentsUtil(true));

        List<SscsFurtherEvidenceDoc> furtherEvidenceDocs = new ArrayList<>();
        furtherEvidenceDocs.add(SscsFurtherEvidenceDoc.builder()
            .value(SscsFurtherEvidenceDocDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("testurl/audio")
                    .documentFilename("test.mp3").build())
                .documentType("representativeEvidence")
                .documentFileName("test.mp3")
                .build())
            .build());

        sscsCaseData.setDraftSscsFurtherEvidenceDocument(furtherEvidenceDocs);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        furtherEvidenceDocs = new ArrayList<>();
        furtherEvidenceDocs.add(SscsFurtherEvidenceDoc.builder()
            .value(SscsFurtherEvidenceDocDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("testurl/video")
                    .documentFilename("test1.mp4").build())
                .documentType("finalDecisionNotice")
                .documentFileName("test1.mp4")
                .build())
            .build());


        sscsCaseData.setDraftSscsFurtherEvidenceDocument(furtherEvidenceDocs);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Map<String, Integer> addedDocuments = new ObjectMapper().readerFor(Map.class)
            .readValue(response.getData().getWorkAllocationFields().getAddedDocuments());

        org.assertj.core.api.Assertions.assertThat(addedDocuments)
            .as("Added documents should only contain evidence added in the most recent event.")
            .containsOnly(org.assertj.core.api.Assertions.entry("videoDocument", 1));
    }

    @Test
    public void givenNoAudioVideoEvidenceAdded_shouldStillClearAddedDocuments() {
        handler = new UploadDocumentFurtherEvidenceAboutToSubmitHandler(new AddedDocumentsUtil(true));

        sscsCaseData.setDraftSscsFurtherEvidenceDocument(new ArrayList<>());
        sscsCaseData.setWorkAllocationFields(WorkAllocationFields.builder()
            .addedDocuments("{audioEvidence=1}")
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        org.assertj.core.api.Assertions.assertThat(response.getData().getWorkAllocationFields().getAddedDocuments())
            .as("Added documents should be cleared regardless of whether audio video evidence has been added.")
            .isNull();
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
        assertEquals(InterlocReviewState.REVIEW_BY_TCW, actualCaseData.getData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE, actualCaseData.getData().getInterlocReferralReason());
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
        assertEquals(InterlocReviewState.REVIEW_BY_TCW, actualCaseData.getData().getInterlocReviewState());
        assertNull(actualCaseData.getData().getDwpState());
        assertNull(actualCaseData.getData().getDraftSscsFurtherEvidenceDocument());
    }

    @Test
    public void givenUploadDocumentFurtherEvidenceAndInterlocReviewStateAlreadyReviewByJudge_thenLeaveAsReviewByJudge() throws IOException {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,"appealCreated",
                DocumentType.OTHER_EVIDENCE.getId(), DocumentType.APPELLANT_EVIDENCE.getId(), UPLOAD_AUDIO_VIDEO_DOCUMENT_FE_CALLBACK_JSON);

        callback.getCaseDetails().getCaseData().setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);

        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(ABOUT_TO_SUBMIT, callback,
                USER_AUTHORISATION);

        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, actualCaseData.getData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE, actualCaseData.getData().getInterlocReferralReason());
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

    @Test
    public void givenHearingRecordingRequestAndEmptySscsHearingRecordingsList_thenThrowError() throws IOException {

        Callback<SscsCaseData> actualCallback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE, "withDwp",
                REQUEST_FOR_HEARING_RECORDING.getId(), APPELLANT_EVIDENCE.getId(), UPLOAD_DOCUMENT_FE_CALLBACK_JSON);

        actualCallback.getCaseDetails().getCaseData().getSscsHearingRecordingCaseData().setRequestingParty(new DynamicList("appellant"));
        actualCallback.getCaseDetails().getCaseData().getSscsHearingRecordingCaseData().setSscsHearingRecordings(Collections.emptyList());

        actualCallback.getCaseDetails().getCaseData().getSscsHearingRecordingCaseData().setRequestableHearingDetails(new DynamicList("an_id11"));

        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(ABOUT_TO_SUBMIT, actualCallback, USER_AUTHORISATION);

        assertEquals(1, actualCaseData.getErrors().size());
        assertEquals("Hearing record not found", actualCaseData.getErrors().toArray()[0]);
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
