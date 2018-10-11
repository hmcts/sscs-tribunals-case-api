package uk.gov.hmcts.sscs.service;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.sscs.email.EmailAttachment.pdf;
import static uk.gov.hmcts.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;
import static uk.gov.hmcts.sscs.util.SyaServiceHelper.getSyaCaseWrapper;

import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.robotics.RoboticsWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.service.RoboticsService;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.email.Email;
import uk.gov.hmcts.sscs.email.RoboticsEmailTemplate;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmailTemplate;
import uk.gov.hmcts.sscs.model.pdf.PdfWrapper;
import uk.gov.hmcts.sscs.service.referencedata.RegionalProcessingCenterService;
import uk.gov.hmcts.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer;

@RunWith(MockitoJUnitRunner.class)
public class SubmitAppealServiceTest {
    private static final String TEMPLATE_PATH = "/templates/appellant_appeal_template.html";

    @Mock
    private AppealNumberGenerator appealNumberGenerator;

    @Mock
    private CcdService ccdService;

    @Mock
    private PDFServiceClient pdfServiceClient;

    @Mock
    private EmailService emailService;

    @Mock
    private RoboticsService roboticsService;

    @Mock
    private AirLookupService airLookupService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> captor;

    private SubmitYourAppealEmailTemplate submitYourAppealEmailTemplate;

    private RoboticsEmailTemplate roboticsEmailTemplate;

    private SubmitAppealService submitAppealService;

    @Mock
    private PdfStoreService pdfStoreService;

    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private IdamService idamService;

    private SyaCaseWrapper appealData = getSyaCaseWrapper();

    private RoboticsWrapper roboticsWrapper;

    private JSONObject json = new JSONObject();

    private SubmitYourAppealToCcdCaseDataDeserializer deserializer;


    @Before
    public void setUp() {
        submitYourAppealEmailTemplate = new SubmitYourAppealEmailTemplate("from", "to", "message");
        roboticsEmailTemplate = new RoboticsEmailTemplate("from", "to", "message");
        regionalProcessingCenterService = new RegionalProcessingCenterService();
        regionalProcessingCenterService.init();

        deserializer = new SubmitYourAppealToCcdCaseDataDeserializer();

        SubmitYourAppealToCcdCaseDataDeserializer submitYourAppealToCcdCaseDataDeserializer = new
                SubmitYourAppealToCcdCaseDataDeserializer();

        submitAppealService = new SubmitAppealService(TEMPLATE_PATH, appealNumberGenerator,
            submitYourAppealToCcdCaseDataDeserializer, ccdService,
            pdfServiceClient, emailService, roboticsService, submitYourAppealEmailTemplate, roboticsEmailTemplate,
                airLookupService, regionalProcessingCenterService, pdfStoreService, idamService);

        given(ccdService.createCase(any(SscsCaseData.class), any(IdamTokens.class)))
            .willReturn(SscsCaseDetails.builder().id(123L).build());

        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder().build());

        roboticsWrapper = RoboticsWrapper.builder().sscsCaseData(
                deserializer.convertSyaToCcdCaseData(appealData)).ccdCaseId(123L).evidencePresent("No").build();

        given(roboticsService.createRobotics(eq(roboticsWrapper))).willReturn(json);
    }

    @Test
    public void shouldSendPdfByEmailWhenCcdIsDown() {
        given(ccdService.createCase(any(SscsCaseData.class), any(IdamTokens.class))).willThrow(new CcdException(
            "Error while creating case in CCD"));

        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), captor.capture()))
            .willReturn(expected);

        roboticsWrapper = RoboticsWrapper.builder().sscsCaseData(
                deserializer.convertSyaToCcdCaseData(appealData)).ccdCaseId(null).evidencePresent("No").build();

        given(roboticsService.createRobotics(eq(roboticsWrapper))).willReturn(json);

        submitAppealService.submitAppeal(appealData);

        verify(ccdService, never()).updateCase(any(), any(), any(), any(), any(), any());
        verify(pdfServiceClient).generateFromHtml(any(), any());
        verify(emailService, times(2).sendEmail(any(Email.class));

        assertNull(getPdfWrapper().getCcdCaseId());
    }

    private PdfWrapper getPdfWrapper() {
        Map<String, Object> placeHolders = captor.getAllValues().get(0);
        return (PdfWrapper) placeHolders.get("PdfWrapper");
    }

    @Test
    public void givenCaseDoesNotExistInCcd_shouldCreateCaseWithAppealDetails() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
            any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(), any())).willReturn(null);

        submitAppealService.submitAppeal(appealData);

        verify(appealNumberGenerator).generate();
        verify(ccdService).createCase(any(SscsCaseData.class), any(IdamTokens.class));
    }

    @Test
    public void givenCaseAlreadyExistsInCcd_shouldNotCreateCaseWithAppealDetails() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any())).willReturn(expected);

        given(ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(any(), any())).willReturn(SscsCaseDetails.builder().build());

        roboticsWrapper = RoboticsWrapper.builder().sscsCaseData(
                deserializer.convertSyaToCcdCaseData(appealData)).ccdCaseId(null).evidencePresent("No").build();

        given(roboticsService.createRobotics(eq(roboticsWrapper))).willReturn(json);

        submitAppealService.submitAppeal(appealData);

        verify(ccdService, never()).createCase(any(SscsCaseData.class), any(IdamTokens.class));
    }

    @Test
    public void shouldCreatePdfWithAppealDetails() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
            any())).willReturn(expected);

        submitAppealService.submitAppeal(appealData);

        Email expectedEmail = submitYourAppealEmailTemplate.generateEmail(
                "Bloggs_33C",
                newArrayList(pdf(expected, "Bloggs_33C.pdf"))
        );
        verify(emailService).sendEmail(expectedEmail);
    }

    @Test
    public void shouldSendRoboticsByEmail() {

        byte[] expected = {};

        given(pdfServiceClient.generateFromHtml(any(byte[].class), any(Map.class))).willReturn(expected);

        submitAppealService.submitAppeal(appealData);

        then(emailService).should(times(2)).sendEmail(any(Email.class));
    }

    @Test
    public void testPostcodeSplit() {
        assertEquals("TN32", submitAppealService.getFirstHalfOfPostcode("TN32 6PL"));
    }

    @Test
    public void testPostcodeSplitWithNoSpace() {
        assertEquals("TN32", submitAppealService.getFirstHalfOfPostcode("TN326PL"));
    }

    @Test
    public void testInvalidPostCode() {
        assertEquals("", submitAppealService.getFirstHalfOfPostcode(""));
    }

    @Test
    public void testNullPostCode() {
        appealData.getAppellant().getContactDetails().setPostCode(null);

        assertEquals("", submitAppealService.getFirstHalfOfPostcode(null));
    }

    @Test
    public void testRegionAddedToCase() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        RegionalProcessingCenter rpc = getRegionalProcessingCenter();
        SscsCaseData caseData = submitAppealService.transformAppealToCaseData(appealData,"Cardiff", rpc);
        assertEquals("Cardiff", caseData.getRegion());
    }

    @Test
    public void shouldStorePdfInDocumentStore() {
        byte[] expected = {1, 2, 3};

        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class))).willReturn(expected);

        submitAppealService.submitAppeal(appealData);

        then(pdfStoreService).should().store(expected, "Bloggs_33C.pdf");
    }

    @Test
    public void shouldUpdateCcdWithPdf() {
        byte[] expected = {1, 2, 3};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class))).willReturn(expected);
        long ccdId = 987L;
        given(ccdService.createCase(any(SscsCaseData.class), any(IdamTokens.class))).willReturn(SscsCaseDetails.builder().id(ccdId).build());
        SscsDocument pdfDocument = new SscsDocument(SscsDocumentDetails.builder().build());
        List<SscsDocument> sscsDocuments = singletonList(pdfDocument);
        given(pdfStoreService.store(expected, "Bloggs_33C.pdf")).willReturn(sscsDocuments);

        roboticsWrapper = RoboticsWrapper.builder().sscsCaseData(
                deserializer.convertSyaToCcdCaseData(appealData)).ccdCaseId(987L).evidencePresent("No").build();

        given(roboticsService.createRobotics(eq(roboticsWrapper))).willReturn(json);

        submitAppealService.submitAppeal(appealData);

        verify(ccdService).updateCase(
                argThat(caseData ->  sscsDocuments.equals(caseData.getSscsDocument())),
                eq(ccdId),
                eq("uploadDocument"),
                any(), any(), any()
        );
    }

    @Test
    public void shouldUpdateCcdWithPdfCombinedWithEvidence() {
        byte[] expected = {1, 2, 3};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class))).willReturn(expected);
        long ccdId = 987L;
        given(ccdService.createCase(any(SscsCaseData.class), any(IdamTokens.class))).willReturn(SscsCaseDetails.builder().id(ccdId)
                .build());
        SscsDocument pdfDocument = new SscsDocument(SscsDocumentDetails.builder().build());
        List<SscsDocument> sscsDocuments = singletonList(pdfDocument);
        given(pdfStoreService.store(expected, "Bloggs_33C.pdf")).willReturn(sscsDocuments);
        SyaCaseWrapper appealData = getSyaCaseWrapper("json/sya_with_evidence.json");

        roboticsWrapper = RoboticsWrapper.builder().sscsCaseData(
                deserializer.convertSyaToCcdCaseData(appealData)).ccdCaseId(987L).evidencePresent("Yes").build();

        given(roboticsService.createRobotics(eq(roboticsWrapper))).willReturn(json);

        submitAppealService.submitAppeal(appealData);

        verify(ccdService).updateCase(
                argThat(caseData -> caseData.getSscsDocument().size() == 3
                        && caseData.getSscsDocument().get(2).equals(sscsDocuments.get(0))),
                eq(ccdId),
                eq("uploadDocument"),
                any(), any(), any()
        );
    }
}
