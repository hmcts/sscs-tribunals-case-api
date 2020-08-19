package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.AppConstants;

@RunWith(JUnitParamsRunner.class)
public class DwpUploadResponseAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private DwpUploadResponseAboutToSubmitHandler dwpUploadResponseAboutToSubmitHandler;
    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Before
    public void setUp() {
        openMocks(this);
        dwpUploadResponseAboutToSubmitHandler = new DwpUploadResponseAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.DWP_UPLOAD_RESPONSE);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1234")
            .benefitCode("002")
            .issueCode("CC")
            .dwpFurtherInfo("Yes")
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
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
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

        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw/binary")
                                .documentUrl("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw")
                                .documentFilename("testResponseDocument.pdf")
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
            () -> assertEquals("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw", response.getData().getDwpResponseDocument().getDocumentLink().getDocumentUrl()),
            () -> assertEquals("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw/binary", response.getData().getDwpResponseDocument().getDocumentLink().getDocumentBinaryUrl()),
            () -> assertEquals(AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX + " on " + todayDate + ".pdf", response.getData().getDwpResponseDocument().getDocumentLink().getDocumentFilename()));
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
    }
}