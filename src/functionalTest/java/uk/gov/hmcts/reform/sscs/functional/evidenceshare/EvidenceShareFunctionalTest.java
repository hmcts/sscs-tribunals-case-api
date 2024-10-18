package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_TEST_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import java.time.LocalDate;
import java.util.List;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

public class EvidenceShareFunctionalTest extends AbstractFunctionalTest {

    public EvidenceShareFunctionalTest() {
        super();
    }

    @BeforeEach
    public void beforeEach() {
        ccdCaseId = null;
    }

    @Test
    public void processANonDigitalAppealWithValidMrn_shouldGenerateADl6AndAddToCcdAndUpdateState() throws Exception {

        createNonDigitalCaseWithEvent(CREATE_TEST_CASE);

        String json = getJson(VALID_APPEAL_CREATED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());
        json = json.replace("CREATED_IN_GAPS_FROM", State.VALID_APPEAL.getId());
        json = json.replaceAll("NINO_TO_BE_REPLACED", getRandomNino());

        simulateCcdCallback(json);

        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);

        SscsCaseData caseData = caseDetails.getData();

        List<SscsDocument> docs = caseData.getSscsDocument();
        assertEquals(1, docs.size());
        assertEquals("dl6-" + ccdCaseId + ".pdf", docs.get(0).getValue().getDocumentFileName());
        assertEquals("withDwp", caseDetails.getState());
        assertEquals(LocalDate.now().toString(), caseData.getDateSentToDwp());
        assertEquals(LocalDate.now().toString(), caseData.getDateCaseSentToGaps());
    }

    @Test
    public void processANonDigitalAppealWithNoValidMrnDate_shouldNotBeSentToDwpAndShouldBeUpdatedToFlagError() throws Exception {

        createNonDigitalCaseWithEvent(CREATE_TEST_CASE);

        String json = getJson(VALID_APPEAL_CREATED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", "");
        json = json.replace("CREATED_IN_GAPS_FROM", State.VALID_APPEAL.getId());
        json = json.replaceAll("NINO_TO_BE_REPLACED", getRandomNino());

        simulateCcdCallback(json);
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);

        assertNull(caseDetails.getData().getSscsDocument());
        assertEquals("validAppeal", caseDetails.getState());
        //assertEquals("failedSending", caseDetails.getData().getHmctsDwpState());
        assertThat(caseDetails.getData().getHmctsDwpState())
                .containsAnyOf("failedSending", "failedRobotics");

    }

    @Test
    public void processAnAppealWithLateMrn_shouldGenerateADl16AndAddToCcdAndUpdateState() throws Exception {
        createNonDigitalCaseWithEvent(CREATE_TEST_CASE);

        String json = getJson(VALID_APPEAL_CREATED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().minusDays(31).toString());
        json = json.replace("CREATED_IN_GAPS_FROM", State.VALID_APPEAL.getId());
        json = json.replaceAll("NINO_TO_BE_REPLACED", getRandomNino());

        simulateCcdCallback(json);
        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);
        SscsCaseData caseData = caseDetails.getData();

        List<SscsDocument> docs = caseData.getSscsDocument();

        assertNotNull(docs);
        assertEquals(1, docs.size());
        assertEquals("dl16-" + ccdCaseId + ".pdf", docs.get(0).getValue().getDocumentFileName());
        assertEquals("withDwp", caseDetails.getState());
        assertEquals(LocalDate.now().toString(), caseData.getDateSentToDwp());
    }

    @Test
    public void processADigitalAppealWithValidMrn_shouldSendToWithDwpState() throws Exception {

        createDigitalCaseWithEvent(CREATE_TEST_CASE);

        String json = getJson(VALID_APPEAL_CREATED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());
        json = json.replace("CREATED_IN_GAPS_FROM", State.READY_TO_LIST.getId());
        json = json.replaceAll("NINO_TO_BE_REPLACED", getRandomNino());

        simulateCcdCallback(json);

        SscsCaseDetails caseDetails = findCaseById(ccdCaseId);

        SscsCaseData caseData = caseDetails.getData();

        assertNull(caseData.getSscsDocument());
        assertEquals("withDwp", caseDetails.getState());
        assertEquals(LocalDate.now().toString(), caseData.getDateSentToDwp());
        assertNull(caseData.getDateCaseSentToGaps());
    }
}
