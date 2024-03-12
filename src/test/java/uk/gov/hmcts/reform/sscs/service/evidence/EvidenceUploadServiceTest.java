package uk.gov.hmcts.reform.sscs.service.evidence;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_DOCUMENT;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.Evidence;
import uk.gov.hmcts.reform.sscs.domain.wrapper.EvidenceDescription;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.OnlineHearingService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.service.conversion.FileToPdfConversionService;
import uk.gov.hmcts.reform.sscs.service.exceptions.EvidenceUploadException;
import uk.gov.hmcts.reform.sscs.service.pdf.MyaEventActionContext;
import uk.gov.hmcts.reform.sscs.service.pdf.StoreEvidenceDescriptionService;
import uk.gov.hmcts.reform.sscs.service.pdf.data.EvidenceDescriptionPdfData;
import uk.gov.hmcts.reform.sscs.service.pdf.data.UploadedEvidence;
import uk.gov.hmcts.reform.sscs.thirdparty.documentmanagement.DocumentStoreService;
import uk.gov.hmcts.reform.sscs.util.AddedDocumentsUtil;

@RunWith(JUnitParamsRunner.class)
public class EvidenceUploadServiceTest {

    public static final String HTTP_ANOTHER_URL = "http://anotherUrl";
    private static final String JP_EMAIL = "jp@gmail.com";
    private static final String REP_EMAIL = "rep@gmail.com";
    private static final String APPELLANT_EMAIL = "app@gmail.com";
    private static final String OTHER_PARTY_EMAIL = "op@gmail.com";
    private static final String OTHER_PARTY_REP_EMAIL = "op-rep@gmail.com";
    private static final String OTHER_PARTY_APPOINTEE_EMAIL = "op-appointee@gmail.com";
    private EvidenceUploadService evidenceUploadService;
    private CcdService ccdService;
    private OnlineHearingService onlineHearingService;
    private String someOnlineHearingId;
    private String someQuestionId;
    private long someCcdCaseId;
    private IdamTokens idamTokens;
    private String fileName;
    private String existingFileName;
    private String documentUrl;
    private MultipartFile file;
    private final Date evidenceCreatedOn = new Date();
    private String someEvidenceId;
    private StoreEvidenceDescriptionService storeEvidenceDescriptionService;
    private EvidenceDescription someDescription;
    private EvidenceManagementService evidenceManagementService;
    private PdfStoreService pdfStoreService;
    private IdamService idamService;
    private FileToPdfConversionService fileToPdfConversionService;

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp() {
        ccdService = mock(CcdService.class);
        onlineHearingService = mock(OnlineHearingService.class);
        someOnlineHearingId = "someOnlinehearingId";
        someQuestionId = "someQuestionId";
        someEvidenceId = "someEvidenceId";

        someCcdCaseId = 123L;

        someDescription = new EvidenceDescription("some description", "idamEmail");

        idamService = mock(IdamService.class);
        idamTokens = mock(IdamTokens.class);
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        storeEvidenceDescriptionService = mock(StoreEvidenceDescriptionService.class);
        fileToPdfConversionService = mock(FileToPdfConversionService.class);
        evidenceManagementService = mock(EvidenceManagementService.class);
        pdfStoreService = mock(PdfStoreService.class);

        evidenceUploadService(new AddedDocumentsUtil(false));

        fileName = "someFileName.txt";
        existingFileName = "oldFileName.txt";
        documentUrl = "http://example.com/document/" + someEvidenceId;
        file = mock(MultipartFile.class);

        UploadResponse uploadResponse = createUploadResponse(fileName);
        when(evidenceManagementService.upload(singletonList(file), "sscs")).thenReturn(uploadResponse);
        when(fileToPdfConversionService.convert(singletonList(file))).thenReturn(singletonList(file));
    }

    private void evidenceUploadService(AddedDocumentsUtil addedDocumentsUtil) {
        evidenceUploadService = new EvidenceUploadService(
            mock(DocumentStoreService.class),
            ccdService,
            idamService,
            onlineHearingService,
            storeEvidenceDescriptionService,
            fileToPdfConversionService,
            evidenceManagementService,
            pdfStoreService,
            addedDocumentsUtil);
    }

    @Test
    public void uploadsEvidenceAndAddsItToDraftSscsDocumentsInCcd() throws IOException {
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetails(someQuestionId, fileName, documentUrl, evidenceCreatedOn);
        final int originalNumberOfSscsDocuments = sscsCaseDetails.getData().getDraftSscsDocument().size();
        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearingId)).thenReturn(Optional.of(sscsCaseDetails));

        when(file.getOriginalFilename()).thenReturn(fileName);
        when(file.getBytes()).thenReturn(fileName.getBytes());

        Optional<Evidence> evidenceOptional = evidenceUploadService.uploadDraftEvidence(someOnlineHearingId, file);

        assertThat(evidenceOptional.isPresent(), is(true));
        Evidence evidence = evidenceOptional.get();
        assertThat(evidence, is(new Evidence(documentUrl, fileName, convertCreatedOnDate(evidenceCreatedOn))));

        verify(ccdService).updateCase(
                hasDraftSscsDocument(originalNumberOfSscsDocuments, documentUrl, fileName),
                eq(someCcdCaseId),
                eq("uploadDraftDocument"),
                eq("SSCS - upload document from MYA"),
                eq("Uploaded a further evidence document"),
                eq(idamTokens)
        );
    }

    @Test
    public void uploadsEvidenceWhenThereAreNotAlreadySscsDocumentsInCcd() throws IOException {
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetailsWithoutCcdDocuments();
        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearingId)).thenReturn(Optional.of(sscsCaseDetails));

        when(file.getOriginalFilename()).thenReturn(fileName);
        when(file.getBytes()).thenReturn(fileName.getBytes());
        Optional<Evidence> evidenceOptional = evidenceUploadService.uploadDraftEvidence(someOnlineHearingId, file);

        assertThat(evidenceOptional.isPresent(), is(true));
        Evidence evidence = evidenceOptional.get();
        assertThat(evidence, is(new Evidence(documentUrl, fileName, convertCreatedOnDate(evidenceCreatedOn))));
        verify(ccdService).updateCase(
                hasDraftSscsDocument(0, documentUrl, fileName),
                eq(someCcdCaseId),
                eq("uploadDraftDocument"),
                eq("SSCS - upload document from MYA"),
                eq("Uploaded a further evidence document"),
                eq(idamTokens)
        );
    }

    @Test
    public void uploadEvidenceForAHearingThatDoesNotExist() {
        String nonExistentHearingId = "nonExistentHearingId";
        when(onlineHearingService.getCcdCase(nonExistentHearingId)).thenReturn(Optional.empty());

        Optional<Evidence> evidence = evidenceUploadService.uploadDraftEvidence(nonExistentHearingId, file);

        assertThat(evidence.isPresent(), is(false));
    }

    @Test
    public void uploadSingleEvidenceForAHearingThatDoesNotExist() {
        String nonExistentHearingId = "nonExistentHearingId";
        when(onlineHearingService.getCcdCase(nonExistentHearingId)).thenReturn(Optional.empty());

        boolean submittedEvidence = evidenceUploadService.submitSingleHearingEvidence(someOnlineHearingId, someDescription, file);

        assertThat(submittedEvidence, is(false));
    }

    @Test
    @Parameters(method =
            "evidenceUploadByAppellantScenario, evidenceUploadByRepScenario, "
                    + "evidenceUploadByJointPartyScenario, evidenceUploadByAppellantWithOtherSubscribersPresenceScenario")
    public void givenANonCorCaseWithScannedDocumentsAndDraftDocument_thenMoveDraftToScannedDocumentsAndUpdateCaseInCcd(
            SscsCaseDetails sscsCaseDetails, EvidenceDescription someDescription, String expectedEvidenceUploadFilename)
        throws IOException {

        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearingId)).thenReturn(Optional.of(sscsCaseDetails));

        UploadedEvidence evidenceDescriptionPdf = mock(UploadedEvidence.class);
        when(storeEvidenceDescriptionService.storePdf(
                someCcdCaseId,
                someOnlineHearingId,
                new EvidenceDescriptionPdfData(sscsCaseDetails, someDescription,
                        singletonList(existingFileName))
        )).thenReturn(new MyaEventActionContext(evidenceDescriptionPdf, sscsCaseDetails));

        byte[] dummyFileContentInBytes = getDummyFileContentInBytes();
        when(evidenceManagementService.download(any(), eq("sscs"))).thenReturn(dummyFileContentInBytes);
        when(evidenceDescriptionPdf.getContent()).thenReturn(new ByteArrayResource(dummyFileContentInBytes));

        String otherEvidenceDocType = "Other evidence";
        SscsDocument combinedEvidenceDoc = getCombinedEvidenceDoc(expectedEvidenceUploadFilename, otherEvidenceDocType);
        when(pdfStoreService.store(any(), eq(expectedEvidenceUploadFilename), eq(otherEvidenceDocType)))
                .thenReturn(Collections.singletonList(combinedEvidenceDoc));

        boolean submittedEvidence = evidenceUploadService.submitHearingEvidence(someOnlineHearingId, someDescription);

        assertThat(submittedEvidence, is(true));

        verify(ccdService).updateCase(
                and(hasSscsScannedDocumentAndSscsDocuments(expectedEvidenceUploadFilename),
                        doesHaveEmptyDraftSscsDocumentsAndEvidenceHandledFlagEqualToNo()),
                eq(someCcdCaseId),
                eq(UPLOAD_DOCUMENT.getCcdType()),
                eq("SSCS - upload evidence from MYA"),
                eq("Uploaded a further evidence document"),
                eq(idamTokens)
        );
    }

    @Test
    @Parameters(method =
            "evidenceUploadByAppellantScenario, evidenceUploadByRepScenario, "
                    + "evidenceUploadByJointPartyScenario, evidenceUploadByAppellantWithOtherSubscribersPresenceScenario,"
                    + "evidenceUploadByOtherPartyPresenceScenario, evidenceUploadByOtherPartyRepPresenceScenario,"
                    + "evidenceUploadByOtherPartyAppointeePresenceScenario")
    public void givenANonCorCaseWithAudiVidoeSingleScannedDocumentsAndDraftDocument_thenMoveDraftToScannedDocumentsAndUpdateCaseInCcd(
            SscsCaseDetails sscsCaseDetails, EvidenceDescription someDescription, String expectedEvidenceUploadFilename)
            throws IOException {

        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearingId)).thenReturn(Optional.of(sscsCaseDetails));

        UploadedEvidence evidenceDescriptionPdf = mock(UploadedEvidence.class);
        when(storeEvidenceDescriptionService.storePdf(
                someCcdCaseId,
                someOnlineHearingId,
                new EvidenceDescriptionPdfData(sscsCaseDetails, someDescription,
                        List.of(existingFileName, fileName))
        )).thenReturn(new MyaEventActionContext(evidenceDescriptionPdf, sscsCaseDetails));

        when(file.getOriginalFilename()).thenReturn(fileName);
        when(file.getBytes()).thenReturn(fileName.getBytes());

        byte[] dummyFileContentInBytes = getDummyFileContentInBytes();
        when(evidenceManagementService.download(any(), eq("sscs"))).thenReturn(dummyFileContentInBytes);
        when(evidenceDescriptionPdf.getContent()).thenReturn(new ByteArrayResource(dummyFileContentInBytes));

        String otherEvidenceDocType = "Other evidence";
        SscsDocument combinedEvidenceDoc = getCombinedEvidenceDoc(expectedEvidenceUploadFilename, otherEvidenceDocType);
        when(pdfStoreService.store(any(), eq(expectedEvidenceUploadFilename), eq(otherEvidenceDocType)))
                .thenReturn(Collections.singletonList(combinedEvidenceDoc));

        boolean submittedEvidence = evidenceUploadService.submitSingleHearingEvidence(someOnlineHearingId, someDescription, file);

        assertThat(submittedEvidence, is(true));

        verify(ccdService).updateCase(
                and(hasSscsScannedDocumentAndSscsDocuments(expectedEvidenceUploadFilename),
                        doesHaveEmptyDraftSscsDocumentsAndEvidenceHandledFlagEqualToNo(YesNo.NO)),
                eq(someCcdCaseId),
                eq(UPLOAD_DOCUMENT.getCcdType()),
                eq("SSCS - upload evidence from MYA"),
                eq("Uploaded a further evidence document"),
                eq(idamTokens)
        );
    }

    @Test
    @Parameters({".mp3, null, REVIEW_BY_TCW", ".mp3, REVIEW_BY_TCW, REVIEW_BY_TCW", ".mp3, REVIEW_BY_JUDGE, REVIEW_BY_JUDGE", ".mp4, null, REVIEW_BY_TCW", ".mp4, REVIEW_BY_TCW, REVIEW_BY_TCW", ".mp4, REVIEW_BY_JUDGE, REVIEW_BY_JUDGE"})
    public void givenACaseWithScannedDocumentsAndDraftAudioOrVideoDocument_thenMoveDraftToScannedDocumentsAndUpdateCaseInCcdAndSetCorrectInterlocReviewState(String fileExtension,
                                                                                                                                                             @Nullable InterlocReviewState initialInterlocReviewState,
                                                                                                                                                             InterlocReviewState expectedInterlocReviewState) throws IOException {

        initCommonParams("someFileName" + fileExtension);
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetailsWithoutCcdDocuments();
        sscsCaseDetails.getData().setSscsDocument(buildSscsDocumentList());
        sscsCaseDetails.getData().setAppeal(Appeal.builder().hearingType("sya").build());

        if (initialInterlocReviewState != null) {
            sscsCaseDetails.getData().setInterlocReviewState(initialInterlocReviewState);
        }

        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearingId)).thenReturn(Optional.of(sscsCaseDetails));
        List<String> files = new ArrayList<>();
        String avFileName = "someFileName" + fileExtension;
        files.add(avFileName);

        UploadedEvidence evidenceDescriptionPdf = mock(UploadedEvidence.class);
        when(storeEvidenceDescriptionService.storePdf(
                someCcdCaseId,
                someOnlineHearingId,
                new EvidenceDescriptionPdfData(sscsCaseDetails, someDescription, files)
        )).thenReturn(new MyaEventActionContext(evidenceDescriptionPdf, sscsCaseDetails));

        when(file.getOriginalFilename()).thenReturn(avFileName);
        when(file.getBytes()).thenReturn(fileName.getBytes());

        byte[] dummyFileContentInBytes = getDummyFileContentInBytes();
        when(evidenceManagementService.download(any(), eq("sscs"))).thenReturn(dummyFileContentInBytes);
        when(evidenceDescriptionPdf.getContent()).thenReturn(new ByteArrayResource(dummyFileContentInBytes));

        UploadResponse uploadResponse = createUploadResponse(avFileName);
        when(evidenceManagementService.upload(singletonList(file), "sscs")).thenReturn(uploadResponse);

        String otherEvidenceDocType = "Other evidence";
        String expectedEvidenceUploadFilename =  "Appellant upload 1 - 123.pdf";

        SscsDocument combinedEvidenceDoc = getCombinedEvidenceDoc(expectedEvidenceUploadFilename, otherEvidenceDocType);
        when(pdfStoreService.store(any(), eq(expectedEvidenceUploadFilename), eq(otherEvidenceDocType)))
                .thenReturn(Collections.singletonList(combinedEvidenceDoc));

        boolean submittedEvidence = evidenceUploadService.submitSingleHearingEvidence(someOnlineHearingId, someDescription, file);

        assertThat(submittedEvidence, is(true));

        verify(ccdService).updateCase(
                and(and(and(and(hasAudioVideoDocumentAndSscsDocuments(avFileName, "http://dm-store/112"),
                        doesHaveEmptyDraftSscsDocumentsAndEvidenceHandledFlagEqualToNo()),
                        argThat(argument -> argument.getInterlocReviewState().equals(expectedInterlocReviewState))),
                        argThat(argument ->  argument.getInterlocReferralReason().equals(InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE))),
                        argThat(argument ->  argument.getHasUnprocessedAudioVideoEvidence().equals(YesNo.YES))),
                eq(someCcdCaseId),
                eq(UPLOAD_DOCUMENT.getCcdType()),
                eq("SSCS - upload evidence from MYA"),
                eq("Uploaded a further evidence document"),
                eq(idamTokens)
        );
    }

    @Test
    public void deleteEvidenceFromCcd() {
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetails(someQuestionId, fileName, documentUrl, evidenceCreatedOn);
        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearingId)).thenReturn(Optional.of(sscsCaseDetails));

        boolean hearingFound = evidenceUploadService.deleteDraftEvidence(someOnlineHearingId, someEvidenceId);

        assertThat(hearingFound, is(true));
        verify(ccdService).updateCase(
                doesNotHaveDraftSscsDocuments(),
                eq(someCcdCaseId),
                eq("uploadDraftDocument"),
                eq("SSCS - evidence deleted"),
                eq("Uploaded a draft evidence deleted"),
                eq(idamTokens)
        );
    }

    @Test
    public void deleteEvidenceIfCaseHadNoEvidence() {
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetailsWithoutCcdDocuments();
        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearingId)).thenReturn(Optional.of(sscsCaseDetails));

        boolean hearingFound = evidenceUploadService.deleteDraftEvidence(someOnlineHearingId, someEvidenceId);

        assertThat(hearingFound, is(true));
        verify(ccdService, never()).updateCase(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void deleteEvidenceForAHearingThatDoesNotExist() {
        String nonExistentHearingId = "nonExistentHearingId";
        when(onlineHearingService.getCcdCase(nonExistentHearingId)).thenReturn(Optional.empty());

        boolean hearingFound = evidenceUploadService.deleteDraftEvidence(nonExistentHearingId, someEvidenceId);

        assertThat(hearingFound, is(false));
    }

    @Test
    public void throwsOnNonLoadablePdf() {

        byte[] badBytes = "notaPdf".getBytes();
        String docType = "statement";
        String caseId = "1234";

        Exception exception = assertThrows(EvidenceUploadException.class, () ->
            EvidenceUploadService.getLoadSafe(badBytes, docType, caseId));

        assertTrue(exception.getMessage().contains("Error when getting PDDocument " + docType
                + " for caseId " + caseId + " with bytes length " + badBytes.length));
    }

    @Test
    public void testRemoveAudioFromList() {
        List<SscsDocument> draftDocuments = new ArrayList<>();
        draftDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("audio1.mp3").build()).build());
        draftDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("audio2.mp3").build()).build());
        draftDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("word1.docx").build()).build());

        assertEquals(2, evidenceUploadService.pullAudioVideoFilesFromDraft(draftDocuments).size());
        assertEquals(1, draftDocuments.size());
    }

    @Test
    public void testRemoveAudioVideoFromList() {
        List<SscsDocument> draftDocuments = new ArrayList<>();
        draftDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("word1.docx").build()).build());
        draftDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("audio1.mp3").build()).build());
        draftDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("audio2.mp3").build()).build());
        draftDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("word2.docx").build()).build());
        draftDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("video1.mp4").build()).build());

        assertEquals(3, evidenceUploadService.pullAudioVideoFilesFromDraft(draftDocuments).size());
        assertEquals(2, draftDocuments.size());
    }

    @Test
    public void testDontRemoveNullFromList() {
        List<SscsDocument> draftDocuments = new ArrayList<>();
        draftDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("audio1.mp3").build()).build());
        draftDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        draftDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("word1.docx").build()).build());

        assertEquals(1, evidenceUploadService.pullAudioVideoFilesFromDraft(draftDocuments).size());
        assertEquals(2, draftDocuments.size());
    }

    @Test
    public void testNoAudioInList() {
        List<SscsDocument> draftDocuments = new ArrayList<>();
        draftDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("text.txt").build()).build());
        draftDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("word1.docx").build()).build());

        assertEquals(0, evidenceUploadService.pullAudioVideoFilesFromDraft(draftDocuments).size());
        assertEquals(2, draftDocuments.size());
    }

    @Test
    public void testBuildScannedDocumentByGivenSscsDoc() {
        SscsCaseData sscsCaseData = createSscsCaseDetailsDraftDocsJustDescription("EvidenceDescription.pdf").getData();
        sscsCaseData.setOtherParties(List.of(new CcdValue<>(OtherParty.builder()
                .id("1")
                .name(Name.builder().firstName("John").lastName("Smith").build())
                .otherPartySubscription(Subscription.builder().email("op@email.com").build())
                .build())));
        SscsDocument evidenceDescriptionDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentDateAdded("2021-01-30")
                .documentLink(DocumentLink.builder().documentBinaryUrl("url/binary").documentFilename("description.pdf").documentUrl("url").build()).build()).build();

        evidenceUploadService.buildUploadedDocumentByGivenSscsDoc(sscsCaseData, evidenceDescriptionDocument, null, "op@email.com");
        assertEquals(1, sscsCaseData.getScannedDocuments().size());
        assertThat(sscsCaseData.getScannedDocuments().get(0).getValue().getOriginalSenderOtherPartyId(), is("1"));
        assertThat(sscsCaseData.getScannedDocuments().get(0).getValue().getOriginalSenderOtherPartyName(), is("John Smith"));
        assertNull(sscsCaseData.getAudioVideoEvidence());
    }

    @Test
    public void testBuildScannedDocumentByGivenSscsDocWithAvEvidenceAndPdf() {
        //This should never happen - but just in case should treat as just an AV evidence upload and ignore the pdf
        SscsCaseData sscsCaseData = createSscsCaseDetailsDraftDocsJustDescription("EvidenceDescription.pdf").getData();
        List<SscsDocument> audioVideoDocuments = new ArrayList<>();

        SscsDocument evidenceDescriptionDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentDateAdded("2021-01-30")
                .documentLink(DocumentLink.builder().documentBinaryUrl("url/binary").documentFilename("description.pdf").documentUrl("url").build()).build()).build();

        SscsDocument draftSscsAudioDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentDateAdded("2021-01-30")
                .documentLink(DocumentLink.builder().documentBinaryUrl("url/binary").documentFilename("audio.mp3").documentUrl("url").build()).build()).build();
        SscsDocument draftSscsVideoDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentDateAdded("2021-01-30")
                .documentLink(DocumentLink.builder().documentBinaryUrl("url/binary").documentFilename("video.mp4").documentUrl("url").build()).build()).build();

        audioVideoDocuments.add(draftSscsAudioDocument);
        audioVideoDocuments.add(draftSscsVideoDocument);

        evidenceUploadService.buildUploadedDocumentByGivenSscsDoc(sscsCaseData, evidenceDescriptionDocument, audioVideoDocuments, null);
        assertNull(sscsCaseData.getScannedDocuments());
        assertEquals(2, sscsCaseData.getAudioVideoEvidence().size());
    }

    @Test
    @Parameters({".mp3, audioDocument", ".mp4, videoDocument"})
    public void givenUploadWithAvEvidence_shouldInsertIntoAddedDocumentsMap(String fileExtension, String documentType) throws IOException {
        evidenceUploadService(new AddedDocumentsUtil(true));

        SscsCaseDetails sscsCaseDetails = createSscsCaseDetailsWithoutCcdDocuments();
        sscsCaseDetails.getData().setSscsDocument(buildSscsDocumentList());
        sscsCaseDetails.getData().setAppeal(Appeal.builder().hearingType("sya").build());

        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearingId)).thenReturn(Optional.of(sscsCaseDetails));
        List<String> files = new ArrayList<>();
        String avFileName = "someFileName" + fileExtension;
        files.add(avFileName);

        UploadedEvidence evidenceDescriptionPdf = mock(UploadedEvidence.class);
        when(storeEvidenceDescriptionService.storePdf(
            someCcdCaseId,
            someOnlineHearingId,
            new EvidenceDescriptionPdfData(sscsCaseDetails, someDescription, files)
        )).thenReturn(new MyaEventActionContext(evidenceDescriptionPdf, sscsCaseDetails));

        when(file.getOriginalFilename()).thenReturn(avFileName);
        when(file.getBytes()).thenReturn(fileName.getBytes());
        byte[] dummyFileContentInBytes = getDummyFileContentInBytes();
        when(evidenceManagementService.download(any(), eq("sscs"))).thenReturn(dummyFileContentInBytes);
        when(evidenceDescriptionPdf.getContent()).thenReturn(new ByteArrayResource(dummyFileContentInBytes));

        UploadResponse uploadResponse = createUploadResponse(avFileName);
        when(evidenceManagementService.upload(singletonList(file), "sscs")).thenReturn(uploadResponse);

        String otherEvidenceDocType = "Other evidence";
        String expectedEvidenceUploadFilename = "Appellant upload 1 - 123.pdf";

        SscsDocument combinedEvidenceDoc = getCombinedEvidenceDoc(expectedEvidenceUploadFilename, otherEvidenceDocType);
        when(pdfStoreService.store(any(), eq(expectedEvidenceUploadFilename), eq(otherEvidenceDocType)))
            .thenReturn(Collections.singletonList(combinedEvidenceDoc));

        evidenceUploadService.submitSingleHearingEvidence(someOnlineHearingId, someDescription, file);

        Map<String, Integer> addedDocuments = new ObjectMapper().readerFor(Map.class)
            .readValue(sscsCaseDetails.getData().getWorkAllocationFields().getAddedDocuments());

        org.assertj.core.api.Assertions.assertThat(addedDocuments)
            .as("One piece of audio or video evidence each have been added, this should be reflected in "
                + "the map. Non a/v uploads should be ignored.")
            .containsOnly(org.assertj.core.api.Assertions.entry(documentType, 1));
    }

    @Test
    public void givenUploadWithNoAvEvidence_shouldClearDocumentsMap() throws IOException {
        evidenceUploadService(new AddedDocumentsUtil(true));

        SscsCaseDetails sscsCaseDetails = createSscsCaseDetailsWithoutCcdDocuments();
        sscsCaseDetails.getData().setSscsDocument(buildSscsDocumentList());
        sscsCaseDetails.getData().setAppeal(Appeal.builder().hearingType("sya").build());
        sscsCaseDetails.getData().setWorkAllocationFields(WorkAllocationFields.builder()
            .addedDocuments("{audioEvidence=1}")
            .build());

        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearingId)).thenReturn(Optional.of(sscsCaseDetails));
        List<String> files = new ArrayList<>();
        String pdfFile = "someFileName.pdf";
        files.add(pdfFile);

        UploadedEvidence evidenceDescriptionPdf = mock(UploadedEvidence.class);
        when(storeEvidenceDescriptionService.storePdf(
            someCcdCaseId,
            someOnlineHearingId,
            new EvidenceDescriptionPdfData(sscsCaseDetails, someDescription, files)
        )).thenReturn(new MyaEventActionContext(evidenceDescriptionPdf, sscsCaseDetails));

        when(file.getOriginalFilename()).thenReturn(pdfFile);
        when(file.getBytes()).thenReturn(fileName.getBytes());
        byte[] dummyFileContentInBytes = getDummyFileContentInBytes();
        when(evidenceManagementService.download(any(), eq("sscs"))).thenReturn(dummyFileContentInBytes);
        when(evidenceDescriptionPdf.getContent()).thenReturn(new ByteArrayResource(dummyFileContentInBytes));

        UploadResponse uploadResponse = createUploadResponse(pdfFile);
        when(evidenceManagementService.upload(singletonList(file), "sscs")).thenReturn(uploadResponse);

        String otherEvidenceDocType = "Other evidence";
        String expectedEvidenceUploadFilename = "Appellant upload 1 - 123.pdf";

        SscsDocument combinedEvidenceDoc = getCombinedEvidenceDoc(expectedEvidenceUploadFilename, otherEvidenceDocType);
        when(pdfStoreService.store(any(), eq(expectedEvidenceUploadFilename), eq(otherEvidenceDocType)))
            .thenReturn(Collections.singletonList(combinedEvidenceDoc));

        evidenceUploadService.submitSingleHearingEvidence(someOnlineHearingId, someDescription, file);

        org.assertj.core.api.Assertions.assertThat(sscsCaseDetails.getData().getWorkAllocationFields().getAddedDocuments())
            .as("Added documents should be cleared each event.")
            .isNull();
    }

    @Test
    public void handlesBadFileRead() throws IOException {
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetailsWithoutCcdDocuments();
        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearingId)).thenReturn(Optional.of(sscsCaseDetails));

        when(file.getOriginalFilename()).thenReturn(fileName);
        when(file.getBytes()).thenThrow(new IOException());
        Optional<Evidence> evidenceOptional = evidenceUploadService.uploadDraftEvidence(someOnlineHearingId, file);

        assertThat(evidenceOptional.isPresent(), is(true));
        Evidence evidence = evidenceOptional.get();
        assertThat(evidence, is(new Evidence(documentUrl, fileName, convertCreatedOnDate(evidenceCreatedOn))));
    }

    private SscsCaseDetails createSscsCaseDetailsDraftDocsJustDescription(String fileName) {
        return SscsCaseDetails.builder()
                .id(someCcdCaseId)
                .data(SscsCaseData.builder()
                        .draftSscsDocument(singletonList(SscsDocument.builder()
                                .value(SscsDocumentDetails.builder()
                                        .documentFileName(fileName)
                                        .documentLink(DocumentLink.builder()
                                                .documentUrl(documentUrl)
                                                .build())
                                        .documentDateAdded(convertCreatedOnDate(evidenceCreatedOn))
                                        .build())
                                .build()))
                        .build())
                .build();
    }

    @Test
    public void testBuildScannedDocumentByGivenSscsDocNoAudioVideo() {
        SscsCaseData sscsCaseData = createSscsCaseDetailsDraftDocsJustDescription("EvidenceDescription.pdf").getData();
        List<SscsDocument> audioVideoDocuments = new ArrayList<>();

        SscsDocument evidenceDescriptionDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentDateAdded("2021-01-30")
                .documentLink(DocumentLink.builder().documentBinaryUrl("url/binary").documentFilename("description.pdf").documentUrl("url").build()).build()).build();

        evidenceUploadService.buildUploadedDocumentByGivenSscsDoc(sscsCaseData, evidenceDescriptionDocument, audioVideoDocuments, null);
        assertEquals(1, sscsCaseData.getScannedDocuments().size());
        assertNull(sscsCaseData.getAudioVideoEvidence());
    }

    @Test
    @Parameters({
        JP_EMAIL + ", JOINT_PARTY, null, null",
        REP_EMAIL + ", REP, null, null",
        APPELLANT_EMAIL + ", APPELLANT, null, null",
        OTHER_PARTY_EMAIL + ", OTHER_PARTY, 1, Oyster Smith",
        OTHER_PARTY_REP_EMAIL + ", OTHER_PARTY_REP, 2, Raj Smith",
        OTHER_PARTY_APPOINTEE_EMAIL + ", OTHER_PARTY_APPOINTEE, 4, Apple Smith"})
    public void givenSscsDocAndAudio_thenSetTheUploaderFromSubscriptionEmail(String idamEmail, UploadParty uploader,
                                                                             @Nullable String otherPartyId,
                                                                             @Nullable String otherPartyName) {
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetailsWithCcdDocumentsSubscription();
        List<SscsDocument> audioVideoDocuments = new ArrayList<>();
        audioVideoDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentLink(DocumentLink.builder().documentBinaryUrl("url/binary").documentFilename("audio2.mp3")
                .documentUrl("url").build())
            .documentFileName("audio2.mp3").build()).build());
        SscsDocument draftSscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentDateAdded("2021-01-30")
                .documentLink(DocumentLink.builder().documentBinaryUrl("url/binary").documentFilename("coversheet").documentUrl("url").build()).build()).build();

        evidenceUploadService.buildUploadedDocumentByGivenSscsDoc(sscsCaseDetails.getData(), draftSscsDocument, audioVideoDocuments, idamEmail);
        assertEquals(1, sscsCaseDetails.getData().getAudioVideoEvidence().size());
        assertEquals(uploader, sscsCaseDetails.getData().getAudioVideoEvidence().get(0).getValue().getPartyUploaded());
        assertEquals(otherPartyId, sscsCaseDetails.getData().getAudioVideoEvidence().get(0).getValue().getOriginalSenderOtherPartyId());
        assertEquals(otherPartyName, sscsCaseDetails.getData().getAudioVideoEvidence().get(0).getValue().getOriginalSenderOtherPartyName());
    }

    @Test
    public void givenListDraftHearingEvidenceIsEmptyThenShouldReturnEmptyEvidence() {
        String identifier = "12345";
        List<SscsDocument> draftSscsDocument = emptyList();
        SscsCaseDetails caseDetails = SscsCaseDetails.builder().data(SscsCaseData.builder().draftSscsDocument(draftSscsDocument).build()).build();
        when(onlineHearingService.getCcdCaseByIdentifier(identifier)).thenReturn(Optional.of(caseDetails));
        List<Evidence> result = evidenceUploadService.listDraftHearingEvidence(identifier);
        assertTrue(result.isEmpty());
    }

    @Test
    public void givenListDraftHearingEvidenceIsNotEmptyThenShouldReturnListOfEvidence() {
        String identifier = "12345";
        List<SscsDocument> draftSscsDocument = buildSscsDocumentList();
        SscsCaseDetails caseDetails = SscsCaseDetails.builder().data(SscsCaseData.builder().draftSscsDocument(draftSscsDocument).build()).build();
        when(onlineHearingService.getCcdCaseByIdentifier(identifier)).thenReturn(Optional.of(caseDetails));
        List<Evidence> result = evidenceUploadService.listDraftHearingEvidence(identifier);
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        assertEquals(draftSscsDocument.get(0).getValue().getDocumentFileName(), result.get(0).getFileName());
        assertEquals(draftSscsDocument.get(1).getValue().getDocumentFileName(), result.get(1).getFileName());
    }

    private SscsDocument getCombinedEvidenceDoc(String combinedEvidenceFilename, String otherEvidenceDocType) {
        DocumentLink documentLink = DocumentLink.builder().documentUrl("http://dm-store/112").build();
        SscsDocumentDetails sscsDocumentDetails = SscsDocumentDetails.builder()
            .documentFileName(combinedEvidenceFilename)
            .documentDateAdded(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
            .documentLink(documentLink)
            .documentType(otherEvidenceDocType)
            .build();
        return SscsDocument.builder().value(sscsDocumentDetails).build();
    }

    private byte[] getDummyFileContentInBytes() throws IOException {
        File file = new File(Objects.requireNonNull(
                this.getClass().getClassLoader().getResource("dummy.pdf")).getFile());
        assertThat(file.getName(), equalTo("dummy.pdf"));
        return Files.readAllBytes(Paths.get(file.getPath()));
    }

    @SuppressWarnings("unused")
    private Object[] evidenceUploadByAppellantScenario() {
        initCommonParams("someFileName.txt");
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetails(someQuestionId, existingFileName,
            documentUrl, evidenceCreatedOn);
        sscsCaseDetails.getData().setScannedDocuments(getScannedDocuments());
        sscsCaseDetails.getData().setSscsDocument(buildSscsDocumentList());
        sscsCaseDetails.getData().setAppeal(Appeal.builder().hearingType("sya").build());
        return new Object[]{
            new Object[]{sscsCaseDetails, someDescription, "Appellant upload 1 - 123.pdf"}
        };
    }

    @NotNull
    private List<SscsDocument> buildSscsDocumentList() {
        SscsDocument descriptionDocument = buildSscsDocumentGivenFilename(
                "temporal unique Id ec7ae162-9834-46b7-826d-fdc9935e3187 Evidence Description -");
        SscsDocument form1DocWithNoDate = buildSscsDocumentGivenFilename("form1");
        form1DocWithNoDate.getValue().setDocumentDateAdded(null);
        List<SscsDocument> sscsList = new ArrayList<>();
        sscsList.add(descriptionDocument);
        sscsList.add(form1DocWithNoDate);
        return sscsList;
    }

    private SscsDocument buildSscsDocumentGivenFilename(String filename) {
        return SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentFileName(filename)
                        .documentLink(DocumentLink.builder()
                                .documentFilename(filename)
                                .documentUrl(HTTP_ANOTHER_URL)
                                .build())
                        .documentDateAdded(convertCreatedOnDate(evidenceCreatedOn))
                        .build())
                .build();
    }

    @SuppressWarnings("unused")
    private Object[] evidenceUploadByRepScenario() {
        initCommonParams("someFileName.txt");
        SscsCaseDetails sscsCaseDetailsWithRepSubs = createSscsCaseDetails(someQuestionId, existingFileName,
                documentUrl, evidenceCreatedOn);
        sscsCaseDetailsWithRepSubs.getData().setSubscriptions(Subscriptions.builder()
                .representativeSubscription(Subscription.builder()
                        .email("rep@email.com")
                        .build())
                .build());
        sscsCaseDetailsWithRepSubs.getData().setScannedDocuments(getScannedDocuments());
        sscsCaseDetailsWithRepSubs.getData().setSscsDocument(buildSscsDocumentList());
        sscsCaseDetailsWithRepSubs.getData().setAppeal(Appeal.builder().hearingType("sya").build());
        EvidenceDescription someDescriptionWithRepEmail = new EvidenceDescription("some description",
                "rep@email.com");

        return new Object[]{
            new Object[]{sscsCaseDetailsWithRepSubs, someDescriptionWithRepEmail, "Representative upload 1 - 123.pdf"}
        };
    }

    @SuppressWarnings("unused")
    private Object[] evidenceUploadByJointPartyScenario() {
        initCommonParams("someFileName.txt");
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetails(someQuestionId, existingFileName,
                documentUrl, evidenceCreatedOn);
        sscsCaseDetails.getData().setSubscriptions(Subscriptions.builder()
                .jointPartySubscription(Subscription.builder()
                        .email("jp@email.com")
                        .build())
                .build());
        sscsCaseDetails.getData().setScannedDocuments(getScannedDocuments());
        sscsCaseDetails.getData().setSscsDocument(buildSscsDocumentList());
        sscsCaseDetails.getData().setAppeal(Appeal.builder().hearingType("sya").build());

        EvidenceDescription someDescription = new EvidenceDescription("some description",
                "jp@email.com");

        return new Object[]{new Object[]{sscsCaseDetails, someDescription, "Joint party upload 1 - 123.pdf"}};
    }

    @SuppressWarnings("unused")
    private Object[] evidenceUploadByAppellantWithOtherSubscribersPresenceScenario() {
        initCommonParams("someFileName.txt");
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetails(someQuestionId, existingFileName,
                documentUrl, evidenceCreatedOn);
        sscsCaseDetails.getData().setSubscriptions(Subscriptions.builder()
                .jointPartySubscription(Subscription.builder()
                        .email("jp@email.com")
                        .build())
                .representativeSubscription(Subscription.builder()
                        .email("rep@email.com")
                        .build())
                .build());
        sscsCaseDetails.getData().setScannedDocuments(getScannedDocuments());
        sscsCaseDetails.getData().setSscsDocument(buildSscsDocumentList());
        sscsCaseDetails.getData().setAppeal(Appeal.builder().hearingType("sya").build());

        return new Object[]{new Object[]{sscsCaseDetails, someDescription, "Appellant upload 1 - 123.pdf"}};
    }

    @SuppressWarnings("unused")
    private Object[] evidenceUploadByOtherPartyPresenceScenario() {
        initCommonParams("someFileName.txt");
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetails(someQuestionId, existingFileName,
                documentUrl, evidenceCreatedOn);
        sscsCaseDetails.getData().setSubscriptions(Subscriptions.builder()
                .jointPartySubscription(Subscription.builder()
                        .email("jp@email.com")
                        .build())
                .representativeSubscription(Subscription.builder()
                        .email("rep@email.com")
                        .build())
                .build());
        sscsCaseDetails.getData().setOtherParties(List.of(new CcdValue<>(OtherParty.builder()
                .id("1")
                .name(Name.builder().firstName("John").lastName("Smith").build())
                .otherPartySubscription(Subscription.builder().email("op@email.com").build())
                .build())));
        sscsCaseDetails.getData().setScannedDocuments(getScannedDocuments());
        sscsCaseDetails.getData().setSscsDocument(buildSscsDocumentList());
        sscsCaseDetails.getData().setAppeal(Appeal.builder().hearingType("sya").build());
        EvidenceDescription someDescription = new EvidenceDescription("some description",
                "op@email.com");
        return new Object[]{new Object[]{sscsCaseDetails, someDescription, "Other party - John Smith upload 1 - 123.pdf"}};
    }

    @SuppressWarnings("unused")
    private Object[] evidenceUploadByOtherPartyRepPresenceScenario() {
        initCommonParams("someFileName.txt");
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetails(someQuestionId, existingFileName,
                documentUrl, evidenceCreatedOn);
        sscsCaseDetails.getData().setSubscriptions(Subscriptions.builder()
                .jointPartySubscription(Subscription.builder()
                        .email("jp@email.com")
                        .build())
                .representativeSubscription(Subscription.builder()
                        .email("rep@email.com")
                        .build())
                .build());
        sscsCaseDetails.getData().setOtherParties(List.of(new CcdValue<>(OtherParty.builder()
                .id("1")
                .name(Name.builder().firstName("John").lastName("Smith").build())
                .rep(Representative.builder()
                        .id("2")
                        .hasRepresentative(YesNo.YES.getValue())
                        .name(Name.builder().firstName("Myles").lastName("Smith").build())
                        .build())
                .otherPartySubscription(Subscription.builder().email("op@email.com").build())
                .otherPartyRepresentativeSubscription(Subscription.builder().email("opRep@email.com").build())
                .build())));
        sscsCaseDetails.getData().setScannedDocuments(getScannedDocuments());
        sscsCaseDetails.getData().setSscsDocument(buildSscsDocumentList());
        sscsCaseDetails.getData().setAppeal(Appeal.builder().hearingType("sya").build());
        EvidenceDescription someDescription = new EvidenceDescription("some description",
                "opRep@email.com");
        return new Object[]{new Object[]{sscsCaseDetails, someDescription, "Other party - Representative Myles Smith upload 1 - 123.pdf"}};
    }

    @SuppressWarnings("unused")
    private Object[] evidenceUploadByOtherPartyAppointeePresenceScenario() {
        initCommonParams("someFileName.txt");
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetails(someQuestionId, existingFileName,
                documentUrl, evidenceCreatedOn);
        sscsCaseDetails.getData().setSubscriptions(Subscriptions.builder()
                .jointPartySubscription(Subscription.builder()
                        .email("jp@email.com")
                        .build())
                .representativeSubscription(Subscription.builder()
                        .email("rep@email.com")
                        .build())
                .build());
        sscsCaseDetails.getData().setOtherParties(List.of(new CcdValue<>(OtherParty.builder()
                .id("1")
                .name(Name.builder().firstName("John").lastName("Smith").build())
                .isAppointee(YesNo.YES.getValue())
                .appointee(Appointee.builder()
                        .id("2")
                        .name(Name.builder().firstName("Trinity").lastName("Smith").build())
                        .build())
                .otherPartyAppointeeSubscription(Subscription.builder().email("opAppointee@email.com").build())
                .build())));
        sscsCaseDetails.getData().setScannedDocuments(getScannedDocuments());
        sscsCaseDetails.getData().setSscsDocument(buildSscsDocumentList());
        sscsCaseDetails.getData().setAppeal(Appeal.builder().hearingType("sya").build());
        EvidenceDescription someDescription = new EvidenceDescription("some description",
                "opAppointee@email.com");

        return new Object[]{new Object[]{sscsCaseDetails, someDescription, "Other party - Appointee Trinity Smith upload 1 - 123.pdf"}};
    }

    @NotNull
    private List<ScannedDocument> getScannedDocuments() {
        ScannedDocument evidenceDocument = ScannedDocument.builder()
            .value(ScannedDocumentDetails.builder()
                .fileName("anotherFileName")
                .url(DocumentLink.builder()
                    .documentUrl("http://anotherUrl")
                    .build())
                .scannedDate(convertCreatedOnDate(evidenceCreatedOn))
                .build())
            .build();
        return singletonList(evidenceDocument);
    }

    private void initCommonParams(String fileName) {
        someOnlineHearingId = "someOnlinehearingId";
        someQuestionId = "someQuestionId";
        someEvidenceId = "someEvidenceId";
        someCcdCaseId = 123L;
        this.fileName = fileName;
        existingFileName = "oldFileName.txt";
        documentUrl = "http://example.com/document/" + someEvidenceId;
        someDescription = new EvidenceDescription("some description", "idamEmail");
    }

    private UploadResponse createUploadResponse(String fileName) {
        Document document = new Document();
        document.createdOn = evidenceCreatedOn;
        document.links = new Document.Links();
        document.links.self = new Document.Link();
        document.links.self.href = documentUrl;
        document.originalDocumentName = fileName;
        UploadResponse.Embedded embedded = mock(UploadResponse.Embedded.class);
        when(embedded.getDocuments()).thenReturn(singletonList(document));
        UploadResponse uploadResponse = mock(UploadResponse.class);
        when(uploadResponse.getEmbedded()).thenReturn(embedded);
        return uploadResponse;
    }

    private SscsCaseData hasSscsDocument(String documentUrl, String fileName, int expectedNumberOfDocs) {
        return argThat(argument -> {
            List<SscsDocument> sscsDocument = argument.getSscsDocument();
            boolean isExpectedStatementCount = isNumberOfDocsWithGivenNameAsExpected(documentUrl, fileName,
                    sscsDocument);
            boolean isExpectedEvidenceDescCount = isNumberOfDocsWithGivenNameAsExpected(HTTP_ANOTHER_URL,
                    "Evidence Description -", sscsDocument);
            return sscsDocument.size() == expectedNumberOfDocs && isExpectedStatementCount && isExpectedEvidenceDescCount;
        });
    }

    private boolean isNumberOfDocsWithGivenNameAsExpected(String documentUrl, String fileName, List<SscsDocument> sscsDocument) {
        return sscsDocument.stream()
                .filter(doc -> doc.getValue().getDocumentFileName().equals(fileName))
                .filter(doc -> doc.getValue().getDocumentLink().getDocumentUrl().equals(documentUrl))
                .count() == 1;
    }

    private SscsCaseData hasSscsScannedDocumentAndSscsDocuments(String expectedStatementPrefix) {
        return argThat(argument -> checkSscsScannedDocument(expectedStatementPrefix,
                argument.getScannedDocuments()) && checkSscsDocuments(argument.getSscsDocument()));
    }

    private SscsCaseData hasAudioVideoDocumentAndSscsDocuments(String expectedFileName, String expectedStatementPrefix) {
        return argThat(argument -> checkSscsAudioVideoDocument(expectedFileName, expectedStatementPrefix,
                argument.getAudioVideoEvidence()) && checkSscsDocuments(argument.getSscsDocument()));
    }

    private boolean checkSscsDocuments(List<SscsDocument> sscsDocument) {
        boolean isExpectedNumberOfDocs = sscsDocument.size() == 1;
        return isExpectedNumberOfDocs && sscsDocument.get(0).getValue().getDocumentFileName().equals("form1");
    }

    private boolean checkSscsScannedDocument(String expectedStatementPrefix,
                                             List<ScannedDocument> scannedDocuments) {
        boolean isExpectedNumberOfScannedDocs = scannedDocuments.size() == 2;
        boolean isExpectedNumberOfAppellantStatements = scannedDocuments.stream()
                .filter(scannedDocument -> scannedDocument.getValue().getFileName().startsWith(expectedStatementPrefix))
                .count() == 1;
        return  isExpectedNumberOfAppellantStatements && isExpectedNumberOfScannedDocs;
    }

    private boolean checkSscsAudioVideoDocument(String expectedFileName, String expectedStatementPrefix,
                                                List<AudioVideoEvidence> audioVideoEvidences) {
        boolean isExpectedNumberOfAvDocs = audioVideoEvidences.size() == 1;
        boolean isExpectedAvFileName = audioVideoEvidences.get(0).getValue().getFileName().equals(expectedFileName);
        boolean isExpectedNumberOfAppellantStatements = audioVideoEvidences.stream()
                .filter(avDoc -> avDoc.getValue().getStatementOfEvidencePdf().getDocumentUrl().startsWith(expectedStatementPrefix))
                .count() == 1;
        return  isExpectedNumberOfAppellantStatements && isExpectedAvFileName && isExpectedNumberOfAvDocs;
    }

    private SscsCaseData hasDraftSscsDocument(int originalNumberOfDocuments, String documentUrl, String fileName) {
        return argThat(argument -> {
            List<SscsDocument> sscsDocument = argument.getDraftSscsDocument();
            return sscsDocument.size() == originalNumberOfDocuments + 1
                    && sscsDocument.get(originalNumberOfDocuments).getValue().getDocumentLink().getDocumentUrl().equals(documentUrl)
                    && sscsDocument.get(originalNumberOfDocuments).getValue().getDocumentFileName().equals(fileName);
        });
    }

    private SscsCaseData doesNotHaveDraftSscsDocuments() {
        return argThat(argument -> {
            List<SscsDocument> sscsDocument = argument.getDraftSscsDocument();
            return sscsDocument.isEmpty();
        });
    }

    private SscsCaseData doesHaveEmptyDraftSscsDocumentsAndEvidenceHandledFlagEqualToNo() {
        return argThat(argument -> {
            List<SscsDocument> sscsDocument = argument.getDraftSscsDocument();
            return sscsDocument.isEmpty() && argument.getEvidenceHandled().equals("No");
        });
    }

    private SscsCaseData doesHaveEmptyDraftSscsDocumentsAndEvidenceHandledFlagEqualToNo(YesNo hasUnprocessedAudioVideoEvidence) {
        return argThat(argument -> {
            List<SscsDocument> sscsDocument = argument.getDraftSscsDocument();
            return sscsDocument.isEmpty() && argument.getEvidenceHandled().equals("No")
                    && argument.getHasUnprocessedAudioVideoEvidence().equals(hasUnprocessedAudioVideoEvidence);
        });
    }

    private SscsCaseData interlocReviewStateSetToReviewByTcw() {
        return argThat(argument -> argument.getInterlocReviewState().equals(InterlocReviewState.REVIEW_BY_TCW));

    }

    private SscsCaseDetails createSscsCaseDetails(String questionId, String fileName, String documentUrl,
                                                  Date evidenceCreatedOn) {

        List<SscsDocument> docs = new ArrayList<>();
        docs.add(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentFileName(fileName)
                        .documentLink(DocumentLink.builder()
                                .documentUrl(documentUrl)
                                .build())
                        .documentDateAdded(convertCreatedOnDate(evidenceCreatedOn))
                        .build()).build());

        return SscsCaseDetails.builder()
            .id(someCcdCaseId)
            .data(SscsCaseData.builder()
                .draftSscsDocument(docs)
                .build())
            .build();
    }

    private SscsCaseDetails createSscsCaseDetailsWithoutCcdDocuments() {
        return SscsCaseDetails.builder().id(someCcdCaseId).data(SscsCaseData.builder().build()).build();
    }

    private SscsCaseDetails createSscsCaseDetailsWithCcdDocumentsSubscription() {
        return SscsCaseDetails.builder().id(someCcdCaseId).data(SscsCaseData.builder()
                .subscriptions(Subscriptions.builder()
                        .jointPartySubscription(Subscription.builder()
                                .email(JP_EMAIL)
                                .build())
                        .representativeSubscription(Subscription.builder()
                                .email(REP_EMAIL)
                                .build())
                        .appellantSubscription(Subscription.builder()
                                .email(APPELLANT_EMAIL)
                                .build())
                        .build())
                .otherParties(List.of(new CcdValue<>(OtherParty.builder()
                        .id("1")
                        .name(Name.builder().firstName("Oyster").lastName("Smith").build())
                        .otherPartySubscription(Subscription.builder().email(OTHER_PARTY_EMAIL).build())
                        .rep(Representative.builder()
                                .id("2")
                                .name(Name.builder().firstName("Raj").lastName("Smith").build())
                                .hasRepresentative(YesNo.YES.getValue())
                                .build())
                        .otherPartyRepresentativeSubscription(Subscription.builder().email(OTHER_PARTY_REP_EMAIL).build())
                        .build()),
                        new CcdValue<>(OtherParty.builder()
                                .id("3")
                                .name(Name.builder().firstName("Orange").lastName("Smith").build())
                                .isAppointee(YesNo.YES.getValue())
                                .appointee(Appointee.builder()
                                        .id("4")
                                        .name(Name.builder().firstName("Apple").lastName("Smith").build())
                                        .build())
                                .otherPartyAppointeeSubscription(Subscription.builder().email(OTHER_PARTY_APPOINTEE_EMAIL).build())
                                .build())))
                .build())
                .build();
    }

    private String convertCreatedOnDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ISO_DATE);
    }
}
