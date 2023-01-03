package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadfurtherevidence;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_TCW;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.util.AddedDocumentsUtil;

@RunWith(JUnitParamsRunner.class)
public class UploadFurtherEvidenceAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private UploadFurtherEvidenceAboutToSubmitHandler handler;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    private SscsCaseData sscsCaseData;

    private AddedDocumentsUtil addedDocumentsUtil;

    @Before
    public void setUp() {
        addedDocumentsUtil = new AddedDocumentsUtil(false);

        MockitoAnnotations.openMocks(this);
        handler = new UploadFurtherEvidenceAboutToSubmitHandler(true, addedDocumentsUtil);
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_FURTHER_EVIDENCE);
        sscsCaseData = SscsCaseData.builder()
            .state(State.VALID_APPEAL)
            .interlocReviewState(REVIEW_BY_TCW)
            .appeal(Appeal.builder()
                .build())
            .build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonUploadFurtherEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenValidCallback_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    @Parameters({
        "fileName, Please add a file name",
        "documentType, Please select a document type",
        "documentLink, Please upload a file",
        "documentUrl, Please upload a file",
        "invalidFileType;docx, You need to upload PDF\\, MP3 or MP4 documents only",
        "invalidFileType;xlsx, You need to upload PDF\\, MP3 or MP4 documents only",
        "invalidFileType;txt, You need to upload PDF\\, MP3 or MP4 documents only",
        "invalidFileType;doc, You need to upload PDF\\, MP3 or MP4 documents only",
        "invalidFileType;mov, You need to upload PDF\\, MP3 or MP4 documents only"})
    public void shouldCatchErrorInDraftFurtherEvidenceDocument(String nullField, String expectedErrorMessage) {
        final List<DraftSscsDocument> draftDocs = getDraftSscsDocuments(
                nullField, nullField.startsWith("invalidFileType") ? format("doc.%s", nullField.split(";")[1]) : "document.pdf");
        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocs);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is(expectedErrorMessage));
        assertThat(response.getData().getDraftFurtherEvidenceDocuments(), is(draftDocs));
        assertThat(response.getData().getSscsDocument(), is(nullValue()));
    }

    @NotNull
    private List<DraftSscsDocument> getDraftSscsDocuments(String nullField, String fileName) {
        final DraftSscsDocument doc = DraftSscsDocument.builder().value(DraftSscsDocumentDetails.builder()
                .documentFileName(nullField.contains("fileName") ? null : fileName)
                .documentType(nullField.contains("documentType") ? null : "appellantEvidence")
                .documentLink(nullField.contains("documentLink") ? null : DocumentLink.builder().documentUrl(
                        nullField.equals("documentUrl") ? null : "documentUrl").documentFilename(fileName).build())
                .build()).build();
        return unmodifiableList(singletonList(doc));
    }

    @Test
    public void shouldHandleNoDraftUploads() {
        sscsCaseData.setDraftFurtherEvidenceDocuments(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getDraftFurtherEvidenceDocuments(), is(nullValue()));
        assertThat(response.getData().getSscsDocument(), is(nullValue()));
    }

    @Test
    @Parameters({"pdf", "PDF", "mp3", "MP3", "mp4", "MP4"})
    public void shouldMoveOneDraftUploadsToSscsDocumentsOrAudioVideoEvidence(String fileType) {
        sscsCaseData.setDraftFurtherEvidenceDocuments(getDraftSscsDocuments("", format("document.%s", fileType)));
        sscsCaseData.setSscsDocument(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getDraftFurtherEvidenceDocuments(), is(nullValue()));
        if (fileType.equalsIgnoreCase("pdf")) {
            assertThat(response.getData().getSscsDocument().size(), is(1));
            assertThat(response.getData().getAudioVideoEvidence(), is(nullValue()));
            assertEquals(YesNo.NO, response.getData().getHasUnprocessedAudioVideoEvidence());
        } else {
            assertThat(response.getData().getSscsDocument(), is(nullValue()));
            assertThat(response.getData().getAudioVideoEvidence().size(), is(1));
            assertThat(response.getData().getAudioVideoEvidence().get(0).getValue().getPartyUploaded(), is(UploadParty.CTSC));
            assertThat(response.getData().getAudioVideoEvidence().get(0).getValue().getOriginalPartySender(), is("Appellant"));
            assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE, response.getData().getInterlocReferralReason());
            assertEquals(YesNo.YES, response.getData().getHasUnprocessedAudioVideoEvidence());
        }
    }

    @Test
    public void givenAMixtureOfAudioVideoAndDocumentEvidence_onlyAudioVideoShouldBeInsertedIntoAddedDocuments()
        throws JsonProcessingException {
        handler = new UploadFurtherEvidenceAboutToSubmitHandler(true,
            new AddedDocumentsUtil(true));

        List<DraftSscsDocument> draftDocuments = new ArrayList<>();
        draftDocuments.add(DraftSscsDocument.builder()
            .value(DraftSscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("testurl")
                    .documentFilename("test.mp4").build())
                .documentType("appellantEvidence")
                .documentFileName("test.mp4")
                .build())
            .build());

        draftDocuments.add(DraftSscsDocument.builder()
            .value(DraftSscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("testurl1")
                    .documentFilename("test1.mp3").build())
                .documentType("appellantEvidence")
                .documentFileName("test1.mp3")
                .build())
            .build());

        draftDocuments.add(DraftSscsDocument.builder()
            .value(DraftSscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("testurl2")
                    .documentFilename("test2.pdf").build())
                .documentType("postponementRequest")
                .documentFileName("test2.pdf")
                .build())
            .build());

        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Map<String, Integer> addedDocuments = new ObjectMapper().readerFor(Map.class)
            .readValue(response.getData().getWorkAllocationFields().getAddedDocuments());

        org.assertj.core.api.Assertions.assertThat(addedDocuments)
            .as("Only audio video evidence should be inserted into added documents map.")
            .containsOnly(org.assertj.core.api.Assertions.entry("audioDocument", 1),
                org.assertj.core.api.Assertions.entry("videoDocument", 1));
    }

    @Test
    public void givenASupplementaryResponseWithAudioVideoEvidenceSentMultipleTimes_shouldInsertMostRecentIntoAddedDocuments()
        throws JsonProcessingException {
        handler = new UploadFurtherEvidenceAboutToSubmitHandler(true,
            new AddedDocumentsUtil(true));

        List<DraftSscsDocument> draftDocuments = new ArrayList<>();
        draftDocuments.add(DraftSscsDocument.builder()
            .value(DraftSscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("testurl")
                    .documentFilename("test.mp4").build())
                .documentType("appellantEvidence")
                .documentFileName("test.mp4")
                .build())
            .build());

        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocuments);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        draftDocuments = new ArrayList<>();
        draftDocuments.add(DraftSscsDocument.builder()
            .value(DraftSscsDocumentDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("testurl1")
                    .documentFilename("test1.mp3").build())
                .documentType("appellantEvidence")
                .documentFileName("test1.mp3")
                .build())
            .build());

        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocuments);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Map<String, Integer> addedDocuments = new ObjectMapper().readerFor(Map.class)
            .readValue(response.getData().getWorkAllocationFields().getAddedDocuments());

        org.assertj.core.api.Assertions.assertThat(addedDocuments)
            .as("Added documents should only contain evidence added in the most recent event.")
            .containsOnly(org.assertj.core.api.Assertions.entry("audioDocument", 1));
    }

    @Test
    public void givenASupplementaryResponseWitNoAudioVideoEvidence_shouldClearAddedDocuments() {
        handler = new UploadFurtherEvidenceAboutToSubmitHandler(true,
            new AddedDocumentsUtil(true));

        sscsCaseData.setDraftFurtherEvidenceDocuments(new ArrayList<>());
        sscsCaseData.setWorkAllocationFields(WorkAllocationFields.builder()
            .addedDocuments("{audioEvidence=1}")
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        org.assertj.core.api.Assertions.assertThat(response.getData().getWorkAllocationFields().getAddedDocuments())
            .as("Added documents should be cleared regardless of whether audio video evidence has been added.")
            .isNull();
    }

    @Test
    @Parameters({"audio.mp3", "video.mp4"})
    public void shouldGiveErrorIfAudioVideoEvidenceHasIncorrectDocumentType(String filename) {
        List<DraftSscsDocument> draftDocuments = Collections.singletonList(DraftSscsDocument.builder()
                .value(DraftSscsDocumentDetails.builder()
                        .documentFileName(filename)
                        .documentType("incorrectType")
                        .documentLink(DocumentLink.builder()
                                .documentUrl("http://dm-store:5005/documents/abe3b75a-7a72-4e68-b136-4349b7d4f655")
                                .documentFilename(filename).build()).build())
                        .build());
        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocuments);
        sscsCaseData.setSscsDocument(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Type not accepted for AV evidence. Select a Type for the party that originally submitted the audio/video evidence"));

    }

    @Test
    public void shouldMoveTwoDraftUploadsToSscsDocumentsWhenOneSscsDocumentExists() {
        ArrayList<DraftSscsDocument> draftSscsDocuments = new ArrayList<>();
        draftSscsDocuments.addAll(getDraftSscsDocuments("", "doc1.pdf"));
        draftSscsDocuments.addAll(getDraftSscsDocuments("", "doc2.pdf"));
        sscsCaseData.setSscsDocument(unmodifiableList(singletonList(SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build())));
        sscsCaseData.setDraftFurtherEvidenceDocuments(unmodifiableList(draftSscsDocuments));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getDraftFurtherEvidenceDocuments(), is(nullValue()));
        assertThat(response.getData().getSscsDocument().size(), is(3));
    }

    @Test
    @Parameters({"doc.mp4", "doc.mp3"})
    public void shouldNotOnlyAllowAudioVisualFilesWhenInterlocReviewStateIsNotReviewByTcw(String fileName) {
        final List<DraftSscsDocument> draftDocs = getDraftSscsDocuments("", fileName);
        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocs);
        sscsCaseData.setInterlocReviewState(AWAITING_ADMIN_ACTION);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("As you have uploaded an MP3 or MP4 file, please set interlocutory review state to 'Review by TCW'"));
        assertThat(response.getData().getDraftFurtherEvidenceDocuments(), is(draftDocs));
        assertThat(response.getData().getSscsDocument(), is(nullValue()));
    }

    @Test
    @Parameters({"REVIEW_BY_TCW, doc.mp4", "REVIEW_BY_TCW, doc.mp3", "REVIEW_BY_JUDGE, doc.mp4", "REVIEW_BY_JUDGE, doc.mp3"})
    public void shouldAllowAudioVisualFilesWhenInterlocReviewStateIsValid(InterlocReviewState interlocReviewState, String fileName) {
        final List<DraftSscsDocument> draftDocs = getDraftSscsDocuments("", fileName);
        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocs);
        sscsCaseData.setInterlocReviewState(interlocReviewState);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
    }

    @Test
    public void givenFurtherEvidenceReceivedAndInterlocReviewStateAlreadyReviewByJudge_thenLeaveStateAsReviewByJudge() {
        SscsCaseData sscsCaseDataBefore = SscsCaseData.builder()
            .state(State.VALID_APPEAL)
            .interlocReviewState(REVIEW_BY_JUDGE)
            .appeal(Appeal.builder()
                .build())
            .build();

        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        final List<DraftSscsDocument> draftDocs = getDraftSscsDocuments("", "doc.mp3");
        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocs);
        sscsCaseData.setInterlocReviewState(REVIEW_BY_TCW);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(REVIEW_BY_JUDGE));
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE, response.getData().getInterlocReferralReason());
    }

    @Test
    @Parameters({"REVIEW_BY_TCW", "AWAITING_INFORMATION", "REVIEW_BY_JUDGE", "NONE", "AWAITING_ADMIN_ACTION", "WELSH_TRANSLATION"})
    public void shouldMovePdfFilesToSscsDocumentsForAnyInterlocReviewState(InterlocReviewState interlocReviewState) {
        final List<DraftSscsDocument> draftDocs = getDraftSscsDocuments("", "doc.pdf");
        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocs);
        sscsCaseData.setInterlocReviewState(interlocReviewState);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getDraftFurtherEvidenceDocuments(), is(nullValue()));
        assertThat(response.getData().getSscsDocument().size(), is(1));
        assertThat(response.getData().getAudioVideoEvidence(), is(nullValue()));
    }

    @Test
    @Parameters({"doc.mp4", "doc.mp3"})
    public void shouldOnlyUploadPdfFilesWhenFeatureFlagIsFalse(String fileName) {
        handler = new UploadFurtherEvidenceAboutToSubmitHandler(false, addedDocumentsUtil);
        final List<DraftSscsDocument> draftDocs = getDraftSscsDocuments("", fileName);
        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocs);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("You need to upload PDF documents only"));
        assertThat(response.getData().getDraftFurtherEvidenceDocuments(), is(draftDocs));
        assertThat(response.getData().getSscsDocument(), is(nullValue()));
    }

}
