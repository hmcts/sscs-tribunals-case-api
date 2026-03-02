package uk.gov.hmcts.reform.sscs.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.sscs.service.AuthorisationService.SERVICE_AUTHORISATION_HEADER;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.ccd.client.model.GetCaseCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.metadatafields.CaseViewField;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.GetCaseCallbackService;

@SpringBootTest(classes = CcdGetCaseCallbackControllerTest.Config.class)
@AutoConfigureMockMvc(addFilters = false)
class CcdGetCaseCallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private AuthorisationService authorisationService;
    @MockitoBean
    private GetCaseCallbackService getCaseCallbackService;

    @Test
    void shouldReturnGetCaseResponseWhenRequestIsValid() throws Exception {
        final GetCaseCallbackResponse response = new GetCaseCallbackResponse();
        final CaseViewField metadataField = new CaseViewField();
        metadataField.setId("[INJECTED_DATA.ENABLE_ADD_OTHER_PARTY_DATA]");
        metadataField.setValue("Yes");
        response.setMetadataFields(List.of(metadataField));
        when(getCaseCallbackService.buildResponse(any())).thenReturn(response);

        final MvcResult mvcResult = mockMvc.perform(post("/ccdGetCase")
                .contentType(MediaType.APPLICATION_JSON)
                .header(SERVICE_AUTHORISATION_HEADER, "s2s-token")
                .content(buildCallbackJson()))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString()).contains("\"metadataFields\"");
        assertThat(mvcResult.getResponse().getContentAsString()).contains("\"value\":\"Yes\"");
        verify(authorisationService).authorise("s2s-token");
        verify(getCaseCallbackService).buildResponse(any());
    }

    @ParameterizedTest
    @CsvSource({
        "false,true",
        "true,false",
        "false,false"
    })
    void shouldReturnBadRequestWhenRequiredInputsAreMissing(final boolean includeServiceAuthHeader,
        final boolean includeBody) throws Exception {
        final var requestBuilder = post("/ccdGetCase")
            .contentType(MediaType.APPLICATION_JSON);

        if (includeServiceAuthHeader) {
            requestBuilder.header(SERVICE_AUTHORISATION_HEADER, "s2s-token");
        }
        if (includeBody) {
            requestBuilder.content(buildCallbackJson());
        }

        mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest());

        verifyNoInteractions(authorisationService);
        verifyNoInteractions(getCaseCallbackService);
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({JacksonAutoConfiguration.class, WebMvcAutoConfiguration.class})
    @Import(CcdGetCaseCallbackController.class)
    static class Config {

    }

    private String buildCallbackJson() throws Exception {
        final SscsCaseData caseData = SscsCaseData.builder().build();
        final CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(
            1234L,
            "SSCS",
            State.WITH_DWP,
            caseData,
            LocalDateTime.now(),
            "Benefit"
        );
        final Callback<SscsCaseData> callback = new Callback<>(
            caseDetails,
            Optional.empty(),
            EventType.ACTION_FURTHER_EVIDENCE,
            false
        );
        return objectMapper.writeValueAsString(callback);
    }
}