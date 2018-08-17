package uk.gov.hmcts.sscs.service;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.email.Email;
import uk.gov.hmcts.sscs.email.RoboticsEmailTemplate;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmailTemplate;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.ccd.SscsDocument;
import uk.gov.hmcts.sscs.model.ccd.SscsDocumentDetails;
import uk.gov.hmcts.sscs.model.pdf.PdfWrapper;
import uk.gov.hmcts.sscs.model.robotics.RoboticsWrapper;
import uk.gov.hmcts.sscs.model.tya.RegionalProcessingCenter;
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

    @Before
    public void setUp() {
        submitYourAppealEmailTemplate = new SubmitYourAppealEmailTemplate("from", "to", "message");
        roboticsEmailTemplate = new RoboticsEmailTemplate("from", "to", "message");
        regionalProcessingCenterService = new RegionalProcessingCenterService();
        regionalProcessingCenterService.init();

        SubmitYourAppealToCcdCaseDataDeserializer submitYourAppealToCcdCaseDataDeserializer = new
                SubmitYourAppealToCcdCaseDataDeserializer();

        submitAppealService = new SubmitAppealService(TEMPLATE_PATH, appealNumberGenerator,
            submitYourAppealToCcdCaseDataDeserializer, ccdService,
            pdfServiceClient, emailService, roboticsService, submitYourAppealEmailTemplate, roboticsEmailTemplate,
                airLookupService, regionalProcessingCenterService, pdfStoreService, false);

        given(ccdService.createCase(any(CaseData.class)))
            .willReturn(CaseDetails.builder().id(123L).build());
    }

    @Test
    public void shouldSendPdfByEmailWhenCcdIsDown() {
        given(ccdService.createCase(any(CaseData.class))).willThrow(new CcdException(
            "Error while creating case in CCD"));

        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), captor.capture()))
            .willReturn(expected);

        submitAppealService.submitAppeal(getSyaCaseWrapper());

        then(pdfServiceClient).should(times(1)).generateFromHtml(any(), any());
        then(emailService).should(times(1)).sendEmail(any(Email.class));

        assertNull(getPdfWrapper().getCcdCaseId());
    }

    private PdfWrapper getPdfWrapper() {
        Map<String, Object> placeHolders = captor.getAllValues().get(0);
        return (PdfWrapper) placeHolders.get("PdfWrapper");
    }

    @Test
    public void shouldCreateCaseWithAppealDetails() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
            any())).willReturn(expected);

        submitAppealService.submitAppeal(appealData);

        verify(appealNumberGenerator).generate();
        verify(ccdService).createCase(any(CaseData.class));
    }

    @Test
    public void shouldCreatePdfWithAppealDetails() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
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
    public void shouldSendRoboticsByEmailWhenFeatureFlagEnabled() {

        ReflectionTestUtils.setField(submitAppealService, "roboticsEnabled", true);
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        RoboticsWrapper roboticsWrapper = RoboticsWrapper.builder().syaCaseWrapper(appealData).ccdCaseId(123L).build();
        byte[] expected = {};
        JSONObject json = new JSONObject();

        given(pdfServiceClient.generateFromHtml(any(byte[].class),
            any(Map.class))).willReturn(expected);
        given(roboticsService.createRobotics(eq(roboticsWrapper))).willReturn(json);

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
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        appealData.getAppellant().getContactDetails().setPostCode(null);

        assertEquals("", submitAppealService.getFirstHalfOfPostcode(null));
    }

    @Test
    public void testRegionAddedToCase() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        RegionalProcessingCenter rpc = getRegionalProcessingCenter();
        CaseData caseData = submitAppealService.transformAppealToCaseData(appealData,"Cardiff", rpc);
        assertEquals("Cardiff", caseData.getRegion());
    }

    @Test
    public void shouldStorePdfInDocumentStore() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
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
        given(ccdService.createCase(any(CaseData.class))).willReturn(CaseDetails.builder().id(ccdId).build());
        SscsDocument pdfDocument = new SscsDocument(SscsDocumentDetails.builder().build());
        List<SscsDocument> sscsDocuments = singletonList(pdfDocument);
        given(pdfStoreService.store(expected, "Bloggs_33C.pdf")).willReturn(sscsDocuments);
        SyaCaseWrapper appealData = getSyaCaseWrapper();

        submitAppealService.submitAppeal(appealData);

        verify(ccdService).updateCase(
                argThat(caseData ->  sscsDocuments.equals(caseData.getSscsDocument())),
                eq(ccdId),
                eq("uploadDocument")
        );
    }

    @Test
    public void shouldUpdateCcdWithPdfCombinedWithEvidence() {
        byte[] expected = {1, 2, 3};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class))).willReturn(expected);
        long ccdId = 987L;
        given(ccdService.createCase(any(CaseData.class))).willReturn(CaseDetails.builder().id(ccdId)
                .build());
        SscsDocument pdfDocument = new SscsDocument(SscsDocumentDetails.builder().build());
        List<SscsDocument> sscsDocuments = singletonList(pdfDocument);
        given(pdfStoreService.store(expected, "Bloggs_33C.pdf")).willReturn(sscsDocuments);
        SyaCaseWrapper appealData = getSyaCaseWrapper("json/sya_with_evidence.json");

        submitAppealService.submitAppeal(appealData);

        verify(ccdService).updateCase(
                argThat(caseData -> caseData.getSscsDocument().size() == 3
                        && caseData.getSscsDocument().get(2).equals(sscsDocuments.get(0))),
                eq(ccdId),
                eq("uploadDocument")
        );
    }
}
