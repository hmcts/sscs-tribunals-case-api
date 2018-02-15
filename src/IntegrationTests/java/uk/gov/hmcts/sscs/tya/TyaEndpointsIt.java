package uk.gov.hmcts.sscs.tya;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import uk.gov.hmcts.sscs.domain.corecase.*;
import uk.gov.hmcts.sscs.service.CcdService;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TyaEndpointsIt {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    CcdService ccdService;

    private String expectedAppeal;

    @Before
    public void setUp() {
        expectedAppeal = getExpectedAppeal();
    }

    public CcdCase createCase() {
        Appeal appeal = new Appeal();
        appeal.setAppealNumber("mj876");

        Appellant appellant = new Appellant(new Name("Mr", "A", "A"), null, "", "", "", "");

        CcdCase ccdCase = new CcdCase();
        ccdCase.setCaseReference("SC/12345");
        ccdCase.setBenefitType("ESA");
        ccdCase.setAppealStatus(EventType.DWP_RESPOND.toString());
        ccdCase.setAppeal(appeal);
        ccdCase.setAppellant(appellant);

        return ccdCase;
    }

    @Test
    public void shouldReturnAnAppealGivenAnAppealNumber() throws Exception {
        CcdCase ccdCase = createCase();
        when(ccdService.findCcdCaseByAppealNumber("1")).thenReturn(ccdCase);

        MvcResult mvcResult = mockMvc.perform(get("/appeals/1"))
            .andExpect(status().isOk())
            .andReturn();
        String result = mvcResult.getResponse().getContentAsString();
        assertThat(result, is(expectedAppeal));
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

    private String getExpectedAppeal() {
        String syaCaseJson = "json/appeal.json";
        URL resource = getClass().getClassLoader().getResource(syaCaseJson);
        try {
            String appeal = IOUtils.toString(resource, Charset.defaultCharset());
            return appeal.replaceAll("\n", "");
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
