package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants.REP_SALUTATION;

import org.junit.Test;
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

        assertEquals("Jane Smith", SendNotificationHelper.getRepSalutation(representative, true));
    }

    @Test
    public void givenMissingName_whenOrganisationIsPresent_thenReturnsOrganisation() {
        Representative representative = mock(Representative.class);
        when(representative.getOrganisation()).thenReturn("Example Legal");

        assertEquals("Example Legal", SendNotificationHelper.getRepSalutation(representative, false));
    }

    @Test
    public void givenMissingName_whenOrganisationMustBeIgnored_thenReturnsDefaultSalutation() {
        Representative representative = mock(Representative.class);
        when(representative.getOrganisation()).thenReturn("Example Legal");

        assertEquals(REP_SALUTATION, SendNotificationHelper.getRepSalutation(representative, true));
    }

    @Test
    public void givenNullOrganisationAndMissingName_thenReturnsDefaultSalutation() {
        Representative representative = mock(Representative.class);

        assertEquals(REP_SALUTATION, SendNotificationHelper.getRepSalutation(representative, false));
    }

    @Test
    public void givenBlankOrganisationAndMissingName_thenReturnsDefaultSalutation() {
        Representative representative = mock(Representative.class);
        when(representative.getOrganisation()).thenReturn("");

        assertEquals(REP_SALUTATION, SendNotificationHelper.getRepSalutation(representative, false));
    }

    @Test
    public void givenNullFirstName_thenUsesOrganisation() {
        assertFallbackToOrganisation(null, "Smith");
    }

    @Test
    public void givenBlankFirstName_thenUsesOrganisation() {
        assertFallbackToOrganisation("", "Smith");
    }

    @Test
    public void givenUndefinedFirstName_thenUsesOrganisation() {
        assertFallbackToOrganisation("undefined", "Smith");
    }

    @Test
    public void givenNullLastName_thenUsesOrganisation() {
        assertFallbackToOrganisation("Jane", null);
    }

    @Test
    public void givenBlankLastName_thenUsesOrganisation() {
        assertFallbackToOrganisation("Jane", "");
    }

    @Test
    public void givenUndefinedLastName_thenUsesOrganisation() {
        assertFallbackToOrganisation("Jane", "undefined");
    }

    @Test
    public void givenUppercaseUndefinedLastName_thenUsesOrganisation() {
        assertFallbackToOrganisation("Jane", "UNDEFINED");
    }

    private void assertFallbackToOrganisation(String firstName, String lastName) {
        Representative representative = mock(Representative.class);
        Name name = mock(Name.class);
        when(representative.getName()).thenReturn(name);
        when(representative.getOrganisation()).thenReturn("Example Legal");
        when(name.getFirstName()).thenReturn(firstName);
        when(name.getLastName()).thenReturn(lastName);

        assertEquals("Example Legal", SendNotificationHelper.getRepSalutation(representative, false));
    }
}
