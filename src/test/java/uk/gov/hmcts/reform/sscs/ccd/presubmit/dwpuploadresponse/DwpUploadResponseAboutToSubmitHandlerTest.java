package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_JUDGE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.model.AppConstants;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

@RunWith(JUnitParamsRunner.class)
public class DwpUploadResponseAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private DwpUploadResponseAboutToSubmitHandler dwpUploadResponseAboutToSubmitHandler;
    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private UserDetailsService userDetailsService;

    private DwpDocumentService dwpDocumentService;

    @Before
    public void setUp() {
        openMocks(this);
        dwpDocumentService = new DwpDocumentService();
        AddNoteService addNoteService = new AddNoteService(userDetailsService);
        dwpUploadResponseAboutToSubmitHandler = new DwpUploadResponseAboutToSubmitHandler(dwpDocumentService, addNoteService);

        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
                .forename("Chris").surname("Davis").build().getFullName());

        when(callback.getEvent()).thenReturn(EventType.DWP_UPLOAD_RESPONSE);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1234")
            .benefitCode("002")
            .issueCode("CC")
            .dwpFurtherInfo("Yes")
            .dwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build())
            .dwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build())
            .appeal(Appeal.builder().build())
            .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getId()).thenReturn(Long.valueOf(sscsCaseData.getCcdCaseId()));
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenADwpUploadResponseEvent_thenReturnTrue() {
        assertTrue(dwpUploadResponseAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonDwpUploadResponseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(dwpUploadResponseAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callback));

    }

    @Test
    public void givenADwpUploadResponseEvent_thenSetCaseCode() {
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals("002CC", response.getData().getCaseCode());
        assertEquals(LocalDate.now().toString(), response.getData().getDwpResponseDate());
    }

    @Test
    public void givenADwpUploadResponseEventWithEmptyBenefitCode_displayAnError() {
        callback.getCaseDetails().getCaseData().setBenefitCode(null);
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Benefit code cannot be empty", error);
        }
    }

    @Test
    public void givenADwpUploadResponseEventWithEmptyIssueCode_displayAnError() {
        callback.getCaseDetails().getCaseData().setIssueCode(null);
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Issue code cannot be empty", error);
        }
    }

    @Test
    public void givenADwpUploadResponseEventWithEmptyDwpResponseDoc_displayAnError() {
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(null);
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("DWP response document cannot be empty.", response.getErrors().iterator().next());
    }

    @Test
    public void givenADwpUploadResponseEventWithEmptyDwpEvidenceBundle_displayAnError() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(null);
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("DWP evidence bundle cannot be empty.", response.getErrors().iterator().next());
    }

    @Test
    public void givenADwpUploadResponseEventWithDwpFurtherInfoIsNo_assertNoErrors() {
        callback.getCaseDetails().getCaseData().setDwpFurtherInfo("No");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(NO, response.getData().getHasUnprocessedAudioVideoEvidence());
    }

    @Test
    public void givenADwpUploadResponseEventWithExistingAudioVideoEvidence_assertFlagIsSet() {
        callback.getCaseDetails().getCaseData().setAudioVideoEvidence(List.of(AudioVideoEvidence.builder().build()));
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(YES, response.getData().getHasUnprocessedAudioVideoEvidence());
    }

    @Test
    public void givenADwpUploadResponseEventWithIssueCodeSetToDD_displayAnError() {
        callback.getCaseDetails().getCaseData().setIssueCode("DD");
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Issue code cannot be set to the default value of DD", error);
        }
    }

    @Test
    public void givenADwpUploadResponseEventWithResponseDocumentsAndDwpDocumentsBundleFeatureFlagOn_assertRenameFilenameAndMoveDocumentsToDwpDocumentsCollection() {

        List<DwpDocument> existingDwpDocuments = new ArrayList();

        existingDwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DwpDocumentType.DWP_RESPONSE.getValue()).documentDateAdded(LocalDate.now().minusDays(1).toString()).documentLink(DocumentLink.builder().documentUrl("existing.com").build()).build()).build());
        existingDwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue()).documentDateAdded(LocalDate.now().minusDays(1).toString()).documentLink(DocumentLink.builder().documentUrl("existing2.com").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(existingDwpDocuments);

        callback.getCaseDetails().getCaseData().setDwpAT38Document(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/abcd-0123-xyzabcdefgh/binary")
                                .documentUrl("http://dm-store:5005/documents/abcd-0123-xyzabcdefgh")
                                .documentFilename("testAT38Document.pdf")
                                .build()
                ).build());

        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-5678-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-5678-xyzabcmnop")
                                .documentFilename("testEvidenceBundleDocument.pdf")
                                .build()
                ).build());

        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("phme");
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedEvidenceBundleDocument.pdf")
                                .build()
                ).build());

        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw/binary")
                                .documentUrl("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw")
                                .documentFilename("testResponseDocument.pdf")
                                .build()
                ).build());


        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/efgh-4567-mnopqrstuvw/binary")
                                .documentUrl("http://dm-store:5005/documents/efgh-4567-mnopqrstuvw")
                                .documentFilename("testEditedResponseDocument.pdf")
                                .build()
                ).build());

        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("Edited reason");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        assertAll("DwpUploadResponseDocument fileName modified but URL remains same",
                () -> assertNull(response.getData().getDwpAT38Document()),
                () -> assertNull(response.getData().getDwpEvidenceBundleDocument()),
                () -> assertNull(response.getData().getDwpEditedEvidenceBundleDocument()),
                () -> assertNull(response.getData().getDwpResponseDocument()),
                () -> assertNull(response.getData().getDwpEditedResponseDocument()),
                () -> assertEquals(3, response.getData().getDwpDocuments().size()),
                () -> assertEquals(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue(), response.getData().getDwpDocuments().get(0).getValue().getDocumentType()),
                () -> assertEquals("http://dm-store:5005/documents/defg-5678-xyzabcmnop", response.getData().getDwpDocuments().get(0).getValue().getDocumentLink().getDocumentUrl()),
                () -> assertEquals("http://dm-store:5005/documents/defg-5678-xyzabcmnop/binary", response.getData().getDwpDocuments().get(0).getValue().getDocumentLink().getDocumentBinaryUrl()),
                () -> assertEquals("http://dm-store:5005/documents/defg-6545-xyzabcmnop", response.getData().getDwpDocuments().get(0).getValue().getEditedDocumentLink().getDocumentUrl()),
                () -> assertEquals("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary", response.getData().getDwpDocuments().get(0).getValue().getEditedDocumentLink().getDocumentBinaryUrl()),
                () -> assertEquals(AppConstants.DWP_DOCUMENT_EDITED_EVIDENCE_FILENAME_PREFIX + " on " + todayDate + ".pdf", response.getData().getDwpDocuments().get(0).getValue().getEditedDocumentLink().getDocumentFilename()),
                () -> assertEquals(AppConstants.DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX + " on " + todayDate + ".pdf", response.getData().getDwpDocuments().get(0).getValue().getDocumentLink().getDocumentFilename()),
                () -> assertEquals("Edited reason", response.getData().getDwpDocuments().get(0).getValue().getDwpEditedEvidenceReason()),
                () -> assertEquals(DwpDocumentType.DWP_RESPONSE.getValue(), response.getData().getDwpDocuments().get(1).getValue().getDocumentType()),
                () -> assertEquals("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw", response.getData().getDwpDocuments().get(1).getValue().getDocumentLink().getDocumentUrl()),
                () -> assertEquals("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw/binary", response.getData().getDwpDocuments().get(1).getValue().getDocumentLink().getDocumentBinaryUrl()),
                () -> assertEquals("http://dm-store:5005/documents/efgh-4567-mnopqrstuvw", response.getData().getDwpDocuments().get(1).getValue().getEditedDocumentLink().getDocumentUrl()),
                () -> assertEquals("http://dm-store:5005/documents/efgh-4567-mnopqrstuvw/binary", response.getData().getDwpDocuments().get(1).getValue().getEditedDocumentLink().getDocumentBinaryUrl()),
                () -> assertEquals(AppConstants.DWP_DOCUMENT_EDITED_RESPONSE_FILENAME_PREFIX + " on " + todayDate + ".pdf", response.getData().getDwpDocuments().get(1).getValue().getEditedDocumentLink().getDocumentFilename()),
                () -> assertEquals(AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX + " on " + todayDate + ".pdf", response.getData().getDwpDocuments().get(1).getValue().getDocumentLink().getDocumentFilename()),
                () -> assertEquals("Edited reason", response.getData().getDwpDocuments().get(1).getValue().getDwpEditedEvidenceReason()),
                () -> assertEquals(DwpDocumentType.AT_38.getValue(), response.getData().getDwpDocuments().get(2).getValue().getDocumentType()),
                () -> assertEquals("http://dm-store:5005/documents/abcd-0123-xyzabcdefgh", response.getData().getDwpDocuments().get(2).getValue().getDocumentLink().getDocumentUrl()),
                () -> assertEquals("http://dm-store:5005/documents/abcd-0123-xyzabcdefgh/binary", response.getData().getDwpDocuments().get(2).getValue().getDocumentLink().getDocumentBinaryUrl()),
                () -> assertEquals(AppConstants.DWP_DOCUMENT_AT38_FILENAME_PREFIX + " on " + todayDate + ".pdf", response.getData().getDwpDocuments().get(2).getValue().getDocumentLink().getDocumentFilename()),
                () -> assertEquals(REVIEW_BY_JUDGE.getId(), response.getData().getInterlocReviewState()));
    }

    @Test
    public void givenAUcCaseWithSingleElementSelected_thenSetCaseCodeToUs() {
        List<String> elementList = new ArrayList<>();
        elementList.add("testElement");
        sscsCaseData.setElementsDisputedList(elementList);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("uc").build());

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("US", response.getData().getIssueCode());
        assertEquals("001", response.getData().getBenefitCode());
        assertEquals("001US", response.getData().getCaseCode());
        assertEquals(DwpState.RESPONSE_SUBMITTED_DWP.getId(), response.getData().getDwpState());
        assertNull(response.getData().getInterlocReviewState());
    }

    @Test
    public void givenAUcCaseWithMultipleElementSelected_thenSetCaseCodeToUm() {
        List<String> elementList = new ArrayList<>();
        elementList.add("testElement");
        elementList.add("testElement2");
        sscsCaseData.setElementsDisputedList(elementList);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("uc").build());

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("UM", response.getData().getIssueCode());
        assertEquals("001", response.getData().getBenefitCode());
        assertEquals("001UM", response.getData().getCaseCode());
        assertEquals(DwpState.RESPONSE_SUBMITTED_DWP.getId(), response.getData().getDwpState());
        assertNull(response.getData().getInterlocReviewState());
    }

    @Test
    public void givenUcCaseWithPhmeNoFurtherInfo_thenSetReviewByJudge() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("phme");
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedEvidenceBundleDocument.pdf")
                                .build()
                ).build());
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedResponseDocument.pdf")
                                .build()
                ).build());
        callback.getCaseDetails().getCaseData().setDwpFurtherInfo("No");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("reviewByJudge", response.getData().getSelectWhoReviewsCase().getValue().getCode());
        assertEquals("phmeRequest", response.getData().getInterlocReferralReason());
        assertEquals(REVIEW_BY_JUDGE.getId(), response.getData().getInterlocReviewState());
        assertEquals(1, response.getData().getAppealNotePad().getNotesCollection().size());
        assertEquals("Referred to interloc for review by judge - PHME request", response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDetail());
        assertEquals(LocalDate.now().toString(), response.getData().getInterlocReferralDate());

        dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenUcCaseWithPhmeNoFurtherInfo2ndCall_thenNoError() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("phme");
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedEvidenceBundleDocument.pdf")
                                .build()
                ).build());
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedResponseDocument.pdf")
                                .build()
                ).build());
        callback.getCaseDetails().getCaseData().setDwpFurtherInfo("No");

        DynamicListItem reviewByJudgeItem = new DynamicListItem("reviewByJudge", null);
        sscsCaseData.setSelectWhoReviewsCase(new DynamicList(reviewByJudgeItem, null));

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("reviewByJudge", response.getData().getSelectWhoReviewsCase().getValue().getCode());
        assertEquals("phmeRequest", response.getData().getInterlocReferralReason());
        assertEquals(REVIEW_BY_JUDGE.getId(), response.getData().getInterlocReviewState());
        assertEquals(1, response.getData().getAppealNotePad().getNotesCollection().size());
        assertEquals("Referred to interloc for review by judge - PHME request", response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDetail());
        assertEquals(LocalDate.now().toString(), response.getData().getInterlocReferralDate());
    }

    @Test
    public void givenUcCaseWithPhmeYesFurtherInfo_thenDontReviewByJudge() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("phme");
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedEvidenceBundleDocument.pdf")
                                .build()
                ).build());

        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedResponseDocument.pdf")
                                .build()
                ).build());

        callback.getCaseDetails().getCaseData().setDwpFurtherInfo("Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getSelectWhoReviewsCase());
        assertEquals("phmeRequest", response.getData().getInterlocReferralReason());
        assertEquals(REVIEW_BY_JUDGE.getId(), response.getData().getInterlocReviewState());
        assertEquals(1, response.getData().getAppealNotePad().getNotesCollection().size());
        assertEquals("Referred to interloc for review by judge - PHME request", response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDetail());
        assertEquals(LocalDate.now().toString(), response.getData().getInterlocReferralDate());
    }

    @Test
    public void givenUcCaseWithPhmeAndEditedEvidenceBundle_thenMustHaveEditedDwpResponseDoc() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("phme");
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedEvidenceBundleDocument.pdf")
                                .build()
                ).build());

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("You must upload an edited DWP response document", error);
        }
    }

    @Test
    public void givenUcCaseWithPhmeAndEditedResponse_thenMustHaveEditedDwpEvidenceBundle() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("phme");
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedEvidenceBundleDocument.pdf")
                                .build()
                ).build());

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("You must upload an edited DWP evidence bundle", error);
        }
    }

    @Test
    public void givenUcCaseWithAppendix12Document_thenMoveDocumentToDwpDocumentsCollection() {
        callback.getCaseDetails().getCaseData().setAppendix12Doc(DwpResponseDocument.builder().documentFileName("testA").documentLink(DocumentLink.builder().documentFilename("My document name.pdf").build()).build());
        List<DwpDocument> dwpResponseDocuments = new ArrayList<>();
        dwpResponseDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentFileName("existingDoc").documentDateAdded(LocalDate.now().minusDays(1).toString()).build()).build());

        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpResponseDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(2, response.getData().getDwpDocuments().size());

        String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        assertEquals("Appendix 12 received on " + todayDate, response.getData().getDwpDocuments().get(0).getValue().getDocumentFileName());
        assertEquals("Appendix 12 received on " + todayDate + ".pdf", response.getData().getDwpDocuments().get(0).getValue().getDocumentLink().getDocumentFilename());
        assertEquals(DwpDocumentType.APPENDIX_12.getValue(), response.getData().getDwpDocuments().get(0).getValue().getDocumentType());
        assertEquals("existingDoc", response.getData().getDwpDocuments().get(1).getValue().getDocumentFileName());
    }

    @Test
    @Parameters(method = "emptyAppendix12Documents")
    public void givenEmptyAppendix12Document_thenDoNotMoveDocumentToDwpDocumentsCollection(@Nullable DwpResponseDocument dwpResponseDocument) {
        callback.getCaseDetails().getCaseData().setAppendix12Doc(dwpResponseDocument);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getDwpDocuments());
    }

    public static Object[][] emptyAppendix12Documents() {
        return new Object[][] {
                {DwpResponseDocument.builder().build()},
                {null}
        };
    }

    @Test
    public void givenUcbSelectedAndNoUcbDocument_displayAnError() {
        sscsCaseData.setDwpUcb(YES.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(null);
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Please upload a UCB document"));
    }

    @Test
    public void givenUcbSelectedIsNo_thenTheFieldsAreCleared() {
        sscsCaseData.setDwpUcb(NO.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(DocumentLink.builder().build());
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(sscsCaseData.getDwpUcb(), is(nullValue()));
        assertThat(sscsCaseData.getDwpUcbEvidenceDocument(), is(nullValue()));
        assertThat(sscsCaseData.getDwpDocuments(), is(nullValue()));
    }

    @Test
    public void givenUcbSelectedAndUploadedUcbDoc_thenNoErrors() {
        sscsCaseData.setDwpUcb(YES.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(DocumentLink.builder().build());
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(sscsCaseData.getDwpUcb(), is(YES.getValue()));
        assertThat(sscsCaseData.getDwpUcbEvidenceDocument(), is(nullValue()));
        assertThat(sscsCaseData.getDwpDocuments().size(), is(1));
    }

    @Test
    public void givenHandleAudioVideoDocuments_thenItMovesToAudioVideoListAndFillsInFieldsSendToTcw() {
        AudioVideoEvidenceDetails audioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder().documentLink(DocumentLink.builder()
                .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("filename").build())
                .rip1Document(DocumentLink.builder()
                        .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("rip1").build()).build();

        sscsCaseData.setDwpUploadAudioVideoEvidence(Collections
                .singletonList(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build()));

        dwpUploadResponseAboutToSubmitHandler.handleAudioVideoDocuments(sscsCaseData);

        assertNull(callback.getCaseDetails().getCaseData().getDwpUploadAudioVideoEvidence());
        assertEquals(1, callback.getCaseDetails().getCaseData().getAudioVideoEvidence().size());
        AudioVideoEvidence audioVideoEvidence = callback.getCaseDetails().getCaseData().getAudioVideoEvidence().get(0);
        assertEquals("/url", audioVideoEvidence.getValue().getDocumentLink().getDocumentUrl());
        assertEquals("rip1", audioVideoEvidence.getValue().getRip1Document().getDocumentFilename());
        assertEquals("filename", audioVideoEvidence.getValue().getFileName());
        assertEquals(UploadParty.DWP, audioVideoEvidence.getValue().getPartyUploaded());
        assertNotNull(audioVideoEvidence.getValue().getDateAdded());
        assertEquals(InterlocReviewState.REVIEW_BY_TCW.getId(), callback.getCaseDetails().getCaseData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE.getId(), callback.getCaseDetails().getCaseData().getInterlocReferralReason());
    }

    @Test
    public void givenHandleAudioVideoDocumentsNoRip1_thenItMovesToAudioVideoListAndFillsInFieldsSendToTcw() {
        AudioVideoEvidenceDetails audioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder().documentLink(DocumentLink.builder()
                .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("filename").build()).build();

        sscsCaseData.setDwpUploadAudioVideoEvidence(Collections
                .singletonList(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build()));

        dwpUploadResponseAboutToSubmitHandler.handleAudioVideoDocuments(sscsCaseData);

        assertNull(callback.getCaseDetails().getCaseData().getDwpUploadAudioVideoEvidence());
        assertEquals(1, callback.getCaseDetails().getCaseData().getAudioVideoEvidence().size());
        AudioVideoEvidence audioVideoEvidence = callback.getCaseDetails().getCaseData().getAudioVideoEvidence().get(0);
        assertEquals("/url", audioVideoEvidence.getValue().getDocumentLink().getDocumentUrl());
        assertNull("rip1", audioVideoEvidence.getValue().getRip1Document());
        assertEquals("filename", audioVideoEvidence.getValue().getFileName());
        assertEquals(UploadParty.DWP, audioVideoEvidence.getValue().getPartyUploaded());
        assertNotNull(audioVideoEvidence.getValue().getDateAdded());
        assertEquals(InterlocReviewState.REVIEW_BY_TCW.getId(), callback.getCaseDetails().getCaseData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE.getId(), callback.getCaseDetails().getCaseData().getInterlocReferralReason());
    }

    @Test
    public void givenHandleNullAudioVideoDocuments_thenNoAudioVideoList() {
        dwpUploadResponseAboutToSubmitHandler.handleAudioVideoDocuments(sscsCaseData);
        assertNull(sscsCaseData.getAudioVideoEvidence());
    }

    @Test
    public void givenHandleEmptyAudioVideoDocuments_thenNoAudioVideoList() {
        sscsCaseData.setDwpUploadAudioVideoEvidence(Collections.emptyList());
        dwpUploadResponseAboutToSubmitHandler.handleAudioVideoDocuments(sscsCaseData);
        assertNull(sscsCaseData.getAudioVideoEvidence());
    }

    @Test
    public void givenExistingAudioVideo_thenItGetsAddedToListSendToTcw() {
        AudioVideoEvidenceDetails audioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder().documentLink(DocumentLink.builder()
                .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("filename").build())
                .rip1Document(DocumentLink.builder()
                        .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("surveillance").build()).build();

        List<AudioVideoEvidence> audioVideoList = new ArrayList<>();
        audioVideoList.add(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build());

        sscsCaseData.setAudioVideoEvidence(new ArrayList<>(
                Arrays.asList(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build())));

        sscsCaseData.setDwpUploadAudioVideoEvidence(new ArrayList<>(
                Arrays.asList(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build())));

        dwpUploadResponseAboutToSubmitHandler.handleAudioVideoDocuments(sscsCaseData);

        assertNull(callback.getCaseDetails().getCaseData().getDwpUploadAudioVideoEvidence());
        assertEquals(2, callback.getCaseDetails().getCaseData().getAudioVideoEvidence().size());
        assertEquals(InterlocReviewState.REVIEW_BY_TCW.getId(), callback.getCaseDetails().getCaseData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE.getId(), callback.getCaseDetails().getCaseData().getInterlocReferralReason());
    }

    @Test
    public void givenHandleAudioVideoDocumentsAndPhme_thenItMovesToAudioVideoListAndFillsInFieldsSendToJudge() {
        AudioVideoEvidenceDetails audioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder().documentLink(DocumentLink.builder()
                .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("filename").build())
                .rip1Document(DocumentLink.builder()
                        .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("rip1").build()).build();

        sscsCaseData.setDwpUploadAudioVideoEvidence(Collections
                .singletonList(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build()));

        sscsCaseData.setDwpEditedEvidenceReason("phme");

        dwpUploadResponseAboutToSubmitHandler.handleAudioVideoDocuments(sscsCaseData);

        assertNull(callback.getCaseDetails().getCaseData().getDwpUploadAudioVideoEvidence());
        assertEquals(1, callback.getCaseDetails().getCaseData().getAudioVideoEvidence().size());
        AudioVideoEvidence audioVideoEvidence = callback.getCaseDetails().getCaseData().getAudioVideoEvidence().get(0);
        assertEquals("/url", audioVideoEvidence.getValue().getDocumentLink().getDocumentUrl());
        assertEquals("rip1", audioVideoEvidence.getValue().getRip1Document().getDocumentFilename());
        assertEquals("filename", audioVideoEvidence.getValue().getFileName());
        assertEquals(UploadParty.DWP, audioVideoEvidence.getValue().getPartyUploaded());
        assertNotNull(audioVideoEvidence.getValue().getDateAdded());
        assertEquals(REVIEW_BY_JUDGE.getId(), callback.getCaseDetails().getCaseData().getInterlocReviewState());
        assertNull(callback.getCaseDetails().getCaseData().getInterlocReferralReason());
    }

    @Test
    public void givenHandleAudioVideoDocumentsAndInterlocReviewStateAlreadyReviewByJudge_thenLeaveAsReviewByJudge() {
        AudioVideoEvidenceDetails audioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder().documentLink(DocumentLink.builder()
                .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("filename").build())
                .rip1Document(DocumentLink.builder()
                        .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("surveillance").build()).build();

        List audioVideoList = new ArrayList<>();
        audioVideoList.add(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build());

        sscsCaseData.setAudioVideoEvidence(new ArrayList<>(
                Arrays.asList(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build())));

        sscsCaseData.setDwpUploadAudioVideoEvidence(new ArrayList<>(
                Arrays.asList(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build())));

        sscsCaseData.setInterlocReviewState(REVIEW_BY_JUDGE.getId());

        dwpUploadResponseAboutToSubmitHandler.handleAudioVideoDocuments(sscsCaseData);

        assertNull(callback.getCaseDetails().getCaseData().getDwpUploadAudioVideoEvidence());
        assertEquals(2, callback.getCaseDetails().getCaseData().getAudioVideoEvidence().size());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), callback.getCaseDetails().getCaseData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE.getId(), callback.getCaseDetails().getCaseData().getInterlocReferralReason());
    }

    @Test
    public void givenADwpUploadResponseEventWithRemovedDwpDocumentsThenHandle() {

        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(new DwpResponseDocument(null, null));
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(new DwpResponseDocument(null, null));
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason(null);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenADwpUploadResponseEventWithRip1DocumentAndNoAvFile_displayAnError() {
        AudioVideoEvidenceDetails audioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder().documentLink(null)
                .rip1Document(DocumentLink.builder().documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("surveillance").build()).build();

        List<AudioVideoEvidence> audioVideoList = new ArrayList<>();
        audioVideoList.add(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build());

        sscsCaseData.setDwpUploadAudioVideoEvidence(audioVideoList);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("You must upload an audio/video document when submitting a RIP 1 document", error);
        }
    }

    @Test
    public void givenADwpUploadResponseEventWithRip1DocumentAndAvFile_thenDoNotDisplayAnError() {
        AudioVideoEvidenceDetails audioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder().documentLink(DocumentLink.builder()
                .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("filename").build())
                .rip1Document(DocumentLink.builder()
                        .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("surveillance").build()).build();

        List<AudioVideoEvidence> audioVideoList = new ArrayList<>();
        audioVideoList.add(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build());

        sscsCaseData.setDwpUploadAudioVideoEvidence(audioVideoList);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenADwpUploadResponseEventWithAvFileAndNoRip1Document_thenDoNotDisplayAnError() {
        AudioVideoEvidenceDetails audioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder().documentLink(DocumentLink.builder()
                .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("filename").build())
                .rip1Document(null).build();

        List<AudioVideoEvidence> audioVideoList = new ArrayList<>();
        audioVideoList.add(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build());

        sscsCaseData.setDwpUploadAudioVideoEvidence(audioVideoList);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenADwpUploadResponseEventWithEmptyDocument_thenHandleThisCorrectly() {

        callback.getCaseDetails().getCaseData().setDwpAT38Document(DwpResponseDocument.builder().build());

        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-5678-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-5678-xyzabcmnop")
                                .documentFilename("testEvidenceBundleDocument.pdf")
                                .build()
                ).build());


        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw/binary")
                                .documentUrl("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw")
                                .documentFilename("testResponseDocument.pdf")
                                .build()
                ).build());

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(2, response.getData().getDwpDocuments().size());
    }

}
