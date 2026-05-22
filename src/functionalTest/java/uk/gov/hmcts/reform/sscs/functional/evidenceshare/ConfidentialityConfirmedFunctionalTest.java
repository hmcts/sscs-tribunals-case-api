package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADD_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CONFIDENTIALITY_CONFIRMED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import java.time.LocalDate;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

class ConfidentialityConfirmedFunctionalTest extends AbstractFunctionalTest {

    private static final String POSTCODE = "IG10 3XX";
    private static final String ADDRESS_LINE_1 = "3 XX Road";
    private static final String TOWN = "Town Test";
    private static final String FIRST_NAME = "Dummy";
    private static final String TITLE = "Mr";
    private static final String USER = "User";
    private static final String CONFIRMED = "confidentiality confirmed";
    private static final String LINE_1 = "first line";
    private static final String COUNTY = "Derbyshire";

    @Autowired
    private IdamService idamService;

    @Autowired
    private UpdateCcdCaseService updateCcdCaseService;

    @Nested
    @EnabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
    class CmToggleOn {

        @Test
        void shouldTransitionToWithDwpStateWhenConfidentialityConfirmed() {

            final SscsCaseDetails caseWithState = createCaseFromEvent(Benefit.CHILD_SUPPORT, VALID_APPEAL_CREATED,
                this::addMinimalCaseData);

            defaultAwait().untilAsserted(() -> {
                var caseDetails = findCaseById(ccdCaseId);
                assertThat(caseDetails.getState()).isEqualTo(State.AWAIT_OTHER_PARTY_DATA.toString());
            });

            var otherParty = buildOtherParty();
            updateCcdCaseService.updateCaseV2(caseWithState.getId(), ADD_OTHER_PARTY_DATA.getCcdType(),
                idamService.getIdamTokens(), cd -> {
                    cd.getData().setOtherParties(List.of(new CcdValue<>(otherParty)));
                    cd.getData().getExtendedSscsCaseData().setAwareOfAnyAdditionalOtherParties(YesNo.YES);
                    cd.getData().setInterlocReviewState(InterlocReviewState.AWAITING_ADMIN_ACTION);
                    return new UpdateCcdCaseService.UpdateResult("add other party", "add other party");
                });

            defaultAwait().untilAsserted(() -> {
                var cdAfterEvent = findCaseById(ccdCaseId);
                assertThat(cdAfterEvent.getState()).isEqualTo(State.AWAIT_CONFIDENTIALITY_REQUIREMENTS.toString());
                assertThat(cdAfterEvent.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.AWAITING_ADMIN_ACTION);
            });

            updateCcdCaseService.updateCaseV2(caseWithState.getId(), CONFIDENTIALITY_CONFIRMED.getCcdType(),
                idamService.getIdamTokens(), cd -> {
                    cd.getData().getOtherParties().getFirst().getValue().setConfidentialityRequired(YesNo.YES);
                    return new UpdateCcdCaseService.UpdateResult(CONFIRMED, CONFIRMED);
                });

            defaultAwait().untilAsserted(() -> {
                var cdAfterEvent = findCaseById(ccdCaseId);
                assertThat(cdAfterEvent.getState()).isEqualTo(State.WITH_DWP.toString());
                assertThat(cdAfterEvent.getData().getDwpDueDate()).isEqualTo(LocalDate.now().plusDays(42).toString());
            });
        }

        private OtherParty buildOtherParty() {
            return OtherParty
                .builder()
                .name(Name.builder().title(TITLE).firstName(FIRST_NAME).lastName(USER).build())
                .address(Address.builder().postcode(POSTCODE).line1(ADDRESS_LINE_1).town(TOWN).build())
                .build();
        }

        private void addMinimalCaseData(SscsCaseData data) {
            data
                .getAppeal()
                .getAppellant()
                .setAddress(Address.builder().line1(LINE_1).town(TOWN).county(COUNTY).postcode(POSTCODE).build());
            data
                .getAppeal()
                .getRep()
                .setAddress(Address.builder().line1(LINE_1).town(TOWN).county(COUNTY).postcode(POSTCODE).build());
            data.setEvidencePresent("Yes");
        }
    }

    @Nested
    @Disabled("Fails against prod CCD")
    class CmToggleOff {
        @Test
        @SneakyThrows
        void shouldNotHandleConfidentialityConfirmedWhenToggleOff() {
            final SscsCaseDetails caseWithState = createCaseFromEvent(Benefit.CHILD_SUPPORT, VALID_APPEAL_CREATED);

            defaultAwait().untilAsserted(() -> {
                var caseDetails = findCaseById(ccdCaseId);
                assertThat(caseDetails.getState()).isNotEqualTo(State.AWAIT_OTHER_PARTY_DATA.toString());
                assertThat(caseDetails.getState()).isNotEqualTo(State.AWAIT_CONFIDENTIALITY_REQUIREMENTS.toString());
            });

            assertThatThrownBy(
                () -> updateCcdCaseService.updateCaseV2(caseWithState.getId(), CONFIDENTIALITY_CONFIRMED.getCcdType(),
                    idamService.getIdamTokens(), (cd) -> {
                        return new UpdateCcdCaseService.UpdateResult(CONFIRMED, CONFIRMED);
                    })).hasMessageContaining("The case status did not qualify for the event");
        }
    }

}
