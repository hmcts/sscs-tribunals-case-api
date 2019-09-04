package uk.gov.hmcts.reform.sscs.tya;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class SubscriptionEndPointsIt {

    public static final String BENEFIT_TYPE_RESPONSE = "{\"benefitType\":\"pip\"}";
    public static final String SUBSCRIPTION_MANAGE_URL = "/appeals/abc2345/subscriptions/subscriptionId";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    CcdClient ccdClient;

    @MockBean
    IdamService idamService;

    CaseDetails caseDetails = CaseDataUtils.buildCaseDetails();


    @Test
    public void shouldReturnBenefitTypeForSubscriptionUpdate() throws Exception {

        String subscriptionRequest = "{\"subscription\":{\"email\":\"email@email.com\"}}";

        when(ccdClient.searchForCaseworker(any(), any())).thenReturn(singletonList(caseDetails));
        when(ccdClient.startEvent(any(), any(), any())).thenReturn(StartEventResponse.builder().build());
        when(ccdClient.submitEventForCaseworker(any(), any(), any())).thenReturn(caseDetails);

        MvcResult mvcResult = mockMvc
                .perform(post(SUBSCRIPTION_MANAGE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(subscriptionRequest))
                .andExpect(status().isOk())
                .andReturn();

        String result = mvcResult.getResponse().getContentAsString();

        assertThat(result, equalTo(BENEFIT_TYPE_RESPONSE));

    }

    @Test
    public void shouldReturnBenefitTypeForDeleteSubscription() throws Exception {

        when(ccdClient.searchForCaseworker(any(), any())).thenReturn(singletonList(caseDetails));
        when(ccdClient.startEvent(any(), any(), any())).thenReturn(StartEventResponse.builder().build());
        when(ccdClient.submitEventForCaseworker(any(), any(), any())).thenReturn(caseDetails);

        MvcResult mvcResult = mockMvc
                .perform(delete(SUBSCRIPTION_MANAGE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String result = mvcResult.getResponse().getContentAsString();

        assertThat(result, equalTo(BENEFIT_TYPE_RESPONSE));

    }
}
