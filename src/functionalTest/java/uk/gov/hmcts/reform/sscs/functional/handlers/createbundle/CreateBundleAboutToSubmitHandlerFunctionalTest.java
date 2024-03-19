package uk.gov.hmcts.reform.sscs.functional.handlers.createbundle;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.functional.handlers.PdfHelper.getPdf;

import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;
import uk.gov.hmcts.reform.sscs.functional.handlers.UploadDocument;


@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
@Slf4j
public class CreateBundleAboutToSubmitHandlerFunctionalTest extends BaseHandler {

    @BeforeEach
    public void setUpTest() {
        super.setUp();
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "PREVIEW_BRANCH_WITH_LOCAL_CCD", matches = "true")
    public void checkEditedDocumentInTheBundleIsCorrect() throws IOException {
        SscsCaseDetails caseDetails = createCase();
        List<UploadDocument> docs = List.of(
            UploadDocument.builder()
                .data(getPdf("dwpResponse"))
                .filename("dwpResponse.pdf")
                .documentType(DwpDocumentType.DWP_RESPONSE.getValue())
                .hasEditedDocumentLink(true)
                .build(),
            UploadDocument.builder()
                .data(getPdf("dwpEvidenceBundle"))
                .filename("dwpEvidenceBundle.pdf")
                .documentType(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue())
                .hasEditedDocumentLink(true)
                .build()
        );
        caseDetails = addDocumentsToCase(caseDetails.getData(), docs);
        caseDetails.getData().setPhmeGranted(YES);

        runEvent(caseDetails.getData(), EventType.CREATE_BUNDLE);

        final SscsCaseDetails updatedCaseDetails = getByCaseId(caseDetails.getId());

        assertThat(updatedCaseDetails.getData().getCaseBundles())
            .hasSize(2)
            .extracting(Bundle::getValue)
            .allSatisfy(bundleDetails -> {
                assertThat(bundleDetails.getFolders())
                    .hasSize(2)
                    .extracting(BundleFolder::getValue)
                    .extracting(BundleFolderDetails::getName)
                    .containsExactly("FTA", "Further additions");

                assertThat(bundleDetails.getFolders())
                    .extracting(BundleFolder::getValue)
                    .extracting(BundleFolderDetails::getDocuments)
                    .extracting(List::size)
                    .containsExactly(2, 0);
            })
            .extracting(BundleDetails::getTitle)
            .containsExactly("SSCS Bundle Original", "SSCS Bundle Edited");

        assertThatBundleStitchedSuccessfully(caseDetails.getId(), 2);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "PREVIEW_BRANCH_WITH_LOCAL_CCD", matches = "true")
    public void checkBundleAdditionIsAddedCorrectly() throws IOException {
        SscsCaseDetails caseDetails = createCase();
        List<UploadDocument> docs = List.of(
            UploadDocument.builder()
                .data(getPdf("appellant"))
                .filename("appellant.pdf")
                .documentType(DocumentType.APPELLANT_EVIDENCE.getValue())
                .bundleAddition("A")
                .build(),
            UploadDocument.builder()
                .data(getPdf("dwpResponse"))
                .filename("dwpResponse.pdf")
                .documentType(DwpDocumentType.DWP_RESPONSE.getValue())
                .build(),
            UploadDocument.builder()
                .data(getPdf("dwpEvidenceBundle"))
                .filename("dwpEvidenceBundle.pdf")
                .documentType(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue())
                .build()
        );
        caseDetails = addDocumentsToCase(caseDetails.getData(), docs);
        runEvent(caseDetails.getData(), EventType.CREATE_BUNDLE);

        final SscsCaseDetails updatedCaseDetails = getByCaseId(caseDetails.getId());

        assertThat(updatedCaseDetails.getData().getCaseBundles())
            .hasSize(1)
            .extracting(Bundle::getValue)
            .allSatisfy(bundleDetails -> {
                assertThat(bundleDetails.getFolders())
                    .hasSize(2)
                    .extracting(BundleFolder::getValue)
                    .extracting(BundleFolderDetails::getName)
                    .containsExactly("FTA", "Further additions");

                assertThat(bundleDetails.getFolders())
                    .extracting(BundleFolder::getValue)
                    .extracting(BundleFolderDetails::getDocuments)
                    .extracting(List::size)
                    .containsExactly(2, 1);
            });

        assertThatBundleStitchedSuccessfully(caseDetails.getId(), 1);
    }

    private void assertThatBundleStitchedSuccessfully(long caseId, int expectedBundles) {
        await()
            .atMost(60, SECONDS)
            .pollInterval(1, SECONDS)
            .untilAsserted(() -> {
                SscsCaseDetails updatedCaseDetails = getByCaseId(caseId);
                assertThat(updatedCaseDetails.getData().getCaseBundles())
                    .isNotEmpty()
                    .extracting(Bundle::getValue)
                    .extracting(BundleDetails::getStitchStatus)
                    .doesNotContainNull()
                    .doesNotContain("NEW");
            });

        SscsCaseDetails updatedCaseDetails = getByCaseId(caseId);

        assertThat(updatedCaseDetails.getData().getCaseBundles())
            .hasSize(expectedBundles)
            .extracting(Bundle::getValue)
            .allSatisfy(bundleDetails -> {
                assertThat(bundleDetails.getStitchedDocument()).isNotNull();
                assertThat(bundleDetails.getStitchStatus()).isEqualTo("DONE");
            });
    }
}
