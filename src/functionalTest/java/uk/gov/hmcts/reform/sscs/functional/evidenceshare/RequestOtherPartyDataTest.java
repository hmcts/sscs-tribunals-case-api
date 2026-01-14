package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.REQUEST_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.SneakyThrows;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

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
