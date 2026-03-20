package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADD_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.SneakyThrows;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
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

@EnabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
class AddOtherPartyDataFunctionalTest extends AbstractFunctionalTest {

    private static final int TIMEOUT = 30;
    private static final String POSTCODE = "TS1 1ST";
    private static final String ADDRESS_LINE_1 = "10 Coverfield Lane";
    private static final String ADDRESS_LINE_2 = "Glen Close";
    private static final String TOWN = "Brighton";
    private static final String COUNTY = "Carmarthenshire";
    private static final String ADD_OTHER_PARTY = "add other party";

    @Autowired
    private IdamService idamService;

    @Autowired
    private UpdateCcdCaseService updateCcdCaseService;

    @Nested
    class ChildSupport {
        @Test
        @SneakyThrows
        void shouldTransitionToCorrectStateWhenOtherPartyDataAddedToCase() {

            final SscsCaseDetails caseWithState = createCaseFromEvent(Benefit.CHILD_SUPPORT, VALID_APPEAL_CREATED);
            await().atMost(TIMEOUT, SECONDS).untilAsserted(() -> {
                var caseDetails = findCaseById(ccdCaseId);
                assertThat(caseDetails.getState()).isEqualTo(State.AWAIT_OTHER_PARTY_DATA.toString());
            });

            updateCcdCaseService.updateCaseV2(caseWithState.getId(), ADD_OTHER_PARTY_DATA.getCcdType(),
                idamService.getIdamTokens(), cd -> {
                    cd.getData().setOtherParties(List.of(new CcdValue<>(buildOtherParty())));
                    cd.getData().getExtendedSscsCaseData().setAwareOfAnyAdditionalOtherParties(YesNo.YES);
                    return new UpdateCcdCaseService.UpdateResult(ADD_OTHER_PARTY, ADD_OTHER_PARTY);
                });

            await().atMost(TIMEOUT, SECONDS).untilAsserted(() -> assertThatPartyAdded(findCaseById(ccdCaseId)));
        }
    }

    @Nested
    class UniversalCredit {
        @Test
        @SneakyThrows
        void shouldGenerateAddOtherPartyDataNotificationDocument() {
            final SscsCaseDetails caseDetails = createCaseFromEvent(Benefit.UC, VALID_APPEAL_CREATED, caseData -> caseData
                .getAppellant()
                .ifPresent(app -> app.setAddress(Address
                    .builder()
                    .line1(ADDRESS_LINE_1)
                    .line2(ADDRESS_LINE_2)
                    .town(TOWN)
                    .county(COUNTY)
                    .postcode(POSTCODE)
                    .build())));

            await()
                .atMost(TIMEOUT, SECONDS)
                .untilAsserted(() -> assertThat(findCaseById(ccdCaseId).getState()).isIn(State.WITH_DWP.toString()));

            updateCcdCaseService.updateCaseV2(caseDetails.getId(), ADD_OTHER_PARTY_DATA.getCcdType(), idamService.getIdamTokens(),
                cd -> {
                    cd.getData().setOtherParties(List.of(new CcdValue<>(buildOtherParty())));
                    cd.getData().getExtendedSscsCaseData().setAwareOfAnyAdditionalOtherParties(YesNo.YES);
                    return new UpdateCcdCaseService.UpdateResult(ADD_OTHER_PARTY, ADD_OTHER_PARTY);
                });

            assertEventuallyInState(caseDetails.getId(), State.AWAIT_CONFIDENTIALITY_REQUIREMENTS.toString());
            await().atMost(TIMEOUT, SECONDS).untilAsserted(() -> assertThatPartyAdded(findCaseById(ccdCaseId)));

            assertThatPdfTextIsCorrect(getDocument(caseDetails.getId(), "addOtherPartyData"), getExpectedContent(caseDetails));
        }

        private static @NonNull String getExpectedContent(final SscsCaseDetails caseDetails) throws IOException {
            return new ClassPathResource("tyanotifications/addotherparty/addOtherPartyDataExpected.template")
                .getContentAsString(StandardCharsets.UTF_8)
                .replace("${CASE_ID}", caseDetails.getId().toString())
                .replace("${OTHER_PARTY_NAME}", "Bella Kiki")
                .replace("${TODAY_DATE}", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
    }

    private void assertThatPartyAdded(SscsCaseDetails cdAfterEvent) {
        assertThat(cdAfterEvent.getState()).isEqualTo(State.AWAIT_CONFIDENTIALITY_REQUIREMENTS.toString());
        assertThat(cdAfterEvent.getData().getExtendedSscsCaseData().getAwareOfAnyAdditionalOtherParties()).isEqualTo(YesNo.YES);
        final OtherParty otherParty = cdAfterEvent.getData().getOtherParties().getFirst().getValue();
        assertThat(otherParty.getName().getTitle()).isEqualTo("Miss");
        assertThat(otherParty.getName().getFirstName()).isEqualTo("Bella");
        assertThat(otherParty.getName().getLastName()).isEqualTo("Kiki");
        assertThat(otherParty.getAddress().getPostcode()).isEqualTo(POSTCODE);
        assertThat(otherParty.getAddress().getTown()).isEqualTo(TOWN);
        assertThat(otherParty.getAddress().getLine1()).isEqualTo(ADDRESS_LINE_1);
    }

    private OtherParty buildOtherParty() {
        return OtherParty
            .builder()
            .name(Name.builder().title("Miss").firstName("Bella").lastName("Kiki").build())
            .address(Address.builder().postcode(POSTCODE).line1(ADDRESS_LINE_1).town(TOWN).build())
            .build();
    }

}
