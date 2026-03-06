package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADD_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CONFIDENTIALITY_CONFIRMED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

class ConfidentialityConfirmedFunctionalTest extends AbstractFunctionalTest {

    private static final String POSTCODE = "IG10 3XX";
    private static final String ADDRESS_LINE_1 = "3 XX Road";
    private static final String TOWN = "Town Test";

    @Autowired
    private IdamService idamService;

    @Autowired
    private UpdateCcdCaseService updateCcdCaseService;

    @Nested
    @EnabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
    class CmToggleOn {

        @Test
        @SneakyThrows
        void shouldTransitionToWithDwpStateWhenConfidentialityConfirmed() {

            final SscsCaseDetails caseWithState = createCaseFromEvent(Benefit.CHILD_SUPPORT,
                VALID_APPEAL_CREATED);

            await().atMost(30, SECONDS).untilAsserted(() -> {
                var caseDetails = findCaseById(ccdCaseId);
                assertThat(caseDetails.getState()).isEqualTo(State.AWAIT_OTHER_PARTY_DATA.toString());
            });

            var otherParty = buildOtherParty("Mr", "Dummy", "User");
            updateCcdCaseService.updateCaseV2(caseWithState.getId(), ADD_OTHER_PARTY_DATA.getCcdType(), idamService.getIdamTokens(), (cd) -> {
                cd.getData().setOtherParties(List.of(new CcdValue<>(otherParty)));
                cd.getData().getExtendedSscsCaseData().setAwareOfAnyAdditionalOtherParties(YesNo.YES);
                cd.getData().setInterlocReviewState(InterlocReviewState.AWAITING_ADMIN_ACTION);
                return new UpdateCcdCaseService.UpdateResult("add other party", "add other party");
            });

            await().atMost(30, SECONDS).untilAsserted(() -> {
                var cdAfterEvent = findCaseById(ccdCaseId);
                assertThat(cdAfterEvent.getState()).isEqualTo(State.AWAIT_CONFIDENTIALITY_REQUIREMENTS.toString());
                assertThat(cdAfterEvent.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.AWAITING_ADMIN_ACTION);
            });

            updateCcdCaseService.updateCaseV2(caseWithState.getId(), CONFIDENTIALITY_CONFIRMED.getCcdType(), idamService.getIdamTokens(), (cd) -> {
                cd.getData().getOtherParties().getFirst().getValue().setConfidentialityRequired(YesNo.YES);
                return new UpdateCcdCaseService.UpdateResult("confidentiality confirmed", "confidentiality confirmed");
            });

            await().atMost(30, SECONDS).untilAsserted(() -> {
                var cdAfterEvent = findCaseById(ccdCaseId);
                assertThat(cdAfterEvent.getState()).isEqualTo(State.WITH_DWP.toString());
                assertThat(cdAfterEvent.getData().getInterlocReviewState()).isEqualTo(null);
            });
        }
    }

    @Nested
    @DisabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
    class CmToggleOff {
        @Test
        @SneakyThrows
        void shouldNotHandleConfidentialityConfirmedWhenToggleOff() {
            final SscsCaseDetails caseWithState = createCaseFromEvent(Benefit.CHILD_SUPPORT,
                VALID_APPEAL_CREATED);

            await().atMost(30, SECONDS).untilAsserted(() -> {
                var caseDetails = findCaseById(ccdCaseId);
                assertThat(caseDetails.getState()).isNotEqualTo(State.AWAIT_OTHER_PARTY_DATA.toString());
                assertThat(caseDetails.getState()).isNotEqualTo(State.AWAIT_CONFIDENTIALITY_REQUIREMENTS.toString());
            });

            assertThatThrownBy(() -> updateCcdCaseService.updateCaseV2(caseWithState.getId(), CONFIDENTIALITY_CONFIRMED.getCcdType(), idamService.getIdamTokens(), (cd) -> {
                return new UpdateCcdCaseService.UpdateResult("confidentiality confirmed", "confidentiality confirmed");
            })).hasMessageContaining("The case status did not qualify for the event");
        }
    }

    private OtherParty buildOtherParty(String title, String firstName, String lastName) {
        return OtherParty.builder()
            .name(Name.builder().title(title).firstName(firstName)
                .lastName(lastName)
                .build())
            .address(Address.builder()
                .postcode(POSTCODE)
                .line1(ADDRESS_LINE_1)
                .town(TOWN)
                .build())
            .build();
    }
}
