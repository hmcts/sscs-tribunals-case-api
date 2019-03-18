package uk.gov.hmcts.reform.sscs.domain.wrapper;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class SyaCaseWrapperTest {

    @Test
    @Parameters(method = "generateSyaCaseWrapperScenarios")
    public void givenAppellantOrAppointeeAreNotProvided_ThenGetContactDetailsShouldNotBreak(
        SyaCaseWrapper syaCaseWrapper, Matcher<SyaContactDetails> matcher) {
        SyaContactDetails contactDetails = syaCaseWrapper.getContactDetails();
        assertThat(contactDetails, matcher);
    }

    @SuppressWarnings({"unused"})
    private Object[] generateSyaCaseWrapperScenarios() {
        SyaCaseWrapper caseWithNullAppellantAndNullAppointee = buildCaseWithAppellantAndAppointeeAreBothNull();
        SyaCaseWrapper caseWithAppellantAndAppointee = buildCaseWithAppellantAndAppointeeNotNull();

        return new Object[]{
            new Object[]{caseWithNullAppellantAndNullAppointee, nullValue()},
            new Object[]{caseWithAppellantAndAppointee,
                equalTo(caseWithAppellantAndAppointee.getAppellant().getContactDetails())}
        };
    }

    private SyaCaseWrapper buildCaseWithAppellantAndAppointeeNotNull() {
        SyaCaseWrapper caseWithNullAppellantAndNullAppointee = new SyaCaseWrapper();

        SyaAppellant appellant = new SyaAppellant();
        SyaContactDetails contactDetails = new SyaContactDetails();
        contactDetails.setEmailAddress("appellant@test.com");
        appellant.setContactDetails(contactDetails);
        caseWithNullAppellantAndNullAppointee.setAppellant(appellant);

        SyaAppointee appointee = new SyaAppointee();
        SyaContactDetails appointeeContactDetails = new SyaContactDetails();
        appointeeContactDetails.setEmailAddress("appointee@test.com");
        appointee.setContactDetails(appointeeContactDetails);
        caseWithNullAppellantAndNullAppointee.setAppointee(appointee);
        return caseWithNullAppellantAndNullAppointee;
    }

    private SyaCaseWrapper buildCaseWithAppellantAndAppointeeAreBothNull() {
        SyaCaseWrapper syaCaseWrapperWithNullAppellantAndNullAppointee = new SyaCaseWrapper();
        syaCaseWrapperWithNullAppellantAndNullAppointee.setAppellant(null);
        syaCaseWrapperWithNullAppellantAndNullAppointee.setAppointee(null);
        return syaCaseWrapperWithNullAppellantAndNullAppointee;
    }
}