package uk.gov.hmcts.reform.sscs.tya;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.sscs.util.SerializeJsonMessageManager.APPEAL_RECEIVED;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.MessageAuthenticationService;
import uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager;


@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TyaEndpointsIt {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    IdamService idamService;

    @MockBean
    CcdService ccdService;

    @MockBean
    MessageAuthenticationService macService;

    IdamTokens idamTokens;


    @Test
    public void shouldReturnAnAppealGivenAnAppealNumber() throws Exception {
        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        when(ccdService.findCaseByAppealNumber("1", idamTokens))
                .thenReturn(SscsCaseDetails.builder().id(1L).data(SerializeJsonMessageManager.APPEAL_RECEIVED_CCD.getDeserializeMessage()).build());

        MvcResult mvcResult = mockMvc.perform(get("/appeals/1"))
            .andExpect(status().isOk())
            .andReturn();
        String result = mvcResult.getResponse().getContentAsString();
        assertJsonEquals(APPEAL_RECEIVED.getSerializedMessage(), result);


    }

    @Test
    public void shouldValidateSurnameAgainstAppealNumber() throws Exception {
        when(ccdService.findCcdCaseByAppealNumberAndSurname("1", "a", idamTokens)).thenReturn(SscsCaseData.builder().build());

        mockMvc.perform(get("/appeals/1/surname/a"))
            .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnNotFoundForInvalidSurname() throws Exception {
        when(ccdService.findCcdCaseByAppealNumberAndSurname("1", "a", idamTokens)).thenReturn(null);

        mockMvc.perform(get("/appeals/1/surname/a"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void shouldValidateMacToken() throws Exception {
        Map<String, Object> macResponseMap = getValidTokenResponseMap();

        when(macService.decryptMacToken("abcde12345"))
                .thenReturn(macResponseMap);

        MvcResult mvcResult = mockMvc.perform(get("/tokens/abcde12345"))
                .andExpect(status().isOk())
                .andReturn();

        String result = mvcResult.getResponse().getContentAsString();

        assertThat(result, equalTo("{\"token\":"
                +  "{\"decryptedToken\":\"de-crypted-token\",\"benefitType\":\"002\","
                +  "\"subscriptionId\":\"subscriptionId\","
                +  "\"appealId\":\"dfdsf435345\"}}"));
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
