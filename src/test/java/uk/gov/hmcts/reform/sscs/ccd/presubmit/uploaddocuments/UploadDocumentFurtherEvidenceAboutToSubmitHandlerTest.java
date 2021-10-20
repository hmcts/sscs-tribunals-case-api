package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.DocumentType.REQUEST_FOR_HEARING_RECORDING;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.FileUploadScenario.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@RunWith(JUnitParamsRunner.class)
public class UploadDocumentFurtherEvidenceAboutToSubmitHandlerTest extends BaseHandlerTest {

    @Mock
    private FooterService footerService;

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String UPLOAD_DOCUMENT_FE_CALLBACK_JSON = "uploaddocument/uploadDocumentFECallback.json";
    private static final String UPLOAD_AUDIO_VIDEO_DOCUMENT_FE_CALLBACK_JSON = "uploaddocument/uploadAudioVideoDocumentFECallback.json";

    UploadDocumentFurtherEvidenceAboutToSubmitHandler handler;

    @Before
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        handler = new UploadDocumentFurtherEvidenceAboutToSubmitHandler(true, footerService);

        when(footerService.isReadablePdf(any())).thenReturn(PdfState.OK);

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
    @Parameters({"audio.mp3","video.mp4"})
    public void handleHappyPathWhenAudioVideoFileUploaded(String fileName) throws IOException {
        UploadDocumentFurtherEvidenceAboutToSubmitHandler handler = new UploadDocumentFurtherEvidenceAboutToSubmitHandler(false, footerService);
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
        assertEquals(InterlocReviewState.REVIEW_BY_TCW.getId(), actualCaseData.getData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE.getId(), actualCaseData.getData().getInterlocReferralReason());
        assertNull(actualCaseData.getData().getDwpState());
        assertNull(actualCaseData.getData().getDraftSscsFurtherEvidenceDocument());
        assertEquals(YesNo.YES, actualCaseData.getData().getHasUnprocessedAudioVideoEvidence());
    }

    @Test
    @Parameters({"audio.mp3","video.mp4"})
    public void errorThrownWhenAudioVideoDocuemtnHasIncorrectDocuemntType(String fileName) throws IOException {
        UploadDocumentFurtherEvidenceAboutToSubmitHandler handler = new UploadDocumentFurtherEvidenceAboutToSubmitHandler(false, footerService);
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
                .filter(error -> error.equalsIgnoreCase("Select the party that originally submitted the audio/video evidence"))
                .count();
        assertEquals(1, numberOfExpectedError);
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

    @Test
    @Parameters({
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,representativeEvidence,appellantEvidence,FILE_UPLOAD_IS_EMPTY",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,representativeEvidence,appellantEvidence,FILE_UPLOAD_IS_NULL",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,,appellantEvidence,null",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,,representativeEvidence,null",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,,,null",
        "ABOUT_TO_SUBMIT,UPLOAD_DOCUMENT_FURTHER_EVIDENCE,withDwp,,,FILE_UPLOAD_IS_NULL"
    })
    public void handleErrorScenariosWhenSomeFieldsAreNotProvided(@Nullable CallbackType callbackType,
                                                                 @Nullable EventType eventType, String state,
                                                                 @Nullable String documentType,
                                                                 @Nullable String documentType2,
                                                                 @Nullable FileUploadScenario fileUploadScenario)
        throws IOException {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(eventType,state, documentType, documentType2,
            UPLOAD_DOCUMENT_FE_CALLBACK_JSON);

        if (FILE_UPLOAD_IS_EMPTY.equals(fileUploadScenario)) {
            callback.getCaseDetails().getCaseData().setDraftSscsFurtherEvidenceDocument(Collections.emptyList());
        }
        if (FILE_UPLOAD_IS_NULL.equals(fileUploadScenario)) {
            callback.getCaseDetails().getCaseData().setDraftSscsFurtherEvidenceDocument(null);
        }

        PreSubmitCallbackResponse<SscsCaseData> actualResponse = handler.handle(callbackType, callback,
            USER_AUTHORISATION);

        assertThatJson(actualResponse.getData()).isEqualTo(callback.getCaseDetails().getCaseData());
        assertNull(actualResponse.getData().getDwpState());
        assertNull(actualResponse.getData().getDraftSscsFurtherEvidenceDocument());
        long numberOfExpectedError = actualResponse.getErrors().stream()
            .filter(error -> error.equalsIgnoreCase("You need to provide a file and a document type"))
            .count();
        assertEquals(1, numberOfExpectedError);
    }

    @Test
    public void handleDocumentUploadWhereUploadedFileIsNotAPdf() throws IOException {
        UploadDocumentFurtherEvidenceAboutToSubmitHandler handler = new UploadDocumentFurtherEvidenceAboutToSubmitHandler(false, footerService);
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,
                "withDwp",
                "representativeEvidence", "appellantEvidence",
                UPLOAD_DOCUMENT_FE_CALLBACK_JSON);
        List<SscsFurtherEvidenceDoc> draftDocuments = Collections.singletonList(SscsFurtherEvidenceDoc.builder()
                .value(SscsFurtherEvidenceDocDetails.builder()
                        .documentFileName("word.docx")
                        .documentType("representativeEvidence")
                        .documentLink(DocumentLink.builder()
                                .documentUrl("http://dm-store:5005/documents/abe3b75a-7a72-4e68-b136-4349b7d4f655")
                                .documentFilename("word.docx").build())
                        .build())
                .build());
        callback.getCaseDetails().getCaseData().setDraftSscsFurtherEvidenceDocument(draftDocuments);
        PreSubmitCallbackResponse<SscsCaseData> actualResponse = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThatJson(actualResponse.getData()).isEqualTo(callback.getCaseDetails().getCaseData());
        assertNull(actualResponse.getData().getDwpState());
        assertNull(actualResponse.getData().getDraftSscsFurtherEvidenceDocument());
        long numberOfExpectedError = actualResponse.getErrors().stream()
                .filter(error -> error.equalsIgnoreCase("You need to upload PDF documents only"))
                .count();
        assertEquals(1, numberOfExpectedError);
    }

    @Test
    public void handleDocumentUploadWhereUploadedFileIsNotAValid() throws IOException {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,
                "withDwp",
                "representativeEvidence", "appellantEvidence",
                UPLOAD_DOCUMENT_FE_CALLBACK_JSON);
        List<SscsFurtherEvidenceDoc> draftDocuments = Collections.singletonList(SscsFurtherEvidenceDoc.builder()
                .value(SscsFurtherEvidenceDocDetails.builder()
                        .documentFileName("word.docx")
                        .documentType("representativeEvidence")
                        .documentLink(DocumentLink.builder()
                                .documentUrl("http://dm-store:5005/documents/abe3b75a-7a72-4e68-b136-4349b7d4f655")
                                .documentFilename("word.docx").build())
                        .build())
                .build());
        callback.getCaseDetails().getCaseData().setDraftSscsFurtherEvidenceDocument(draftDocuments);
        PreSubmitCallbackResponse<SscsCaseData> actualResponse = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThatJson(actualResponse.getData()).isEqualTo(callback.getCaseDetails().getCaseData());
        assertNull(actualResponse.getData().getDwpState());
        assertNull(actualResponse.getData().getDraftSscsFurtherEvidenceDocument());
        long numberOfExpectedError = actualResponse.getErrors().stream()
                .filter(error -> error.equalsIgnoreCase("You need to upload PDF, MP3 or MP4 file only"))
                .count();
        assertEquals(1, numberOfExpectedError);
    }


    @Test
    public void handleDocumentUploadWhereUploadedFileIsNotAReadablePdf() throws Exception {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,
                "withDwp",
                "representativeEvidence", "appellantEvidence",
                UPLOAD_DOCUMENT_FE_CALLBACK_JSON);

        List<SscsFurtherEvidenceDoc> draftDocuments = Collections.singletonList(SscsFurtherEvidenceDoc.builder()
                .value(SscsFurtherEvidenceDocDetails.builder()
                        .documentFileName("badPdf.pdf")
                        .documentType("representativeEvidence")
                        .documentLink(DocumentLink.builder()
                                .documentUrl("http://dm-store:5005/documents/abe3b75a-7a72-4e68-b136-4349b7d4f655")
                                .documentFilename("bdPdf.pdf").build())
                        .build())
                .build());

        when(footerService.isReadablePdf(any())).thenReturn(PdfState.UNREADABLE);

        callback.getCaseDetails().getCaseData().setDraftSscsFurtherEvidenceDocument(draftDocuments);
        PreSubmitCallbackResponse<SscsCaseData> actualResponse = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThatJson(actualResponse.getData()).isEqualTo(callback.getCaseDetails().getCaseData());
        assertNull(actualResponse.getData().getDwpState());
        assertNull(actualResponse.getData().getDraftSscsFurtherEvidenceDocument());

        long numberOfExpectedError = actualResponse.getErrors().stream()
                .filter(error -> error.equalsIgnoreCase("Your PDF Document is not readable."))
                .count();
        assertEquals(1, numberOfExpectedError);
    }

    @Test
    public void handleDocumentUploadWhereUploadedFileIsPasswordProtectedPdf() throws Exception {
        Callback<SscsCaseData> callback = buildTestCallbackGivenData(UPLOAD_DOCUMENT_FURTHER_EVIDENCE,
                "withDwp",
                "representativeEvidence", "appellantEvidence",
                UPLOAD_DOCUMENT_FE_CALLBACK_JSON);

        List<SscsFurtherEvidenceDoc> draftDocuments = Collections.singletonList(SscsFurtherEvidenceDoc.builder()
                .value(SscsFurtherEvidenceDocDetails.builder()
                        .documentFileName("badPdf.pdf")
                        .documentType("representativeEvidence")
                        .documentLink(DocumentLink.builder()
                                .documentUrl("http://dm-store:5005/documents/abe3b75a-7a72-4e68-b136-4349b7d4f655")
                                .documentFilename("bdPdf.pdf").build())
                        .build())
                .build());

        when(footerService.isReadablePdf(any())).thenReturn(PdfState.PASSWORD_ENCRYPTED);

        callback.getCaseDetails().getCaseData().setDraftSscsFurtherEvidenceDocument(draftDocuments);
        PreSubmitCallbackResponse<SscsCaseData> actualResponse = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThatJson(actualResponse.getData()).isEqualTo(callback.getCaseDetails().getCaseData());
        assertNull(actualResponse.getData().getDwpState());
        assertNull(actualResponse.getData().getDraftSscsFurtherEvidenceDocument());

        long numberOfExpectedError = actualResponse.getErrors().stream()
                .filter(error -> error.equalsIgnoreCase("Your PDF Document cannot be password protected."))
                .count();
        assertEquals(1, numberOfExpectedError);
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

        List<HearingRecordingRequest> hearingRecordingRequests = new ArrayList<>();
        hearingRecordingRequests.add(HearingRecordingRequest.builder().build());

        actualCallback.getCaseDetails().getCaseData().getSscsHearingRecordingCaseData().setRequestedHearings(hearingRecordingRequests);

        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(ABOUT_TO_SUBMIT, actualCallback, USER_AUTHORISATION);

        assertEquals(2, actualCaseData.getData().getSscsHearingRecordingCaseData().getRequestedHearings().size());
        assertEquals("reps-some-name.pdf", actualCaseData.getData().getSscsHearingRecordingCaseData().getRequestedHearings().get(1).getValue().getRequestDocument().getDocumentFilename());

        assertEquals(1, actualCaseData.getData().getScannedDocuments().size());
        assertEquals("appellant-some-name.pdf", actualCaseData.getData().getScannedDocuments().get(0).getValue().getFileName());

        HearingRecordingRequest hearingRecordingRequest = actualCaseData.getData().getSscsHearingRecordingCaseData().getRequestedHearings().get(1);
        assertEquals("appellant", hearingRecordingRequest.getValue().getRequestingParty());
        assertEquals(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), hearingRecordingRequest.getValue().getDateRequested());

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
