package uk.gov.hmcts.reform.sscs.functional.handlers.createbundle;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.hmcts.reform.sscs.functional.handlers.PdfHelper.getPdf;

import java.util.List;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;
import uk.gov.hmcts.reform.sscs.functional.handlers.UploadDocument;


@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
@Slf4j
public class CreateBundleAboutToSubmitHandlerFunctionalTest extends BaseHandler {

    @Test
    public void checkEditedDocumentInTheBundleIsCorrect() throws Exception {
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
        caseDetails.getData().setPhmeGranted(YesNo.YES);

        runEvent(caseDetails.getData(), EventType.CREATE_BUNDLE);

        final SscsCaseDetails updatedCaseDetails = getByCaseId(caseDetails.getId());
        assertThat(updatedCaseDetails.getData().getCaseBundles(), is(notNullValue()));
        assertThat(updatedCaseDetails.getData().getCaseBundles().size(), is(2));
        final BundleDetails bundle1 = updatedCaseDetails.getData().getCaseBundles().get(0).getValue();
        final BundleDetails bundle2 = updatedCaseDetails.getData().getCaseBundles().get(1).getValue();
        assertThat(bundle1.getTitle(), is("SSCS Bundle Original"));
        assertThat(bundle2.getTitle(), is("SSCS Bundle Edited"));
        validateBundleFolder(bundle1.getFolders());
        validateBundleFolder(bundle2.getFolders());

        verifyAsyncBundleStatus(updatedCaseDetails);
        final SscsCaseDetails asyncUpdate = getByCaseId(caseDetails.getId());
        assertThat(asyncUpdate.getData().getCaseBundles().get(0).getValue().getStitchStatus(), is("DONE"));
        assertThat(asyncUpdate.getData().getCaseBundles().get(0).getValue().getStitchedDocument(), is(notNullValue()));
        assertThat(asyncUpdate.getData().getCaseBundles().get(1).getValue().getStitchStatus(), is("DONE"));
        assertThat(asyncUpdate.getData().getCaseBundles().get(1).getValue().getStitchedDocument(), is(notNullValue()));

    }

    private void validateBundleFolder(List<BundleFolder> bundleFolders) {
        assertThat(bundleFolders.size(), is(2));
        assertThat(bundleFolders.get(0).getValue().getName(), is("DWP"));
        assertThat(bundleFolders.get(0).getValue().getDocuments().size(), is(2));
        assertThat(bundleFolders.get(1).getValue().getName(), is("Further additions"));
        assertThat(bundleFolders.get(1).getValue().getDocuments().size(), is(2));
    }

    @Test
    public void checkBundleAdditionIsAddedCorrectly() throws Exception {
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
        assertThat(updatedCaseDetails.getData().getCaseBundles(), is(notNullValue()));
        assertThat(updatedCaseDetails.getData().getCaseBundles().size(), is(1));
        final List<BundleFolder> folders = updatedCaseDetails.getData().getCaseBundles().get(0).getValue().getFolders();
        assertThat(folders.size(), is(2));
        assertThat(folders.get(0).getValue().getName(), is("DWP"));
        assertThat(folders.get(0).getValue().getDocuments().size(), is(2));
        assertThat(folders.get(1).getValue().getName(), is("Further additions"));
        assertThat(folders.get(1).getValue().getDocuments().size(), is(3));

        verifyAsyncBundleStatus(updatedCaseDetails);
        final SscsCaseDetails asyncUpdate = getByCaseId(caseDetails.getId());
        assertThat(asyncUpdate.getData().getCaseBundles().get(0).getValue().getStitchStatus(), is("DONE"));
        assertThat(asyncUpdate.getData().getCaseBundles().get(0).getValue().getStitchedDocument(), is(notNullValue()));
    }

    private void verifyAsyncBundleStatus(final SscsCaseDetails caseDetails) {
        IntStream.range(0, 30)
                .peek(i -> log.info("sleeping one second to verify the async bundle action has taken place."))
                .peek(i -> sleepOneSecond())
                .mapToObj(i -> getByCaseId(caseDetails.getId()))
                .anyMatch(data -> data.getData().getCaseBundles() != null
                        && data.getData().getCaseBundles().size() > 0
                        && !equalsIgnoreCase(data.getData().getCaseBundles().get(0).getValue().getStitchStatus(), "NEW")
                );
    }

    private void sleepOneSecond() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("Error sleeping", e);
        }
    }

}
