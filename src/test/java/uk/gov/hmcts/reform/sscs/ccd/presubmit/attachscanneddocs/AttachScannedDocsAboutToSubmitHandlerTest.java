package uk.gov.hmcts.reform.sscs.ccd.presubmit.attachscanneddocs;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class AttachScannedDocsAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private AttachScannedDocsAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new AttachScannedDocsAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.ATTACH_SCANNED_DOCS);
        sscsCaseData = SscsCaseData.builder().createdInGapsFrom("readyToList").appeal(Appeal.builder().build()).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonAttachScannedDocsEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenAnAttachScannedDocsEvent_thenSetEvidenceAndAvFlagsToNo() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.NO.getValue(), response.getData().getEvidenceHandled());
        assertEquals(YesNo.NO, response.getData().getHasUnprocessedAudioVideoEvidence());
    }

    @Test
    public void givenAnAttachScannedDocsEventAndAvEvidenceAlreadyOnCase_thenSetEvidenceFlagToNoAndAvFlagToYes() {
        sscsCaseData.setAudioVideoEvidence(Collections.singletonList(AudioVideoEvidence.builder().value(AudioVideoEvidenceDetails.builder().build()).build()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.NO.getValue(), response.getData().getEvidenceHandled());
        assertEquals(YesNo.YES, response.getData().getHasUnprocessedAudioVideoEvidence());
    }

    @Test
    public void givenAnAttachScannedDocEventHasAnEditedUrl_thenCheckThatEditedUrlAndOtherScannedDocsRemain() {
        ReflectionTestUtils.setField(handler, "deletedRedactedDocEnabled", true);
        sscsCaseData.setScannedDocuments(List.of(ScannedDocument.builder().value(ScannedDocumentDetails.builder().build()).build()));

        ScannedDocument document1 = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .editedUrl(DocumentLink.builder().documentUrl("edited url")
                                .documentBinaryUrl("edited binary url").documentFilename("edited scanned doc").documentHash("hash edited").build())
                        .url(DocumentLink.builder().documentUrl("original url")
                                .documentBinaryUrl("original binary url").documentFilename("original scanned doc").documentHash("hash original").build())
                        .type("Other")
                        .scannedDate("20 Jun 2023")
                        .build()
        ).build();

        ScannedDocument document2 = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .url(DocumentLink.builder().documentUrl("original url")
                                .documentBinaryUrl("original binary url").documentFilename("original scanned doc").documentHash("hash original").build())
                        .type("Other")
                        .scannedDate("20 Jun 2023")
                        .build()
        ).build();

        ScannedDocument document3 = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .editedUrl(DocumentLink.builder().documentUrl("edited url2")
                                .documentBinaryUrl("edited binary url2").documentFilename("edited scanned doc2").documentHash("hash edited2").build())
                        .url(DocumentLink.builder().documentUrl("original url2")
                                .documentBinaryUrl("original binary url2").documentFilename("original scanned doc2").documentHash("hash original2").build())
                        .type("Other")
                        .scannedDate("24 Jun 2023")
                        .build()
        ).build();

        ScannedDocument document4 = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .url(DocumentLink.builder().documentUrl("original url2")
                                .documentBinaryUrl("original binary url2").documentFilename("original scanned doc2").documentHash("hash original2").build())
                        .type("Other")
                        .scannedDate("24 Jun 2023")
                        .build()
        ).build();

        ScannedDocument documentWithoutEditedUrl = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .fileName("test")
                        .url(DocumentLink.builder().documentUrl("test url")
                                .documentBinaryUrl("test binary url").documentFilename("test filename").documentHash("test hash").build())
                        .type("Other")
                        .scannedDate("17 Apr 2023")
                        .build()
        ).build();

        CaseData caseData = SscsCaseData.builder().scannedDocuments(List.of(document2, document4, documentWithoutEditedUrl)).build();
        CaseData caseDataBefore = SscsCaseData.builder().scannedDocuments(List.of(document1,document3, documentWithoutEditedUrl)).build();

        CaseDetails<? extends CaseData> caseDetails = new CaseDetails<>(1L, "", null, caseData, null, null);
        CaseDetails<? extends CaseData> caseDetailsBefore = new CaseDetails<>(1L, "", null, caseDataBefore, null, null);

        Callback<SscsCaseData> testCallback = new Callback(caseDetails, Optional.of(caseDetailsBefore), EventType.ATTACH_SCANNED_DOCS, false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, testCallback, USER_AUTHORISATION);

        assertEquals(List.of(document1, document3, documentWithoutEditedUrl), response.getData().getScannedDocuments());
        assertEquals(document1.getValue().getEditedUrl(), response.getData().getScannedDocuments().get(0).getValue().getEditedUrl());
        assertEquals(document3.getValue().getEditedUrl(), response.getData().getScannedDocuments().get(1).getValue().getEditedUrl());
    }

    @Test
    public void givenNoScannedDocsInitiallyOnCase_thenAssertNotNullForWhenFirstScannedDocIsSubmitted() {
        ReflectionTestUtils.setField(handler, "deletedRedactedDocEnabled", true);

        ScannedDocument firstDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .editedUrl(DocumentLink.builder().documentUrl("edited url")
                                .documentBinaryUrl("edited binary url").documentFilename("edited scanned doc").documentHash("hash edited").build())
                        .url(DocumentLink.builder().documentUrl("original url")
                                .documentBinaryUrl("original binary url").documentFilename("original scanned doc").documentHash("hash original").build())
                        .type("Other")
                        .scannedDate("20 Jun 2023")
                        .build()
        ).build();

        CaseData caseData = SscsCaseData.builder().scannedDocuments(List.of(firstDocument)).build();
        CaseData caseDataBefore = SscsCaseData.builder().scannedDocuments(null).build();

        CaseDetails<? extends CaseData> caseDetails = new CaseDetails<>(1L, "", null, caseData, null, null);
        CaseDetails<? extends CaseData> caseDetailsBefore = new CaseDetails<>(1L, "", null, caseDataBefore, null, null);

        Callback<SscsCaseData> testCallback = new Callback(caseDetails, Optional.of(caseDetailsBefore), EventType.ATTACH_SCANNED_DOCS, false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, testCallback, USER_AUTHORISATION);

        assertNotNull("Scanned document in case data should not be null, after passing through firstDocument", response.getData().getScannedDocuments());
    }

    @Test
    public void givenScannedDocIsInitiallyOnCase_thenAssertThatNoErrorsShowForWhenNoScannedDocIsSubmitted() {
        ReflectionTestUtils.setField(handler, "deletedRedactedDocEnabled", true);

        ScannedDocument initialDocument = ScannedDocument.builder().value(
                ScannedDocumentDetails.builder()
                        .editedUrl(DocumentLink.builder().documentUrl("edited url")
                                .documentBinaryUrl("edited binary url").documentFilename("edited scanned doc").documentHash("hash edited").build())
                        .url(DocumentLink.builder().documentUrl("original url")
                                .documentBinaryUrl("original binary url").documentFilename("original scanned doc").documentHash("hash original").build())
                        .type("Other")
                        .scannedDate("20 Jun 2023")
                        .build()
        ).build();

        CaseData caseData = SscsCaseData.builder().scannedDocuments(null).build();
        CaseData caseDataBefore = SscsCaseData.builder().scannedDocuments(List.of(initialDocument)).build();

        CaseDetails<? extends CaseData> caseDetails = new CaseDetails<>(1L, "", null, caseData, null, null);
        CaseDetails<? extends CaseData> caseDetailsBefore = new CaseDetails<>(1L, "", null, caseDataBefore, null, null);


        Callback<SscsCaseData> testCallback = new Callback(caseDetails, Optional.of(caseDetailsBefore), EventType.ATTACH_SCANNED_DOCS, false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, testCallback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
    }
}