package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;

import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

class RequestOtherPartyDataTest extends AbstractFunctionalTest {

    private static final String CM_CONF_ENABLED = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED";

    @Nested
    @EnabledIfEnvironmentVariable(named = CM_CONF_ENABLED, matches = "true")
    class CmToggleOn {

        @ParameterizedTest
        @CsvSource({"PIP,withDwp", "CHILD_SUPPORT,awaitOtherPartyData"})
        @SneakyThrows
        void shouldTransitionForValidAppealCreated(Benefit benefit, String expectedState) {
            SscsCaseDetails caseWithState = createCaseFromEvent(benefit, EventType.VALID_APPEAL_CREATED);

            assertEventuallyInState(caseWithState.getId(), expectedState);
        }

        @ParameterizedTest
        @CsvSource({"PIP,withDwp", "CHILD_SUPPORT,awaitOtherPartyData"})
        @SneakyThrows
        void shouldTransitionForValidAppeal(Benefit benefit, String expectedState) {
            SscsCaseDetails caseWithState = createCaseFromEvent(benefit, EventType.INCOMPLETE_APPLICATION_RECEIVED);

            makeCaseValid(caseWithState);

            simulateCcdCallback(caseWithState, VALID_APPEAL, EventType.VALID_APPEAL);

            assertEventuallyInState(caseWithState.getId(), expectedState);
        }

        private void makeCaseValid(SscsCaseDetails caseWithState) {
            var appeal = caseWithState.getData().getAppeal();
            var appellant = appeal.getAppellant();

            applyAddress(appellant.getAddress(), "1 Upper West Street", "Birmingham", "Birmingshire", "SW1A 1AA");
            applyName(appellant.getAppointee().getName(), "Mrs", "Elaine", "Cooper");
            applyAddress(appellant.getAppointee().getAddress(), "1 Sion Way", "Manchester", "Manchestershire", "SW1A 1AA");
            applyName(appeal.getRep().getName(), "Mr", "Donald", "Smith");
            applyAddress(appeal.getRep().getAddress(), "1 Main Street", "Dudley", "Dudleyshire", "SW1A 1AA");

            appeal.getHearingOptions().setExcludeDates(List.of());
            appeal.setHearingType("paper");

            updateCaseEvent(EventType.VALID_APPEAL, caseWithState);
        }

        private void applyName(Name name, String title, String firstName, String lastName) {
            name.setTitle(title);
            name.setFirstName(firstName);
            name.setLastName(lastName);
        }

        private void applyAddress(Address address, String line1, String town, String county, String postcode) {
            address.setLine1(line1);
            address.setTown(town);
            address.setCounty(county);
            address.setPostcode(postcode);
        }
    }

    @Nested
    @DisabledIfEnvironmentVariable(named = CM_CONF_ENABLED, matches = "true")
    class CmToggleOff {

        @ParameterizedTest
        @CsvSource({"PIP,VALID_APPEAL_CREATED,withDwp", "CHILD_SUPPORT,VALID_APPEAL_CREATED,withDwp"})
        @SneakyThrows
        void shouldTransitionForValidAppealCreated(Benefit benefit, EventType eventType, String expectedState) {
            SscsCaseDetails caseWithState = createCaseFromEvent(benefit, eventType);
            assertEventuallyInState(caseWithState.getId(), expectedState);
        }
    }

    private void assertEventuallyInState(long caseId, String expectedState) {
        await().atMost(60, SECONDS).pollInterval(2, SECONDS).untilAsserted(() -> {
            SscsCaseDetails caseDetails = findCaseById(Long.toString(caseId));
            assertThat(caseDetails.getState()).isEqualTo(expectedState);
        });
    }
}
