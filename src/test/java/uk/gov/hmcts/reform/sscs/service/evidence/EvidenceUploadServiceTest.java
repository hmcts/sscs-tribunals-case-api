package uk.gov.hmcts.reform.sscs.service.evidence;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ATTACH_SCANNED_DOCS;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
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
import uk.gov.hmcts.reform.sscs.thirdparty.documentmanagement.DocumentManagementService;

@RunWith(JUnitParamsRunner.class)
public class EvidenceUploadServiceTest {

    public static final String HTTP_ANOTHER_URL = "http://anotherUrl";
    private static final String JP_EMAIL = "jp@gmail.com";
    private static final String REP_EMAIL = "rep@gmail.com";
    private static final String APPELLANT_EMAIL = "app@gmail.com";
    private EvidenceUploadService evidenceUploadService;
    private CcdService ccdService;
    private OnlineHearingService onlineHearingService;
    private String someOnlineHearingId;
    private String someQuestionId;
    private long someCcdCaseId;
    private IdamTokens idamTokens;
    private String fileName;
    private String documentUrl;
    private MultipartFile file;
    private final Date evidenceCreatedOn = new Date();
    private String someEvidenceId;
    private StoreEvidenceDescriptionService storeEvidenceDescriptionService;
    private EvidenceDescription someDescription;
    private EvidenceManagementService evidenceManagementService;
    private PdfStoreService pdfStoreService;

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

        IdamService idamService = mock(IdamService.class);
        idamTokens = mock(IdamTokens.class);
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        storeEvidenceDescriptionService = mock(StoreEvidenceDescriptionService.class);
        FileToPdfConversionService fileToPdfConversionService = mock(FileToPdfConversionService.class);
        evidenceManagementService = mock(EvidenceManagementService.class);
        pdfStoreService = mock(PdfStoreService.class);
        DocumentManagementService documentManagementService = mock(DocumentManagementService.class);

        evidenceUploadService = new EvidenceUploadService(
                documentManagementService,
                ccdService,
                idamService,
                onlineHearingService,
                storeEvidenceDescriptionService,
            fileToPdfConversionService,
            evidenceManagementService,
            pdfStoreService);
        fileName = "someFileName.txt";
        documentUrl = "http://example.com/document/" + someEvidenceId;
        file = mock(MultipartFile.class);

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(singletonList(file), "sscs")).thenReturn(uploadResponse);
        when(fileToPdfConversionService.convert(singletonList(file))).thenReturn(singletonList(file));
    }

    @Test
    public void uploadsEvidenceAndAddsItToDraftSscsDocumentsInCcd() throws IOException {
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetails(someQuestionId, fileName, documentUrl, evidenceCreatedOn);
        final int originalNumberOfSscsDocuments = sscsCaseDetails.getData().getDraftSscsDocument().size();
        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearingId)).thenReturn(Optional.of(sscsCaseDetails));

        when(file.getOriginalFilename()).thenReturn(fileName);
        when(file.getBytes()).thenReturn(fileName.getBytes());

        Optional<Evidence> evidenceOptional = evidenceUploadService.uploadDraftHearingEvidence(someOnlineHearingId, file);

        assertThat(evidenceOptional.isPresent(), is(true));
        Evidence evidence = evidenceOptional.get();
        assertThat(evidence, is(new Evidence(documentUrl, fileName, convertCreatedOnDate(evidenceCreatedOn))));

        verify(ccdService).updateCase(
                hasDraftSscsDocument(originalNumberOfSscsDocuments, documentUrl, fileName),
                eq(someCcdCaseId),
                eq("uploadDraftDocument"),
                eq("SSCS - upload document from MYA"),
                eq("Updated SSCS"),
                eq(idamTokens)
        );
    }

    @Test
    public void uploadsEvidenceWhenThereAreNotAlreadySscsDocumentsInCcd() throws IOException {
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetailsWithoutCcdDocuments();
        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearingId)).thenReturn(Optional.of(sscsCaseDetails));

        when(file.getOriginalFilename()).thenReturn(fileName);
        when(file.getBytes()).thenReturn(fileName.getBytes());
        Optional<Evidence> evidenceOptional = evidenceUploadService.uploadDraftHearingEvidence(someOnlineHearingId, file);

        assertThat(evidenceOptional.isPresent(), is(true));
        Evidence evidence = evidenceOptional.get();
        assertThat(evidence, is(new Evidence(documentUrl, fileName, convertCreatedOnDate(evidenceCreatedOn))));
        verify(ccdService).updateCase(
                hasDraftSscsDocument(0, documentUrl, fileName),
                eq(someCcdCaseId),
                eq("uploadDraftDocument"),
                eq("SSCS - upload document from MYA"),
                eq("Updated SSCS"),
                eq(idamTokens)
        );
    }

    @Test
    public void uploadEvidenceForAHearingThatDoesNotExist() {
        String nonExistentHearingId = "nonExistentHearingId";
        when(onlineHearingService.getCcdCase(nonExistentHearingId)).thenReturn(Optional.empty());

        Optional<Evidence> evidence = evidenceUploadService.uploadDraftHearingEvidence(nonExistentHearingId, file);

        assertThat(evidence.isPresent(), is(false));
    }


    @Test
    @Parameters(method = "evidenceUploadByAppellantScenario, evidenceUploadByRepScenario, "
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
                singletonList("someFileName.txt"))
        )).thenReturn(new MyaEventActionContext(evidenceDescriptionPdf, sscsCaseDetails));

        byte[] dummyFileContentInBytes = getDummyFileContentInBytes();
        when(evidenceManagementService.download(ArgumentMatchers.any(), eq("sscs"))).thenReturn(dummyFileContentInBytes);
        when(evidenceDescriptionPdf.getContent()).thenReturn(new ByteArrayResource(dummyFileContentInBytes));

        String otherEvidenceDocType = "Other evidence";
        SscsDocument combinedEvidenceDoc = getCombinedEvidenceDoc(expectedEvidenceUploadFilename, otherEvidenceDocType);
        when(pdfStoreService.store(ArgumentMatchers.any(), eq(expectedEvidenceUploadFilename), eq(otherEvidenceDocType)))
            .thenReturn(Collections.singletonList(combinedEvidenceDoc));

        boolean submittedEvidence = evidenceUploadService.submitHearingEvidence(someOnlineHearingId, someDescription);

        assertThat(submittedEvidence, is(true));

        verify(ccdService).updateCase(
            and(hasSscsScannedDocumentAndSscsDocuments(expectedEvidenceUploadFilename),
                doesHaveEmptyDraftSscsDocumentsAndEvidenceHandledFlagEqualToNo()),
            eq(someCcdCaseId),
            eq(ATTACH_SCANNED_DOCS.getCcdType()),
            eq("SSCS - upload evidence from MYA"),
            eq("Uploaded a further evidence document"),
            eq(idamTokens)
        );
    }

    @Test
    public void deleteEvidenceFromCcd() {
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetails(someQuestionId, fileName, documentUrl, evidenceCreatedOn);
        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearingId)).thenReturn(Optional.of(sscsCaseDetails));

        boolean hearingFound = evidenceUploadService.deleteDraftHearingEvidence(someOnlineHearingId, someEvidenceId);

        assertThat(hearingFound, is(true));
        verify(ccdService).updateCase(
                doesNotHaveDraftSscsDocuments(),
                eq(someCcdCaseId),
                eq("uploadDraftDocument"),
                eq("SSCS - evidence deleted"),
                eq("Updated SSCS"),
                eq(idamTokens)
        );
    }

    @Test
    public void deleteEvidenceIfCaseHadNoEvidence() {
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetailsWithoutCcdDocuments();
        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearingId)).thenReturn(Optional.of(sscsCaseDetails));

        boolean hearingFound = evidenceUploadService.deleteDraftHearingEvidence(someOnlineHearingId, someEvidenceId);

        assertThat(hearingFound, is(true));
        verify(ccdService, never()).updateCase(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void deleteEvidenceForAHearingThatDoesNotExist() {
        String nonExistentHearingId = "nonExistentHearingId";
        when(onlineHearingService.getCcdCase(nonExistentHearingId)).thenReturn(Optional.empty());

        boolean hearingFound = evidenceUploadService.deleteDraftHearingEvidence(nonExistentHearingId, someEvidenceId);

        assertThat(hearingFound, is(false));
    }

    @Test
    public void throwsOnNonLoadablePdf() {

        byte[] badBytes = "notaPdf".getBytes();
        String docType = "statement";
        String caseId = "1234";

        Exception exception = assertThrows(EvidenceUploadException.class, () -> {
            evidenceUploadService.getLoadSafe(badBytes, docType, caseId);

        });
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
        List<SscsDocument> audioVideoDocuments = new ArrayList<>();

        SscsDocument evidenceDescriptionDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentDateAdded("2021-01-30")
                .documentLink(DocumentLink.builder().documentBinaryUrl("url/binary").documentFilename("description.pdf").documentUrl("url").build()).build()).build();

        SscsDocument draftSscsAudioDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentDateAdded("2021-01-30")
                .documentLink(DocumentLink.builder().documentBinaryUrl("url/binary").documentFilename("audio.mp3").documentUrl("url").build()).build()).build();
        SscsDocument draftSscsVideoDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentDateAdded("2021-01-30")
                .documentLink(DocumentLink.builder().documentBinaryUrl("url/binary").documentFilename("video.mp4").documentUrl("url").build()).build()).build();

        audioVideoDocuments.add(draftSscsAudioDocument);
        audioVideoDocuments.add(draftSscsVideoDocument);

        evidenceUploadService.buildScannedDocumentByGivenSscsDoc(sscsCaseData, evidenceDescriptionDocument, audioVideoDocuments, null);
        assertEquals(1, sscsCaseData.getScannedDocuments().size());
        assertEquals(2, sscsCaseData.getAudioVideoEvidence().size());
    }

    @Test
    public void handlesBadFileRead() throws IOException {
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetailsWithoutCcdDocuments();
        when(onlineHearingService.getCcdCaseByIdentifier(someOnlineHearingId)).thenReturn(Optional.of(sscsCaseDetails));

        when(file.getOriginalFilename()).thenReturn(fileName);
        when(file.getBytes()).thenThrow(new IOException());
        Optional<Evidence> evidenceOptional = evidenceUploadService.uploadDraftHearingEvidence(someOnlineHearingId, file);

        assertThat(evidenceOptional.isPresent(), is(true));
        Evidence evidence = evidenceOptional.get();
        assertThat(evidence, is(new Evidence(documentUrl, fileName, convertCreatedOnDate(evidenceCreatedOn))));
    }

    private SscsCaseDetails createSscsCaseDetailsDraftDocsJustDescription(String fileName) {
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder()
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

        return sscsCaseDetails;
    }

    @Test
    public void testBuildScannedDocumentByGivenSscsDocNoAudioVideo() {
        SscsCaseData sscsCaseData = createSscsCaseDetailsDraftDocsJustDescription("EvidenceDescription.pdf").getData();
        List<SscsDocument> audioVideoDocuments = new ArrayList<>();

        SscsDocument evidenceDescriptionDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentDateAdded("2021-01-30")
                .documentLink(DocumentLink.builder().documentBinaryUrl("url/binary").documentFilename("description.pdf").documentUrl("url").build()).build()).build();

        evidenceUploadService.buildScannedDocumentByGivenSscsDoc(sscsCaseData, evidenceDescriptionDocument, audioVideoDocuments, null);
        assertEquals(1, sscsCaseData.getScannedDocuments().size());
        assertNull(sscsCaseData.getAudioVideoEvidence());
    }

    @Test
    public void testBuildScannedDocumentByGivenSscsDocAndAudio() {
        List<SscsDocument> audioVideoDocuments = new ArrayList<>();
        audioVideoDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("word1.docx").build()).build());
        audioVideoDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("audio2.mp3").build()).build());
        audioVideoDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("word2.docx").build()).build());

        SscsDocument draftSscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentDateAdded("2021-01-30")
                .documentLink(DocumentLink.builder().documentBinaryUrl("url/binary").documentFilename("coversheet").documentUrl("url").build()).build()).build();

        SscsCaseData sscsCaseData = createSscsCaseDetailsDraftDocsJustDescription("EvidenceDescription.pdf").getData();

        evidenceUploadService.buildScannedDocumentByGivenSscsDoc(sscsCaseData, draftSscsDocument, audioVideoDocuments, null);
        assertEquals(3, sscsCaseData.getAudioVideoEvidence().size());
        assertEquals(1, sscsCaseData.getScannedDocuments().size());
    }

    @Test
    @Parameters({JP_EMAIL + ", JOINT_PARTY", REP_EMAIL + ", REP", APPELLANT_EMAIL + ", APPELLANT"})
    public void givenSscsDocAndAudio_thenSetTheUploaderFromSubscriptionEmail(String idamEmail, AudioVideoUploadParty uploader) {
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetailsWithCcdDocumentsSubscription();
        List<SscsDocument> audioVideoDocuments = new ArrayList<>();
        audioVideoDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("audio2.mp3").build()).build());
        SscsDocument draftSscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentDateAdded("2021-01-30")
                .documentLink(DocumentLink.builder().documentBinaryUrl("url/binary").documentFilename("coversheet").documentUrl("url").build()).build()).build();

        evidenceUploadService.buildScannedDocumentByGivenSscsDoc(sscsCaseDetails.getData(), draftSscsDocument, audioVideoDocuments, idamEmail);
        assertEquals(1, sscsCaseDetails.getData().getAudioVideoEvidence().size());
        assertEquals(uploader, sscsCaseDetails.getData().getAudioVideoEvidence().get(0).getValue().getPartyUploaded());
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

    private Object[] evidenceUploadByAppellantScenario() {
        initCommonParams();
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetails(someQuestionId, fileName,
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

    private Object[] evidenceUploadByRepScenario() {
        initCommonParams();
        SscsCaseDetails sscsCaseDetailsWithRepSubs = createSscsCaseDetails(someQuestionId, fileName,
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

    private Object[] evidenceUploadByJointPartyScenario() {
        initCommonParams();
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetails(someQuestionId, fileName,
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

    private Object[] evidenceUploadByAppellantWithOtherSubscribersPresenceScenario() {
        initCommonParams();
        SscsCaseDetails sscsCaseDetails = createSscsCaseDetails(someQuestionId, fileName,
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

    private void initCommonParams() {
        someOnlineHearingId = "someOnlinehearingId";
        someQuestionId = "someQuestionId";
        someEvidenceId = "someEvidenceId";
        someCcdCaseId = 123L;
        fileName = "someFileName.txt";
        documentUrl = "http://example.com/document/" + someEvidenceId;
        someDescription = new EvidenceDescription("some description", "idamEmail");
    }

    private UploadResponse createUploadResponse() {
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

    private SscsCaseData hasDraftCorDocument(int originalNumberOfCorDocuments, String someQuestionId, String documentUrl, String fileName) {
        return argThat(argument -> {
            List<CorDocument> corDocument = argument.getDraftCorDocument();
            return corDocument.size() == originalNumberOfCorDocuments + 1
                    && corDocument.get(originalNumberOfCorDocuments).getValue().getQuestionId().equals(someQuestionId)
                    && corDocument.get(originalNumberOfCorDocuments).getValue().getDocument().getDocumentLink().getDocumentUrl().equals(documentUrl)
                    && corDocument.get(originalNumberOfCorDocuments).getValue().getDocument().getDocumentFileName().equals(fileName);
        });
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

    private SscsCaseData hasDraftSscsDocument(int originalNumberOfDocuments, String documentUrl, String fileName) {
        return argThat(argument -> {
            List<SscsDocument> sscsDocument = argument.getDraftSscsDocument();
            return sscsDocument.size() == originalNumberOfDocuments + 1
                    && sscsDocument.get(originalNumberOfDocuments).getValue().getDocumentLink().getDocumentUrl().equals(documentUrl)
                    && sscsDocument.get(originalNumberOfDocuments).getValue().getDocumentFileName().equals(fileName);
        });
    }

    private SscsCaseData hasCorDocument(int originalNumberOfDocuments, String questionId, String documentUrl, String fileName) {
        return argThat(argument -> {
            List<CorDocument> sscsDocument = argument.getCorDocument();
            return sscsDocument.size() == originalNumberOfDocuments + 1
                    && sscsDocument.get(originalNumberOfDocuments).getValue().getQuestionId().equals(questionId)
                    && sscsDocument.get(originalNumberOfDocuments).getValue().getDocument().getDocumentLink().getDocumentUrl().equals(documentUrl)
                    && sscsDocument.get(originalNumberOfDocuments).getValue().getDocument().getDocumentFileName().equals(fileName);
        });
    }

    private SscsCaseData doesNotHaveDraftCorDocuments() {
        return argThat(argument -> {
            List<CorDocument> corDocument = argument.getDraftCorDocument();
            return corDocument.isEmpty();
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

    private SscsCaseDetails createSscsCaseDetails(String questionId, String fileName, String documentUrl,
                                                  Date evidenceCreatedOn) {
        return SscsCaseDetails.builder()
            .id(someCcdCaseId)
            .data(SscsCaseData.builder()
                .draftCorDocument(singletonList(CorDocument.builder()
                    .value(CorDocumentDetails.builder()
                        .questionId(questionId)
                        .document(SscsDocumentDetails.builder()
                            .documentFileName(fileName)
                            .documentLink(DocumentLink.builder()
                                .documentUrl(documentUrl)
                                .build())
                            .documentDateAdded(convertCreatedOnDate(evidenceCreatedOn))
                            .build())
                        .build())
                    .build()))
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
                .build()).build();
    }

    private String convertCreatedOnDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ISO_DATE);
    }
}
