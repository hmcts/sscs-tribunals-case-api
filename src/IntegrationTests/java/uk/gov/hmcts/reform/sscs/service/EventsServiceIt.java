package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SEND_TO_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SENT_TO_DWP;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.idam.Authorize;
import uk.gov.hmcts.reform.sscs.idam.IdamApiClient;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_it.properties")
@AutoConfigureMockMvc
public class EventsServiceIt {

    @MockBean
    private CcdClient ccdClient;

    @MockBean
    private IdamApiClient idamApiClient;

    @MockBean
    @Qualifier("authTokenGenerator")
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthorisationService authService;

    @Before
    public void setup() {
        given(ccdClient.startEvent(any(), any(), anyString())).willReturn(StartEventResponse.builder().eventId("12345").build());
        given(ccdClient.submitEventForCaseworker(any(), any(), any())).willReturn(CaseDetails.builder().id(123456789876L).build());

        Authorize authorize = new Authorize("redirectUrl/", "code", "token");
        given(idamApiClient.authorizeCodeType(anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(authorize);
        given(idamApiClient.authorizeToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(authorize);

        given(authTokenGenerator.generate()).willReturn("authToken");
        given(idamApiClient.getUserDetails(anyString())).willReturn(new UserDetails("userId"));
    }

    @Test
    public void givenAValidAppealEventSyaCase_thenUpdateCaseWithSentToDwpEvent() throws Exception {
        String json = getCase("json/appealReceivedCallback.json");
        json = json.replaceAll("appealReceived", "validAppeal");

        mockMvc.perform(post("/send")
                .contentType(MediaType.APPLICATION_JSON)
                .header("serviceauthorization", "testServiceAuth")
                .content(json))
                .andExpect(status().isOk());

        verify(ccdClient).startEvent(any(), any(), eq(SENT_TO_DWP.getCcdType()));
        verify(ccdClient).submitEventForCaseworker(any(), any(), any());
    }

    @Test
    public void givenAnInterlocValidAppealEventSyaCase_thenUpdateCaseWithSentToDwpEvent() throws Exception {
        String json = getCase("json/appealReceivedCallback.json");
        json = json.replaceAll("appealReceived", "interlocValidAppeal");

        mockMvc.perform(post("/send")
                .contentType(MediaType.APPLICATION_JSON)
                .header("serviceauthorization", "testServiceAuth")
                .content(json))
                .andExpect(status().isOk());

        verify(ccdClient).startEvent(any(), any(), eq(SENT_TO_DWP.getCcdType()));
        verify(ccdClient).submitEventForCaseworker(any(), any(), any());
    }

    @Test
    public void givenAValidAppealEventBulkScanCase_thenUpdateCaseWithSentToDwpEvent() throws Exception {
        String json = getCase("json/appealReceivedCallback.json");
        json = json.replaceAll("appealReceived", "validAppeal");
        json = json.replaceAll("ONLINE", "PAPER");

        mockMvc.perform(post("/send")
                .contentType(MediaType.APPLICATION_JSON)
                .header("serviceauthorization", "testServiceAuth")
                .content(json))
                .andExpect(status().isOk());

        verify(ccdClient).startEvent(any(), any(), eq(SEND_TO_DWP.getCcdType()));
        verify(ccdClient).submitEventForCaseworker(any(), any(), any());
    }

    @Test
    public void givenAnInterlocValidAppealEventBulkScanCase_thenUpdateCaseWithSentToDwpEvent() throws Exception {
        String json = getCase("json/appealReceivedCallback.json");
        json = json.replaceAll("appealReceived", "interlocValidAppeal");
        json = json.replaceAll("ONLINE", "PAPER");

        mockMvc.perform(post("/send")
                .contentType(MediaType.APPLICATION_JSON)
                .header("serviceauthorization", "testServiceAuth")
                .content(json))
                .andExpect(status().isOk());

        verify(ccdClient).startEvent(any(), any(), eq(SEND_TO_DWP.getCcdType()));
        verify(ccdClient).submitEventForCaseworker(any(), any(), any());
    }

    private String getCase(String path) {
        String syaCaseJson = path;
        URL resource = getClass().getClassLoader().getResource(syaCaseJson);
        try {
            return IOUtils.toString(resource, Charset.defaultCharset());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
