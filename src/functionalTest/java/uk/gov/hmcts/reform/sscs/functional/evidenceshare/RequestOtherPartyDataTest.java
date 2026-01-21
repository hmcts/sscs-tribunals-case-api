package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

class RequestOtherPartyDataTest extends AbstractFunctionalTest {

    @Nested
    class CmToggleOn {
        @ParameterizedTest
        @CsvSource({"PIP,withDwp", "CHILD_SUPPORT,awaitOtherPartyData"})
        @SneakyThrows
        @EnabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
        void shouldTransitionToCorrectStateForValidAppealCreatedEventBasedOnBenefit(Benefit benefit, String expectedState) {

            final SscsCaseDetails caseWithState = createCaseFromEvent(benefit, EventType.VALID_APPEAL_CREATED);

            defaultAwait().untilAsserted(() -> {
                SscsCaseDetails caseDetails = findCaseById(Long.toString(caseWithState.getId()));
                assertThat(caseDetails.getState()).isEqualTo(expectedState);
            });
        }

        @ParameterizedTest
        @CsvSource({"PIP,withDwp", "CHILD_SUPPORT,awaitOtherPartyData"})
        @SneakyThrows
        @EnabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
        void shouldTransitionToCorrectStateForValidAppealEventBasedOnBenefit(Benefit benefit, String expectedState) {

            final SscsCaseDetails caseWithState = createCaseFromEvent(benefit, EventType.INCOMPLETE_APPLICATION_RECEIVED);
            updateCaseDataToBeValid(caseWithState);

            simulateCcdCallback(caseWithState, VALID_APPEAL, EventType.VALID_APPEAL);

            defaultAwait().untilAsserted(() -> {
                SscsCaseDetails caseDetails = findCaseById(Long.toString(caseWithState.getId()));
                assertThat(caseDetails.getState()).isEqualTo(expectedState);
            });
        }

        private void updateCaseDataToBeValid(SscsCaseDetails caseWithState) {

            applyAddress(caseWithState.getData().getAppeal().getAppellant().getAddress(), "1 Upper West Street", "Birmingham",
                "Birmingshire", "SW1A 1AA");

            applyName(caseWithState.getData().getAppeal().getAppellant().getAppointee().getName(), "Mrs", "Elaine", "Cooper");
            applyAddress(caseWithState.getData().getAppeal().getAppellant().getAppointee().getAddress(), "1 Sion Way",
                "Manchester", "Manchestershire", "SW1A 1AA");

            applyName(caseWithState.getData().getAppeal().getRep().getName(), "Mr", "Donald", "Smith");
            applyAddress(caseWithState.getData().getAppeal().getRep().getAddress(), "1 Main Street", "Dudley", "Dudleyshire",
                "SW1A 1AA");

            caseWithState.getData().getAppeal().getHearingOptions().setExcludeDates(List.of());
            caseWithState.getData().getAppeal().setHearingType("paper");

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
    class CmToggleOff {
        @ParameterizedTest
        @CsvSource({"PIP,VALID_APPEAL_CREATED,withDwp", "CHILD_SUPPORT,VALID_APPEAL_CREATED,withDwp"})
        @SneakyThrows
        @DisabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
        void shouldTransitionToCorrectStateForValidAppealEventBasedOnBenefit(Benefit benefit, EventType eventType,
                                                                             String expectedState) {

            final SscsCaseDetails caseWithState = createCaseFromEvent(benefit, eventType);

            defaultAwait().untilAsserted(() -> {
                SscsCaseDetails caseDetails = findCaseById(Long.toString(caseWithState.getId()));
                assertThat(caseDetails.getState()).isEqualTo(expectedState);
            });
        }

    }

}
