package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class SscsPdfServiceTest {

    private SscsPdfService service;

    private static final String TEMPLATE_PATH = "/templates/appellant_appeal_template.html";

    private static final String WELSH_TEMPLATE_PATH = "/templates/appellant_appeal_welsh_template.html";

    @Mock
    private PDFServiceClient pdfServiceClient;

    @Mock
    CcdPdfService ccdPdfService;

    SscsCaseData caseData = buildCaseData();

    @Before
    public void setup() {
        initMocks(this);
        service = new SscsPdfService(TEMPLATE_PATH, WELSH_TEMPLATE_PATH, pdfServiceClient, ccdPdfService);
    }

    @Test
    public void createValidWelshPdfAndSendEmailAndStoreInDocumentStore() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);
        caseData.setLanguagePreferenceWelsh("Yes");
        service.generatePdf(caseData, 1L, "appellantEvidence", "fileName");

        verify(pdfServiceClient).generateFromHtml(any(), any());
        verify(ccdPdfService).updateDoc(eq("fileName"), any(), eq(1L), any(), eq("appellantEvidence"));
    }


    @Test
    public void createValidPdfAndSendEmailAndStoreInDocumentStore() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        service.generatePdf(caseData, 1L, "appellantEvidence", "fileName");

        verify(pdfServiceClient).generateFromHtml(any(), any());
        verify(ccdPdfService).updateDoc(eq("fileName"), any(), eq(1L), any(), eq("appellantEvidence"));
    }

    @Test
    public void givenUserDoesNotWantToAttendHearing_createValidPdfAndSendEmailAndStoreInDocumentStore() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        caseData.getAppeal().setHearingOptions(HearingOptions.builder().wantsToAttend("No").build());

        service.generatePdf(caseData, 1L, "appellantEvidence", "fileName");

        verify(pdfServiceClient).generateFromHtml(any(), any());
        verify(ccdPdfService).updateDoc(eq("fileName"), any(), eq(1L), any(), eq("appellantEvidence"));
    }

    @Test
    public void givenNoRepresentative_createValidPdfAndSendEmailAndStoreInDocumentStore() {
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), any())).willReturn(expected);

        caseData.getAppeal().setRep(Representative.builder().hasRepresentative("No").build());

        service.generatePdf(caseData, 1L, "appellantEvidence", "fileName");

        verify(pdfServiceClient).generateFromHtml(any(), any());
        verify(ccdPdfService).updateDoc(eq("fileName"), any(), eq(1L), any(), eq("appellantEvidence"));
    }
}
