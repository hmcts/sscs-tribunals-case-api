package uk.gov.hmcts.reform.sscs.ccd.presubmit.supplementaryresponse;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;

import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentSubtype;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;

@RunWith(JUnitParamsRunner.class)
public class SupplementaryResponseAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private SupplementaryResponseAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new SupplementaryResponseAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.DWP_SUPPLEMENTARY_RESPONSE);

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED"})
    public void givenANonSupplementaryResponseEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenASupplementaryResponseEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenASupplementaryResponseWithDwpSupplementaryResponseDoc_thenMoveToScannedDocsList() {
        sscsCaseData.setDwpSupplementaryResponseDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test.doc").documentUrl("myurl").build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getScannedDocuments().size());
        assertEquals("test.doc", response.getData().getScannedDocuments().get(0).getValue().getFileName());
        assertEquals("myurl", response.getData().getScannedDocuments().get(0).getValue().getUrl().getDocumentUrl());
        assertEquals("other", response.getData().getScannedDocuments().get(0).getValue().getType());
        assertEquals(DocumentSubtype.DWP_EVIDENCE.getValue(), response.getData().getScannedDocuments().get(0).getValue().getSubtype());

        assertEquals("supplementaryResponse", response.getData().getDwpState());
        assertEquals(NO, response.getData().getEvidenceHandled());
        assertNull(response.getData().getDwpSupplementaryResponseDoc());
    }

    @Test
    public void givenASupplementaryResponseWithDwpOtherDoc_thenMoveToScannedDocsList() {
        sscsCaseData.setDwpOtherDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test.doc").documentUrl("myurl").build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getScannedDocuments().size());
        assertEquals("test.doc", response.getData().getScannedDocuments().get(0).getValue().getFileName());
        assertEquals("myurl", response.getData().getScannedDocuments().get(0).getValue().getUrl().getDocumentUrl());
        assertEquals("other", response.getData().getScannedDocuments().get(0).getValue().getType());
        assertEquals(DocumentSubtype.DWP_EVIDENCE.getValue(), response.getData().getScannedDocuments().get(0).getValue().getSubtype());

        assertEquals("supplementaryResponse", response.getData().getDwpState());
        assertEquals(NO, response.getData().getEvidenceHandled());
        assertNull(response.getData().getDwpOtherDoc());
    }

    @Test
    public void givenASupplementaryResponseWithEmptyDocs_thenHandleRequestAndShowError() {
        sscsCaseData.setDwpOtherDoc(DwpResponseDocument.builder().documentLink(null).build());
        sscsCaseData.setDwpSupplementaryResponseDoc(DwpResponseDocument.builder().documentLink(null).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getScannedDocuments());
        assertEquals("Supplementary response document cannot be empty", response.getErrors().toArray()[0]);
    }

    @Test
    public void givenASupplementaryResponseWithDwpSupplementaryResponseDocAndDwpOtherDoc_thenMoveBothToScannedDocsList() {
        sscsCaseData.setDwpSupplementaryResponseDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test1.doc").documentUrl("myurl1").build()).build());
        sscsCaseData.setDwpOtherDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test2.doc").documentUrl("myurl2").build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(2, response.getData().getScannedDocuments().size());
        assertEquals("test1.doc", response.getData().getScannedDocuments().get(0).getValue().getFileName());
        assertEquals("myurl1", response.getData().getScannedDocuments().get(0).getValue().getUrl().getDocumentUrl());
        assertEquals("other", response.getData().getScannedDocuments().get(0).getValue().getType());
        assertEquals(DocumentSubtype.DWP_EVIDENCE.getValue(), response.getData().getScannedDocuments().get(0).getValue().getSubtype());
        assertEquals("test2.doc", response.getData().getScannedDocuments().get(1).getValue().getFileName());
        assertEquals("myurl2", response.getData().getScannedDocuments().get(1).getValue().getUrl().getDocumentUrl());
        assertEquals("other", response.getData().getScannedDocuments().get(1).getValue().getType());
        assertEquals(DocumentSubtype.DWP_EVIDENCE.getValue(), response.getData().getScannedDocuments().get(0).getValue().getSubtype());

        assertEquals("supplementaryResponse", response.getData().getDwpState());
        assertEquals(NO, response.getData().getEvidenceHandled());
        assertNull(response.getData().getDwpOtherDoc());
    }

    @Test
    public void givenASupplementaryResponseWithExistingScannedDocsAndDwpSupplementaryResponseDocAndDwpOtherDoc_thenMoveBothToScannedDocsListWithExistingDocs() {
        sscsCaseData.setDwpSupplementaryResponseDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test1.doc").documentUrl("myurl1").build()).build());
        sscsCaseData.setDwpOtherDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test2.doc").documentUrl("myurl2").build()).build());

        List<ScannedDocument> scannedDocuments = new ArrayList<>();
        scannedDocuments.add(ScannedDocument.builder().value(ScannedDocumentDetails.builder().fileName("existingFile").build()).build());
        sscsCaseData.setScannedDocuments(scannedDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(3, response.getData().getScannedDocuments().size());
        assertEquals("existingFile", response.getData().getScannedDocuments().get(0).getValue().getFileName());
        assertEquals("test1.doc", response.getData().getScannedDocuments().get(1).getValue().getFileName());
        assertEquals("myurl1", response.getData().getScannedDocuments().get(1).getValue().getUrl().getDocumentUrl());
        assertEquals("other", response.getData().getScannedDocuments().get(1).getValue().getType());
        assertEquals(DocumentSubtype.DWP_EVIDENCE.getValue(), response.getData().getScannedDocuments().get(1).getValue().getSubtype());
        assertEquals("test2.doc", response.getData().getScannedDocuments().get(2).getValue().getFileName());
        assertEquals("myurl2", response.getData().getScannedDocuments().get(2).getValue().getUrl().getDocumentUrl());
        assertEquals("other", response.getData().getScannedDocuments().get(2).getValue().getType());
        assertEquals(DocumentSubtype.DWP_EVIDENCE.getValue(), response.getData().getScannedDocuments().get(2).getValue().getSubtype());

        assertEquals("supplementaryResponse", response.getData().getDwpState());
        assertEquals(NO, response.getData().getEvidenceHandled());
        assertNull(response.getData().getDwpOtherDoc());
    }

    @Test
    public void givenASupplementaryResponseWithDwpSupplementaryResponseDocAndAudioVideoDwpOtherDoc_thenMoveOtherDocToAudioVideoEvidenceList() {
        sscsCaseData.setDwpSupplementaryResponseDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test1.doc").documentUrl("myurl1").build()).build());
        sscsCaseData.setDwpOtherDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test2.mp3").documentUrl("myurl2").build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getScannedDocuments().size());
        assertEquals("test1.doc", response.getData().getScannedDocuments().get(0).getValue().getFileName());
        assertEquals("myurl1", response.getData().getScannedDocuments().get(0).getValue().getUrl().getDocumentUrl());
        assertEquals("other", response.getData().getScannedDocuments().get(0).getValue().getType());
        assertEquals(DocumentSubtype.DWP_EVIDENCE.getValue(), response.getData().getScannedDocuments().get(0).getValue().getSubtype());
        assertEquals(1, response.getData().getAudioVideoEvidence().size());
        assertEquals("test2.mp3", response.getData().getAudioVideoEvidence().get(0).getValue().getFileName());
        assertEquals("myurl2", response.getData().getAudioVideoEvidence().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals(UploadParty.DWP, response.getData().getAudioVideoEvidence().get(0).getValue().getPartyUploaded());

        assertEquals("supplementaryResponse", response.getData().getDwpState());
        assertEquals(InterlocReviewState.REVIEW_BY_TCW.getId(), response.getData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE.getId(), response.getData().getInterlocReferralReason());
        assertEquals(NO, response.getData().getEvidenceHandled());
        assertNull(response.getData().getDwpOtherDoc());
        assertNull(response.getData().getShowRip1DocPage());
        assertEquals(YES, response.getData().getHasUnprocessedAudioVideoEvidence());
    }

    @Test
    public void givenASupplementaryResponseWithDwpSupplementaryResponseDocAndAudioVideoDwpOtherDocAndRip1Doc_thenMoveOtherDocToAudioVideoEvidenceListAndLinkRip1Doc() {
        sscsCaseData.setDwpSupplementaryResponseDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test1.doc").documentUrl("myurl1").build()).build());
        sscsCaseData.setDwpOtherDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test2.mp3").documentUrl("myurl2").build()).build());
        sscsCaseData.setRip1Doc(DocumentLink.builder().documentFilename("test3.pdf").documentUrl("myurl3").build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getScannedDocuments().size());
        assertEquals("test1.doc", response.getData().getScannedDocuments().get(0).getValue().getFileName());
        assertEquals("myurl1", response.getData().getScannedDocuments().get(0).getValue().getUrl().getDocumentUrl());
        assertEquals("other", response.getData().getScannedDocuments().get(0).getValue().getType());
        assertEquals(DocumentSubtype.DWP_EVIDENCE.getValue(), response.getData().getScannedDocuments().get(0).getValue().getSubtype());
        assertEquals(1, response.getData().getAudioVideoEvidence().size());
        assertEquals("test2.mp3", response.getData().getAudioVideoEvidence().get(0).getValue().getFileName());
        assertEquals("myurl2", response.getData().getAudioVideoEvidence().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("myurl3", response.getData().getAudioVideoEvidence().get(0).getValue().getRip1Document().getDocumentUrl());
        assertEquals(UploadParty.DWP, response.getData().getAudioVideoEvidence().get(0).getValue().getPartyUploaded());

        assertEquals("supplementaryResponse", response.getData().getDwpState());
        assertEquals(InterlocReviewState.REVIEW_BY_TCW.getId(), response.getData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE.getId(), response.getData().getInterlocReferralReason());
        assertEquals(NO, response.getData().getEvidenceHandled());
        assertNull(response.getData().getDwpOtherDoc());
        assertNull(response.getData().getRip1Doc());
        assertNull(response.getData().getShowRip1DocPage());
    }

    @Test
    public void givenASupplementaryResponseAndInterlocReviewStateAlreadyReviewByJudge_thenLeaveAsReviewByJudge() {
        sscsCaseData.setDwpSupplementaryResponseDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test1.doc").documentUrl("myurl1").build()).build());
        sscsCaseData.setDwpOtherDoc(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("test2.mp3").documentUrl("myurl2").build()).build());
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE.getId());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("supplementaryResponse", response.getData().getDwpState());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), response.getData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE.getId(), response.getData().getInterlocReferralReason());
    }
}
