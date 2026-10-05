package uk.gov.hmcts.reform.sscs.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.MASKED_STRING_VALUE;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getMaskedEmail;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getMaskedPhone;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sscs.exception.GetCaseException;
import uk.gov.hmcts.reform.sscs.model.service.ServiceHearingRequest;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyDetails;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.ServiceHearingValues;
import uk.gov.hmcts.reform.sscs.model.service.linkedcases.ServiceLinkedCases;
import uk.gov.hmcts.reform.sscs.model.single.hearing.IndividualDetails;
import uk.gov.hmcts.reform.sscs.service.ServiceHearingsService;
import uk.gov.hmcts.reform.sscs.util.LogCaptureExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integration")
class ServiceHearingsControllerTest {

    private static final long CASE_ID = 1625080769409918L;

    private static final long MISSING_CASE_ID = 99250807409918L;

    private static final long HEARING_ID = 123L;

    private static final String SERVICE_HEARING_VALUES_URL = "/serviceHearingValues";

    private static final String SERVICE_LINKED_CASES_URL = "/serviceLinkedCases";

    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    public ServiceHearingsService serviceHearingsService;

    @RegisterExtension
    private final LogCaptureExtension logCapture =
            new LogCaptureExtension(ServiceHearingsController.class);

    @DisplayName("When Authorization and Case ID valid "
            + "should return the case name with a with 200 response code")
    @Test
    void testPostRequestServiceHearingValues() throws Exception {

        ServiceHearingRequest request = ServiceHearingRequest.builder()
                .caseId(String.valueOf(CASE_ID))
                .build();

        IndividualDetails individualDetails1 = IndividualDetails.builder()
                .firstName("Paddington")
                .lastName("Bear")
                .hearingChannelEmail(List.of("paddington.bear@example.com"))
                .hearingChannelPhone(List.of("07123456789"))
                .build();
        PartyDetails partyDetails1 = PartyDetails.builder()
                .partyName("Paddington Bear")
                .individualDetails(individualDetails1)
                .build();

        given(serviceHearingsService.getServiceHearingValues(request))
            .willReturn(ServiceHearingValues.builder()
                    .hmctsInternalCaseName("Internal Case Name")
                    .publicCaseName("Public Case Name")
                    .listingComments("Listing Comments")
                    .parties(List.of(partyDetails1))
                .build());

        mockMvc.perform(post(SERVICE_HEARING_VALUES_URL)
                        .contentType(APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    ServiceHearingValues response = mapper.readValue(responseBody, ServiceHearingValues.class);
                    assertThat(response.getHmctsInternalCaseName()).isEqualTo("Internal Case Name");
                    assertThat(response.getPublicCaseName()).isEqualTo("Public Case Name");
                    assertThat(response.getListingComments()).isEqualTo("Listing Comments");
                    assertThat(response.getParties().size()).isEqualTo(1);
                    assertThat(response.getParties()).isEqualTo(List.of(partyDetails1));
                });

        logCapture
                .assertLogContains("hmctsInternalCaseName=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("publicCaseName=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("listingComments=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("partyName=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("firstName=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("lastName=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("hearingChannelEmail=[" + getMaskedEmail("paddington.bear@example.com"), Level.INFO)
                .assertLogContains("hearingChannelPhone=[" + getMaskedPhone("07123456789"), Level.INFO)

                .assertLogDoesNotContain("Internal Case Name", Level.INFO)
                .assertLogDoesNotContain("Public Case Name", Level.INFO)
                .assertLogDoesNotContain("Listing Comments", Level.INFO)
                .assertLogDoesNotContain("Paddington", Level.INFO)
                .assertLogDoesNotContain("paddington.bear@example.com", Level.INFO)
                .assertLogDoesNotContain("07123456789", Level.INFO);
    }

    @DisplayName("When Case Not Found should return a with 404 response code")
    @Test
    void testPostRequestServiceHearingValues_missingCase() throws Exception {
        ServiceHearingRequest request = ServiceHearingRequest.builder()
                .caseId(String.valueOf(MISSING_CASE_ID))
                .build();

        given(serviceHearingsService.getServiceHearingValues(request))
            .willThrow(GetCaseException.class);

        mockMvc.perform(post(SERVICE_HEARING_VALUES_URL)
                        .contentType(APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @DisplayName("When Authorization and Case ID valid should return the case name with a with 200 response code")
    @Test
    void testPostRequestServiceLinkedCases() throws Exception {
        ServiceHearingRequest request = ServiceHearingRequest.builder()
            .caseId(String.valueOf(CASE_ID))
            .hearingId(String.valueOf(HEARING_ID))
            .build();

        given(serviceHearingsService.getServiceLinkedCases(request))
            .willReturn(List.of(ServiceLinkedCases.builder()
                .build()));


        mockMvc.perform(post(SERVICE_LINKED_CASES_URL)
                        .contentType(APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void testPostRequestServiceLinkedCases_missingCase() throws Exception {
        ServiceHearingRequest request = ServiceHearingRequest.builder()
                .caseId(String.valueOf(MISSING_CASE_ID))
                .hearingId(String.valueOf(HEARING_ID))
                .build();

        given(serviceHearingsService.getServiceLinkedCases(request))
            .willThrow(GetCaseException.class);

        mockMvc.perform(post(SERVICE_LINKED_CASES_URL)
                        .contentType(APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    public static String asJsonString(final Object obj) throws JsonProcessingException {
        return mapper.writeValueAsString(obj);
    }
}
