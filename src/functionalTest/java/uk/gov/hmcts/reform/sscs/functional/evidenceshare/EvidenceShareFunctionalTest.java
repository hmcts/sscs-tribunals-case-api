package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_TEST_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.functional.utilities.idam.model.User;

class EvidenceShareFunctionalTest extends AbstractFunctionalTest {

    public EvidenceShareFunctionalTest() {
        super();
    }

    @BeforeEach
    void beforeEach() {
        ccdCaseId = null;
    }

    // Toggle off scenario
    @Test
    void processANonDigitalAppealWithValidMrn_shouldGenerateADl6AndAddToCcdAndUpdateState() throws Exception {

        User user = getAccess().withUser(User.builder()
            .email("system.update.stuart.16@hmcts.net")
            .forename("Service")
            .surname("Account")
            .roles(List.of(
                "caseworker",
                "caseworker-sscs",
                "caseworker-sscs-superuser",
                "caseworker-sscs-clerk",
                "caseworker-sscs-systemupdate",
                "caseworker-sscs-judge",
                "caseworker-sscs-dwpresponsewriter",
                "caseworker-sscs-registrar",
                "caseworker-caa"))
            .build());


        // Use the use in the following method

        createNonDigitalCaseWithEvent(CREATE_TEST_CASE, Benefit.CHILD_SUPPORT, State.VALID_APPEAL, user.tokens());
//        createNonDigitalCaseWithEvent(CREATE_TEST_CASE);

        String json = getJson(VALID_APPEAL_CREATED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());
        json = json.replace("CREATED_IN_GAPS_FROM", State.VALID_APPEAL.getId());
        json = json.replaceAll("NINO_TO_BE_REPLACED", getRandomNino());

        simulateCcdCallback(json, user.tokens().getServiceAuthorization());

        defaultAwait().untilAsserted(() -> {
            SscsCaseDetails caseDetails = findCaseById(ccdCaseId);

            SscsCaseData caseData = caseDetails.getData();

            List<SscsDocument> docs = caseData.getSscsDocument();
            assertNotNull(docs);
            assertEquals(1, docs.size());
            assertEquals("dl6-" + ccdCaseId + ".pdf", docs.getFirst().getValue().getDocumentFileName());
            assertEquals("withDwp", caseDetails.getState());
            assertEquals(LocalDate.now().toString(), caseData.getDateSentToDwp());
            //since the SUBMITTED callback no longer contains the updated caseData, the dateCaseSentToGaps will not be present
            //better to test that in the UTs
        });
    }

}