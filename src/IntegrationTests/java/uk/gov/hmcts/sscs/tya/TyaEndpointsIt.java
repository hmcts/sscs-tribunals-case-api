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
import uk.gov.hmcts.sscs.domain.tya.RegionalProcessingCenter;
import uk.gov.hmcts.sscs.service.CcdService;
import uk.gov.hmcts.sscs.service.MessageAuthenticationService;

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
        ccdCase.setRegionalProcessingCenter(populateRegionalProcessingCenter());

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

    @Test
    public void shouldValidateMacToken() throws Exception {
        when(macService.validateMacTokenAndReturnBenefitType("abcde12345")).thenReturn("002");

        MvcResult mvcResult = mockMvc.perform(get("/appeals/tokens/abcde12345"))
                .andExpect(status().isOk())
                .andReturn();

        String result = mvcResult.getResponse().getContentAsString();
        assertThat(result, is("{\"benefitType\":\"002\"}"));
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


    private RegionalProcessingCenter populateRegionalProcessingCenter() {
        RegionalProcessingCenter regionalProcessingCenter = new RegionalProcessingCenter();
        regionalProcessingCenter.setName("BIRMINGHAM");
        regionalProcessingCenter.setAddress1("HM Courts & Tribunals Service");
        regionalProcessingCenter.setAddress2("Social Security & Child Support Appeals");
        regionalProcessingCenter.setAddress3("Administrative Support Centre");
        regionalProcessingCenter.setAddress4("PO Box 14620");
        regionalProcessingCenter.setCity("BIRMINGHAM");
        regionalProcessingCenter.setPostcode("B16 6FR");
        regionalProcessingCenter.setPhoneNumber("0300 123 1142");
        regionalProcessingCenter.setFaxNumber("0126 434 7983");
        return regionalProcessingCenter;
    }
}
