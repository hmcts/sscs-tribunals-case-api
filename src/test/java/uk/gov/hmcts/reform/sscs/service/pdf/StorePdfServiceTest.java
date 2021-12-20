package uk.gov.hmcts.reform.sscs.service.pdf;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import java.net.URISyntaxException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.UpdateDocParams;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfAppealDetails;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.CcdPdfService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.service.pdf.data.PdfData;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.OldPdfService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.PdfService;

public class StorePdfServiceTest {
    private static final String TITLE = "title";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String NINO = "nino";
    private static final String CASE_ID = "caseId";

    private PdfService pdfService;
    private CcdPdfService sscsPdfService;
    private long caseId;
    private Object pdfContent;
    private String fileNamePrefix;
    private StorePdfService<?, PdfData> storePdfService;
    private IdamTokens idamTokens;
    private String someOnlineHearingId;
    private PdfStoreService pdfStoreService;

    @Before
    public void setUp() {
        pdfService = mock(OldPdfService.class);
        sscsPdfService = mock(CcdPdfService.class);
        IdamService idamService = mock(IdamService.class);
        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        caseId = 123L;
        pdfContent = new Object();
        fileNamePrefix = "test name";
        pdfStoreService = mock(PdfStoreService.class);
        storePdfService = new StorePdfService<Object, PdfData>(
            pdfService, "sometemplate","sometemplate", sscsPdfService, idamService, pdfStoreService) {

            @Override
            protected String documentNamePrefix(SscsCaseDetails caseDetails, String onlineHearingId, PdfData data) {
                return fileNamePrefix;
            }

            @Override
            protected Object getPdfContent(PdfData caseDetails, String onlineHearingId, PdfAppealDetails appealDetails) {
                return pdfContent;
            }
        };
        someOnlineHearingId = "someOnlineHearingId";
    }

    @Test
    public void storePdf() {
        SscsCaseDetails caseDetails = createCaseDetails();
        byte[] expectedPdfBytes = {2, 4, 6, 0, 1};
        when(pdfService.createPdf(pdfContent, "sometemplate")).thenReturn(expectedPdfBytes);
        String expectedCaseId = "expectedCcdCaseId";
        UpdateDocParams params = UpdateDocParams.builder().pdf(expectedPdfBytes).fileName(fileNamePrefix + CASE_ID + ".pdf").caseId(caseId).caseData(caseDetails.getData()).documentType("Other evidence").build();
        when(sscsPdfService.updateDoc(params))
                .thenReturn(SscsCaseData.builder().ccdCaseId(expectedCaseId).build());

        MyaEventActionContext myaEventActionContext = storePdfService.storePdf(caseId, someOnlineHearingId, new PdfData(caseDetails));

        verify(sscsPdfService).updateDoc(params);
        assertThat(myaEventActionContext.getPdf().getContent(), is(new ByteArrayResource(expectedPdfBytes)));
        assertThat(myaEventActionContext.getPdf().getName(), is(fileNamePrefix + CASE_ID + ".pdf"));
        assertThat(myaEventActionContext.getDocument().getData().getCcdCaseId(), is(expectedCaseId));
    }

    @Test
    public void storeWelshPdf() {
        SscsCaseDetails caseDetails = createCaseDetails();
        caseDetails.getData().setLanguagePreferenceWelsh("Yes");
        byte[] expectedPdfBytes = {2, 4, 6, 0, 1};
        when(pdfService.createPdf(pdfContent, "sometemplate")).thenReturn(expectedPdfBytes);
        String expectedCaseId = "expectedCcdCaseId";

        UpdateDocParams params = UpdateDocParams.builder().pdf(expectedPdfBytes).fileName(fileNamePrefix + CASE_ID + ".pdf").caseId(caseId).caseData(caseDetails.getData()).documentType("Other evidence").build();
        when(sscsPdfService.updateDoc(params))
                .thenReturn(SscsCaseData.builder().ccdCaseId(expectedCaseId).build());

        MyaEventActionContext myaEventActionContext = storePdfService.storePdf(caseId, someOnlineHearingId, new PdfData(caseDetails));

        verify(sscsPdfService).updateDoc(params);
        assertThat(myaEventActionContext.getPdf().getContent(), is(new ByteArrayResource(expectedPdfBytes)));
        assertThat(myaEventActionContext.getPdf().getName(), is(fileNamePrefix + CASE_ID + ".pdf"));
        assertThat(myaEventActionContext.getDocument().getData().getCcdCaseId(), is(expectedCaseId));
    }

    @Test
    public void storePdfAndUpdate() {
        SscsCaseDetails caseDetails = createCaseDetails();
        byte[] expectedPdfBytes = {2, 4, 6, 0, 1};
        when(pdfService.createPdf(pdfContent, "sometemplate")).thenReturn(expectedPdfBytes);
        String expectedCaseId = "expectedCcdCaseId";
        UpdateDocParams params = UpdateDocParams.builder().pdf(expectedPdfBytes).fileName(fileNamePrefix + CASE_ID + ".pdf").caseId(caseId).caseData(caseDetails.getData()).documentType("Other evidence").build();
        when(sscsPdfService.mergeDocIntoCcd(params, idamTokens))
                .thenReturn(SscsCaseData.builder().ccdCaseId(expectedCaseId).build());

        MyaEventActionContext myaEventActionContext = storePdfService.storePdfAndUpdate(caseId, someOnlineHearingId, new PdfData(caseDetails));

        verify(sscsPdfService).mergeDocIntoCcd(params, idamTokens);
        assertThat(myaEventActionContext.getPdf().getContent(), is(new ByteArrayResource(expectedPdfBytes)));
        assertThat(myaEventActionContext.getPdf().getName(), is(fileNamePrefix + CASE_ID + ".pdf"));
        assertThat(myaEventActionContext.getDocument().getData().getCcdCaseId(), is(expectedCaseId));
    }

    @Test
    public void doNotStorePdfIfCaseAlreadyHasATribunalsView() throws URISyntaxException {
        String documentUrl = "http://example.com/someDocument";
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .ccdCaseId(CASE_ID)
                .sscsDocument(singletonList(SscsDocument.builder()
                        .value(SscsDocumentDetails.builder()
                                .documentFileName(fileNamePrefix + CASE_ID + ".pdf")
                                .documentLink(DocumentLink.builder().documentUrl(documentUrl).build())
                                .build())
                        .build()))
                .appeal(Appeal.builder().appellant(Appellant.builder().name(Name.builder()
                        .title("Mr")
                        .firstName("Jean")
                        .lastName("Valjean")
                        .build()
                ).build()).build()).build();
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        byte[] expectedPdfBytes = {2, 4, 6, 0, 1};
        when(pdfStoreService.download(documentUrl)).thenReturn(expectedPdfBytes);

        MyaEventActionContext myaEventActionContext = storePdfService.storePdf(caseId, someOnlineHearingId, new PdfData(sscsCaseDetails));

        verify(sscsPdfService, never()).mergeDocIntoCcd(anyString(), any(), anyLong(), any(), any());
        assertThat(myaEventActionContext.getPdf().getContent(), is(new ByteArrayResource(expectedPdfBytes)));
        assertThat(myaEventActionContext.getPdf().getName(), is(fileNamePrefix + CASE_ID + ".pdf"));
        assertThat(myaEventActionContext.getDocument(), is(sscsCaseDetails));
    }

    private SscsCaseDetails createCaseDetails() {
        return SscsCaseDetails.builder()
                .id(1234567890L)
                .data(SscsCaseData.builder()
                        .ccdCaseId(CASE_ID)
                        .appeal(Appeal.builder()
                                .appellant(Appellant.builder()
                                        .name(Name.builder()
                                                .title(TITLE)
                                                .firstName(FIRST_NAME)
                                                .lastName(LAST_NAME)
                                                .build())
                                        .identity(Identity.builder()
                                                .nino(NINO)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }
}
