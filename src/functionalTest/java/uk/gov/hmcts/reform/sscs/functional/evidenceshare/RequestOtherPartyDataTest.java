package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.APPEAL_TO_PROCEED;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsInterlocDirectionDocument;

class RequestOtherPartyDataTest extends AbstractFunctionalTest {

    private static final String CM_CONF_ENABLED = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED";

    @Nested
    @EnabledIfEnvironmentVariable(named = CM_CONF_ENABLED, matches = "true")
    class CmToggleOn {

        @ParameterizedTest
        @CsvSource({"PIP,withDwp", "CHILD_SUPPORT,awaitOtherPartyData"})
        @SneakyThrows
        void shouldTransitionForValidAppealCreated(Benefit benefit, String expectedState) {
            final SscsCaseDetails caseWithState = createCaseFromEvent(benefit, EventType.VALID_APPEAL_CREATED);

            assertEventuallyInState(caseWithState.getId(), expectedState);
        }

        @ParameterizedTest
        @CsvSource({"PIP,withDwp", "CHILD_SUPPORT,awaitOtherPartyData"})
        @SneakyThrows
        void shouldTransitionForAppealToProceed(Benefit benefit, String expectedState) {
            final SscsCaseDetails caseWithState = createCaseFromEvent(benefit, EventType.INCOMPLETE_APPLICATION_RECEIVED);

            makeCaseValid(caseWithState);
            updateCaseEvent(EventType.VALID_APPEAL, caseWithState);

            assertEventuallyInState(caseWithState.getId(), expectedState);
        }

        @ParameterizedTest
        @CsvSource({"PIP,withDwp", "CHILD_SUPPORT,awaitOtherPartyData"})
        @SneakyThrows
        void shouldTransitionForValidAppeal(Benefit benefit, String expectedState) {
            final SscsCaseDetails caseWithState = createCaseFromEvent(benefit, EventType.INCOMPLETE_APPLICATION_RECEIVED);

            updateCaseEvent(EventType.NON_COMPLIANT_SEND_TO_INTERLOC, caseWithState);

            allowCaseToProceed(caseWithState);
            updateCaseEvent(EventType.DIRECTION_ISSUED, caseWithState);

            assertEventuallyInState(caseWithState.getId(), expectedState);

        }

        private void allowCaseToProceed(SscsCaseDetails caseWithState) throws IOException {
            caseWithState.getData().setDirectionTypeDl(
                new DynamicList(new DynamicListItem(APPEAL_TO_PROCEED.toString(), APPEAL_TO_PROCEED.getLabel()), null));
            final UploadResponse upload = uploadDocToDocMgmtStore("evidence-document.pdf");
            caseWithState.getData().setSscsInterlocDirectionDocument(
                new
                    SscsInterlocDirectionDocument("PDF", "Decision", LocalDate.now(),
                    DocumentLink.builder().documentFilename(upload.getDocuments().getFirst().originalDocumentName)
                        .documentBinaryUrl(upload.getDocuments().getFirst().links.binary.href)
                        .documentUrl(upload.getDocuments().getFirst().links.self.href).build()));
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
