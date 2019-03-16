package uk.gov.hmcts.reform.sscs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaBenefitType;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.Authorize;
import uk.gov.hmcts.reform.sscs.idam.IdamApiClient;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.Draft;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class SyaControllerTest {

    private final long ccdId = 1L;
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IdamApiClient idamApiClient;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setUp() {
        mockIdamApi();
        mockCcdApi();
        mockS2sApi();
    }

    private void mockS2sApi() {
        given(authTokenGenerator.generate()).willReturn("s2s token");
    }

    private void mockCcdApi() {
        given(coreCaseDataApi.startForCaseworker(
                anyString(), anyString(), anyString(), anyString(), anyString(), eq("draft")))
                .willReturn(StartEventResponse.builder().build());
        given(coreCaseDataApi.submitForCaseworker(
                anyString(), anyString(), anyString(), anyString(), anyString(), eq(true),
                any(CaseDataContent.class)))
                .willReturn(CaseDetails.builder().id(ccdId).build());
    }

    private void mockIdamApi() {
        Authorize authorize = Authorize.builder()
                .code("idam code")
                .accessToken("idam token")
                .build();
        given(idamApiClient.authorizeCodeType(anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(authorize);
        given(idamApiClient.authorizeToken(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(authorize);
        given(idamApiClient.getUserDetails(anyString())).willReturn(UserDetails.builder().id("idam user Id").build());
    }

    @Test
    public void givenAnDraftIsSaved_shouldReturnCreatedAndTheId() throws Exception {
        SyaCaseWrapper syaCaseWrapper = new SyaCaseWrapper();
        syaCaseWrapper.setBenefitType(new SyaBenefitType("PIP", "pip benefit"));

        mockMvc.perform(post("/drafts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(syaCaseWrapper)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().json(asJsonString(Draft.builder().id(ccdId).build())));
    }

}