package uk.gov.hmcts.reform.sscs.service.pdf;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.UpdateDocParams;
import uk.gov.hmcts.reform.sscs.domain.wrapper.Statement;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.CcdPdfService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.service.pdf.data.AppellantStatementPdfData;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.PdfService;

@RunWith(JUnitParamsRunner.class)
@ExtendWith(MockitoExtension.class)
public class StoreAppellantStatementServiceTest {

    private static final String APPELLANT_STATEMENT_1 = "Appellant statement 1 - ";
    private static final String APPELLANT_STATEMENT_2 = "Appellant statement 2 - ";
    private static final String APPELLANT_STATEMENT_3 = "Appellant statement 3 - ";
    private static final String APPELLANT_STATEMENT_1_1234567890_PDF = "Appellant statement 1 - 1234567890.pdf";
    private static final String APPELLANT_STATEMENT_2_1234567890_PDF = "Appellant statement 2 - 1234567890.pdf";
    private static final String APPELLANT_STATEMENT_1_1234_5678_9012_3456_PDF = "Appellant statement 1 - 1234-5678-9012-3456.pdf";
    private static final String APPELLANT_STATEMENT_2_1234_5678_9012_3456_PDF = "Appellant statement 2 - 1234-5678-9012-3456.pdf";
    private static final String OTHER_EVIDENCE = "Other evidence";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.WARN);

    @Mock
    private PdfService pdfService;
    @Mock
    private CcdPdfService ccdPdfService;
    @Mock
    private IdamService idamService;
    @Mock
    private PdfStoreService pdfStoreService;

    private StoreAppellantStatementService storeAppellantStatementService;

    @Before
    public void setUp() {
        storeAppellantStatementService = spy(new StoreAppellantStatementService(pdfService,
            "templatePath","templatePath", ccdPdfService, idamService, pdfStoreService));
    }

    @Test
    @Parameters(method = "generateDifferentCaseDataScenarios")
    public void givenCaseDetails_shouldWorkOutDocumentPrefix(SscsCaseData sscsCaseData, String expectedFileName,
                                                             String tya) {
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        Statement statement = new Statement("some statement body text", tya);
        AppellantStatementPdfData data = new AppellantStatementPdfData(sscsCaseDetails, statement);

        String documentPrefix = storeAppellantStatementService.documentNamePrefix(sscsCaseDetails,
                "onlineHearingId", data);

        assertThat(documentPrefix, is(expectedFileName));
    }

    private Object[] generateDifferentCaseDataScenarios() {
        Subscription appellantSubscription = Subscription.builder()
                .tya("someTyaAppellantCode")
                .build();
        Subscription representativeSubscription = Subscription.builder()
                .tya("someTyaRepsCode")
                .build();

        // some corner case scenarios
        SscsCaseData sscsCaseDataWithNoDocs = SscsCaseData.builder().build();
        SscsCaseData sscsCaseDataWithDocWithNullValue = SscsCaseData.builder()
                .scannedDocuments(singletonList(ScannedDocument.builder()
                        .value(null)
                        .build()))
                .build();
        SscsCaseData sscsCaseDataWithDocWithEmptyFilename = SscsCaseData.builder()
                .scannedDocuments(singletonList(ScannedDocument.builder()
                        .value(ScannedDocumentDetails.builder()
                                .fileName("")
                                .build())
                        .build()))
                .build();
        SscsCaseData sscsCaseDataWithDocWithNullFilename = SscsCaseData.builder()
                .scannedDocuments(singletonList(ScannedDocument.builder()
                        .value(ScannedDocumentDetails.builder()
                                .fileName(null)
                                .build())
                        .build()))
                .build();

        return new Object[]{
            new Object[]{sscsCaseDataWithNoDocs, APPELLANT_STATEMENT_1, null},
            new Object[]{buildCaseDataWithSscsDocumentAndGivenScannedDocumentFilename(
                    APPELLANT_STATEMENT_1_1234567890_PDF), APPELLANT_STATEMENT_3, null},
            new Object[]{buildCaseDataWithSscsDocumentAndGivenScannedDocumentFilename(
                    APPELLANT_STATEMENT_2_1234567890_PDF), APPELLANT_STATEMENT_3, null},
            new Object[]{sscsCaseDataWithDocWithNullValue, APPELLANT_STATEMENT_1, null},
            new Object[]{sscsCaseDataWithDocWithEmptyFilename, APPELLANT_STATEMENT_1, null},
            new Object[]{sscsCaseDataWithDocWithNullFilename, APPELLANT_STATEMENT_1, null},
            new Object[]{buildCaseDataWithNoDocsAndWithGivenSubs(appellantSubscription, representativeSubscription),
                "Representative statement 1 - ", "someTyaRepsCode"},
            new Object[]{buildCaseDataWithRepStatementAndGivenSubs(appellantSubscription, representativeSubscription),
                "Representative statement 3 - ", "someTyaRepsCode"},
            new Object[]{buildCaseDataWithRepStatementAndGivenSubs(null, null),
                "Appellant statement 3 - ", "someTyaRepsCode"},
            new Object[]{buildCaseDataWithRepStatementAndGivenSubs(null, null),
                "Appellant statement 3 - ", null},
            new Object[]{buildCaseDataWithRepStatementAndGivenSubs(appellantSubscription, representativeSubscription),
                "Appellant statement 3 - ", null},
            new Object[]{buildCaseDataWithRepStatementAndGivenSubs(appellantSubscription, Subscription.builder()
                    .tya(null).build()), "Appellant statement 3 - ", "someTyaRepsCode"},
            new Object[]{sscsCaseDataWithNoDocs, APPELLANT_STATEMENT_1, "someTyaAppellantCode"},
            new Object[]{buildCaseDataWithSscsDocumentAndGivenScannedDocumentFilename(
                    "Some other document.txt"), APPELLANT_STATEMENT_2, "someTyaAppellantCode"},
            new Object[]{buildCaseDataWithSscsDocumentAndGivenScannedDocumentFilename(
                    APPELLANT_STATEMENT_1_1234_5678_9012_3456_PDF), "Appellant statement 3 - ", "someTyaAppellantCode"},
            new Object[]{sscsCaseDataWithDocWithNullValue, APPELLANT_STATEMENT_1, "someTyaAppellantCode"},
            new Object[]{sscsCaseDataWithDocWithEmptyFilename, APPELLANT_STATEMENT_1, "someTyaAppellantCode"},
            new Object[]{sscsCaseDataWithDocWithNullFilename, APPELLANT_STATEMENT_1, "someTyaAppellantCode"}
        };
    }

    private SscsCaseData buildCaseDataWithNoDocsAndWithGivenSubs(Subscription appellantSubscription, Subscription representativeSubscription) {
        return SscsCaseData.builder()
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(appellantSubscription)
                        .representativeSubscription(representativeSubscription)
                        .build())
                .build();
    }

    @NotNull
    private SscsCaseData buildCaseDataWithRepStatementAndGivenSubs(Subscription appellantSubscription,
                                                                   Subscription representativeSubscription) {
        SscsCaseData sscsCaseDataWithRepStatement = buildCaseDataWithSscsDocumentAndGivenScannedDocumentFilename(
                "Representative statement 1 - 1234-5678-9012-3456.pdf");
        sscsCaseDataWithRepStatement.setSubscriptions(Subscriptions.builder()
                .appellantSubscription(appellantSubscription)
                .representativeSubscription(representativeSubscription)
                .build());
        return sscsCaseDataWithRepStatement;
    }

    private SscsCaseData buildCaseDataWithSscsDocumentAndGivenScannedDocumentFilename(String scannedDocFilename) {
        return SscsCaseData.builder()
                .scannedDocuments(singletonList(ScannedDocument.builder()
                        .value(ScannedDocumentDetails.builder()
                                .fileName(scannedDocFilename)
                                .url(DocumentLink.builder()
                                        .documentUrl("http://dm-store/scannedDoc")
                                        .build())
                                .build())
                        .build()))
                .sscsDocument(singletonList(SscsDocument.builder()
                        .value(SscsDocumentDetails.builder()
                                .documentFileName(StoreAppellantStatementServiceTest.APPELLANT_STATEMENT_2_1234_5678_9012_3456_PDF)
                                .documentLink(DocumentLink.builder()
                                        .documentUrl("http://dm-store/sscsDoc")
                                        .build())
                                .build())
                        .build()))
                .build();
    }

    @Test
    @Parameters({
        "someTyaAppellantCode, Appellant statement 3 - 1234567890.pdf",
        "someTyaRepsCode, Representative statement 3 - 1234567890.pdf"
    })
    public void givenCaseDataWithSomeOtherStatement_shouldCallTheStorePdfWithTheCorrectPdfName(
        String tya, String expectedFilename) {
        when(pdfService.createPdf(any(), eq("templatePath"))).thenReturn(new byte[0]);

        SscsCaseDetails caseDetails = buildSscsCaseDetailsTestData();
        UpdateDocParams params = UpdateDocParams.builder().pdf(new byte[0]).fileName(expectedFilename).caseId(1234567890L).caseData(caseDetails.getData()).documentType(OTHER_EVIDENCE).build();
        when(ccdPdfService.mergeDocIntoCcd(eq(params),
            any(IdamTokens.class)))
            .thenReturn(SscsCaseData.builder().build());

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());

        Statement statement = new Statement("some statement", tya);
        AppellantStatementPdfData data = new AppellantStatementPdfData(caseDetails, statement);

        storeAppellantStatementService.storePdfAndUpdate(1234567890L, "onlineHearingId", data);

        verify(ccdPdfService, times(1)).mergeDocIntoCcd(eq(params), any(IdamTokens.class));
        verify(pdfStoreService, never()).storeDocument(any(), anyString(), anyString());
    }

    @Test
    public void givenCaseDataWithPdfStatementAlreadyCreated_shouldCallTheLoadPdf() {
        doReturn(APPELLANT_STATEMENT_1_1234_5678_9012_3456_PDF)
            .when(storeAppellantStatementService)
            .documentNamePrefix(any(SscsCaseDetails.class), anyString(), any(AppellantStatementPdfData.class));

        doReturn(false)
            .when(storeAppellantStatementService)
            .pdfHasNotAlreadyBeenCreated(any(SscsCaseDetails.class), anyString());

        when(pdfStoreService.download(eq("http://dm-store/scannedDoc")))
            .thenReturn(new byte[0]);


        SscsCaseDetails caseDetails = buildSscsCaseDetailsTestData();
        Statement statement = new Statement("some statement", "someAppealNumber");
        AppellantStatementPdfData data = new AppellantStatementPdfData(caseDetails, statement);

        storeAppellantStatementService.storePdf(1L, "onlineHearingId", data);

        verify(pdfStoreService, times(1))
            .download(eq("http://dm-store/scannedDoc"));
        verify(ccdPdfService, never()).mergeDocIntoCcd(anyString(), any(), anyLong(), any(), any());
        verify(idamService, never()).getIdamTokens();
        verify(pdfService, never()).createPdf(any(), anyString());
    }

    private SscsCaseDetails buildSscsCaseDetailsTestData() {
        SscsCaseData caseData = buildCaseDataWithSscsDocumentAndGivenScannedDocumentFilename(
                APPELLANT_STATEMENT_1_1234_5678_9012_3456_PDF);
        caseData.setCcdCaseId("1234567890");
        caseData.setAppeal(Appeal.builder()
                .appellant(Appellant.builder()
                        .name(Name.builder()
                                .title("Mr")
                                .firstName("firstName")
                                .lastName("lastName")
                                .build())
                        .identity(Identity.builder()
                                .nino("ab123456c")
                                .build())
                        .build())
                .build());
        caseData.setSubscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder()
                        .tya("someTyaAppellantCode")
                        .build())
                .representativeSubscription(Subscription.builder()
                        .tya("someTyaRepsCode")
                        .build())
                .build());
        caseData.setCaseReference("SC0022222");

        return SscsCaseDetails.builder()
                .id(1234567890L)
                .data(caseData)
                .build();
    }
}
