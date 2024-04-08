package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;
import uk.gov.hmcts.reform.sscs.domain.email.RoboticsEmailTemplate;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.EvidenceShareConfig;
import uk.gov.hmcts.reform.sscs.helper.EmailHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.model.dwp.Mapping;
import uk.gov.hmcts.reform.sscs.model.dwp.OfficeMapping;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonMapper;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonValidator;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;

@RunWith(JUnitParamsRunner.class)
public class RoboticsServiceTest {

    RoboticsService roboticsService;

    @Mock
    PdfStoreService pdfStoreService;

    @Mock
    EmailService emailService;

    EmailHelper emailHelper;

    @Mock
    RoboticsJsonMapper roboticsJsonMapper;

    @Mock
    RoboticsJsonValidator roboticsJsonValidator;

    @Mock
    RoboticsEmailTemplate roboticsEmailTemplate;

    @Mock
    EvidenceShareConfig evidenceShareConfig;

    @Mock
    DwpAddressLookupService dwpAddressLookupService;

    @Mock
    CcdService ccdService;

    @Mock
    IdamService idamService;

    SscsCcdConvertService convertService;

    LocalDate localDate;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Captor
    private ArgumentCaptor<List<EmailAttachment>> captor;

    @Captor
    private ArgumentCaptor<SscsCaseData> caseDataCaptor;

    private SscsCaseData sscsCaseData;

    private CaseDetails<SscsCaseData> caseData;

    @Before
    public void setup() {
        openMocks(this);

        emailHelper = new EmailHelper();
        convertService = new SscsCcdConvertService();

        roboticsService = new RoboticsService(
            pdfStoreService,
            emailService,
            emailHelper,
            roboticsJsonMapper,
            roboticsJsonValidator,
            roboticsEmailTemplate,
            evidenceShareConfig,
            dwpAddressLookupService,
            ccdService,
            idamService,
            1,
            1);

        localDate = LocalDate.now();

        JSONObject mappedJson = mock(JSONObject.class);
        given(roboticsJsonMapper.map(any())).willReturn(mappedJson);
        given(evidenceShareConfig.getSubmitTypes()).willReturn(Collections.singletonList("paper"));

        sscsCaseData = buildCaseData("Bloggs");
        sscsCaseData.setCcdCaseId("1");
        sscsCaseData.getAppeal().getAppellant().getIdentity().setNino("789123");
        caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null, "Benefit");
    }

    @Test
    @Parameters({"CARDIFF", "GLASGOW", "", "null"})
    public void generatingRoboticsSendsAnEmail(String rpcName) {

        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name(rpcName).build());

        given(pdfStoreService.download(any())).willReturn(null);

        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null, "Benefit");
        roboticsService.sendCaseToRobotics(caseData);

        boolean isScottish = StringUtils.equalsAnyIgnoreCase(rpcName, "GLASGOW");
        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123 for Robot [1]"), captor.capture(), eq(isScottish), eq(false));
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.size(), is(1));
        assertThat(attachmentResult.get(0).getFilename(), is("Bloggs_123.txt"));
        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(any(), any());
        verify(emailService).sendEmail(eq(1L), any());
        verify(ccdService, never()).updateCase(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @Parameters({"CARDIFF", "GLASGOW", "", "null"})
    public void generatingRoboticsWhenThrowValidationErrors(String rpcName) {

        Set<String> errorSet = new HashSet<>();
        errorSet.add("Surname is missing");
        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name(rpcName).build());

        given(pdfStoreService.download(any())).willReturn(null);

        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null, "Benefit");
        caseData.getCaseData().getAppeal().getAppellant().getName().setLastName("");
        when(roboticsJsonValidator.validate(any(), any())).thenReturn(errorSet);
        roboticsService.sendCaseToRobotics(caseData);

        boolean isScottish = StringUtils.equalsAnyIgnoreCase(rpcName, "GLASGOW");
        verify(roboticsEmailTemplate).generateEmail(eq("_123 for Robot [1]"), captor.capture(), eq(isScottish), eq(false));
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.size(), is(1));
        assertThat(attachmentResult.get(0).getFilename(), is("_123.txt"));
        assertThat(caseData.getCaseData().getHmctsDwpState(), is("failedRobotics"));
        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(any(), any());
        verify(emailService).sendEmail(eq(1L), any());
        verify(ccdService).updateCase(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @Parameters({"Paper", "Online"})
    public void givenACaseWithEvidenceToDownload_thenCreateRoboticsFileWithDownloadedEvidence(String receivedVia) {

        byte[] expectedBytes = {1, 2, 3};
        given(pdfStoreService.download("www.download.com")).willReturn(expectedBytes);

        List<SscsDocument> documents = new ArrayList<>();
        documents.add(SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentFileName("test.jpg")
                .documentLink(DocumentLink.builder().documentUrl("www.download.com").documentFilename("fileName.pdf").build())
                .build())
            .build());

        sscsCaseData.getAppeal().setReceivedVia(receivedVia);
        sscsCaseData.setSscsDocument(documents);
        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null, "Benefit");

        roboticsService.sendCaseToRobotics(caseData);

        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123 for Robot [1]"), captor.capture(), eq(false), eq(false));
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.get(0).getFilename(), is("Bloggs_123.txt"));

        if (receivedVia.equals("Paper")) {
            assertThat(attachmentResult.size(), is(1));
        } else {
            assertThat(attachmentResult.size(), is(2));
            assertThat(attachmentResult.get(1).getFilename(), is("fileName.pdf"));
        }

        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(any(), any());
        verify(emailService).sendEmail(eq(1L), any());
        verify(ccdService, never()).updateCase(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    public void givenAdditionalEvidenceHasEmptyFileName_doNotDownloadAdditionalEvidenceAndStillGenerateRoboticsAndSendEmail() {

        byte[] expectedBytes = {1, 2, 3};
        given(pdfStoreService.download("www.download.com")).willReturn(expectedBytes);

        Map<String, byte[]> expectedAdditionalEvidence = new HashMap<>();
        expectedAdditionalEvidence.put("test.jpg", expectedBytes);

        List<SscsDocument> documents = new ArrayList<>();
        documents.add(SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentFileName(null)
                .documentLink(DocumentLink.builder().documentUrl("www.download.com").build())
                .build())
            .build());

        sscsCaseData.setSscsDocument(documents);

        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null, "Benefit");

        roboticsService.sendCaseToRobotics(caseData);

        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123 for Robot [1]"), captor.capture(), eq(false), eq(false));
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.get(0).getFilename(), is("Bloggs_123.txt"));
        assertThat(attachmentResult.size(), is(1));

        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(any(), any());
        verify(emailService).sendEmail(eq(1L), any());
        verify(ccdService, never()).updateCase(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    public void givenAdditionalEvidenceHasEmptyFileNameAndIsPipAeTrue_doNotDownloadAdditionalEvidenceAndStillGenerateRoboticsAndSendEmail() {

        byte[] expectedBytes = {1, 2, 3};
        given(pdfStoreService.download("www.download.com")).willReturn(expectedBytes);

        Map<String, byte[]> expectedAdditionalEvidence = new HashMap<>();
        expectedAdditionalEvidence.put("test.jpg", expectedBytes);

        List<SscsDocument> documents = new ArrayList<>();
        documents.add(SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentFileName(null)
                .documentLink(DocumentLink.builder().documentUrl("www.download.com").build())
                .build())
            .build());

        sscsCaseData.getAppeal().getMrnDetails().setDwpIssuingOffice("DWP PIP (AE)");
        sscsCaseData.setSscsDocument(documents);

        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null, "Benefit");

        JSONObject actual = roboticsService.sendCaseToRobotics(caseData);

        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123 for Robot [1]"), captor.capture(), eq(false), eq(true));
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.get(0).getFilename(), is("Bloggs_123.txt"));
        assertThat(attachmentResult.size(), is(1));

        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(any(), any());
        verify(emailService).sendEmail(eq(1L), any());
        verify(ccdService, never()).updateCase(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    public void givenNonDigitalCaseAndHasAdditionalEvidenceAndIsPipAeTrue_DownloadAdditionalEvidenceAndGenerateRoboticsAndSendEmail() {

        byte[] expectedBytes = {1, 2, 3};
        given(pdfStoreService.download("www.download.com")).willReturn(expectedBytes);

        Map<String, byte[]> expectedAdditionalEvidence = new HashMap<>();
        expectedAdditionalEvidence.put("test.jpg", expectedBytes);

        List<SscsDocument> documents = new ArrayList<>();
        documents.add(SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentFileName("test.jpg")
                .documentLink(DocumentLink.builder().documentUrl("www.download.com").build())
                .build())
            .build());

        sscsCaseData.getAppeal().getMrnDetails().setDwpIssuingOffice("DWP PIP (AE)");
        sscsCaseData.setSscsDocument(documents);

        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null, "Benefit");

        roboticsService.sendCaseToRobotics(caseData);

        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123 for Robot [1]"), captor.capture(), eq(false), eq(true));
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.get(0).getFilename(), is("Bloggs_123.txt"));
        assertThat(attachmentResult.size(), is(1));

        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(any(), any());
        verify(emailService).sendEmail(eq(1L), any());
        verify(ccdService, never()).updateCase(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    public void givenCaseIsDigitalAndHasAdditionalEvidenceAndIsPipAeTrue_doNotDownloadAdditionalEvidenceAndStillGenerateRoboticsAndSendEmail() {

        byte[] expectedBytes = {1, 2, 3};
        given(pdfStoreService.download("www.download.com")).willReturn(expectedBytes);

        Map<String, byte[]> expectedAdditionalEvidence = new HashMap<>();
        expectedAdditionalEvidence.put("test.jpg", expectedBytes);

        List<SscsDocument> documents = new ArrayList<>();
        documents.add(SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentFileName(null)
                .documentLink(DocumentLink.builder().documentUrl("www.download.com").build())
                .build())
            .build());

        sscsCaseData.getAppeal().getMrnDetails().setDwpIssuingOffice("DWP PIP (AE)");
        sscsCaseData.setSscsDocument(documents);

        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null, "Benefit");
        caseData.getCaseData().setCreatedInGapsFrom(READY_TO_LIST.getId());

        roboticsService.sendCaseToRobotics(caseData);

        verify(roboticsEmailTemplate).generateEmail(eq("Bloggs_123 for Robot [1]"), captor.capture(), eq(false), eq(true));
        List<EmailAttachment> attachmentResult = captor.getValue();

        assertThat(attachmentResult.get(0).getFilename(), is("Bloggs_123.txt"));
        assertThat(attachmentResult.size(), is(1));

        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(any(), any());
        verify(emailService).sendEmail(eq(1L), any());
        verify(ccdService, never()).updateCase(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @Parameters({"ESA, Balham DRT, Watford DRT", "PIP, DWP PIP (1), DWP PIP (2)", "PIP, PIP (AE), DWP PIP (AE)"})
    public void givenACaseAndDwpIssuingOfficeIsClosed_thenFindNewIssuingOfficeAndUpdateCaseInCcdAndRobotics(String benefitType, String existingOffice, String newOffice) {
        given(dwpAddressLookupService.getDwpMappingByOffice(benefitType, existingOffice)).willReturn(Optional.of(
            OfficeMapping.builder().code(existingOffice).mapping(Mapping.builder().ccd(newOffice).build()).build()));

        sscsCaseData.getAppeal().getMrnDetails().setDwpIssuingOffice(existingOffice);
        sscsCaseData.getAppeal().getBenefitType().setCode(benefitType);

        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null, "Benefit");

        roboticsService.sendCaseToRobotics(caseData);

        assertThat(caseData.getCaseData().getAppeal().getMrnDetails().getDwpIssuingOffice(), is(newOffice));
        verify(ccdService, never()).updateCase(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @Parameters({"ESA, Balham DRT, Watford DRT", "PIP, DWP PIP (1), DWP PIP (2)", "PIP, PIP (AE), DWP PIP (AE)"})
    public void givenACaseAndDwpOriginatingOfficeIsClosed_thenFindNewOriginatingOfficeAndUpdateCaseInCcdAndRobotics(String benefitType, String existingOffice, String newOffice) {
        given(dwpAddressLookupService.getDwpMappingByOffice(benefitType, existingOffice)).willReturn(Optional.of(
            OfficeMapping.builder().code(existingOffice).mapping(Mapping.builder().ccd(newOffice).build()).build()));

        sscsCaseData.getAppeal().getBenefitType().setCode(benefitType);

        DynamicListItem value = new DynamicListItem(existingOffice, existingOffice);
        sscsCaseData.setDwpOriginatingOffice(new DynamicList(value, Collections.singletonList(value)));

        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null, "Benefit");

        roboticsService.sendCaseToRobotics(caseData);

        assertThat(caseData.getCaseData().getDwpOriginatingOffice().getValue().getCode(), is(newOffice));
        verify(ccdService, never()).updateCase(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @Parameters({"ESA, Balham DRT, Watford DRT", "PIP, DWP PIP (1), DWP PIP (2)", "PIP, PIP (AE), DWP PIP (AE)"})
    public void givenACaseAndDwpPresentingOfficeIsClosed_thenFindNewPresentingOfficeAndUpdateCaseInCcdAndRobotics(String benefitType, String existingOffice, String newOffice) {
        given(dwpAddressLookupService.getDwpMappingByOffice(benefitType, existingOffice)).willReturn(Optional.of(
            OfficeMapping.builder().code(existingOffice).mapping(Mapping.builder().ccd(newOffice).build()).build()));

        sscsCaseData.getAppeal().getBenefitType().setCode(benefitType);

        DynamicListItem value = new DynamicListItem(existingOffice, existingOffice);
        sscsCaseData.setDwpPresentingOffice(new DynamicList(value, Collections.singletonList(value)));

        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null, "Benefit");

        roboticsService.sendCaseToRobotics(caseData);

        assertThat(caseData.getCaseData().getDwpPresentingOffice().getValue().getCode(), is(newOffice));
        verify(ccdService, never()).updateCase(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @Parameters({"ESA, Balham DRT, Watford DRT", "PIP, DWP PIP (1), DWP PIP (2)", "PIP, PIP (AE), DWP PIP (AE)"})
    public void givenACaseAndOfficeUsedInAllOfficeFieldsIsClosed_thenFindNewOfficeAndUpdateAllOfficesForCaseInCcdAndRobotics(String benefitType, String existingOffice, String newOffice) {
        given(dwpAddressLookupService.getDwpMappingByOffice(benefitType, existingOffice)).willReturn(Optional.of(
            OfficeMapping.builder().code(existingOffice).mapping(Mapping.builder().ccd(newOffice).build()).build()));

        sscsCaseData.getAppeal().getBenefitType().setCode(benefitType);
        sscsCaseData.getAppeal().getMrnDetails().setDwpIssuingOffice(existingOffice);

        DynamicListItem value = new DynamicListItem(existingOffice, existingOffice);
        sscsCaseData.setDwpOriginatingOffice(new DynamicList(value, Collections.singletonList(value)));
        sscsCaseData.setDwpPresentingOffice(new DynamicList(value, Collections.singletonList(value)));

        CaseDetails<SscsCaseData> caseData = new CaseDetails<>(1L, null, APPEAL_CREATED, sscsCaseData, null, "Benefit");

        roboticsService.sendCaseToRobotics(caseData);

        assertThat(caseData.getCaseData().getAppeal().getMrnDetails().getDwpIssuingOffice(), is(newOffice));
        assertThat(caseData.getCaseData().getDwpOriginatingOffice().getValue().getCode(), is(newOffice));
        assertThat(caseData.getCaseData().getDwpPresentingOffice().getValue().getCode(), is(newOffice));
        verify(ccdService, never()).updateCase(any(), any(), anyString(), anyString(), anyString(), any());
    }
}
