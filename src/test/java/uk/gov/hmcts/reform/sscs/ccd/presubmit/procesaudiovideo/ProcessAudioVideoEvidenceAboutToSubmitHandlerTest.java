package uk.gov.hmcts.reform.sscs.ccd.presubmit.procesaudiovideo;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.RIP1;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.DIRECTION_ACTION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.ADMIT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.EXCLUDE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.ISSUE_DIRECTIONS_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.SEND_TO_ADMIN;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.SEND_TO_JUDGE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidence;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentStaging;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.Note;
import uk.gov.hmcts.reform.sscs.ccd.domain.NoteDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.ProcessAudioVideoReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoEvidenceAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class ProcessAudioVideoEvidenceAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String DOCUMENT_URL = "dm-store/documents/123";

    private ProcessAudioVideoEvidenceAboutToSubmitHandler handler;

    @Spy
    private DocumentConfiguration documentConfiguration;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Mock
    private FooterService footerService;

    @Mock
    private UserDetailsService userDetailsService;

    private SscsDocument expectedDocument;

    @BeforeEach
    void setUp() {
        final Map<EventType, String> englishEventTypeDocs = new EnumMap<>(EventType.class);
        englishEventTypeDocs.put(EventType.DIRECTION_ISSUED, "TB-SCS-GNO-ENG-directions-notice-v2.docx");

        final Map<LanguagePreference, Map<EventType, String>> documents = new EnumMap<>(LanguagePreference.class);
        documents.put(LanguagePreference.ENGLISH, englishEventTypeDocs);

        documentConfiguration.setDocuments(documents);
        handler = new ProcessAudioVideoEvidenceAboutToSubmitHandler(footerService, userDetailsService);

        sscsCaseData = SscsCaseData.builder()
            .documentGeneration(DocumentGeneration.builder()
                .signedBy("User")
                .signedRole("Judge")
                .build())
            .processAudioVideoAction(new DynamicList(ISSUE_DIRECTIONS_NOTICE.getCode()))
            .documentStaging(DocumentStaging.builder()
                .dateAdded(LocalDate.now().minusDays(1))
                .previewDocument(DocumentLink.builder()
                    .documentUrl(DOCUMENT_URL)
                    .documentBinaryUrl(DOCUMENT_URL + "/binary")
                    .documentFilename("directionIssued.pdf")
                    .build())
                .build())
            .directionDueDate(LocalDate.now().plusDays(1).toString())
            .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
            .interlocReviewState(InterlocReviewState.REVIEW_BY_TCW)
            .selectedAudioVideoEvidence(new DynamicList("test.com")).selectedAudioVideoEvidenceDetails(AudioVideoEvidenceDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("music.mp3").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                .fileName("music.mp3")
                .partyUploaded(UploadParty.APPELLANT)
                .dateAdded(LocalDate.now())
                .statementOfEvidencePdf(DocumentLink.builder().documentFilename("statement1.pdf").documentUrl("statement1.url").documentBinaryUrl("statement1.url/binary").build())
                .build())
            .audioVideoEvidence(new ArrayList<>(Arrays.asList(AudioVideoEvidence.builder().value(
                AudioVideoEvidenceDetails.builder()
                    .documentLink(DocumentLink.builder().documentFilename("music.mp3").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                    .fileName("music.mp3")
                    .partyUploaded(UploadParty.APPELLANT)
                    .dateAdded(LocalDate.now())
                    .statementOfEvidencePdf(DocumentLink.builder().documentFilename("statement1.pdf").build())
                    .build())
                .build(),
                AudioVideoEvidence.builder().value(
                    AudioVideoEvidenceDetails.builder()
                        .documentLink(DocumentLink.builder().documentFilename("music1.mp3").documentUrl("test1.com").documentBinaryUrl("test.com/binary").build())
                        .fileName("music1.mp3")
                        .partyUploaded(UploadParty.APPELLANT)
                        .dateAdded(LocalDate.now())
                        .build())
                    .build())))
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("APPELLANT")
                        .lastName("LastNamE")
                        .build())
                    .identity(Identity.builder().build())
                    .build())
                .build()).build();

        expectedDocument = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentFileName(sscsCaseData.getDocumentStaging().getPreviewDocument().getDocumentFilename())
                .documentLink(sscsCaseData.getDocumentStaging().getPreviewDocument())
                .documentDateAdded(LocalDate.now().minusDays(1).toString())
                .documentType(AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE.getValue())
                .build()).build();

        lenient().when(callback.getCaseDetails()).thenReturn(caseDetails);
        lenient().when(callback.getEvent()).thenReturn(EventType.PROCESS_AUDIO_VIDEO);
        lenient().when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        lenient().when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn("John Lewis");
        lenient().when(footerService.getNextBundleAddition(any())).thenReturn("A");
        lenient().when(footerService.addFooter(any(), any(), eq("A"))).thenReturn(DocumentLink.builder().documentFilename("New doc with footer").build());
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    void givenANonHandleEvidenceEvent_thenReturnFalse(final EventType eventType) {
        lenient().when(callback.getEvent()).thenReturn(eventType);
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenAValidHandleAndEventType_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenANonCallbackType_thenReturnFalse(final CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @Test
    void shouldShowError_whenThereIsNoActionSelected() {
        sscsCaseData.setProcessAudioVideoAction(null);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).containsExactly("Select an action to process the audio/video evidence");
    }

    @Test
    void shouldShowError_whenThereIsNoPreviewDocument() {
        sscsCaseData.getDocumentStaging().setPreviewDocument(null);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).containsExactly("There is no document notice");
    }

    @Test
    void givenAdmitEvidenceFromAppellant_willRemoveDocFromAudioVideoEvidenceAndInterlocReviewStateAndAddToSscsDocumentsCollection() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ADMIT_EVIDENCE.getCode()));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDocumentStaging().getPreviewDocument()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedBy()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedRole()).isNull();
        assertThat(response.getData().getDocumentGeneration().getGenerateNotice()).isNull();
        assertThat(response.getData().getDocumentStaging().getDateAdded()).isNull();

        final DocumentLink expectedDocumentLink = DocumentLink.builder().documentFilename("statement1.pdf").documentUrl("statement1.url").documentBinaryUrl("statement1.url/binary").build();

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        verify(footerService).addFooter(expectedDocumentLink, "Statement of audio/video evidence", "A");

        assertThat(response.getData().getInterlocReviewState()).isNull();
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.NONE);
        assertThat(response.getData().getDwpState()).isEqualTo(DIRECTION_ACTION_REQUIRED);
        assertThat(response.getData().getAudioVideoEvidence()).hasSize(1);
        assertThat(response.getData().getSscsDocument()).hasSize(1);
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getAvDocumentLink()).isEqualTo(DocumentLink.builder().documentFilename("music.mp3").documentUrl("test.com").documentBinaryUrl("test.com/binary").build());
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getBundleAddition()).isEqualTo("A");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDateApproved()).isEqualTo(LocalDate.now().toString());
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentDateAdded()).isEqualTo(LocalDate.now().toString());
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentFileName()).isEqualTo("Addition A - Appellant - Statement for A/V file: music.mp3");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentType()).isEqualTo(DocumentType.AUDIO_DOCUMENT.getValue());
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getPartyUploaded().getLabel()).isEqualTo("Appellant");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getOriginalSenderOtherPartyName()).isNull();
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getOriginalSenderOtherPartyId()).isNull();
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentLink().getDocumentFilename()).isEqualTo("New doc with footer");
        assertThat(response.getData().getHasUnprocessedAudioVideoEvidence()).isEqualTo(YesNo.YES);
    }

    @Test
    void givenAdmitEvidenceFromAppellantWithExistingSscsDocuments_willClearAudioVideoEvidenceAndInterlocReviewStateAndAddToSscsDocumentsCollection() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ADMIT_EVIDENCE.getCode()));

        final List<SscsDocument> sscsDocuments = singletonList(SscsDocument.builder().value(
                SscsDocumentDetails.builder()
                        .documentLink(DocumentLink.builder().documentFilename("existing.mp3").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                        .documentFileName("existing.mp3")
                        .partyUploaded(UploadParty.DWP)
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .build())
                .build());
        sscsCaseData.setSscsDocument(sscsDocuments);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDocumentStaging().getPreviewDocument()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedBy()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedRole()).isNull();
        assertThat(response.getData().getDocumentGeneration().getGenerateNotice()).isNull();
        assertThat(response.getData().getDocumentStaging().getDateAdded()).isNull();

        final DocumentLink expectedDocumentLink = DocumentLink.builder().documentFilename("statement1.pdf").documentUrl("statement1.url").documentBinaryUrl("statement1.url/binary").build();

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        verify(footerService).addFooter(expectedDocumentLink,"Statement of audio/video evidence","A");

        assertThat(response.getData().getInterlocReviewState()).isNull();
        assertThat(response.getData().getDwpState()).isEqualTo(DIRECTION_ACTION_REQUIRED);
        assertThat(response.getData().getAudioVideoEvidence()).hasSize(1);
        assertThat(response.getData().getSscsDocument()).hasSize(2);
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getAvDocumentLink()).isEqualTo(DocumentLink.builder().documentFilename("music.mp3").documentUrl("test.com").documentBinaryUrl("test.com/binary").build());
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getBundleAddition()).isEqualTo("A");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDateApproved()).isEqualTo(LocalDate.now().toString());
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentDateAdded()).isEqualTo(LocalDate.now().toString());
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentFileName()).isEqualTo("Addition A - Appellant - Statement for A/V file: music.mp3");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentType()).isEqualTo(DocumentType.AUDIO_DOCUMENT.getValue());
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getPartyUploaded().getLabel()).isEqualTo("Appellant");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentLink().getDocumentFilename()).isEqualTo("New doc with footer");
    }

    @Test
    void givenAdmitEvidenceOlderThanExistingSscsDocument_thenSscsDocumentsAreOrderedByDateDescendingNotInsertionOrder() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ADMIT_EVIDENCE.getCode()));

        final AudioVideoEvidenceDetails selectedAudioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("music.mp3").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                .fileName("music.mp3")
                .partyUploaded(UploadParty.APPELLANT)
                .dateAdded(LocalDate.now().minusDays(10))
                .statementOfEvidencePdf(DocumentLink.builder().documentFilename("statement1.pdf").documentUrl("statement1.url").documentBinaryUrl("statement1.url/binary").build())
                .build();
        sscsCaseData.setSelectedAudioVideoEvidenceDetails(selectedAudioVideoEvidenceDetails);

        final List<SscsDocument> sscsDocuments = singletonList(SscsDocument.builder().value(
                        SscsDocumentDetails.builder()
                                .documentLink(DocumentLink.builder().documentFilename("existing.pdf").documentUrl("existing.com").documentBinaryUrl("existing.com/binary").build())
                                .documentFileName("existing.pdf")
                                .partyUploaded(UploadParty.DWP)
                                .documentDateAdded(LocalDate.now().toString())
                                .build())
                .build());
        sscsCaseData.setSscsDocument(sscsDocuments);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument()).hasSize(2);
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentFileName()).isEqualTo("existing.pdf");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentDateAdded()).isEqualTo(LocalDate.now().toString());
        assertThat(response.getData().getSscsDocument().get(1).getValue().getDocumentFileName()).isEqualTo("Addition A - Appellant - Statement for A/V file: music.mp3");
        assertThat(response.getData().getSscsDocument().get(1).getValue().getDocumentDateAdded()).isEqualTo(LocalDate.now().minusDays(10).toString());
    }

    @Test
    void givenAdmitEvidenceFromAppellantWithExistingSscsDocumentsAndNoStatementOfEvidence_willClearAudioVideoEvidenceAndInterlocReviewStateAndAddToSscsDocumentsCollection() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ADMIT_EVIDENCE.getCode()));

        final AudioVideoEvidenceDetails selectedAudioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder()
                        .documentLink(DocumentLink.builder().documentFilename("music.mp3").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                        .fileName("music.mp3")
                        .partyUploaded(UploadParty.APPELLANT)
                        .dateAdded(LocalDate.now())
                        .statementOfEvidencePdf(null)
                        .build();

        sscsCaseData.setSelectedAudioVideoEvidenceDetails(selectedAudioVideoEvidenceDetails);

        final List<AudioVideoEvidence> audioVideoEvidence = new ArrayList<>(Arrays.asList(AudioVideoEvidence.builder().value(
                        AudioVideoEvidenceDetails.builder()
                                .documentLink(DocumentLink.builder().documentFilename("music.mp3").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                                .fileName("music.mp3")
                                .partyUploaded(UploadParty.APPELLANT)
                                .dateAdded(LocalDate.now())
                                .statementOfEvidencePdf(null)
                                .build())
                                .build(),
                        AudioVideoEvidence.builder().value(
                                AudioVideoEvidenceDetails.builder()
                                        .documentLink(DocumentLink.builder().documentFilename("music1.mp3").documentUrl("test1.com").documentBinaryUrl("test.com/binary").build())
                                        .fileName("music1.mp3")
                                        .partyUploaded(UploadParty.APPELLANT)
                                        .dateAdded(LocalDate.now())
                                        .build())
                                .build()));

        sscsCaseData.setAudioVideoEvidence(audioVideoEvidence);

        final List<SscsDocument> sscsDocuments = singletonList(SscsDocument.builder().value(
                SscsDocumentDetails.builder()
                        .documentLink(DocumentLink.builder().documentFilename("existing.mp3").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                        .documentFileName("existing.mp3")
                        .partyUploaded(UploadParty.DWP)
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .build())
                .build());
        sscsCaseData.setSscsDocument(sscsDocuments);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDocumentStaging().getPreviewDocument()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedBy()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedRole()).isNull();
        assertThat(response.getData().getDocumentGeneration().getGenerateNotice()).isNull();
        assertThat(response.getData().getDocumentStaging().getDateAdded()).isNull();

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        assertThat(response.getData().getInterlocReviewState()).isNull();
        assertThat(response.getData().getDwpState()).isEqualTo(DIRECTION_ACTION_REQUIRED);
        assertThat(response.getData().getAudioVideoEvidence()).hasSize(1);
        assertThat(response.getData().getSscsDocument()).hasSize(2);
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getAvDocumentLink()).isEqualTo(DocumentLink.builder().documentFilename("music.mp3").documentUrl("test.com").documentBinaryUrl("test.com/binary").build());
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getBundleAddition()).isNull();
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDateApproved()).isEqualTo(LocalDate.now().toString());
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentDateAdded()).isEqualTo(LocalDate.now().toString());
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentFileName()).isEqualTo("music.mp3");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentType()).isEqualTo(DocumentType.AUDIO_DOCUMENT.getValue());
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getPartyUploaded().getLabel()).isEqualTo("Appellant");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentLink()).isNull();
    }

    @Test
    void givenAdmitEvidenceIsNotAnMp3OrMp4_thenDisplayError() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ADMIT_EVIDENCE.getCode()));

        final AudioVideoEvidenceDetails evidenceDetails = AudioVideoEvidenceDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("nonvideo.pdf").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                .fileName("nonvideo.pdf")
                .partyUploaded(UploadParty.DWP)
                .dateAdded(LocalDate.now())
                .build();

        sscsCaseData.setSelectedAudioVideoEvidenceDetails(evidenceDetails);

        final List<AudioVideoEvidence> videoList = new ArrayList<>(singletonList(AudioVideoEvidence.builder().value(evidenceDetails).build()));

        sscsCaseData.setAudioVideoEvidence(videoList);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).containsExactly("Evidence cannot be included as it is not in .mp3 or .mp4 format");
    }

    @Test
    void givenAdmitEvidenceFromDwp_willClearAudioVideoEvidenceAndInterlocReviewStateAndAddToDwpDocumentsCollection() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ADMIT_EVIDENCE.getCode()));

        final AudioVideoEvidenceDetails evidenceDetails = AudioVideoEvidenceDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("video.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                .fileName("video.mp4")
                .partyUploaded(UploadParty.DWP)
                .dateAdded(LocalDate.now())
                .build();

        final List<AudioVideoEvidence> videoList = new ArrayList<>(singletonList(AudioVideoEvidence.builder().value(evidenceDetails).build()));

        sscsCaseData.setSelectedAudioVideoEvidenceDetails(evidenceDetails);

        sscsCaseData.setAudioVideoEvidence(videoList);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDocumentStaging().getPreviewDocument()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedBy()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedRole()).isNull();
        assertThat(response.getData().getDocumentGeneration().getGenerateNotice()).isNull();
        assertThat(response.getData().getDocumentStaging().getDateAdded()).isNull();

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        assertThat(response.getData().getInterlocReviewState()).isNull();
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.NONE);
        assertThat(response.getData().getDwpState()).isEqualTo(DIRECTION_ACTION_REQUIRED);
        assertThat(response.getData().getAudioVideoEvidence()).isNull();
        assertThat(response.getData().getDwpDocuments()).hasSize(1);
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getAvDocumentLink()).isEqualTo(DocumentLink.builder().documentFilename("video.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build());
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getDateApproved()).isEqualTo(LocalDate.now().toString());
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getDocumentDateAdded()).isEqualTo(LocalDate.now().toString());
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getDocumentFileName()).isEqualTo("video.mp4");
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getDocumentType()).isEqualTo("videoDocument");
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getPartyUploaded().getLabel()).isEqualTo("DWP");
        assertThat(response.getData().getHasUnprocessedAudioVideoEvidence()).isEqualTo(YesNo.NO);
    }

    @Test
    void givenAdmitEvidenceFromDwpWithRip1Document_willClearAudioVideoEvidenceAndInterlocReviewStateAndAddToDwpDocumentsCollection() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ADMIT_EVIDENCE.getCode()));

        final AudioVideoEvidenceDetails evidenceDetails = AudioVideoEvidenceDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("video.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                .fileName("video.mp4")
                .partyUploaded(UploadParty.DWP)
                .dateAdded(LocalDate.now())
                .rip1Document(DocumentLink.builder().documentFilename("rip1.pdf").documentUrl("rip1.com").documentBinaryUrl("rip1.com/binary").build())
                .build();

        sscsCaseData.setSelectedAudioVideoEvidenceDetails(evidenceDetails);

        final List<AudioVideoEvidence> videoList = new ArrayList<>(singletonList(AudioVideoEvidence.builder().value(evidenceDetails).build()));

        sscsCaseData.setAudioVideoEvidence(videoList);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDocumentStaging().getPreviewDocument()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedBy()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedRole()).isNull();
        assertThat(response.getData().getDocumentGeneration().getGenerateNotice()).isNull();
        assertThat(response.getData().getDocumentStaging().getDateAdded()).isNull();

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        verify(footerService).addFooter(DocumentLink.builder().documentFilename("RIP 1 document uploaded on " + LocalDate.now() + ".pdf").documentUrl("rip1.com").documentBinaryUrl("rip1.com/binary").build(), "RIP 1 document", "A");

        assertThat(response.getData().getInterlocReviewState()).isNull();
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.NONE);
        assertThat(response.getData().getDwpState()).isEqualTo(DIRECTION_ACTION_REQUIRED);
        assertThat(response.getData().getAudioVideoEvidence()).isNull();
        assertThat(response.getData().getDwpDocuments()).hasSize(1);
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getAvDocumentLink()).isEqualTo(DocumentLink.builder().documentFilename("video.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build());
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getBundleAddition()).isNull();
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getDateApproved()).isEqualTo(LocalDate.now().toString());
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getDocumentDateAdded()).isEqualTo(LocalDate.now().toString());
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getDocumentFileName()).isEqualTo("video.mp4");
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getDocumentType()).isEqualTo("videoDocument");
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getPartyUploaded().getLabel()).isEqualTo("DWP");
        // RIP1
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getBundleAddition()).isEqualTo("A");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentType()).isEqualTo(RIP1.getValue());
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentLink().getDocumentFilename()).isEqualTo("New doc with footer");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentFileName()).isEqualTo("Addition A - DWP - RIP 1 document for A/V file: video.mp4");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getPartyUploaded().getLabel()).isEqualTo("DWP");
    }

    @Test
    void givenAdmitEvidenceFromDwpWithExistingDwpDocumentsAndNoRip1_willClearAudioVideoEvidenceAndInterlocReviewStateAndAddToDwpDocumentsCollectionAndShouldNotAddRip1ToBundle() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ADMIT_EVIDENCE.getCode()));

        final AudioVideoEvidenceDetails evidenceDetails = AudioVideoEvidenceDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("video.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                .fileName("video.mp4")
                .partyUploaded(UploadParty.DWP)
                .dateAdded(LocalDate.now())
                .build();

        final List<AudioVideoEvidence> videoList = new ArrayList<>(singletonList(AudioVideoEvidence.builder().value(evidenceDetails).build()));

        sscsCaseData.setSelectedAudioVideoEvidenceDetails(evidenceDetails);

        sscsCaseData.setAudioVideoEvidence(videoList);

        final List<DwpDocument> dwpDocuments = singletonList(DwpDocument.builder().value(
                DwpDocumentDetails.builder()
                        .documentLink(DocumentLink.builder().documentFilename("existing.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                        .documentFileName("existing.mp4")
                        .partyUploaded(UploadParty.DWP)
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .build())
                .build());
        sscsCaseData.setDwpDocuments(dwpDocuments);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDocumentStaging().getPreviewDocument()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedBy()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedRole()).isNull();
        assertThat(response.getData().getDocumentGeneration().getGenerateNotice()).isNull();
        assertThat(response.getData().getDocumentStaging().getDateAdded()).isNull();

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        assertThat(response.getData().getInterlocReviewState()).isNull();
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.NONE);
        assertThat(response.getData().getDwpState()).isEqualTo(DIRECTION_ACTION_REQUIRED);
        assertThat(response.getData().getAudioVideoEvidence()).isNull();
        assertThat(response.getData().getDwpDocuments()).hasSize(2);
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getAvDocumentLink()).isEqualTo(DocumentLink.builder().documentFilename("video.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build());
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getBundleAddition()).isNull();
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getDateApproved()).isEqualTo(LocalDate.now().toString());
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getDocumentDateAdded()).isEqualTo(LocalDate.now().toString());
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getDocumentFileName()).isEqualTo("video.mp4");
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getDocumentType()).isEqualTo("videoDocument");
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getPartyUploaded().getLabel()).isEqualTo("DWP");

        //FIXME: Check RIP1 empty in sscs documents
    }

    @Test
    void excludeEvidence_willClearAudioVideoEvidenceAndInterlocReviewState() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(EXCLUDE_EVIDENCE.getCode()));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDocumentStaging().getPreviewDocument()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedBy()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedRole()).isNull();
        assertThat(response.getData().getDocumentGeneration().getGenerateNotice()).isNull();
        assertThat(response.getData().getDocumentStaging().getDateAdded()).isNull();

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        assertThat(response.getData().getInterlocReviewState()).isNull();
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.NONE);
        assertThat(response.getData().getDwpState()).isEqualTo(DIRECTION_ACTION_REQUIRED);
        assertThat(response.getData().getAudioVideoEvidence()).hasSize(1);
    }

    @Test
    void excludeEvidenceWithNoMoreAudioVideoEvidenceToProcess_willClearAudioVideoEvidenceCollectionAndInterlocReviewState() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(EXCLUDE_EVIDENCE.getCode()));
        sscsCaseData.getAudioVideoEvidence().remove(1);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDocumentStaging().getPreviewDocument()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedBy()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedRole()).isNull();
        assertThat(response.getData().getDocumentGeneration().getGenerateNotice()).isNull();
        assertThat(response.getData().getDocumentStaging().getDateAdded()).isNull();

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        assertThat(response.getData().getInterlocReviewState()).isNull();
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.NONE);
        assertThat(response.getData().getDwpState()).isEqualTo(DIRECTION_ACTION_REQUIRED);
        assertThat(response.getData().getAudioVideoEvidence()).isNull();
    }

    @Test
    void sendToJudge_shouldSetInterlocReviewState_toReviewByJudge() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(SEND_TO_JUDGE.getCode()));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verifyNoInteractions(footerService);
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.REVIEW_BY_JUDGE);
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE);
        assertThat(response.getData().getInterlocReferralDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void processIssueDirectionNoticeForWelshAppeal_shouldSetWelshInterlocReviewState_toAwaitingInformation() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ISSUE_DIRECTIONS_NOTICE.getCode()));
        sscsCaseData.setLanguagePreferenceWelsh(YesNo.YES.getValue());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE), any(), any(), eq(null), eq(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED));

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getData().getWelshInterlocNextReviewState()).isEqualTo(InterlocReviewState.AWAITING_INFORMATION.getCcdDefinition());
        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.WELSH_TRANSLATION);
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE);
        assertThat(response.getData().getInterlocReferralDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void sendToJudgeForWelshAppeal_shouldSetWelshInterlocReviewState_toReviewByJudge() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(SEND_TO_JUDGE.getCode()));
        sscsCaseData.setLanguagePreferenceWelsh(YesNo.YES.getValue());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verifyNoInteractions(footerService);
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getData().getWelshInterlocNextReviewState()).isEqualTo(InterlocReviewState.REVIEW_BY_JUDGE.getCcdDefinition());
        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.WELSH_TRANSLATION);
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE);
        assertThat(response.getData().getInterlocReferralDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void givenSendToAdminEventSelected_verifySetInterlocReviewState() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(SEND_TO_ADMIN.getCode()));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.AWAITING_ADMIN_ACTION);
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE);
    }

    @Test
    void givenSendToAdminEventSelectedForWelshAppeal_verifySetWelshInterlocNextReviewState() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(SEND_TO_ADMIN.getCode()));
        sscsCaseData.setLanguagePreferenceWelsh(YesNo.YES.getValue());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getWelshInterlocNextReviewState()).isEqualTo(InterlocReviewState.AWAITING_ADMIN_ACTION.getCcdDefinition());
        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.WELSH_TRANSLATION);
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE);
    }

    @ParameterizedTest
    @EnumSource(value = ProcessAudioVideoActionDynamicListItems.class, names = {"SEND_TO_JUDGE", "SEND_TO_ADMIN"})
    void shouldAddNote_whenActionIsSelected(final ProcessAudioVideoActionDynamicListItems action) {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(action.getCode()));
        final String note = "This is a note";
        final String userName = "John Lewis";
        sscsCaseData.setTempNoteDetail(note);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verifyNoInteractions(footerService);
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getData().getTempNoteDetail()).isNull();
        assertThat(response.getData().getAppealNotePad().getNotesCollection()).hasSize(1);
        assertThat(response.getData().getAppealNotePad().getNotesCollection().getFirst()).isEqualTo(Note.builder().value(NoteDetails.builder().noteDate(LocalDate.now().toString()).noteDetail(note).author(userName).build()).build());
    }

    @Test
    void shouldAddNoteAndNoUserDetails_thenThrowsException() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenThrow(new IllegalStateException("Unable to obtain signed in user details"));

        sscsCaseData.setProcessAudioVideoAction(new DynamicList(SEND_TO_ADMIN.getCode()));
        sscsCaseData.setTempNoteDetail("This is a note");

        assertThatThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenAdmitEvidenceFromDwpWithRip1DocumentForWelshCase_willSetInterlocReviewStateAndDocumentTranslationStatus() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.ADMIT_EVIDENCE.getCode()));
        sscsCaseData.setLanguagePreferenceWelsh("Yes");

        final AudioVideoEvidenceDetails evidenceDetails = AudioVideoEvidenceDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("video.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                .fileName("video.mp4")
                .partyUploaded(UploadParty.DWP)
                .dateAdded(LocalDate.now())
                .rip1Document(DocumentLink.builder().documentFilename("rip1.pdf").documentUrl("rip1.com").documentBinaryUrl("rip1.com/binary").build())
                .build();

        sscsCaseData.setSelectedAudioVideoEvidenceDetails(evidenceDetails);

        final List<AudioVideoEvidence> videoList = new ArrayList<>(singletonList(AudioVideoEvidence.builder().value(evidenceDetails).build()));

        sscsCaseData.setAudioVideoEvidence(videoList);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDocumentStaging().getPreviewDocument()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedBy()).isNull();
        assertThat(response.getData().getDocumentGeneration().getSignedRole()).isNull();
        assertThat(response.getData().getDocumentGeneration().getGenerateNotice()).isNull();
        assertThat(response.getData().getDocumentStaging().getDateAdded()).isNull();

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE), any(), any(), eq(null), eq(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED));
        verify(footerService).addFooter(DocumentLink.builder().documentFilename("RIP 1 document uploaded on " + LocalDate.now() + ".pdf").documentUrl("rip1.com").documentBinaryUrl("rip1.com/binary").build(), "RIP 1 document", "A");

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.WELSH_TRANSLATION);
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.NONE);
        assertThat(response.getData().getDwpState()).isEqualTo(DwpState.DIRECTION_ACTION_REQUIRED);
        assertThat(response.getData().getAudioVideoEvidence()).isNull();
        assertThat(response.getData().getDwpDocuments()).hasSize(1);
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getAvDocumentLink()).isEqualTo(DocumentLink.builder().documentFilename("video.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build());
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getDateApproved()).isEqualTo(LocalDate.now().toString());
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getDocumentDateAdded()).isEqualTo(LocalDate.now().toString());
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getDocumentFileName()).isEqualTo("video.mp4");
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getDocumentType()).isEqualTo("videoDocument");
        assertThat(response.getData().getDwpDocuments().getFirst().getValue().getPartyUploaded().getLabel()).isEqualTo("DWP");
        // RIP1
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentTranslationStatus()).isEqualTo(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED);
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getBundleAddition()).isEqualTo("A");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentType()).isEqualTo(RIP1.getValue());
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentLink().getDocumentFilename()).isEqualTo("New doc with footer");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentFileName()).isEqualTo("Addition A - DWP - RIP 1 document for A/V file: video.mp4");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getPartyUploaded().getLabel()).isEqualTo("DWP");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "ADMIT_EVIDENCE, AWAITING_INFORMATION, AWAITING_INFORMATION",
        "ADMIT_EVIDENCE, REVIEW_BY_JUDGE, REVIEW_BY_JUDGE",
        "ADMIT_EVIDENCE, AWAITING_ADMIN_ACTION, AWAITING_ADMIN_ACTION",
        "SEND_TO_ADMIN, CLEAR_INTERLOC_REVIEW_STATE, null",
        "SEND_TO_ADMIN, null, AWAITING_ADMIN_ACTION"
    }, nullValues = "null")
    void givenProcessAudioVideoReviewStateSelected_overrideTheInterlocReviewState(final ProcessAudioVideoActionDynamicListItems action,
                                                                                   final ProcessAudioVideoReviewState overrideState,
                                                                                   final InterlocReviewState finalState) {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(action.getCode()));
        sscsCaseData.setProcessAudioVideoReviewState(overrideState);

        final AudioVideoEvidenceDetails evidenceDetails = AudioVideoEvidenceDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("video.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                .fileName("video.mp4")
                .partyUploaded(UploadParty.DWP)
                .dateAdded(LocalDate.now())
                .rip1Document(DocumentLink.builder().documentFilename("rip1.pdf").documentUrl("rip1.com").documentBinaryUrl("rip1.com/binary").build())
                .build();

        sscsCaseData.setSelectedAudioVideoEvidenceDetails(evidenceDetails);

        final List<AudioVideoEvidence> videoList = new ArrayList<>(singletonList(AudioVideoEvidence.builder().value(evidenceDetails).build()));

        sscsCaseData.setAudioVideoEvidence(videoList);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getProcessAudioVideoReviewState()).isNull();
        if (finalState == null) {
            assertThat(response.getData().getInterlocReviewState()).isNull();
        } else {
            assertThat(response.getData().getInterlocReviewState()).isEqualTo(finalState);
        }
    }

    @Test
    void givenAdmitEvidence_willCopyOtherPartyFieldsToSscsDocuments() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ADMIT_EVIDENCE.getCode()));
        sscsCaseData.getAudioVideoEvidence().getFirst().getValue().setOriginalSenderOtherPartyId("1");
        sscsCaseData.getAudioVideoEvidence().getFirst().getValue().setOriginalSenderOtherPartyName("Other Party");
        sscsCaseData.setSelectedAudioVideoEvidenceDetails(sscsCaseData.getAudioVideoEvidence().getFirst().getValue());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentFileName()).isEqualTo("Addition A - Appellant - Statement for A/V file: music.mp3");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentType()).isEqualTo(DocumentType.AUDIO_DOCUMENT.getValue());
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getPartyUploaded().getLabel()).isEqualTo("Appellant");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentLink().getDocumentFilename()).isEqualTo("New doc with footer");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getOriginalSenderOtherPartyName()).isEqualTo("Other Party");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getOriginalSenderOtherPartyId()).isEqualTo("1");
        assertThat(response.getData().getHasUnprocessedAudioVideoEvidence()).isEqualTo(YesNo.YES);
    }
}
