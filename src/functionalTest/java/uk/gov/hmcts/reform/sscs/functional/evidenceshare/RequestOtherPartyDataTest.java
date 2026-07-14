package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

@org.junit.jupiter.api.Disabled
class RequestOtherPartyDataTest extends AbstractFunctionalTest {

    private static final String LINE_1 = "first line";
    private static final String TOWN = "Derby";
    private static final String COUNTY = "Derbyshire";
    private static final String POSTCODE = "AB12 3CD";
    private static final String CM_CONF_ENABLED = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED";

    @Nested
    @EnabledIfEnvironmentVariable(named = CM_CONF_ENABLED, matches = "true")
    @org.junit.jupiter.api.Disabled
class CmToggleOn {

        @Test
        @SneakyThrows
        void shouldTransitionForValidAppealCreated() {
            final SscsCaseDetails caseWithState = createCaseFromEvent(Benefit.CHILD_SUPPORT, EventType.VALID_APPEAL_CREATED,
                RequestOtherPartyDataTest.this::addMinimalCaseData);
            assertEventuallyInState(caseWithState.getId(), "awaitOtherPartyData");
        }
    }

    @Nested
    @EnabledIfEnvironmentVariable(named = CM_CONF_ENABLED, matches = "false")
    @org.junit.jupiter.api.Disabled
class CmToggleOff {

        @Test
        @SneakyThrows
        void shouldTransitionForValidAppealCreated() {
            SscsCaseDetails caseWithState = createCaseFromEvent(Benefit.CHILD_SUPPORT, EventType.VALID_APPEAL_CREATED,
                RequestOtherPartyDataTest.this::addMinimalCaseData);
            assertEventuallyInState(caseWithState.getId(), "withDwp");
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
        data.setEvidencePresent("Yes");
    }

}
