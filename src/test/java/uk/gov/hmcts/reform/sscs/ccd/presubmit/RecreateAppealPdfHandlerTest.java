package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.EmailService;
import uk.gov.hmcts.reform.sscs.service.SscsPdfService;

@RunWith(MockitoJUnitRunner.class)
public class RecreateAppealPdfHandlerTest {

    private static final Long CCD_CASE_ID = 1234567890L;
    private static final String DOCUMENT_URL = "http://dm-store:4506/documents/35d53efc-a30d-4b0d-b5a9-312d52bb1a4d";
    private static final String EVIDENCE_URL = "http://dm-store:4506/documents/35d53efc-a45c-a30d-b5a9-412d52bb1a4d";
    @Mock
    private SscsPdfService sscsPdfService;

    @Mock
    private IdamService idamService;


    private IdamTokens idamTokens;

    @Mock
    private EmailService emailService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;


    private RecreateAppealPdfHandler recreateAppealPdfHandler;

    @Before
    public void setUp() {
        when(callback.getEvent()).thenReturn(EventType.CREATE_APPEAL_PDF);
        SscsCaseData caseData = buildCaseDataWithoutPdf();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        recreateAppealPdfHandler = new RecreateAppealPdfHandler(sscsPdfService,
                emailService,
                idamService
        );
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotCallPdfServiceIfEventIsDifferent() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);

        recreateAppealPdfHandler.handle(SUBMITTED, callback);
    }

    @Test
    public void shouldCallPdfService() throws CcdException {

        when(emailService.generateUniqueEmailId(caseDetails.getCaseData().getAppeal().getAppellant())).thenReturn("Test");

        PreSubmitCallbackResponse<SscsCaseData> response = recreateAppealPdfHandler.handle(SUBMITTED, callback);

        assertEquals("No", response.getData().getEvidencePresent());
        assertNotNull(caseDetails.getCaseData().getAppeal().getAppellant().getAppointee());

        verify(emailService, times(2)).generateUniqueEmailId(eq(caseDetails.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService,times(1)).generateAndSendPdf(eq(caseDetails.getCaseData()), any(), eq(idamTokens), any());
    }

    @Test
    public void shouldCallPdfServiceWhenNoAppointee() throws CcdException {

        when(emailService.generateUniqueEmailId(caseDetails.getCaseData().getAppeal().getAppellant())).thenReturn("Test");

        caseDetails.getCaseData().getAppeal().getAppellant().getAppointee().setName(null);

        PreSubmitCallbackResponse<SscsCaseData> response = recreateAppealPdfHandler.handle(SUBMITTED, callback);

        assertEquals("No", caseDetails.getCaseData().getEvidencePresent());
        assertNull(caseDetails.getCaseData().getAppeal().getAppellant().getAppointee());

        verify(emailService, times(2)).generateUniqueEmailId(eq(caseDetails.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService).generateAndSendPdf(eq(caseDetails.getCaseData()), any(), eq(idamTokens), any());
    }

    @Test
    public void shouldNotCallPdfService() throws CcdException {

        SscsCaseData caseDataWithPdf = buildCaseDataWithPdf();

        when(caseDetails.getCaseData()).thenReturn(caseDataWithPdf);

        when(emailService.generateUniqueEmailId(caseDataWithPdf.getAppeal().getAppellant())).thenReturn("Test");

        recreateAppealPdfHandler.handle(SUBMITTED, callback);

        assertNull(caseDetails.getCaseData().getEvidencePresent());

        verify(emailService, times(1)).generateUniqueEmailId(eq(caseDetails.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService, never()).generateAndSendPdf(eq(caseDetails.getCaseData()), any(), eq(idamTokens), any());
    }

    @Test
    public void shouldCallPdfServiceWhenSscsDocumentIsNull() {
        SscsCaseData caseDataWithNullSscsDocument = buildCaseDataWithNullSscsDocument();

        when(caseDetails.getCaseData()).thenReturn(caseDataWithNullSscsDocument);

        when(emailService.generateUniqueEmailId(caseDataWithNullSscsDocument.getAppeal().getAppellant())).thenReturn("Test");

        PreSubmitCallbackResponse<SscsCaseData> response = recreateAppealPdfHandler.handle(SUBMITTED, callback);

        assertEquals("No", response.getData().getEvidencePresent());
        assertNotNull(caseDetails.getCaseData().getAppeal().getAppellant().getAppointee());

        verify(emailService, times(2)).generateUniqueEmailId(eq(caseDetails.getCaseData().getAppeal().getAppellant()));
        verify(sscsPdfService,times(1)).generateAndSendPdf(eq(caseDetails.getCaseData()), any(), eq(idamTokens), any());
    }

    private SscsCaseData buildCaseDataWithoutPdf() {
        SscsCaseData caseData = CaseDataUtils.buildCaseData();
        caseData.setSscsDocument(Collections.emptyList());
        caseData.setCcdCaseId(CCD_CASE_ID.toString());
        return caseData;
    }

    private SscsCaseData buildCaseDataWithNullSscsDocument() {
        SscsCaseData caseData = CaseDataUtils.buildCaseData();
        caseData.setSscsDocument(null);
        caseData.setCcdCaseId(CCD_CASE_ID.toString());
        return caseData;
    }

    private SscsCaseData buildCaseDataWithPdf() {
        SscsCaseData caseData = buildCaseDataWithoutPdf();
        caseData.setSscsDocument(buildDocuments());
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
