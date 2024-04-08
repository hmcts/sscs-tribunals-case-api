package uk.gov.hmcts.reform.sscs.tya;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.DWP_RESPOND_OVERDUE_CASE_ID;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.document.DocumentDownloadClientApi;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;


@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_it.properties")
@AutoConfigureMockMvc(addFilters = false)
public class TyaEndpointsIt {

    private static final long CASE_ID = 123456789L;
    private static final String DOC_ID = "6819915a-52fa-4ee2-abb1-7880985806e7";
    private static final String URL = "/documents/6819915a-52fa-4ee2-abb1-7880985806e7/binary";
    private static final String AUTH_TOKEN = "GHAS78232JKAS888";
    private static final String PDF = "PDF";
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    IdamService idamService;

    @MockBean
    CcdService ccdService;

    @MockBean
    CcdClient ccdClient;

    @MockBean
    DocumentDownloadClientApi documentDownloadClientApi;

    @MockBean
    AuthTokenGenerator authTokenGenerator;

    @MockBean
    AirLookupService airLookupService;

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

    @Test
    public void shouldReturnTheDocumentGivenADmStoreUrl() throws Exception {
        when(authTokenGenerator.generate()).thenReturn(AUTH_TOKEN);
        when(documentDownloadClientApi.downloadBinary("oauth2Token", AUTH_TOKEN,"caseworker,citizen","sscs", URL))
                .thenReturn(ResponseEntity.of(Optional.of(new ByteArrayResource(PDF.getBytes()))));

        MvcResult mvcResult = mockMvc.perform(get("/document?url=" + DOC_ID))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(), is(PDF));
    }

}
