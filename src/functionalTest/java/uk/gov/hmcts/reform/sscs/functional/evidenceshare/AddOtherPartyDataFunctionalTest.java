package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADD_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

class AddOtherPartyDataFunctionalTest extends AbstractFunctionalTest {

    private static final String POSTCODE = "IG10 3XX";
    private static final String ADDRESS_LINE_1 = "3 XX Road";
    private static final String TOWN = "Town Test";
    private static final String ADD_OTHER_PARTY = "add other party";

    @Autowired
    private IdamService idamService;

    @Autowired
    private UpdateCcdCaseService updateCcdCaseService;

    @Test
    @SneakyThrows
    @EnabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
    void shouldTransitionToCorrectStateWhenOtherPartyDataAddedToCase() {

        final SscsCaseDetails caseWithState = createCaseFromEvent(Benefit.CHILD_SUPPORT, VALID_APPEAL_CREATED);

        await().atMost(30, SECONDS).untilAsserted(() -> {
            var caseDetails = findCaseById(ccdCaseId);
            assertThat(caseDetails.getState()).isEqualTo(State.AWAIT_OTHER_PARTY_DATA.toString());
        });
        // add other party
        var otherParty = buildOtherParty();

        updateCcdCaseService.updateCaseV2(caseWithState.getId(), ADD_OTHER_PARTY_DATA.getCcdType(), idamService.getIdamTokens(),
            cd -> {
                cd.getData().setOtherParties(List.of(new CcdValue<>(otherParty)));
                cd.getData().getExtendedSscsCaseData().setAwareOfAnyAdditionalOtherParties(YesNo.YES);
                return new UpdateCcdCaseService.UpdateResult(ADD_OTHER_PARTY, ADD_OTHER_PARTY);
            });

        await().atMost(30, SECONDS).untilAsserted(() -> {
            var cdAfterEvent = findCaseById(ccdCaseId);

            assertThat(cdAfterEvent.getState()).isEqualTo(State.AWAIT_CONFIDENTIALITY_REQUIREMENTS.toString());
            assertThat(cdAfterEvent.getData().getExtendedSscsCaseData().getAwareOfAnyAdditionalOtherParties()).isEqualTo(
                YesNo.YES);
            assertThat(cdAfterEvent.getData().getOtherParties().getFirst().getValue().getName().getTitle()).isEqualTo("Miss");
            assertThat(cdAfterEvent.getData().getOtherParties().getFirst().getValue().getName().getFirstName()).isEqualTo(
                "Bella");
            assertThat(cdAfterEvent.getData().getOtherParties().getFirst().getValue().getName().getLastName()).isEqualTo("Kiki");
            assertThat(cdAfterEvent.getData().getOtherParties().getFirst().getValue().getAddress().getPostcode()).isEqualTo(
                POSTCODE);
            assertThat(cdAfterEvent.getData().getOtherParties().getFirst().getValue().getAddress().getTown()).isEqualTo(TOWN);
            assertThat(cdAfterEvent.getData().getOtherParties().getFirst().getValue().getAddress().getLine1()).isEqualTo(
                ADDRESS_LINE_1);
            assertThat(cdAfterEvent.getData().getExtendedSscsCaseData().getConfidentialityTab())
                .contains("Other Party 1 | Bella Kiki | Undetermined |");
        });
    }

    private OtherParty buildOtherParty() {
        return OtherParty.builder().name(Name.builder().title("Miss").firstName("Bella").lastName("Kiki").build())
            .address(Address.builder().postcode(POSTCODE).line1(ADDRESS_LINE_1).town(TOWN).build()).build();
    }
}
