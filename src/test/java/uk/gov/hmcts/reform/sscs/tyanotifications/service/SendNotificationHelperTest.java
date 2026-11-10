package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants.REP_SALUTATION;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;

public class SendNotificationHelperTest {

    @Test
    public void givenCompleteRepresentativeName_whenIgnoreOrganisation_thenReturnsFullName() {
        Representative representative = mock(Representative.class);
        Name name = mock(Name.class);
        when(representative.getName()).thenReturn(name);
        when(name.getFirstName()).thenReturn("Jane");
        when(name.getLastName()).thenReturn("Smith");
        when(name.getFullNameNoTitle()).thenReturn("Jane Smith");

        assertThat(SendNotificationHelper.getRepSalutation(representative, true)).isEqualTo("Jane Smith");
    }

    @Test
    public void givenMissingName_whenOrganisationIsPresent_thenReturnsOrganisation() {
        Representative representative = mock(Representative.class);
        when(representative.getOrganisation()).thenReturn("Example Legal");

        assertThat(SendNotificationHelper.getRepSalutation(representative, false)).isEqualTo("Example Legal");
    }

    @Test
    public void givenMissingName_whenOrganisationMustBeIgnored_thenReturnsDefaultSalutation() {
        Representative representative = mock(Representative.class);
        when(representative.getOrganisation()).thenReturn("Example Legal");

        assertThat(SendNotificationHelper.getRepSalutation(representative, true)).isEqualTo(REP_SALUTATION);
    }

    @Test
    public void givenNullOrganisationAndMissingName_thenReturnsDefaultSalutation() {
        Representative representative = mock(Representative.class);

        assertThat(SendNotificationHelper.getRepSalutation(representative, false)).isEqualTo(REP_SALUTATION);
    }

    @Test
    public void givenBlankOrganisationAndMissingName_thenReturnsDefaultSalutation() {
        Representative representative = mock(Representative.class);
        when(representative.getOrganisation()).thenReturn("");

        assertThat(SendNotificationHelper.getRepSalutation(representative, false)).isEqualTo(REP_SALUTATION);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "undefined"})
    void givenInvalidFirstName_thenUsesOrganisation(String firstName) {
        assertFallbackToOrganisation(firstName, "Smith");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "undefined", "UNDEFINED"})
    void givenInvalidLastName_thenUsesOrganisation(String lastName) {
        assertFallbackToOrganisation("Jane", lastName);
    }

    private void assertFallbackToOrganisation(String firstName, String lastName) {
        Representative representative = mock(Representative.class);
        Name name = mock(Name.class);
        when(representative.getName()).thenReturn(name);
        when(representative.getOrganisation()).thenReturn("Example Legal");
        when(name.getFirstName()).thenReturn(firstName);
        when(name.getLastName()).thenReturn(lastName);

        assertThat(SendNotificationHelper.getRepSalutation(representative, false)).isEqualTo("Example Legal");
    }
}
