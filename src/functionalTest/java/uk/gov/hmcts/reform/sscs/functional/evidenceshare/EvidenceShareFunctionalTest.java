package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_TEST_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

public class EvidenceShareFunctionalTest extends AbstractFunctionalTest {

    public EvidenceShareFunctionalTest() {
        super();
    }

    @BeforeEach
    void beforeEach() {
        ccdCaseId = null;
    }

    @Test
    void processANonDigitalAppealWithNoValidMrnDate_shouldNotBeSentToDwpAndShouldBeUpdatedToFlagError() throws Exception {

        createNonDigitalCaseWithEvent();

        String json = getJson(VALID_APPEAL_CREATED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", "");
        json = json.replace("CREATED_IN_GAPS_FROM", State.VALID_APPEAL.getId());
        json = json.replaceAll("NINO_TO_BE_REPLACED", getRandomNino());

        simulateCcdCallback(json);

        defaultAwait().untilAsserted(() -> {
            SscsCaseDetails caseDetails = findCaseById(ccdCaseId);

            assertThat(caseDetails.getData().getSscsDocument()).isNull();
            assertThat(caseDetails.getState()).isEqualTo("validAppeal");
            assertThat(caseDetails.getData().getHmctsDwpState())
                .containsAnyOf("failedSending", "failedRobotics");
        });
    }

    @Test
    void processANonDigitalAppealWithValidMrn_shouldGenerateADl6AndAddToCcdAndUpdateState() throws Exception {

        createNonDigitalCaseWithEvent();

        String json = getJson(VALID_APPEAL_CREATED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());
        json = json.replace("CREATED_IN_GAPS_FROM", State.VALID_APPEAL.getId());
        json = json.replaceAll("NINO_TO_BE_REPLACED", getRandomNino());

        simulateCcdCallback(json);

        defaultAwait().untilAsserted(() -> {
            SscsCaseDetails caseDetails = findCaseById(ccdCaseId);

            SscsCaseData caseData = caseDetails.getData();

            List<SscsDocument> docs = caseData.getSscsDocument();
            assertThat(docs).isNotNull();
            assertThat(docs).hasSize(1);
            assertThat(docs.getFirst().getValue().getDocumentFileName()).isEqualTo("dl6-" + ccdCaseId + ".pdf");
            assertThat(caseDetails.getState()).isEqualTo("withDwp");
            assertThat(caseData.getDateSentToDwp()).isEqualTo(LocalDate.now().toString());
            //since the SUBMITTED callback no longer contains the updated caseData, the dateCaseSentToGaps will not be present
            //better to test that in the UTs
        });
    }

    @Test
    void processAnAppealWithLateMrn_shouldGenerateADl16AndAddToCcdAndUpdateState() throws Exception {
        createNonDigitalCaseWithEvent();

        String json = getJson(VALID_APPEAL_CREATED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().minusDays(31).toString());
        json = json.replace("CREATED_IN_GAPS_FROM", State.VALID_APPEAL.getId());
        json = json.replaceAll("NINO_TO_BE_REPLACED", getRandomNino());

        simulateCcdCallback(json);

        defaultAwait().untilAsserted(() -> {
            SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
            SscsCaseData caseData = caseDetails.getData();

            List<SscsDocument> docs = caseData.getSscsDocument();

            assertThat(docs).isNotNull();
            assertThat(docs).hasSize(1);
            assertThat(docs.getFirst().getValue().getDocumentFileName()).isEqualTo("dl16-" + ccdCaseId + ".pdf");
            assertThat(caseDetails.getState()).isEqualTo("withDwp");
            assertThat(caseData.getDateSentToDwp()).isEqualTo(LocalDate.now().toString());
        });
    }

    @Test
    void processADigitalAppealWithValidMrn_shouldSendToWithDwpState() throws Exception {

        createDigitalCaseWithEvent(CREATE_TEST_CASE);

        String json = getJson(VALID_APPEAL_CREATED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());
        json = json.replace("CREATED_IN_GAPS_FROM", State.READY_TO_LIST.getId());
        json = json.replaceAll("NINO_TO_BE_REPLACED", getRandomNino());

        simulateCcdCallback(json);

        defaultAwait().untilAsserted(() -> {
            SscsCaseDetails caseDetails = findCaseById(ccdCaseId);

            SscsCaseData caseData = caseDetails.getData();

            assertThat(caseData.getSscsDocument()).isNull();
            assertThat(caseDetails.getState()).isEqualTo("withDwp");
            assertThat(caseData.getDateSentToDwp()).isEqualTo(LocalDate.now().toString());
            assertThat(caseData.getDateCaseSentToGaps()).isNull();
        });
    }
}