package uk.gov.hmcts.sscs.tya;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.sscs.model.tya.SubscriptionRequest;
import uk.gov.hmcts.sscs.service.TribunalsService;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class SubscriptionEndPointsIt {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    TribunalsService tribunalsService;

    @Test
    public void shouldReturnBenefitTypeForSubscriptionUpdate() throws Exception {

        String subscriptionRequest = "{\"subscription\":{\"email\":\"email@email.com\","
                + "\"mobileNumber\" : \"0777777777\"}}";
        when(tribunalsService.updateSubscription(anyString(), any(SubscriptionRequest.class)))
                .thenReturn("002");

        MvcResult mvcResult = mockMvc.perform(post("/appeals/abc2345/subscriptions/subscriptionId")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(subscriptionRequest))
                .andExpect(status().isOk())
                .andReturn();

        String result = mvcResult.getResponse().getContentAsString();

        assertThat(result, equalTo("{\"benefitType\":\"002\"}"));

    }
}
