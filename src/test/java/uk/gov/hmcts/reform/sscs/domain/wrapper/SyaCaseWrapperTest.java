package uk.gov.hmcts.reform.sscs.domain.wrapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.hamcrest.Matcher;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class SyaCaseWrapperTest {

    @ParameterizedTest
    @MethodSource("generateSyaCaseWrapperScenarios")
    public void givenAppellantOrAppointeeAreNotProvided_ThenGetContactDetailsShouldNotBreak(
        SyaCaseWrapper syaCaseWrapper, Matcher<SyaContactDetails> matcher) {
        SyaContactDetails contactDetails = syaCaseWrapper.getContactDetails();
        assertThat(contactDetails, matcher);
    }

    @SuppressWarnings({"unused"})
    private static Object[] generateSyaCaseWrapperScenarios() {
        SyaCaseWrapper caseWithNullAppellantAndNullAppointee = buildCaseWithAppellantAndAppointeeAreBothNull();
        SyaCaseWrapper caseWithAppellantAndAppointee = buildCaseWithAppellantAndAppointeeNotNull();
        SyaCaseWrapper caseWithAppellantWithSameAddressAsAppointeeAndNullAppointee =
            buildCaseWithAppellantAndNullAppointee();

        return new Object[]{
            new Object[]{caseWithNullAppellantAndNullAppointee, nullValue()},
            new Object[]{caseWithAppellantWithSameAddressAsAppointeeAndNullAppointee,
                equalTo(caseWithAppellantWithSameAddressAsAppointeeAndNullAppointee.getAppellant().getContactDetails())},
            new Object[]{caseWithAppellantAndAppointee,
                equalTo(caseWithAppellantAndAppointee.getAppellant().getContactDetails())}
        };
    }

    private static SyaCaseWrapper buildCaseWithAppellantAndNullAppointee() {
        SyaCaseWrapper caseWithAppellantAndNullAppointee = new SyaCaseWrapper();
        caseWithAppellantAndNullAppointee.setAppellant(buildSyaAppellant(true));
        caseWithAppellantAndNullAppointee.setIsAppointee(true);
        caseWithAppellantAndNullAppointee.setIsAppointee(true);
        caseWithAppellantAndNullAppointee.setAppointee(null);
        return caseWithAppellantAndNullAppointee;
    }

    private static SyaCaseWrapper buildCaseWithAppellantAndAppointeeNotNull() {
        SyaCaseWrapper caseWithAppellantAndAppointeeNotNull = new SyaCaseWrapper();
        caseWithAppellantAndAppointeeNotNull.setAppellant(buildSyaAppellant(false));
        caseWithAppellantAndAppointeeNotNull.setAppointee(buildSyaAppointee());
        return caseWithAppellantAndAppointeeNotNull;
    }

    private static SyaAppointee buildSyaAppointee() {
        SyaAppointee appointee = new SyaAppointee();
        SyaContactDetails appointeeContactDetails = new SyaContactDetails();
        appointeeContactDetails.setEmailAddress("appointee@test.com");
        appointee.setContactDetails(appointeeContactDetails);
        return appointee;
    }

    private static SyaAppellant buildSyaAppellant(boolean isAddressSameAsAppointee) {
        SyaAppellant appellant = new SyaAppellant();
        SyaContactDetails contactDetails = new SyaContactDetails();
        contactDetails.setEmailAddress("appellant@test.com");
        appellant.setContactDetails(contactDetails);
        appellant.setIsAddressSameAsAppointee(isAddressSameAsAppointee);
        return appellant;
    }

    private static SyaCaseWrapper buildCaseWithAppellantAndAppointeeAreBothNull() {
        SyaCaseWrapper syaCaseWrapperWithNullAppellantAndNullAppointee = new SyaCaseWrapper();
        syaCaseWrapperWithNullAppellantAndNullAppointee.setAppellant(null);
        syaCaseWrapperWithNullAppellantAndNullAppointee.setAppointee(null);
        return syaCaseWrapperWithNullAppellantAndNullAppointee;
    }
}