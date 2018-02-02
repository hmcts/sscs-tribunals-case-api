package uk.gov.hmcts.sscs.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class TribunalsControllerTest {

    TribunalsController  controller = new TribunalsController();

    @Test
    public void testToReturnNotFoundResponseCodeForRootContext() {
        //When
        ResponseEntity<?> responseEntity = controller.getRootContext();

        //Then
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

}