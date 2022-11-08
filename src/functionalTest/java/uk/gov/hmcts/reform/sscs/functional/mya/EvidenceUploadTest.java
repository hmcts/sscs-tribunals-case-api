package uk.gov.hmcts.reform.sscs.functional.mya;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidence;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

public class EvidenceUploadTest extends BaseFunctionTest {

    @Test
    public void uploadThenSubmitEvidenceToAppeal() throws IOException, JSONException, InterruptedException {
        CreatedCcdCase createdCcdCase = createCase();

        Thread.sleep(5000L);

        JSONArray draftHearingEvidence = sscsMyaBackendRequests.getDraftHearingEvidence(createdCcdCase.getCaseId());
        assertThat(draftHearingEvidence.length(), is(0));

        sscsMyaBackendRequests.uploadHearingEvidence(createdCcdCase.getCaseId(), "evidence.png");

        draftHearingEvidence = sscsMyaBackendRequests.getDraftHearingEvidence(createdCcdCase.getCaseId());
        assertThat(draftHearingEvidence.length(), is(1));
        assertThat(draftHearingEvidence.getJSONObject(0).getString("file_name"), is("evidence.pdf"));

        sscsMyaBackendRequests.submitHearingEvidence(createdCcdCase.getCaseId(), "some description");

        draftHearingEvidence = sscsMyaBackendRequests.getDraftHearingEvidence(createdCcdCase.getCaseId());
        assertThat(draftHearingEvidence.length(), is(0));

        SscsCaseDetails caseDetails = getCaseDetails(createdCcdCase.getCaseId());

        List<ScannedDocument> scannedDocument = caseDetails.getData().getScannedDocuments();
        assertThat(scannedDocument.size(), is(1));
        String expectedEvidenceUploadFilename = String.format("Appellant upload 1 - %s.pdf", caseDetails.getId());
        assertThat(scannedDocument.get(0).getValue().getFileName(), is(expectedEvidenceUploadFilename));
    }

    @Test
    public void uploadAudioThenSubmitEvidenceToAppeal() throws IOException, JSONException, InterruptedException {
        CreatedCcdCase createdCcdCase = createCase();

        Thread.sleep(5000L);

        JSONArray draftHearingEvidence = sscsMyaBackendRequests.getDraftHearingEvidence(createdCcdCase.getCaseId());
        assertThat(draftHearingEvidence.length(), is(0));

        sscsMyaBackendRequests.uploadHearingEvidence(createdCcdCase.getCaseId(), "evidence.mp3");

        draftHearingEvidence = sscsMyaBackendRequests.getDraftHearingEvidence(createdCcdCase.getCaseId());
        assertThat(draftHearingEvidence.length(), is(1));
        assertThat(draftHearingEvidence.getJSONObject(0).getString("file_name"), is("evidence.mp3"));

        sscsMyaBackendRequests.submitHearingEvidence(createdCcdCase.getCaseId(), "some description");

        draftHearingEvidence = sscsMyaBackendRequests.getDraftHearingEvidence(createdCcdCase.getCaseId());
        assertThat(draftHearingEvidence.length(), is(0));

        SscsCaseDetails caseDetails = getCaseDetails(createdCcdCase.getCaseId());

        assertNull(caseDetails.getData().getScannedDocuments());
        List<AudioVideoEvidence> audioVideoEvidences = caseDetails.getData().getAudioVideoEvidence();
        assertThat(audioVideoEvidences.size(), is(1));
        assertThat(audioVideoEvidences.get(0).getValue().getFileName(), is("evidence.mp3"));

        String expectedEvidenceUploadFilename = String.format("Appellant upload 1 - %s.pdf", caseDetails.getId());
        assertThat(audioVideoEvidences.get(0).getValue().getStatementOfEvidencePdf().getDocumentFilename(), is(expectedEvidenceUploadFilename));
    }

    @Test
    public void getEvidenceCoverSheet() throws IOException {
        CreatedCcdCase createdCcdCase = createCase();

        String coversheet = sscsMyaBackendRequests.getCoversheet(createdCcdCase.getCaseId());
        assertThat(coversheet, is("evidence_cover_sheet.pdf"));
    }

    @Test
    public void shouldDeleteUploadEvidenceForAnAppeal() throws IOException {
        CreatedCcdCase createdCcdCase = createCase();
        sscsMyaBackendRequests.uploadHearingEvidence(createdCcdCase.getCaseId(), "evidence.png");
        SscsCaseDetails caseDetails = getCaseDetails(createdCcdCase.getCaseId());
        JSONArray draftHearingEvidence = sscsMyaBackendRequests.getDraftHearingEvidence(createdCcdCase.getCaseId());
        assertThat(draftHearingEvidence.length(), is(1));
        sscsMyaBackendRequests.deleteUploadEvidence(caseDetails.getId(), draftHearingEvidence.getJSONObject(0).getString("id"));
    }
}
