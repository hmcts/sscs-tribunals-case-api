package uk.gov.hmcts.sscs.controller;

import static org.hamcrest.CoreMatchers.containsString;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import uk.gov.hmcts.sscs.domain.corecase.*;
import uk.gov.hmcts.sscs.service.CcdService;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    CcdService ccdService;


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
    public void shouldDoSomething() throws Exception {
        CcdCase ccdCase = createCase();
        when(ccdService.findCcdCaseByAppealNumber("1")).thenReturn(ccdCase);

        mockMvc.perform(get("/appeals/1/surname/a"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(expected)));
    }

    String expected = "{\"appeal\":{\"caseReference\":\"SC/12345\",\"appealNumber\":\"mj876\",\"status\":\"DWP_RESPOND\",\"benefitType\":\"esa\",\"name\":\"Mr A A\",\"latestEvents\":[],\"historicalEvents\":[]}}";
}
