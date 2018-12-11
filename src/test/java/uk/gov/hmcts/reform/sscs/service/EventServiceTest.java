package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.model.NotificationEventType.CREATE_APPEAL_PDF;
import static uk.gov.hmcts.reform.sscs.model.NotificationEventType.DO_NOT_SEND;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mail.javamail.JavaMailSender;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(MockitoJUnitRunner.class)
public class EventServiceTest {

    public static final Long CCD_CASE_ID = 1234567890L;
    public static final String DOCUMENT_URL = "http://dm-store:4506/documents/35d53efc-a30d-4b0d-b5a9-312d52bb1a4d";
    @Mock
    private SscsPdfService sscsPdfService;

    @Mock
    private IdamService idamService;

    @Mock
    private RoboticsService roboticsService;

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @Mock
    private CcdService ccdService;

    private IdamTokens idamTokens;

    private EmailService emailService;

    private EventService eventService;

    @Before
    public void setUp() {
        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        emailService = new EmailService(mock(JavaMailSender.class));
        eventService = new EventService(sscsPdfService,
                idamService,
                ccdService);
    }

    @Test
    public void shouldCallPdfService() throws CcdException {

        SscsCaseData caseData = buildCaseDataWithoutPdf();
        SscsCaseDetails caseDetails = SscsCaseDetails.builder().data(caseData).id(CCD_CASE_ID).build();

        when(ccdService.getByCaseId(CCD_CASE_ID, idamTokens)).thenReturn(caseDetails);

        boolean handled = eventService.handleEvent(CREATE_APPEAL_PDF, caseData);

        assertTrue(handled);

        verify(sscsPdfService).generateAndSendPdf(eq(caseData), any(), eq(idamTokens));
    }

    @Test
    public void shouldNotCallPdfService() throws CcdException {

        SscsCaseData caseData = buildCaseDataWithPdf();
        SscsCaseDetails caseDetails = SscsCaseDetails.builder().data(caseData).id(CCD_CASE_ID).build();

        when(ccdService.getByCaseId(CCD_CASE_ID, idamTokens)).thenReturn(caseDetails);

        boolean handled = eventService.handleEvent(CREATE_APPEAL_PDF, caseData);

        assertTrue(handled);

        verify(sscsPdfService).generateAndSendPdf(eq(caseData), any(), eq(idamTokens));
    }

    @Test
    public void shouldNotHandleEvent() throws CcdException {

        boolean handled = eventService.handleEvent(DO_NOT_SEND, null);

        verify(sscsPdfService, never()).generateAndSendPdf(any(), any(), any());

        assertFalse(handled);

    }

    private SscsCaseData buildCaseDataWithoutPdf() {
        SscsCaseData caseData = CaseDataUtils.buildCaseData();
        caseData.setCcdCaseId(CCD_CASE_ID.toString());
        return caseData;
    }

    private SscsCaseData buildCaseDataWithPdf() {
        SscsCaseData caseData = buildCaseDataWithoutPdf();
        caseData.setSscsDocument(buildDocuments(caseData));
        return caseData;
    }

    private List<SscsDocument> buildDocuments(SscsCaseData caseData) {
        List<SscsDocument> list = new ArrayList<>();

        String fileName = emailService.generateUniqueEmailId(caseData.getAppeal().getAppellant()) + ".pdf";

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
        return list;
    }

}
