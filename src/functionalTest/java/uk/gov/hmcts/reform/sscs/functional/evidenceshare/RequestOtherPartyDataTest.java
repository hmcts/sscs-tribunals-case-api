package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

class RequestOtherPartyDataTest extends AbstractFunctionalTest {

    private static final String LINE_1 = "first line";
    private static final String TOWN = "Derby";
    private static final String COUNTY = "Derbyshire";
    private static final String POSTCODE = "AB12 3CD";
    private static final String CM_CONF_ENABLED = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED";

    @Nested
    @EnabledIfEnvironmentVariable(named = CM_CONF_ENABLED, matches = "true")
    class CmToggleOn {

        @ParameterizedTest
        @CsvSource({"CHILD_SUPPORT,awaitOtherPartyData"})
        @SneakyThrows
        void shouldTransitionForValidAppealCreated(Benefit benefit, String expectedState) {
            final SscsCaseDetails caseWithState = createCaseFromEvent(benefit, EventType.VALID_APPEAL_CREATED,
                RequestOtherPartyDataTest.this::addMinimalCaseData);
            assertEventuallyInState(caseWithState.getId(), expectedState);
        }
    }

    @Nested
    @DisabledIfEnvironmentVariable(named = CM_CONF_ENABLED, matches = "true")
    class CmToggleOff {

        @ParameterizedTest
        @CsvSource({"CHILD_SUPPORT,withDwp"})
        @SneakyThrows
        void shouldTransitionForValidAppealCreated(Benefit benefit, String expectedState) {
            SscsCaseDetails caseWithState = createCaseFromEvent(benefit, EventType.VALID_APPEAL_CREATED,
                RequestOtherPartyDataTest.this::addMinimalCaseData);
            assertEventuallyInState(caseWithState.getId(), expectedState);
        }
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
        data.getAppeal().getAppellant().getIdentity().setNino(getRandomNino());
        data.setEvidencePresent("Yes");
    }

}
