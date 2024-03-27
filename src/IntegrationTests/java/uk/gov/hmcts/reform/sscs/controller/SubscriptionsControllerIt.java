package uk.gov.hmcts.reform.sscs.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.MessageAuthenticationService;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_it.properties")
@AutoConfigureMockMvc(addFilters = false)
public class SubscriptionsControllerIt {

    @MockBean
    protected AirLookupService airLookupService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    MessageAuthenticationService macService;

    @Test
    public void shouldValidateMacToken() throws Exception {
        Map<String, Object> macResponseMap = getValidTokenResponseMap();

        when(macService.decryptMacToken("abcde12345"))
                .thenReturn(macResponseMap);

        MvcResult mvcResult = mockMvc.perform(get("/tokens/abcde12345"))
                .andExpect(status().isOk())
                .andReturn();

        String result = mvcResult.getResponse().getContentAsString();

        assertEquals("{\"token\":"
                + "{\"decryptedToken\":\"de-crypted-token\",\"benefitType\":\"002\","
                + "\"subscriptionId\":\"subscriptionId\","
                + "\"appealId\":\"dfdsf435345\"}}", result);
    }


    private Map<String, Object> getValidTokenResponseMap() {
        Map<String, Object> macResponseMap = new HashMap<>();
        macResponseMap.put("decryptedToken", "de-crypted-token");
        macResponseMap.put("benefitType", "002");
        macResponseMap.put("subscriptionId", "subscriptionId");
        macResponseMap.put("appealId", "dfdsf435345");
        return macResponseMap;
    }
}
