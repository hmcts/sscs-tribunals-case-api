package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

class AddOtherPartyDataTest extends AbstractFunctionalTest {

    @Nested
    class CmToggleOn {
        @ParameterizedTest
        @CsvSource({"CHILD_SUPPORT,VALID_APPEAL_CREATED,awaitOtherPartyData"
        })
        @SneakyThrows
        @EnabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
        void shouldTransitionToCorrectStateForValidAppealEventBasedOnBenefit(Benefit benefit, EventType eventType,
                                                                             String expectedState) {

            final SscsCaseDetails caseWithState = createCaseFromEvent(benefit, eventType);

            defaultAwait().untilAsserted(() -> {
                SscsCaseDetails caseDetails = findCaseById(Long.toString(caseWithState.getId()));
                assertThat(caseDetails.getState()).isEqualTo(expectedState);
            });
        }
    }

    @Nested
    class CmToggleOff {
        @ParameterizedTest
        @CsvSource({"CHILD_SUPPORT,VALID_APPEAL_CREATED,awaitOtherPartyData"
        })
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
