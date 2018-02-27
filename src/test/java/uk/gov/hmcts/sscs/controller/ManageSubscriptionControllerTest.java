package uk.gov.hmcts.sscs.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import uk.gov.hmcts.sscs.exception.InvalidSubscriptionTokenException;
import uk.gov.hmcts.sscs.service.MessageAuthenticationService;

@RunWith(MockitoJUnitRunner.class)
public class ManageSubscriptionControllerTest {

    @Mock
    private MessageAuthenticationService messageAuthenticationService;

    private ManageSubscriptionController manageSubscriptionController;

    @Before
    public void setUp() {
        manageSubscriptionController = new ManageSubscriptionController(messageAuthenticationService);
    }

    @Test
    public void validateMacToken() {
        // Given
        when(messageAuthenticationService.validateMacTokenAndReturnBenefitType(anyString())).thenReturn("002");

        // When
        ResponseEntity<String> benefitType = manageSubscriptionController.validateMacToken("abcde12345");

        // Then
        assertThat(benefitType.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(benefitType.getBody(), equalTo("{\"benefitType\":\"002\"}"));
    }

    @Test(expected = InvalidSubscriptionTokenException.class)
    public void shouldThrowInvalidSubscriptionTokenException() {
        // Given
        when(messageAuthenticationService.validateMacTokenAndReturnBenefitType(anyString()))
                .thenThrow(InvalidSubscriptionTokenException.class);

        // When
        manageSubscriptionController.validateMacToken("abcde12345");
    }


}