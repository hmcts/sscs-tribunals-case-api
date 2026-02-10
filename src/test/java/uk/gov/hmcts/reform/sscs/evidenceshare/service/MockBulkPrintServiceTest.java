package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;

import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.thirdparty.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.BulkPrintServiceHelper;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.MockBulkPrintService;
import uk.gov.hmcts.reform.sscs.service.CcdNotificationsPdfService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;


@RunWith(MockitoJUnitRunner.class)
public class MockBulkPrintServiceTest {

    @Mock
    private PdfStoreService pdfStoreService;

    @Mock
    private PDFServiceClient pdfServiceClient;

    @Mock
    private CcdService ccdService;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;

    private MockBulkPrintService mockBulkPrintService;

    private CcdNotificationsPdfService ccdNotificationsPdfService;

    private BulkPrintServiceHelper bulkPrintServiceHelper;

    @Before
    public void setUp() {
        ccdNotificationsPdfService = new CcdNotificationsPdfService(pdfStoreService, pdfServiceClient, ccdService, updateCcdCaseService, idamService);
        bulkPrintServiceHelper = new BulkPrintServiceHelper(ccdNotificationsPdfService);
        this.mockBulkPrintService = new MockBulkPrintService(ccdNotificationsPdfService, bulkPrintServiceHelper);

    }

    @Test
    public void sendToMockBulkPrint() {
        Optional<UUID> letterIdOptional = mockBulkPrintService.sendToBulkPrint(singletonList(new Pdf("myData".getBytes(), "file.pdf")),
            SscsCaseData.builder().build(), null);
        assertEquals(Optional.of(UUID.fromString("abc123ca-c336-11e9-9cb5-123456789abc")), letterIdOptional);
    }

    @Test
    public void sendToMockBulkPrintReasonableAdjustment() {
        Optional<UUID> letterIdOptional = mockBulkPrintService.sendToBulkPrint(
            singletonList(new Pdf("myData".getBytes(), "file.pdf")),
            SscsCaseData.builder().ccdCaseId("12345678").build(), APPELLANT_LETTER, EventType.VALID_APPEAL_CREATED, null);
    }
}
