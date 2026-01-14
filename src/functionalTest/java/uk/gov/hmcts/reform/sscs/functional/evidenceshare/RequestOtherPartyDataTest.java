package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.SneakyThrows;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
        @Test
        @SneakyThrows
        void shouldSendToDWPForValidAppealEventAndNonChildSupportCases() {
            Benefit benefit = Benefit.PIP;
            SscsCaseDetails caseWithState = createCaseWithState(benefit, VALID_APPEAL_CREATED,
                State.VALID_APPEAL.getId());

            simulateCcdCallback(getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(
                mapToCallback(caseWithState, benefit, State.getById(caseWithState.getState()),
                    VALID_APPEAL_CREATED)));

            defaultAwait().untilAsserted(() -> {
                SscsCaseDetails caseDetails = findCaseById(Long.toString(caseWithState.getId()));
                assertThat(caseDetails.getState()).isEqualTo("withDwp");
            });
        }

        @Test
        @SneakyThrows
        void shouldTransitionToAwaitOtherPartyDataForValidAppealEventAndChildSupportCases() {
            Benefit benefit = Benefit.CHILD_SUPPORT;
            SscsCaseDetails caseWithState = createCaseWithState(benefit, VALID_APPEAL_CREATED,
                State.VALID_APPEAL.getId());

            simulateCcdCallback(getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(
                mapToCallback(caseWithState, benefit, State.getById(caseWithState.getState()),
                    VALID_APPEAL_CREATED)));

            defaultAwait().untilAsserted(() -> {
                SscsCaseDetails caseDetails = findCaseById(Long.toString(caseWithState.getId()));
                assertThat(caseDetails.getState()).isEqualTo("awaitOtherPartyData");
            });
        }
    }

    @Nested
    class CmToggleOff {
        @Test
        void shouldSendToDWPForValidAppealEvent() {

        }

    }

    private @NonNull Callback<SscsCaseData> mapToCallback(SscsCaseDetails caseWithState, Benefit benefit, State state,
                                                          EventType eventType) {
        caseWithState.getData().getAppeal().getMrnDetails().setDwpIssuingOffice(benefit == Benefit.CHILD_SUPPORT ? "Child Support" : "Balham DRT");
        caseWithState.getData().getAppeal()
            .setBenefitType(BenefitType.builder().code(benefit.getShortName()).description(benefit.getDescription()).build());

        return new Callback<>(
            new CaseDetails<>(caseWithState.getId(), caseWithState.getJurisdiction(), state, caseWithState.getData(),
                LocalDateTime.now(), caseWithState.getCaseTypeId()), Optional.empty(), eventType, true);
    }

}
