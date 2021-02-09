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
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_JUDGE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.AppConstants;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;

@RunWith(JUnitParamsRunner.class)
public class DwpUploadResponseAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private DwpUploadResponseAboutToSubmitHandler dwpUploadResponseAboutToSubmitHandler;
    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private DwpDocumentService dwpDocumentService;

    @Before
    public void setUp() {
        openMocks(this);
        dwpDocumentService = new DwpDocumentService();
        dwpUploadResponseAboutToSubmitHandler = new DwpUploadResponseAboutToSubmitHandler(dwpDocumentService, false);

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
    public void givenADwpUploadResponseEventWithEmptyDwpFurtherInfo_displayAnError() {
        callback.getCaseDetails().getCaseData().setDwpFurtherInfo(null);
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("Further information to assist the tribunal cannot be empty.", response.getErrors().iterator().next());
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
    public void givenADwpUploadResponseEventWithResponseDocuments_assertRenameFilename() {

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

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        assertAll("DwpUploadResponseDocument fileName modified but URL remains same",
            () -> assertEquals("http://dm-store:5005/documents/abcd-0123-xyzabcdefgh", response.getData().getDwpAT38Document().getDocumentLink().getDocumentUrl()),
            () -> assertEquals("http://dm-store:5005/documents/abcd-0123-xyzabcdefgh/binary", response.getData().getDwpAT38Document().getDocumentLink().getDocumentBinaryUrl()),
            () -> assertEquals(AppConstants.DWP_DOCUMENT_AT38_FILENAME_PREFIX + " on " + todayDate + ".pdf", response.getData().getDwpAT38Document().getDocumentLink().getDocumentFilename()),
            () -> assertEquals("http://dm-store:5005/documents/defg-5678-xyzabcmnop", response.getData().getDwpEvidenceBundleDocument().getDocumentLink().getDocumentUrl()),
            () -> assertEquals("http://dm-store:5005/documents/defg-5678-xyzabcmnop/binary", response.getData().getDwpEvidenceBundleDocument().getDocumentLink().getDocumentBinaryUrl()),
            () -> assertEquals(AppConstants.DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX + " on " + todayDate + ".pdf", response.getData().getDwpEvidenceBundleDocument().getDocumentLink().getDocumentFilename()),
            () -> assertEquals("http://dm-store:5005/documents/defg-6545-xyzabcmnop", response.getData().getDwpEditedEvidenceBundleDocument().getDocumentLink().getDocumentUrl()),
            () -> assertEquals("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary", response.getData().getDwpEditedEvidenceBundleDocument().getDocumentLink().getDocumentBinaryUrl()),
            () -> assertEquals(AppConstants.DWP_DOCUMENT_EDITED_EVIDENCE_FILENAME_PREFIX + " on " + todayDate + ".pdf", response.getData().getDwpEditedEvidenceBundleDocument().getDocumentLink().getDocumentFilename()),
            () -> assertEquals("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw", response.getData().getDwpResponseDocument().getDocumentLink().getDocumentUrl()),
            () -> assertEquals("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw/binary", response.getData().getDwpResponseDocument().getDocumentLink().getDocumentBinaryUrl()),
            () -> assertEquals(AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX + " on " + todayDate + ".pdf", response.getData().getDwpResponseDocument().getDocumentLink().getDocumentFilename()),
            () -> assertEquals("http://dm-store:5005/documents/efgh-4567-mnopqrstuvw", response.getData().getDwpEditedResponseDocument().getDocumentLink().getDocumentUrl()),
            () -> assertEquals("http://dm-store:5005/documents/efgh-4567-mnopqrstuvw/binary", response.getData().getDwpEditedResponseDocument().getDocumentLink().getDocumentBinaryUrl()),
            () -> assertEquals(REVIEW_BY_JUDGE.getId(), response.getData().getInterlocReviewState()),
            () -> assertEquals(AppConstants.DWP_DOCUMENT_EDITED_RESPONSE_FILENAME_PREFIX + " on " + todayDate + ".pdf", response.getData().getDwpEditedResponseDocument().getDocumentLink().getDocumentFilename()));
    }

    @Test
    public void givenADwpUploadResponseEventWithResponseDocumentsAndDwpDocumentsBundleFeatureFlagOn_assertRenameFilenameAndMoveDocumentsToDwpDocumentsCollection() {

        ReflectionTestUtils.setField(dwpUploadResponseAboutToSubmitHandler, "dwpDocumentsBundleFeature", true);

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
                () -> assertEquals(DwpDocumentType.AT_38.getValue(), response.getData().getDwpDocuments().get(1).getValue().getDocumentType()),
                () -> assertEquals("http://dm-store:5005/documents/abcd-0123-xyzabcdefgh", response.getData().getDwpDocuments().get(1).getValue().getDocumentLink().getDocumentUrl()),
                () -> assertEquals("http://dm-store:5005/documents/abcd-0123-xyzabcdefgh/binary", response.getData().getDwpDocuments().get(1).getValue().getDocumentLink().getDocumentBinaryUrl()),
                () -> assertEquals(AppConstants.DWP_DOCUMENT_AT38_FILENAME_PREFIX + " on " + todayDate + ".pdf", response.getData().getDwpDocuments().get(1).getValue().getDocumentLink().getDocumentFilename()),
                () -> assertEquals(DwpDocumentType.DWP_RESPONSE.getValue(), response.getData().getDwpDocuments().get(2).getValue().getDocumentType()),
                () -> assertEquals("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw", response.getData().getDwpDocuments().get(2).getValue().getDocumentLink().getDocumentUrl()),
                () -> assertEquals("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw/binary", response.getData().getDwpDocuments().get(2).getValue().getDocumentLink().getDocumentBinaryUrl()),
                () -> assertEquals("http://dm-store:5005/documents/efgh-4567-mnopqrstuvw", response.getData().getDwpDocuments().get(2).getValue().getEditedDocumentLink().getDocumentUrl()),
                () -> assertEquals("http://dm-store:5005/documents/efgh-4567-mnopqrstuvw/binary", response.getData().getDwpDocuments().get(2).getValue().getEditedDocumentLink().getDocumentBinaryUrl()),
                () -> assertEquals(AppConstants.DWP_DOCUMENT_EDITED_RESPONSE_FILENAME_PREFIX + " on " + todayDate + ".pdf", response.getData().getDwpDocuments().get(2).getValue().getEditedDocumentLink().getDocumentFilename()),
                () -> assertEquals(AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX + " on " + todayDate + ".pdf", response.getData().getDwpDocuments().get(2).getValue().getDocumentLink().getDocumentFilename()),
                () -> assertEquals("Edited reason", response.getData().getDwpDocuments().get(2).getValue().getDwpEditedEvidenceReason()),
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
    }

    @Test
    public void givenUcCaseWithPhmeYesFurtherInfo_thenMustHaveReason() {
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

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("If edited evidence is added a reason must be selected", error);
        }
    }

    @Test
    public void givenUcCaseWithPhmeEvidence_thenMustHaveResponseAsWell() {
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
            assertEquals("You must submit both an edited response document and an edited evidence bundle", error);
        }
    }

    @Test
    public void givenUcCaseWithPhmeResponse_thenMustHaveResponseAsWell() {
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
            assertEquals("You must submit both an edited response document and an edited evidence bundle", error);
        }
    }

    @Test
    public void givenUcCaseWithAppendix12Document_thenMoveDocumentToDwpDocumentsCollection() {
        callback.getCaseDetails().getCaseData().setAppendix12Doc(DwpResponseDocument.builder().documentFileName("testA").build());
        List<DwpDocument> dwpResponseDocuments = new ArrayList<>();
        dwpResponseDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentFileName("existingDoc").documentDateAdded(LocalDate.now().minusDays(1).toString()).build()).build());

        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpResponseDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(2, response.getData().getDwpDocuments().size());

        assertEquals("Appendix 12 document", response.getData().getDwpDocuments().get(0).getValue().getDocumentFileName());
        assertEquals(DwpDocumentType.APPENDIX_12.getValue(), response.getData().getDwpDocuments().get(0).getValue().getDocumentType());
        assertEquals("existingDoc", response.getData().getDwpDocuments().get(1).getValue().getDocumentFileName());
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

}