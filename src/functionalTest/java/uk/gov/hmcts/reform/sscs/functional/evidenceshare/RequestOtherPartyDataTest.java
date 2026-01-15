package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

class RequestOtherPartyDataTest extends AbstractFunctionalTest {

    @Nested
    class CmToggleOn {
        @ParameterizedTest
        @CsvSource({"PIP,VALID_APPEAL_CREATED,withDwp", "CHILD_SUPPORT,VALID_APPEAL_CREATED,awaitOtherPartyData"})
        @SneakyThrows
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
        @Test
        void shouldSendToDwpForValidAppealEvent() {

        }

    }

}
