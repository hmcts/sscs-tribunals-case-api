package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_TEST_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.functional.utilities.idam.annotations.IdamUser;
import uk.gov.hmcts.reform.sscs.functional.utilities.idam.annotations.WithIdamUsers;
import uk.gov.hmcts.reform.sscs.functional.utilities.idam.model.User;

/**
 * Example with custom code to create users on the fly and use of annotations to create/use them
 *
 * @WithIdamUsers is responsible for creating the users if they don't exist.
 * @IdamUser is responsible for loading the users and injecting them into the test
 * <p>
 * Uses resources/idam-users.json to create using the required names/roles. By default the user will have super user rights; but this can be over-riden on a per email if required.
 */
@WithIdamUsers(emails = {"system.update.cm.toggle-on@hmcts.net", "system.update.cm.toggle-off@hmcts.net"})
class EvidenceShareFunctionalExampleOneTest extends AbstractFunctionalTest {

    EvidenceShareFunctionalExampleOneTest() {
        super();
    }

    @BeforeEach
    void beforeEach() {
        ccdCaseId = null;
    }

    // Toggle off scenario
    @Test
    void processANonDigitalAppealWithValidMrn_shouldGenerateADl6AndAddToCcdAndUpdateStateToggleOn(@IdamUser(email = "system.update.cm.toggle-on@hmcts.net") User user) throws Exception {

        createNonDigitalCaseWithEvent(CREATE_TEST_CASE, Benefit.CHILD_SUPPORT, State.VALID_APPEAL, user.tokens());

        String json = getJson(VALID_APPEAL_CREATED.getCcdType());
        json = json.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId);
        json = json.replace("MRN_DATE_TO_BE_REPLACED", LocalDate.now().toString());
        json = json.replace("CREATED_IN_GAPS_FROM", State.VALID_APPEAL.getId());
        json = json.replaceAll("NINO_TO_BE_REPLACED", getRandomNino());

        simulateCcdCallback(json, user.tokens());

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

    @ParameterizedTest(name = "firstName={0}, surname={1}")
    @MethodSource("provideUserArguments")
    void processANonDigitalAppealWithValidMrn_shouldGenerateADl6AndAddToCcdAndUpdateStateToggleParamMethod(String firstName, String surname, @IdamUser(email = "system.update.cm.toggle-off@hmcts.net") User user) throws Exception {

        createNonDigitalCaseWithEvent(CREATE_TEST_CASE, Benefit.CHILD_SUPPORT, State.VALID_APPEAL, user.tokens());
    }

    private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> provideUserArguments() {
        return java.util.stream.Stream.of(
            org.junit.jupiter.params.provider.Arguments.of("John", "Smith"),
            org.junit.jupiter.params.provider.Arguments.of("Jane", "Doe"),
            org.junit.jupiter.params.provider.Arguments.of("Bob", "Johnson")
        );
    }

}