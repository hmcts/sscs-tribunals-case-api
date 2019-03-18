package uk.gov.hmcts.reform.sscs.domain.wrapper;

import org.junit.Test;

public class SyaCaseWrapperTest {

    @Test
    public void givenNullAppellantAndNullAppointee_thenGetContactDetails_shouldReturnNull() {
        SyaAppellant appellant = new SyaAppellant();
        SyaAppointee appointee = new SyaAppointee();

        SyaCaseWrapper syaCaseWrapper = new SyaCaseWrapper();
        syaCaseWrapper.setAppellant(appellant);

        syaCaseWrapper.getContactDetails();


    }
}