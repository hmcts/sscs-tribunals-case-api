package uk.gov.hmcts.sscs.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.exception.InvalidSubscriptionTokenException;
import uk.gov.hmcts.sscs.model.ccd.Subscription;
import uk.gov.hmcts.sscs.service.MessageAuthenticationService;
import uk.gov.hmcts.sscs.service.TribunalsService;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionsControllerTest {

    public static final String APPEAL_ID = "appeal-id";

    @Mock
    private TribunalsService tribunalsService;

    @Mock
    private MessageAuthenticationService macService;

    private SubscriptionsController controller;

    @Before
    public void setUp() {
        controller = new SubscriptionsController(macService, tribunalsService);
    }

    @Test
    public void validateMacToken() {
        // Given
        when(macService.validateMacTokenAndReturnBenefitType(anyString())).thenReturn("002");

        // When
        ResponseEntity<String> benefitType = controller.validateMacToken("abcde12345");

        // Then
        assertThat(benefitType.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(benefitType.getBody(), equalTo("{\"benefitType\":\"002\"}"));
    }

    @Test(expected = InvalidSubscriptionTokenException.class)
    public void shouldThrowInvalidSubscriptionTokenException() {
        // Given
        when(macService.validateMacTokenAndReturnBenefitType(anyString()))
            .thenThrow(InvalidSubscriptionTokenException.class);

        // When
        controller.validateMacToken("abcde12345");
    }

    @Test
    public void unsubscribeForGivenAppealNumber() throws CcdException {
        // Given
        when(tribunalsService.unsubscribe(APPEAL_ID, "reason")).thenReturn("benefitTypeValue");

        // When
        ResponseEntity<String> benefitType = controller.unsubscribe(APPEAL_ID, "reason");

        // Then
        assertThat(benefitType.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(benefitType.getBody(), equalTo("{\"benefitType\":\"benefitTypeValue\"}"));
    }

    @Test
    public void updateSubscriptionForGivenAppealNumber() throws Exception {
        // Given
        when(tribunalsService.updateSubscription(eq(APPEAL_ID), any(Subscription.class))).thenReturn("benefitTypeValue");

        Subscription subscription = Subscription.builder()
                .email("testuser@gmail.com")
                .mobile("07788996655")
                .subscribeEmail("Yes")
                .subscribeSms("Yes")
                .build();

        //When
        ResponseEntity<String> benefitType = controller.updateSubscription(APPEAL_ID, subscription);

        // Then
        assertThat(benefitType.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(benefitType.getBody(), equalTo("{\"benefitType\":\"benefitTypeValue\"}"));
    }


}