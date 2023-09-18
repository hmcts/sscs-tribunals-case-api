package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.pdf.service.client.exception.PDFServiceClientException;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.helper.EmailHelper;
import uk.gov.hmcts.reform.sscs.service.SscsPdfService;

@RunWith(JUnitParamsRunner.class)
public class Sscs1PdfHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final Long CCD_CASE_ID = 1234567890L;
    private static final String DOCUMENT_URL = "http://dm-store:4506/documents/35d53efc-a30d-4b0d-b5a9-312d52bb1a4d";
    private static final String EVIDENCE_URL = "http://dm-store:4506/documents/35d53efc-a45c-a30d-b5a9-412d52bb1a4d";
    @Mock
    private SscsPdfService sscsPdfService;

    @Mock
    private EmailHelper emailHelper;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private Sscs1PdfHandler sscs1PdfHandler;

    @Before
    public void setUp() {
        openMocks(this);

        when(callback.getEvent()).thenReturn(EventType.CREATE_APPEAL_PDF);
        SscsCaseData caseData = buildCaseDataWithoutPdf();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        sscs1PdfHandler = new Sscs1PdfHandler(sscsPdfService, emailHelper);
    }

    @Test
    @Parameters({
        "CREATE_APPEAL_PDF",
        "VALID_APPEAL_CREATED",
        "DRAFT_TO_VALID_APPEAL_CREATED",
        "NON_COMPLIANT",
        "DRAFT_TO_NON_COMPLIANT",
        "INCOMPLETE_APPLICATION_RECEIVED",
        "DRAFT_TO_INCOMPLETE_APPLICATION",
    })
    public void givenASscs1PdfHandlerEventForSyaCases_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        assertTrue(sscs1PdfHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({
        "CREATE_APPEAL_PDF, true",
        "VALID_APPEAL_CREATED, false",
        "DRAFT_TO_VALID_APPEAL_CREATED, false",
        "NON_COMPLIANT, false",
        "DRAFT_TO_NON_COMPLIANT, false",
        "INCOMPLETE_APPLICATION_RECEIVED, false",
        "DRAFT_TO_INCOMPLETE_APPLICATION, false",
    })
    public void givenASscs1PdfHandlerEventForBulkScanCases_thenReturnAllowableValue(EventType eventType, boolean allowable) {
        caseDetails.getCaseData().getAppeal().setReceivedVia("Paper");
        when(callback.getEvent()).thenReturn(eventType);

        assertEquals(allowable, sscs1PdfHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonSscs1PdfHandlerEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(sscs1PdfHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void shouldCallPdfService() throws CcdException {

        when(emailHelper.generateUniqueEmailId(caseDetails.getCaseData().getAppeal().getAppellant())).thenReturn("Test");

        sscs1PdfHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(emailHelper).generateUniqueEmailId(eq(caseDetails.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService).generatePdf(eq(caseDetails.getCaseData()), any(), any(), any());
    }

    @Test
    public void shouldCallPdfServiceWhenNoAppointee() throws CcdException {

        when(emailHelper.generateUniqueEmailId(caseDetails.getCaseData().getAppeal().getAppellant())).thenReturn("Test");

        caseDetails.getCaseData().getAppeal().getAppellant().getAppointee().setName(null);

        PreSubmitCallbackResponse<SscsCaseData> response = sscs1PdfHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getAppeal().getAppellant().getAppointee());

        verify(emailHelper).generateUniqueEmailId(eq(caseDetails.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService).generatePdf(eq(caseDetails.getCaseData()), any(), any(), any());
    }

    @Test
    public void shouldCallPdfServiceWhenSscsDocumentIsNull() {
        SscsCaseData caseDataWithNullSscsDocument = buildCaseDataWithNullSscsDocument();

        when(caseDetails.getCaseData()).thenReturn(caseDataWithNullSscsDocument);

        when(emailHelper.generateUniqueEmailId(caseDetails.getCaseData().getAppeal().getAppellant())).thenReturn("Test");

        PreSubmitCallbackResponse<SscsCaseData> response = sscs1PdfHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("No", response.getData().getEvidencePresent());

        verify(emailHelper).generateUniqueEmailId(eq(caseDetails.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService).generatePdf(eq(caseDetails.getCaseData()), any(), any(), any());
    }

    @Test
    public void shouldCallPdfServiceWhenSscsDocumentIsPopulated() {
        SscsCaseData caseDataWithSscsDocument = buildCaseDataWithPdf();

        when(caseDetails.getCaseData()).thenReturn(caseDataWithSscsDocument);

        when(emailHelper.generateUniqueEmailId(caseDetails.getCaseData().getAppeal().getAppellant())).thenReturn("Bla");

        PreSubmitCallbackResponse<SscsCaseData> response = sscs1PdfHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Yes", response.getData().getEvidencePresent());

        verify(emailHelper).generateUniqueEmailId(eq(caseDetails.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService).generatePdf(eq(caseDetails.getCaseData()), any(), any(), any());
    }

    @Test
    public void givenPdfAlreadyExists_shouldNotCallPdfService() throws CcdException {

        SscsCaseData caseDataWithPdf = buildCaseDataWithPdf();

        when(caseDetails.getCaseData()).thenReturn(caseDataWithPdf);

        when(emailHelper.generateUniqueEmailId(caseDataWithPdf.getAppeal().getAppellant())).thenReturn("Test");

        sscs1PdfHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(caseDetails.getCaseData().getEvidencePresent());

        verify(emailHelper).generateUniqueEmailId(eq(caseDetails.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService, never()).generatePdf(eq(caseDetails.getCaseData()), any(), any(), any());
    }

    @Test
    public void givenPdfServiceExceptionThrown_thenCarryOnWithCaseCreation() {
        when(sscsPdfService.generatePdf(eq(caseDetails.getCaseData()), any(), any(), any())).thenThrow(new PDFServiceClientException(new Exception("Error")));

        PreSubmitCallbackResponse<SscsCaseData> response = sscs1PdfHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("1234567890", response.getData().getCcdCaseId());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        sscs1PdfHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void shouldReturnErrorIfNullCreatedDate() throws CcdException {
        when(emailHelper.generateUniqueEmailId(caseDetails.getCaseData().getAppeal().getAppellant())).thenReturn("Test");

        callback.getCaseDetails().getCaseData().setCaseCreated(null);

        PreSubmitCallbackResponse<SscsCaseData> response = sscs1PdfHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
    }

    private SscsCaseData buildCaseDataWithoutPdf() {
        SscsCaseData caseData = CaseDataUtils.buildCaseData();
        caseData.setSscsDocument(Collections.emptyList());
        caseData.setCcdCaseId(CCD_CASE_ID.toString());
        return caseData;
    }

    private SscsCaseData buildCaseDataWithPdf() {
        SscsCaseData caseData = buildCaseDataWithoutPdf();
        caseData.setSscsDocument(buildDocuments());
        return caseData;
    }

    private SscsCaseData buildCaseDataWithNullSscsDocument() {
        SscsCaseData caseData = CaseDataUtils.buildCaseData();
        caseData.setSscsDocument(null);
        caseData.setCcdCaseId(CCD_CASE_ID.toString());
        return caseData;
    }

    private List<SscsDocument> buildDocuments() {
        List<SscsDocument> list = new ArrayList<>();

        String fileName = "Test.pdf";
        String evidenceName = "Test.jpg";

        list.add(SscsDocument.builder()
                .value(
                        SscsDocumentDetails.builder()
                                .documentDateAdded("2018-12-05")
                                .documentFileName(fileName)
                                .documentLink(
                                        DocumentLink.builder()
                                                .documentUrl(DOCUMENT_URL)
                                                .documentBinaryUrl(DOCUMENT_URL + "/binary")
                                                .documentFilename(fileName)
                                                .build()
                                )
                                .build()
                )
                .build()
        );
        list.add(SscsDocument.builder()
                .value(
                        SscsDocumentDetails.builder()
                                .documentDateAdded("2018-12-05")
                                .documentFileName(evidenceName)
                                .documentLink(
                                        DocumentLink.builder()
                                                .documentUrl(EVIDENCE_URL)
                                                .documentBinaryUrl(EVIDENCE_URL + "/binary")
                                                .documentFilename(evidenceName)
                                                .build()
                                )
                                .build()
                )
                .build()
        );
        return list;
    }

}
