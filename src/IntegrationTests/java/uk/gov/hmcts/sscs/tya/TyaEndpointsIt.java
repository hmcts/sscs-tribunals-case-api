package uk.gov.hmcts.sscs.tya;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.sscs.util.SerializeJsonMessageManager.APPEAL_RECEIVED;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.service.CcdService;
import uk.gov.hmcts.sscs.service.MessageAuthenticationService;
import uk.gov.hmcts.sscs.util.SerializeJsonMessageManager;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TyaEndpointsIt {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    CcdService ccdService;

    @MockBean
    MessageAuthenticationService macService;

    @Test
    public void shouldReturnAnAppealGivenAnAppealNumber() throws Exception {
        when(ccdService.findCcdCaseByAppealNumber("1"))
                .thenReturn(SerializeJsonMessageManager.APPEAL_RECEIVED_CCD.getDeserializeMessage());

        MvcResult mvcResult = mockMvc.perform(get("/appeals/1"))
            .andExpect(status().isOk())
            .andReturn();
        String result = mvcResult.getResponse().getContentAsString();
        assertJsonEquals(APPEAL_RECEIVED.getSerializedMessage(), result);
    }

    @Test
    public void shouldValidateSurnameAgainstAppealNumber() throws Exception {
        when(ccdService.findCcdCaseByAppealNumberAndSurname("1", "a")).thenReturn(new CcdCase());

        mockMvc.perform(get("/appeals/1/surname/a"))
            .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnNotFoundForInvalidSurname() throws Exception {
        when(ccdService.findCcdCaseByAppealNumberAndSurname("1", "a")).thenReturn(null);

        mockMvc.perform(get("/appeals/1/surname/a"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void shouldValidateMacToken() throws Exception {
        when(macService.validateMacTokenAndReturnBenefitType("abcde12345")).thenReturn("002");

        MvcResult mvcResult = mockMvc.perform(get("/appeals/tokens/abcde12345"))
                .andExpect(status().isOk())
                .andReturn();

        String result = mvcResult.getResponse().getContentAsString();
        assertThat(result, is("{\"benefitType\":\"002\"}"));
    }
}
