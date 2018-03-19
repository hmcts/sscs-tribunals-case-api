package uk.gov.hmcts.sscs.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.exception.InvalidSubscriptionTokenException;
import uk.gov.hmcts.sscs.model.tya.SubscriptionRequest;
import uk.gov.hmcts.sscs.service.MessageAuthenticationService;
import uk.gov.hmcts.sscs.service.TribunalsService;


@RunWith(MockitoJUnitRunner.class)
public class SubscriptionsControllerTest {

    public static final String APPEAL_ID = "appeal-id";
    public static final String CONTENT = "{\"email\":\"test@gmail.com\",\"mobileNumber\": \"+447777777777\"}";

    @Mock
    private TribunalsService tribunalsService;

    @Mock
    private MessageAuthenticationService macService;

    private MockMvc mockMvc;

    private SubscriptionsController controller;

    @Before
    public void setUp() {
        controller = new SubscriptionsController(macService, tribunalsService);
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    public void shouldValidateToken() throws Exception {
        Map<String,Object> tokenDetails = new HashMap<>();
        tokenDetails.put("subscriptionId", 1L);
        tokenDetails.put("appealId","sdkfdkwfid");
        tokenDetails.put("decryptedToken", "rgfd");
        tokenDetails.put("benefitType","pip");

        given(macService.decryptMacToken("token123")).willReturn(tokenDetails);

        mockMvc.perform(get("/tokens/token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CONTENT)
                .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"token\":{\"decryptedToken\":\"rgfd\",\"benefitType\":\"pip\",\"subscriptionId\":1,\"appealId\":\"sdkfdkwfid\"}}"));
    }

    @Test(expected = InvalidSubscriptionTokenException.class)
    public void shouldThrowInvalidSubscriptionTokenException() throws JsonProcessingException {
        // Given
        when(macService.decryptMacToken(anyString()))
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
        when(tribunalsService.updateSubscription(eq(APPEAL_ID), any(SubscriptionRequest.class)))
                .thenReturn("benefitTypeValue");

        SubscriptionRequest subscriptionRequest = getSubscriptionRequest();

        //When
        ResponseEntity<String> benefitType = controller
                .updateSubscription(subscriptionRequest, APPEAL_ID, "subid");

        // Then
        assertThat(benefitType.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(benefitType.getBody(), equalTo("{\"benefitType\":\"benefitTypeValue\"}"));
    }

    private SubscriptionRequest getSubscriptionRequest() {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setEmail("email@email.com");
        return subscriptionRequest;
    }


}