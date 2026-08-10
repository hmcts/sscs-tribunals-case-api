package uk.gov.hmcts.reform.sscs.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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


    @DisplayName("When Authorization and Case ID valid "
            + "should return the case name with a with 200 response code")
    @Test
    void testPostRequestServiceHearingValues() throws Exception {

        ServiceHearingRequest request = ServiceHearingRequest.builder()
                .caseId(String.valueOf(CASE_ID))
                .build();

        given(serviceHearingsService.getServiceHearingValues(request))
            .willReturn(ServiceHearingValues.builder()
                .build());

        mockMvc.perform(post(SERVICE_HEARING_VALUES_URL)
                        .contentType(APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andDo(print())
                .andExpect(status().isOk());
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

    @DisplayName("getServiceHearingValuesForLogging should handle null parties")
    @Test
    void testGetServiceHearingValuesForLoggingWithNullParties() throws Exception {
        ServiceHearingValues serviceHearingValues = ServiceHearingValues.builder()
                .parties(null)
                .build();

        Method method = ServiceHearingsController.class.getDeclaredMethod("getServiceHearingValuesForLogging", ServiceHearingValues.class);
        method.setAccessible(true);
        String result = (String) method.invoke(new ServiceHearingsController(serviceHearingsService), serviceHearingValues);

        assertThat(result).isNotNull();
        assertThat(result).contains("parties=null");
    }

    @DisplayName("getServiceHearingValuesForLogging should handle empty parties list")
    @Test
    void testGetServiceHearingValuesForLoggingWithEmptyPartiesList() throws Exception {
        ServiceHearingValues serviceHearingValues = ServiceHearingValues.builder()
                .parties(new ArrayList<>())
                .build();

        Method method = ServiceHearingsController.class.getDeclaredMethod("getServiceHearingValuesForLogging", ServiceHearingValues.class);
        method.setAccessible(true);
        String result = (String) method.invoke(new ServiceHearingsController(serviceHearingsService), serviceHearingValues);

        assertThat(result).isNotNull();
        assertThat(result).contains("parties=[]");

    }

    @DisplayName("getServiceHearingValuesForLogging should mask multiple parties")
    @Test
    void testGetServiceHearingValuesForLoggingMasksMultipleParties() throws Exception {
        IndividualDetails individualDetails1 = IndividualDetails.builder()
                .firstName("John")
                .lastName("Doe")
                .hearingChannelEmail(List.of("john.doe@example.com"))
                .hearingChannelPhone(List.of("07123456789"))
                .build();
        PartyDetails partyDetails1 = PartyDetails.builder()
                .partyName("John Doe")
                .individualDetails(individualDetails1)
                .build();

        IndividualDetails individualDetails2 = IndividualDetails.builder()
                .firstName("Jane")
                .lastName("Smith")
                .hearingChannelEmail(List.of("jane.smith@example.com"))
                .hearingChannelPhone(List.of("07987654321"))
                .build();
        PartyDetails partyDetails2 = PartyDetails.builder()
                .partyName("Jane Smith")
                .individualDetails(individualDetails2)
                .build();

        List<PartyDetails> parties = new ArrayList<>();
        parties.add(partyDetails1);
        parties.add(partyDetails2);

        ServiceHearingValues serviceHearingValues = ServiceHearingValues.builder()
                .parties(parties)
                .build();

        Method method = ServiceHearingsController.class.getDeclaredMethod("getServiceHearingValuesForLogging", ServiceHearingValues.class);
        method.setAccessible(true);
        String result = (String) method.invoke(new ServiceHearingsController(serviceHearingsService), serviceHearingValues);

        assertThat(result).isNotNull();
        assertThat(result).doesNotContain("John");
        assertThat(result).doesNotContain("Doe");
        assertThat(result).doesNotContain("john.doe@example.com");
        assertThat(result).doesNotContain("07123456789");
        assertThat(result).doesNotContain("Jane");
        assertThat(result).doesNotContain("Smith");
        assertThat(result).doesNotContain("jane.smith@example.com");
        assertThat(result).doesNotContain("07987654321");
    }
}
