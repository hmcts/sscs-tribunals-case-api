package uk.gov.hmcts.reform.sscs.tya;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.DWP_RESPOND_OVERDUE_CASE_ID;

import java.util.Collections;
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
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;


@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_it.properties")
@AutoConfigureMockMvc
public class TyaEndpointsIt {

    private static final long CASE_ID = 123456789L;
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    IdamService idamService;

    @MockBean
    CcdService ccdService;

    @MockBean
    CcdClient ccdClient;

    IdamTokens idamTokens;

    @Test
    public void shouldReturnAnAppealGivenACaseId() throws Exception {
        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        Map<String, Object> data = new HashMap<>();
        data.put("caseCreated", "2019-06-06");
        data.put("appeal", Collections.singletonMap("benefitType", Collections.singletonMap("code", "PIP")));

        when(ccdClient.readForCaseworker(idamTokens, CASE_ID))
            .thenReturn(CaseDetails.builder().data(data).id(CASE_ID).build());

        MvcResult mvcResult = mockMvc.perform(get("/appeals?caseId=" + CASE_ID))
            .andExpect(status().isOk())
            .andReturn();

        String result = mvcResult.getResponse().getContentAsString();
        assertJsonEquals(DWP_RESPOND_OVERDUE_CASE_ID.getSerializedMessage(), result);
    }

}
