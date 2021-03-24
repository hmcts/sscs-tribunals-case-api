package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.ResourceManager;

public class SscsPdfServiceTest {

    private SscsPdfService service;

    private static final String TEMPLATE_PATH = "/templates/appellant_appeal_template.html";

    private static final String WELSH_TEMPLATE_PATH = "/templates/appellant_appeal_welsh_template.html";

    @Mock
    private PDFServiceClient pdfServiceClient;

    @Mock
    CcdPdfService ccdPdfService;

    @Mock
    ResourceManager resourceManager;

    @Mock
    private WelshBenefitTypeTranslator welshBenefitTranslator;

    SscsCaseData caseData = buildCaseData();


    @Before
    public void setup() {
        openMocks(this);
        service = new SscsPdfService(TEMPLATE_PATH, WELSH_TEMPLATE_PATH, pdfServiceClient, ccdPdfService, resourceManager, welshBenefitTranslator);
    }

    @Test
    public void createValidWelshPdfAndSendEmailAndStoreInDocumentStore() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);
        given(welshBenefitTranslator.translate(caseData)).willReturn("Taliad Annibyniaeth Personol (PIP)");
        caseData.setLanguagePreferenceWelsh("Yes");
        caseData.getAppeal().getAppellant().getIdentity().setDob("2000-12-31");
        caseData.setCaseCreated("2020-12-31");
        service.generatePdf(caseData, 1L, "appellantEvidence", "fileName");

        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pdfServiceClient).generateFromHtml(any(), argumentCaptor.capture());
        verify(ccdPdfService).updateDoc(eq("fileName"), any(), eq(1L), any(), eq("appellantEvidence"),
                eq(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED));

        List<String> list =  (List) argumentCaptor.getValue().get("welsh_exclude_dates");
        assertEquals("30 Mehefin 2018",((List) argumentCaptor.getValue().get("welsh_exclude_dates")).get(0));
        assertEquals("30 Gorffennaf 2018",((List) argumentCaptor.getValue().get("welsh_exclude_dates")).get(1));
        assertEquals("30 Awst 2018",((List) argumentCaptor.getValue().get("welsh_exclude_dates")).get(2));
        assertEquals("31 Rhagfyr 2000",argumentCaptor.getValue().get("appellant_appointee_identity_dob"));
        assertEquals("31 Rhagfyr 2000",argumentCaptor.getValue().get("appellant_identity_dob"));
        assertEquals("29 Mehefin 2018",argumentCaptor.getValue().get("date_of_decision"));
        assertEquals("31 Rhagfyr 2020",argumentCaptor.getValue().get("welshCurrentDate"));
        assertEquals("Taliad Annibyniaeth Personol (PIP)",argumentCaptor.getValue().get("welshBenefitType"));
        assertEquals("nac ydw",argumentCaptor.getValue().get("welshEvidencePresent"));
        assertEquals("ydw",argumentCaptor.getValue().get("welshWantsToAttend"));
    }


    @Test
    public void createValidPdfAndSendEmailAndStoreInDocumentStore() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        service.generatePdf(caseData, 1L, "appellantEvidence", "fileName");

        verify(pdfServiceClient).generateFromHtml(any(), any());
        verify(ccdPdfService).updateDoc(eq("fileName"), any(), eq(1L), any(), eq("appellantEvidence"),
                eq(null));
    }

    @Test
    public void createValidWelshPdfWithMrnDateNull() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);
        caseData.getAppeal().getMrnDetails().setMrnDate(null);
        caseData.setLanguagePreferenceWelsh("yes");
        service.generatePdf(caseData, 1L, "appellantEvidence", "fileName");

        verify(pdfServiceClient).generateFromHtml(any(), any());
    }

    @Test
    public void givenUserDoesNotWantToAttendHearing_createValidPdfAndSendEmailAndStoreInDocumentStore() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        caseData.getAppeal().setHearingOptions(HearingOptions.builder().wantsToAttend("No").build());

        service.generatePdf(caseData, 1L, "appellantEvidence", "fileName");

        verify(pdfServiceClient).generateFromHtml(any(), any());
        verify(ccdPdfService).updateDoc(eq("fileName"), any(), eq(1L), any(), eq("appellantEvidence"),
                eq(null));

    }

    @Test
    public void givenNoRepresentative_createValidPdfAndSendEmailAndStoreInDocumentStore() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        caseData.getAppeal().setRep(Representative.builder().hasRepresentative("No").build());

        service.generatePdf(caseData, 1L, "appellantEvidence", "fileName");

        verify(pdfServiceClient).generateFromHtml(any(), any());
        verify(ccdPdfService).updateDoc(eq("fileName"), any(), eq(1L), any(), eq("appellantEvidence"),
                eq(null));
    }

    @Test
    public void givenLanguagePreferenceWelsh_createValidPdfAndSendEmailAndStoreInDocumentStoreWithTranslationStatusReqd() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        caseData.getAppeal().setRep(Representative.builder().hasRepresentative("No").build());
        caseData.setLanguagePreferenceWelsh("Yes");

        service.generatePdf(caseData, 1L, "appellantEvidence", "fileName");

        verify(pdfServiceClient).generateFromHtml(any(), any());
        verify(ccdPdfService).updateDoc(eq("fileName"), any(), eq(1L), any(), eq("appellantEvidence"),
                eq(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED));
    }
}
