package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadfurtherevidence;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.lenient;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DraftSscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DraftSscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.WorkAllocationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.util.AddedDocumentsUtil;

@ExtendWith(MockitoExtension.class)
class UploadFurtherEvidenceAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private UploadFurtherEvidenceAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    private SscsCaseData sscsCaseData;

    private AddedDocumentsUtil addedDocumentsUtil;

    @BeforeEach
    void setUp() {
        addedDocumentsUtil = new AddedDocumentsUtil(false);

        handler = new UploadFurtherEvidenceAboutToSubmitHandler(true, addedDocumentsUtil);
        lenient().when(callback.getEvent()).thenReturn(EventType.UPLOAD_FURTHER_EVIDENCE);
        sscsCaseData = SscsCaseData.builder()
            .state(State.VALID_APPEAL)
            .interlocReviewState(REVIEW_BY_TCW)
            .appeal(Appeal.builder()
                .build())
            .build();
        lenient().when(callback.getCaseDetails()).thenReturn(caseDetails);
        lenient().when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenANonUploadFurtherEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void givenValidCallback_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenANonCallbackType_thenReturnFalse(final CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertThatThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION))
            .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "fileName, Please add a file name",
        "documentType, Please select a document type",
        "documentLink, Please upload a file",
        "documentUrl, Please upload a file",
        "invalidFileType;docx, 'You need to upload PDF, MP3 or MP4 documents only'",
        "invalidFileType;xlsx, 'You need to upload PDF, MP3 or MP4 documents only'",
        "invalidFileType;txt, 'You need to upload PDF, MP3 or MP4 documents only'",
        "invalidFileType;doc, 'You need to upload PDF, MP3 or MP4 documents only'",
        "invalidFileType;mov, 'You need to upload PDF, MP3 or MP4 documents only'"
    })
    void shouldCatchErrorInDraftFurtherEvidenceDocument(final String nullField, final String expectedErrorMessage) {
        final List<DraftSscsDocument> draftDocs = getDraftSscsDocuments(
                nullField, nullField.startsWith("invalidFileType") ? format("doc.%s", nullField.split(";")[1]) : "document.pdf");
        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocs);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).containsExactly(expectedErrorMessage);
        assertThat(response.getData().getDraftFurtherEvidenceDocuments()).isEqualTo(draftDocs);
        assertThat(response.getData().getSscsDocument()).isNull();
    }

    private List<DraftSscsDocument> getDraftSscsDocuments(final String nullField, final String fileName) {
        final DraftSscsDocument doc = DraftSscsDocument.builder().value(DraftSscsDocumentDetails.builder()
                .documentFileName(nullField.contains("fileName") ? null : fileName)
                .documentType(nullField.contains("documentType") ? null : "appellantEvidence")
                .documentLink(nullField.contains("documentLink") ? null : DocumentLink.builder().documentUrl(
                        nullField.equals("documentUrl") ? null : "documentUrl").documentFilename(fileName).build())
                .build()).build();
        return unmodifiableList(singletonList(doc));
    }

    @Test
    void shouldHandleNoDraftUploads() {
        sscsCaseData.setDraftFurtherEvidenceDocuments(null);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getDraftFurtherEvidenceDocuments()).isNull();
        assertThat(response.getData().getSscsDocument()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"pdf", "PDF", "mp3", "MP3", "mp4", "MP4"})
    void shouldMoveOneDraftUploadsToSscsDocumentsOrAudioVideoEvidence(final String fileType) {
        sscsCaseData.setDraftFurtherEvidenceDocuments(getDraftSscsDocuments("", format("document.%s", fileType)));
        sscsCaseData.setSscsDocument(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getDraftFurtherEvidenceDocuments()).isNull();
        if (fileType.equalsIgnoreCase("pdf")) {
            assertThat(response.getData().getSscsDocument()).hasSize(1);
            assertThat(response.getData().getAudioVideoEvidence()).isNull();
            assertThat(response.getData().getHasUnprocessedAudioVideoEvidence()).isEqualTo(YesNo.NO);
        } else {
            assertThat(response.getData().getSscsDocument()).isNull();
            assertThat(response.getData().getAudioVideoEvidence()).hasSize(1);
            assertThat(response.getData().getAudioVideoEvidence().getFirst().getValue().getPartyUploaded()).isEqualTo(UploadParty.CTSC);
            assertThat(response.getData().getAudioVideoEvidence().getFirst().getValue().getOriginalPartySender()).isEqualTo("Appellant");
            assertThat(response.getData().getInterlocReferralReason()).isEqualTo(REVIEW_AUDIO_VIDEO_EVIDENCE);
            assertThat(response.getData().getHasUnprocessedAudioVideoEvidence()).isEqualTo(YesNo.YES);
        }
    }

    @Test
    void givenAMixtureOfAudioVideoAndDocumentEvidence_onlyAudioVideoShouldBeInsertedIntoAddedDocuments()
        throws JsonProcessingException {
        handler = new UploadFurtherEvidenceAboutToSubmitHandler(true,
            new AddedDocumentsUtil(true));

        final List<DraftSscsDocument> draftDocuments = new ArrayList<>();
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

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        final Map<String, Integer> addedDocuments = new ObjectMapper().readerFor(Map.class)
            .readValue(response.getData().getWorkAllocationFields().getAddedDocuments());

        assertThat(addedDocuments)
            .as("Only audio video evidence should be inserted into added documents map.")
            .containsOnly(entry("audioDocument", 1), entry("videoDocument", 1));
    }

    @Test
    void givenASupplementaryResponseWithAudioVideoEvidenceSentMultipleTimes_shouldInsertMostRecentIntoAddedDocuments()
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
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        final Map<String, Integer> addedDocuments = new ObjectMapper().readerFor(Map.class)
            .readValue(response.getData().getWorkAllocationFields().getAddedDocuments());

        assertThat(addedDocuments)
            .as("Added documents should only contain evidence added in the most recent event.")
            .containsOnly(entry("audioDocument", 1));
    }

    @Test
    void givenASupplementaryResponseWitNoAudioVideoEvidence_shouldClearAddedDocuments() {
        handler = new UploadFurtherEvidenceAboutToSubmitHandler(true,
            new AddedDocumentsUtil(true));

        sscsCaseData.setDraftFurtherEvidenceDocuments(new ArrayList<>());
        sscsCaseData.setWorkAllocationFields(WorkAllocationFields.builder()
            .addedDocuments("{audioEvidence=1}")
            .build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getWorkAllocationFields().getAddedDocuments())
            .as("Added documents should be cleared regardless of whether audio video evidence has been added.")
            .isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"audio.mp3", "video.mp4"})
    void shouldGiveErrorIfAudioVideoEvidenceHasIncorrectDocumentType(final String filename) {
        final List<DraftSscsDocument> draftDocuments = Collections.singletonList(DraftSscsDocument.builder()
                .value(DraftSscsDocumentDetails.builder()
                        .documentFileName(filename)
                        .documentType("incorrectType")
                        .documentLink(DocumentLink.builder()
                                .documentUrl("http://dm-store:5005/documents/abe3b75a-7a72-4e68-b136-4349b7d4f655")
                                .documentFilename(filename).build()).build())
                        .build());
        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocuments);
        sscsCaseData.setSscsDocument(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).containsExactly("Type not accepted for AV evidence. Select a Type for the party that originally submitted the audio/video evidence");
    }

    @Test
    void shouldMoveTwoDraftUploadsToSscsDocumentsWhenOneSscsDocumentExists() {
        final ArrayList<DraftSscsDocument> draftSscsDocuments = new ArrayList<>();
        draftSscsDocuments.addAll(getDraftSscsDocuments("", "doc1.pdf"));
        draftSscsDocuments.addAll(getDraftSscsDocuments("", "doc2.pdf"));
        sscsCaseData.setSscsDocument(unmodifiableList(singletonList(SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build())));
        sscsCaseData.setDraftFurtherEvidenceDocuments(unmodifiableList(draftSscsDocuments));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getDraftFurtherEvidenceDocuments()).isNull();
        assertThat(response.getData().getSscsDocument()).hasSize(3);
    }

    @Test
    void shouldSortMergedSscsDocumentsByDateAddedDescending() {
        final SscsDocument olderExistingDoc = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("old.pdf")
                .documentDateAdded("2020-01-01")
                .build()).build();
        final SscsDocument newerExistingDoc = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("mid.pdf")
                .documentDateAdded("2023-06-15")
                .build()).build();
        sscsCaseData.setSscsDocument(new ArrayList<>(List.of(olderExistingDoc, newerExistingDoc)));
        sscsCaseData.setDraftFurtherEvidenceDocuments(getDraftSscsDocuments("", "new.pdf"));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument())
                .extracting(doc -> doc.getValue().getDocumentFileName())
                .containsExactly("new.pdf", "mid.pdf", "old.pdf");
    }

    @ParameterizedTest
    @ValueSource(strings = {"doc.mp4", "doc.mp3"})
    void shouldNotOnlyAllowAudioVisualFilesWhenInterlocReviewStateIsNotReviewByTcw(final String fileName) {
        final List<DraftSscsDocument> draftDocs = getDraftSscsDocuments("", fileName);
        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocs);
        sscsCaseData.setInterlocReviewState(AWAITING_ADMIN_ACTION);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).containsExactly("As you have uploaded an MP3 or MP4 file, please set interlocutory review state to 'Review by TCW'");
        assertThat(response.getData().getDraftFurtherEvidenceDocuments()).isEqualTo(draftDocs);
        assertThat(response.getData().getSscsDocument()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "REVIEW_BY_TCW, doc.mp4",
        "REVIEW_BY_TCW, doc.mp3",
        "REVIEW_BY_JUDGE, doc.mp4",
        "REVIEW_BY_JUDGE, doc.mp3"
    })
    void shouldAllowAudioVisualFilesWhenInterlocReviewStateIsValid(final InterlocReviewState interlocReviewState, final String fileName) {
        final List<DraftSscsDocument> draftDocs = getDraftSscsDocuments("", fileName);
        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocs);
        sscsCaseData.setInterlocReviewState(interlocReviewState);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenFurtherEvidenceReceivedAndInterlocReviewStateAlreadyReviewByJudge_thenLeaveStateAsReviewByJudge() {
        final SscsCaseData sscsCaseDataBefore = SscsCaseData.builder()
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

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(REVIEW_BY_JUDGE);
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(REVIEW_AUDIO_VIDEO_EVIDENCE);
    }

    @ParameterizedTest
    @EnumSource(value = InterlocReviewState.class, names = {"REVIEW_BY_TCW", "AWAITING_INFORMATION", "REVIEW_BY_JUDGE", "NONE", "AWAITING_ADMIN_ACTION", "WELSH_TRANSLATION"})
    void shouldMovePdfFilesToSscsDocumentsForAnyInterlocReviewState(final InterlocReviewState interlocReviewState) {
        final List<DraftSscsDocument> draftDocs = getDraftSscsDocuments("", "doc.pdf");
        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocs);
        sscsCaseData.setInterlocReviewState(interlocReviewState);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getDraftFurtherEvidenceDocuments()).isNull();
        assertThat(response.getData().getSscsDocument()).hasSize(1);
        assertThat(response.getData().getAudioVideoEvidence()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"doc.mp4", "doc.mp3"})
    void shouldOnlyUploadPdfFilesWhenFeatureFlagIsFalse(final String fileName) {
        handler = new UploadFurtherEvidenceAboutToSubmitHandler(false, addedDocumentsUtil);
        final List<DraftSscsDocument> draftDocs = getDraftSscsDocuments("", fileName);
        sscsCaseData.setDraftFurtherEvidenceDocuments(draftDocs);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).containsExactly("You need to upload PDF documents only");
        assertThat(response.getData().getDraftFurtherEvidenceDocuments()).isEqualTo(draftDocs);
        assertThat(response.getData().getSscsDocument()).isNull();
    }

}
