package uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.IBCA_BENEFIT_CODE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pdf.service.client.exception.PDFServiceClientException;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.helper.EmailHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;
import uk.gov.hmcts.reform.sscs.service.SscsPdfService;

@ExtendWith(MockitoExtension.class)
public class CreateCaseAboutToSubmitHandlerTest {
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

    @Mock
    private SscsCaseData mockedCaseData;

    @Mock
    private Appeal mockedAppeal;

    @Mock
    private Appellant mockedAppellant;

    @Mock
    private Name mockedName;

    @Mock
    private Identity mockedIdentity;

    @Mock
    private VerbalLanguagesService verbalLanguagesService;

    private CreateCaseAboutToSubmitHandler createCaseAboutToSubmitHandler;

    @BeforeEach
    void setUp() {
        when(callback.getEvent()).thenReturn(EventType.CREATE_APPEAL_PDF);
        SscsCaseData caseData = buildCaseDataWithoutPdf();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        createCaseAboutToSubmitHandler = new CreateCaseAboutToSubmitHandler(sscsPdfService, emailHelper, verbalLanguagesService);
    }

    @ParameterizedTest
    @CsvSource({
        "CREATE_APPEAL_PDF",
        "VALID_APPEAL_CREATED",
        "DRAFT_TO_VALID_APPEAL_CREATED",
        "NON_COMPLIANT",
        "DRAFT_TO_NON_COMPLIANT",
        "INCOMPLETE_APPLICATION_RECEIVED",
        "DRAFT_TO_INCOMPLETE_APPLICATION",
    })
    void givenASscs1PdfHandlerEventForSyaCases_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        assertTrue(createCaseAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @ParameterizedTest
    @CsvSource({
        "CREATE_APPEAL_PDF, true",
        "VALID_APPEAL_CREATED, false",
        "DRAFT_TO_VALID_APPEAL_CREATED, false",
        "NON_COMPLIANT, false",
        "DRAFT_TO_NON_COMPLIANT, false",
        "INCOMPLETE_APPLICATION_RECEIVED, false",
        "DRAFT_TO_INCOMPLETE_APPLICATION, false",
    })
    void givenASscs1PdfHandlerEventForBulkScanCases_thenReturnAllowableValue(EventType eventType, boolean allowable) {
        caseDetails.getCaseData().getAppeal().setReceivedVia("Paper");
        when(callback.getEvent()).thenReturn(eventType);

        assertEquals(allowable, createCaseAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    void givenANonSscs1PdfHandlerEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(createCaseAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    void shouldCallPdfService() throws CcdException {

        when(emailHelper.generateUniqueEmailId(caseDetails.getCaseData().getAppeal().getAppellant())).thenReturn("Test");

        createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(emailHelper).generateUniqueEmailId(eq(caseDetails.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService).generatePdf(eq(caseDetails.getCaseData()), any(), eq("sscs1"), any());
    }


    @Test
    void shouldCallPdfServiceWhenIbca() throws CcdException {
        when(caseDetails.getCaseData()).thenReturn(mockedCaseData);
        when(mockedCaseData.getCaseCreated()).thenReturn("");
        when(mockedCaseData.getCcdCaseId()).thenReturn("1021");
        when(mockedCaseData.getBenefitCode()).thenReturn(IBCA_BENEFIT_CODE);
        when(mockedCaseData.getAppeal()).thenReturn(mockedAppeal);
        when(mockedAppeal.getHearingOptions()).thenReturn(HearingOptions.builder().build());
        when(mockedAppeal.getMrnDetails()).thenReturn(MrnDetails.builder().build());
        when(mockedAppeal.getAppellant()).thenReturn(mockedAppellant);
        when(mockedAppellant.getName()).thenReturn(mockedName);
        when(mockedAppellant.getIdentity()).thenReturn(mockedIdentity);
        when(mockedName.getLastName()).thenReturn("appellantLastName");
        when(mockedIdentity.getIbcaReference()).thenReturn("ibcaRef");
        when(mockedCaseData.getRegionalProcessingCenter()).thenReturn(RegionalProcessingCenter.builder().build());

        createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(emailHelper, never()).generateUniqueEmailId(any());
        String expectedFilename = String.format("%s_%s", "appellantLastName", "ibcaRef") + ".pdf";
        verify(sscsPdfService).generatePdf(eq(caseDetails.getCaseData()), any(), any(), eq(expectedFilename));
    }


    @Test
    void isIbcFalseIfNullBenefitType() throws CcdException {
        when(caseDetails.getCaseData()).thenReturn(mockedCaseData);
        when(mockedCaseData.getCaseCreated()).thenReturn("");
        when(mockedCaseData.getCcdCaseId()).thenReturn("1021");
        when(mockedCaseData.getBenefitCode()).thenReturn("");
        when(mockedCaseData.getAppeal()).thenReturn(mockedAppeal);
        when(mockedAppeal.getAppellant()).thenReturn(mockedAppellant);

        createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(emailHelper, times(1)).generateUniqueEmailId(any());
        verify(sscsPdfService).generatePdf(eq(caseDetails.getCaseData()), any(), any(), any());
    }

    @Test
    void shouldCallPdfServiceWhenNoAppointee() throws CcdException {

        when(emailHelper.generateUniqueEmailId(caseDetails.getCaseData().getAppeal().getAppellant())).thenReturn("Test");

        caseDetails.getCaseData().getAppeal().getAppellant().getAppointee().setName(null);

        PreSubmitCallbackResponse<SscsCaseData> response = createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getAppeal().getAppellant().getAppointee());

        verify(emailHelper).generateUniqueEmailId(eq(caseDetails.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService).generatePdf(eq(caseDetails.getCaseData()), any(), eq("sscs1"), any());
    }

    @Test
    void whenCaseCreated_shouldUpdatePoAttendingAndTribinalDirectPoAttendToNo() throws CcdException {
        when(emailHelper.generateUniqueEmailId(caseDetails.getCaseData().getAppeal().getAppellant())).thenReturn("Test");
        caseDetails.getCaseData().getAppeal().getAppellant().getAppointee().setName(null);

        PreSubmitCallbackResponse<SscsCaseData> response = createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(response.getData().getPoAttendanceConfirmed(), YesNo.NO);
        assertEquals(response.getData().getTribunalDirectPoToAttend(), YesNo.NO);

    }

    @Test
    void shouldCallPdfServiceWhenSscsDocumentIsNull() {
        SscsCaseData caseDataWithNullSscsDocument = buildCaseDataWithNullSscsDocument();

        when(caseDetails.getCaseData()).thenReturn(caseDataWithNullSscsDocument);

        when(emailHelper.generateUniqueEmailId(caseDetails.getCaseData().getAppeal().getAppellant())).thenReturn("Test");

        PreSubmitCallbackResponse<SscsCaseData> response = createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("No", response.getData().getEvidencePresent());

        verify(emailHelper).generateUniqueEmailId(eq(caseDetails.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService).generatePdf(eq(caseDetails.getCaseData()), any(), eq("sscs1"), any());
    }

    @Test
    void shouldCallPdfServiceWhenSscsDocumentIsPopulated() {
        SscsCaseData caseDataWithSscsDocument = buildCaseDataWithPdf();

        when(caseDetails.getCaseData()).thenReturn(caseDataWithSscsDocument);

        when(emailHelper.generateUniqueEmailId(caseDetails.getCaseData().getAppeal().getAppellant())).thenReturn("Bla");

        PreSubmitCallbackResponse<SscsCaseData> response = createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Yes", response.getData().getEvidencePresent());

        verify(emailHelper).generateUniqueEmailId(eq(caseDetails.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService).generatePdf(eq(caseDetails.getCaseData()), any(), eq("sscs1"), any());
    }

    @Test
    void shouldSetPdfFileNameWithIbcaReferenceWhenBenefitIsIbca() {
        SscsCaseData caseDataWithSscsDocument = buildCaseData("Test", "infectedBloodCompensation", "IBCA");
        caseDataWithSscsDocument.setCcdCaseId(CCD_CASE_ID.toString());
        caseDataWithSscsDocument.setBenefitCode(IBCA_BENEFIT_CODE);
        caseDataWithSscsDocument.getAppeal().getAppellant().getIdentity().setIbcaReference("IBCA12345");
        caseDataWithSscsDocument.setSscsDocument(buildDocuments());

        when(caseDetails.getCaseData()).thenReturn(caseDataWithSscsDocument);

        PreSubmitCallbackResponse<SscsCaseData> response = createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Yes", response.getData().getEvidencePresent());

        verify(sscsPdfService).generatePdf(eq(caseDetails.getCaseData()), any(), eq("sscs8"), any());
    }

    @Test
    void shouldCallPdfServiceWhenSscsDocumentIsNullWhenBenefitCodeIsIbca() {
        SscsCaseData caseDataWithNullSscsDocument = buildCaseData("Test", "infectedBloodCompensation", "IBCA");
        caseDataWithNullSscsDocument.setCcdCaseId(CCD_CASE_ID.toString());
        caseDataWithNullSscsDocument.setBenefitCode(IBCA_BENEFIT_CODE);
        caseDataWithNullSscsDocument.getAppeal().getAppellant().getIdentity().setIbcaReference("IBCA12345");
        caseDataWithNullSscsDocument.setSscsDocument(null);

        when(caseDetails.getCaseData()).thenReturn(caseDataWithNullSscsDocument);

        PreSubmitCallbackResponse<SscsCaseData> response = createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("No", response.getData().getEvidencePresent());

        verify(sscsPdfService).generatePdf(eq(caseDetails.getCaseData()), any(), eq("sscs8"), any());
    }

    @Test
    void givenPdfAlreadyExists_shouldNotCallPdfService() throws CcdException {

        SscsCaseData caseDataWithPdf = buildCaseDataWithPdf();

        when(caseDetails.getCaseData()).thenReturn(caseDataWithPdf);

        when(emailHelper.generateUniqueEmailId(caseDataWithPdf.getAppeal().getAppellant())).thenReturn("Test");

        createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(caseDetails.getCaseData().getEvidencePresent());

        verify(emailHelper).generateUniqueEmailId(eq(caseDetails.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService, never()).generatePdf(eq(caseDetails.getCaseData()), any(), any(), any());
    }

    @Test
    void givenPdfServiceExceptionThrown_thenCarryOnWithCaseCreation() {
        when(sscsPdfService.generatePdf(eq(caseDetails.getCaseData()), any(), any(), any())).thenThrow(new PDFServiceClientException(new Exception("Error")));

        PreSubmitCallbackResponse<SscsCaseData> response = createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("1234567890", response.getData().getCcdCaseId());
    }

    @Test
    void givenLanguageHasBeenSet_thenConfirmCorrectLanguageNameAndSetIt() {
        String oldLanguageName = "Putonghue";
        String newLanguageName = "Mandarin";

        caseDetails.getCaseData().getAppeal().getHearingOptions().setLanguages(oldLanguageName);
        when(verbalLanguagesService.getVerbalLanguage(oldLanguageName)).thenReturn(Language.builder()
                .nameEn(newLanguageName).build());

        PreSubmitCallbackResponse<SscsCaseData> response = createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(newLanguageName, response.getData().getAppeal().getHearingOptions().getLanguages());
    }

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThrows(IllegalStateException.class, () ->
                createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    void shouldReturnErrorIfNullCreatedDate() throws CcdException {
        callback.getCaseDetails().getCaseData().setCaseCreated(null);
        callback.getCaseDetails().getCaseData().setBenefitCode("015");
        PreSubmitCallbackResponse<SscsCaseData> response = createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains("The Case Created Date must be set to generate the SSCS5"));
    }

    @Test
    void shouldPreserveDwpIsOfficerAttendingValue() {
        caseDetails.getCaseData().setDwpIsOfficerAttending(YesNo.YES.toString());

        PreSubmitCallbackResponse<SscsCaseData> response = createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertTrue(YesNo.isYes(response.getData().getDwpIsOfficerAttending()));
    }

    @Test
    void whenBenefitCodeIsNotNull_shouldSetCorrectCaseCode() {
        caseDetails.getCaseData().setBenefitCode("001");

        PreSubmitCallbackResponse<SscsCaseData> response = createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals("001DD", response.getData().getCaseCode());
    }

    @Test
    void whenBenefitAndIssueCodeIsNotNull_shouldSetCorrectCaseCode() {
        caseDetails.getCaseData().setBenefitCode("001");
        caseDetails.getCaseData().setIssueCode("US");

        PreSubmitCallbackResponse<SscsCaseData> response = createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals("001US", response.getData().getCaseCode());
    }


    private SscsCaseData buildCaseDataWithoutPdf() {
        SscsCaseData caseData = buildCaseData();
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
        SscsCaseData caseData = buildCaseData();
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
