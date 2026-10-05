package uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_APPEAL_PDF;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;
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
import uk.gov.hmcts.reform.sscs.service.SscsPdfService;

@ExtendWith(MockitoExtension.class)
class CreateCaseAboutToSubmitHandlerTest {
    
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final Long CCD_CASE_ID = 1234567890L;
    private static final String DOCUMENT_URL = "http://dm-store:4506/documents/35d53efc-a30d-4b0d-b5a9-312d52bb1a4d";
    private static final String EVIDENCE_URL = "http://dm-store:4506/documents/35d53efc-a45c-a30d-b5a9-412d52bb1a4d";
    
    @Mock
    private SscsPdfService sscsPdfService;
    @Mock
    private EmailHelper emailHelper;

    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;
    private Appeal appeal;

    private CreateCaseAboutToSubmitHandler createCaseAboutToSubmitHandler;

    @BeforeEach
    void setUp() {
        appeal = Appeal.builder()
                .hearingOptions(HearingOptions.builder().build())
                .mrnDetails(MrnDetails.builder().build())
                .appellant(Appellant.builder()
                        .name(Name.builder().lastName("appellantLastName").build())
                        .identity(Identity.builder().ibcaReference("IBCA12345").build())
                        .build())
                .build();
        caseDetails =
                new CaseDetails<>(1234L, "SSCS", WITH_DWP, buildCaseDataWithoutPdf(), now(), "Benefit");
        callback = new Callback<>(caseDetails, empty(), CREATE_APPEAL_PDF, false);

        createCaseAboutToSubmitHandler =
                new CreateCaseAboutToSubmitHandler(sscsPdfService, emailHelper, false);
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
        var caseData = buildCaseData();
        var caseDetailsData =
                new CaseDetails<>(1234L, "SSCS", WITH_DWP, caseData, now(), "Benefit");
        var callbackData = new Callback<>(caseDetailsData, empty(), eventType, false);

        assertThat(createCaseAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callbackData)).isTrue();
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
        var caseData = buildCaseData();
        caseData.getAppeal().setReceivedVia("Paper");
        var caseDetailsData =
                new CaseDetails<>(1234L, "SSCS", WITH_DWP, caseData, now(), "Benefit");
        var callbackData = new Callback<>(caseDetailsData, empty(), eventType, false);

        assertThat(createCaseAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callbackData)).isEqualTo(allowable);
    }

    @Test
    void givenANonSscs1PdfHandlerEvent_thenReturnFalse() {
        var caseData = buildCaseData();
        var caseDetailsData =
                new CaseDetails<>(1234L, "SSCS", WITH_DWP, caseData, now(), "Benefit");
        var callbackData = new Callback<>(caseDetailsData, empty(), APPEAL_RECEIVED, false);

        assertThat(createCaseAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callbackData)).isFalse();
    }

    @Test
    void shouldCallPdfService() throws CcdException {
        var caseData = buildCaseDataWithoutPdf();
        var caseDetailsData =
                new CaseDetails<>(1234L, "SSCS", WITH_DWP, caseData, now(), "Benefit");
        var callbackData = new Callback<>(caseDetailsData, empty(), CREATE_APPEAL_PDF, false);

        when(emailHelper.generateUniqueEmailId(caseDetailsData.getCaseData().getAppeal().getAppellant()))
                .thenReturn("Test");

        createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callbackData, USER_AUTHORISATION);

        verify(emailHelper).generateUniqueEmailId(eq(caseDetailsData.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService).generatePdf(eq(caseDetailsData.getCaseData()), any(), eq("sscs1"), any());
    }


    @Test
    void shouldCallPdfServiceWhenIbca() throws CcdException {
        var caseData = SscsCaseData.builder()
                .caseReference("").caseCreated("").ccdCaseId("1021").benefitCode(IBCA_BENEFIT_CODE).appeal(appeal)
                .regionalProcessingCenter(RegionalProcessingCenter.builder().build())
                .build();
        caseData.getAppeal().getAppellant().getIdentity().setIbcaReference("ibcaRef");
        var caseDetailsData =
                new CaseDetails<>(1234L, "SSCS", WITH_DWP, caseData, now(), "Benefit");
        var callbackData = new Callback<>(caseDetailsData, empty(), CREATE_APPEAL_PDF, false);

        createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callbackData, USER_AUTHORISATION);

        verify(emailHelper, never()).generateUniqueEmailId(any());
        String expectedFilename = String.format("%s_%s", "appellantLastName", "ibcaRef") + ".pdf";
        verify(sscsPdfService).generatePdf(eq(caseDetailsData.getCaseData()), any(), any(), eq(expectedFilename));
    }


    @Test
    void isIbcFalseIfNullBenefitType() throws CcdException {
        var caseData = SscsCaseData.builder()
                .caseReference("").caseCreated("").ccdCaseId("1021").benefitCode("").appeal(appeal).build();
        var caseDetailsData =
                new CaseDetails<>(1234L, "SSCS", WITH_DWP, caseData, now(), "Benefit");
        var callbackData = new Callback<>(caseDetailsData, empty(), CREATE_APPEAL_PDF, false);

        createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callbackData, USER_AUTHORISATION);

        verify(emailHelper, times(1)).generateUniqueEmailId(any());
        verify(sscsPdfService).generatePdf(eq(caseDetailsData.getCaseData()), any(), any(), any());
    }

    @Test
    void shouldCallPdfServiceWhenNoAppointee() throws CcdException {
        when(emailHelper.generateUniqueEmailId(caseDetails.getCaseData().getAppeal().getAppellant()))
                .thenReturn("Test");
        caseDetails.getCaseData().getAppeal().getAppellant().getAppointee().setName(null);

        PreSubmitCallbackResponse<SscsCaseData> response =
                createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getAppeal().getAppellant().getAppointee()).isNull();
        verify(emailHelper).generateUniqueEmailId(eq(caseDetails.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService).generatePdf(eq(caseDetails.getCaseData()), any(), eq("sscs1"), any());
    }

    @Test
    void whenCaseCreated_shouldUpdatePoAttendingAndTribinalDirectPoAttendToNo() throws CcdException {
        when(emailHelper.generateUniqueEmailId(caseDetails.getCaseData().getAppeal().getAppellant())).thenReturn("Test");
        caseDetails.getCaseData().getAppeal().getAppellant().getAppointee().setName(null);

        PreSubmitCallbackResponse<SscsCaseData> response = createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getPoAttendanceConfirmed()).isEqualTo(YesNo.NO);
        assertThat(response.getData().getTribunalDirectPoToAttend()).isEqualTo(YesNo.NO);

    }

    @Test
    void shouldCallPdfServiceWhenSscsDocumentIsNull() {
        SscsCaseData caseDataWithNullSscsDocument = buildCaseDataWithNullSscsDocument();
        var caseDetailsData =
                new CaseDetails<>(1234L, "SSCS", WITH_DWP, caseDataWithNullSscsDocument, now(), "Benefit");
        var callbackData = new Callback<>(caseDetailsData, empty(), CREATE_APPEAL_PDF, false);
        when(emailHelper.generateUniqueEmailId(caseDetailsData.getCaseData().getAppeal().getAppellant()))
                .thenReturn("Test");

        PreSubmitCallbackResponse<SscsCaseData> response =
                createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callbackData, USER_AUTHORISATION);

        assertThat(response.getData().getEvidencePresent()).isEqualTo("No");
        verify(emailHelper).generateUniqueEmailId(eq(caseDetailsData.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService).generatePdf(eq(caseDetailsData.getCaseData()), any(), eq("sscs1"), any());
    }

    @Test
    void shouldCallPdfServiceWhenSscsDocumentIsPopulated() {
        SscsCaseData caseDataWithSscsDocument = buildCaseDataWithPdf();
        var caseDetailsData =
                new CaseDetails<>(1234L, "SSCS", WITH_DWP, caseDataWithSscsDocument, now(), "Benefit");
        var callbackData = new Callback<>(caseDetailsData, empty(), CREATE_APPEAL_PDF, false);
        when(emailHelper.generateUniqueEmailId(caseDetailsData.getCaseData().getAppeal().getAppellant()))
                .thenReturn("Bla");

        PreSubmitCallbackResponse<SscsCaseData> response =
                createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callbackData, USER_AUTHORISATION);

        assertThat(response.getData().getEvidencePresent()).isEqualTo("Yes");
        verify(emailHelper).generateUniqueEmailId(eq(caseDetailsData.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService).generatePdf(eq(caseDetailsData.getCaseData()), any(), eq("sscs1"), any());
    }

    @Test
    void shouldSetPdfFileNameWithIbcaReferenceWhenBenefitIsIbca() {
        SscsCaseData caseDataWithSscsDocument =
                buildCaseData("Test", "infectedBloodCompensation", "IBCA");
        caseDataWithSscsDocument.setCcdCaseId(CCD_CASE_ID.toString());
        caseDataWithSscsDocument.setBenefitCode(IBCA_BENEFIT_CODE);
        caseDataWithSscsDocument.getAppeal().getAppellant().getIdentity().setIbcaReference("IBCA12345");
        caseDataWithSscsDocument.setSscsDocument(buildDocuments());
        var caseDetailsData =
                new CaseDetails<>(1234L, "SSCS", WITH_DWP, caseDataWithSscsDocument, now(), "Benefit");
        var callbackData = new Callback<>(caseDetailsData, empty(), CREATE_APPEAL_PDF, false);

        PreSubmitCallbackResponse<SscsCaseData> response =
                createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callbackData, USER_AUTHORISATION);

        assertThat(response.getData().getEvidencePresent()).isEqualTo("Yes");
        verify(sscsPdfService).generatePdf(eq(caseDetailsData.getCaseData()), any(), eq("sscs8"), any());
    }

    @Test
    void shouldCallPdfServiceWhenSscsDocumentIsNullWhenBenefitCodeIsIbca() {
        SscsCaseData caseDataWithNullSscsDocument =
                buildCaseData("Test", "infectedBloodCompensation", "IBCA");
        caseDataWithNullSscsDocument.setCcdCaseId(CCD_CASE_ID.toString());
        caseDataWithNullSscsDocument.setBenefitCode(IBCA_BENEFIT_CODE);
        caseDataWithNullSscsDocument.getAppeal().getAppellant().getIdentity().setIbcaReference("IBCA12345");
        caseDataWithNullSscsDocument.setSscsDocument(null);
        var caseDetailsData =
                new CaseDetails<>(1234L, "SSCS", WITH_DWP, caseDataWithNullSscsDocument, now(), "Benefit");
        var callbackData = new Callback<>(caseDetailsData, empty(), CREATE_APPEAL_PDF, false);

        PreSubmitCallbackResponse<SscsCaseData> response =
                createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callbackData, USER_AUTHORISATION);

        assertThat(response.getData().getEvidencePresent()).isEqualTo("No");
        verify(sscsPdfService).generatePdf(eq(caseDetailsData.getCaseData()), any(), eq("sscs8"), any());
    }

    @Test
    void givenPdfAlreadyExists_shouldNotCallPdfService() throws CcdException {
        SscsCaseData caseDataWithPdf = buildCaseDataWithPdf();
        var caseDetailsData =
                new CaseDetails<>(1234L, "SSCS", WITH_DWP, caseDataWithPdf, now(), "Benefit");
        var callbackData = new Callback<>(caseDetailsData, empty(), CREATE_APPEAL_PDF, false);
        when(emailHelper.generateUniqueEmailId(caseDataWithPdf.getAppeal().getAppellant())).thenReturn("Test");

        createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callbackData, USER_AUTHORISATION);

        assertThat(caseDetailsData.getCaseData().getEvidencePresent()).isNull();
        verify(emailHelper).generateUniqueEmailId(eq(caseDetailsData.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService, never()).generatePdf(eq(caseDetailsData.getCaseData()), any(), any(), any());
    }

    @Test
    void givenPdfServiceExceptionThrown_thenCarryOnWithCaseCreation() {
        when(sscsPdfService.generatePdf(eq(caseDetails.getCaseData()), any(), any(), any())).thenThrow(new PDFServiceClientException(new Exception("Error")));

        PreSubmitCallbackResponse<SscsCaseData> response = createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getCcdCaseId()).isEqualTo("1234567890");
    }

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        var callbackData = new Callback<>(caseDetails, empty(), APPEAL_RECEIVED, false);

        assertThatThrownBy(() ->
                createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callbackData, USER_AUTHORISATION))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldReturnErrorIfNullCreatedDate() throws CcdException {
        callback.getCaseDetails().getCaseData().setCaseCreated(null);
        callback.getCaseDetails().getCaseData().setBenefitCode("015");
        PreSubmitCallbackResponse<SscsCaseData> response =
                createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors()).contains("The Case Created Date must be set to generate the SSCS5");
    }

    @Test
    void shouldPreserveDwpIsOfficerAttendingValue() {
        caseDetails.getCaseData().setDwpIsOfficerAttending(YesNo.YES.toString());

        PreSubmitCallbackResponse<SscsCaseData> response =
                createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(YesNo.isYes(response.getData().getDwpIsOfficerAttending())).isTrue();
    }

    @Test
    void whenBenefitCodeIsNotNull_shouldSetCorrectCaseCode() {
        caseDetails.getCaseData().setBenefitCode("001");

        PreSubmitCallbackResponse<SscsCaseData> response =
                createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getCaseCode()).isEqualTo("001DD");
    }

    @Test
    void whenBenefitAndIssueCodeIsNotNull_shouldSetCorrectCaseCode() {
        caseDetails.getCaseData().setBenefitCode("001");
        caseDetails.getCaseData().setIssueCode("US");

        PreSubmitCallbackResponse<SscsCaseData> response =
                createCaseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getCaseCode()).isEqualTo("001US");
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
