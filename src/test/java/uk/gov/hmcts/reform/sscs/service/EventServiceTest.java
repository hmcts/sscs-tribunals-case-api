package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_APPEAL_PDF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(MockitoJUnitRunner.class)
public class EventServiceTest {

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

    private EventService eventService;

    @Before
    public void setUp() {
        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        eventService = new EventService(sscsPdfService,
                idamService,
                emailService);
    }

    @Test
    public void shouldReturnFalseSendEventIfDifferent() {
        SscsCaseData caseData = null;
        boolean validEvent = eventService.sendEvent(APPEAL_RECEIVED, caseData);

        assertFalse(validEvent);
    }

    @Test
    public void shouldReturnTrueSendEvent() {
        SscsCaseData caseData = null;
        boolean validEvent = eventService.sendEvent(CREATE_APPEAL_PDF, caseData);

        assertTrue(validEvent);
    }

    @Test
    public void shouldNotCallPdfServiceIfEventIsDifferent() {
        SscsCaseData caseData = buildCaseDataWithoutPdf();

        boolean handled = eventService.handleEvent(APPEAL_RECEIVED, caseData);

        assertFalse(handled);

        verify(emailService, never()).generateUniqueEmailId(eq(caseData.getAppeal().getAppellant()));
        verify(sscsPdfService, never()).generateAndSendPdf(eq(caseData), any(), eq(idamTokens), any());
    }

    @Test
    public void shouldCallPdfService() throws CcdException {

        SscsCaseData caseData = buildCaseDataWithoutPdf();

        when(emailService.generateUniqueEmailId(caseData.getAppeal().getAppellant())).thenReturn("Test");

        boolean handled = eventService.handleEvent(CREATE_APPEAL_PDF, caseData);

        assertTrue(handled);
        assertEquals("No", caseData.getEvidencePresent());
        assertNotNull(caseData.getAppeal().getAppellant().getAppointee());

        verify(emailService, times(2)).generateUniqueEmailId(eq(caseData.getAppeal().getAppellant()));
        verify(sscsPdfService,times(1)).generateAndSendPdf(eq(caseData), any(), eq(idamTokens), any());
    }

    @Test
    public void shouldCallPdfServiceWhenNoAppointee() throws CcdException {

        SscsCaseData caseData = buildCaseDataWithoutPdf();

        when(emailService.generateUniqueEmailId(caseData.getAppeal().getAppellant())).thenReturn("Test");

        caseData.getAppeal().getAppellant().getAppointee().setName(null);

        boolean handled = eventService.handleEvent(CREATE_APPEAL_PDF, caseData);

        assertTrue(handled);
        assertEquals("No", caseData.getEvidencePresent());
        assertNull(caseData.getAppeal().getAppellant().getAppointee());

        verify(emailService, times(2)).generateUniqueEmailId(eq(caseData.getAppeal().getAppellant()));
        verify(sscsPdfService).generateAndSendPdf(eq(caseData), any(), eq(idamTokens), any());
    }

    @Test
    public void shouldNotCallPdfService() throws CcdException {

        SscsCaseData caseData = buildCaseDataWithPdf();

        when(emailService.generateUniqueEmailId(caseData.getAppeal().getAppellant())).thenReturn("Test");

        boolean handled = eventService.handleEvent(CREATE_APPEAL_PDF, caseData);

        assertTrue(handled);
        assertNull(caseData.getEvidencePresent());

        verify(emailService, times(1)).generateUniqueEmailId(eq(caseData.getAppeal().getAppellant()));
        verify(sscsPdfService, never()).generateAndSendPdf(eq(caseData), any(), eq(idamTokens), any());
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
