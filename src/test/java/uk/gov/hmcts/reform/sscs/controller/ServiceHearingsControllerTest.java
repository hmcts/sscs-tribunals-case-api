package uk.gov.hmcts.reform.sscs.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.MASKED_STRING_VALUE;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getMaskedEmail;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getMaskedPhone;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.sscs.exception.GetCaseException;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.model.service.ServiceHearingRequest;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyDetails;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.ServiceHearingValues;
import uk.gov.hmcts.reform.sscs.model.single.hearing.IndividualDetails;
import uk.gov.hmcts.reform.sscs.service.ServiceHearingsService;
import uk.gov.hmcts.reform.sscs.util.LogCaptureExtension;


@Slf4j
@ExtendWith(MockitoExtension.class)
public class ServiceHearingsControllerTest {

    @Mock
    ServiceHearingsService serviceHearingsService;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @RegisterExtension
    private final LogCaptureExtension logCapture =
            new LogCaptureExtension(ServiceHearingsController.class);

    private ServiceHearingsController serviceHearingsController;
    private static final long CASE_ID = 1625080769409918L;

    @Test
    void getServiceHearingValuesForLoggingWithNullParties() throws ListingException, JsonProcessingException, GetCaseException, UpdateCaseException {

        ServiceHearingRequest request = ServiceHearingRequest.builder()
                .caseId(String.valueOf(CASE_ID))
                .build();

        when(serviceHearingsService.getServiceHearingValues(request)).thenReturn(
                ServiceHearingValues.builder()
                        .parties(null)
                        .build());

        serviceHearingsController = new ServiceHearingsController(serviceHearingsService, objectMapper);

        var response = serviceHearingsController.serviceHearingValues(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        logCapture.assertLogContains("parties=null", Level.INFO);
    }

    @Test
    void getServiceHearingValuesForLoggingWithEmptyPartiesList() throws Exception {
        ServiceHearingRequest request = ServiceHearingRequest.builder()
                .caseId(String.valueOf(CASE_ID))
                .build();

        when(serviceHearingsService.getServiceHearingValues(request)).thenReturn(
                ServiceHearingValues.builder()
                        .parties(new ArrayList<>())
                        .build()
        );

        serviceHearingsController = new ServiceHearingsController(serviceHearingsService, objectMapper);

        var response = serviceHearingsController.serviceHearingValues(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        logCapture.assertLogContains("parties=[]", Level.INFO);
    }

    @Test
    void getServiceHearingValuesForLoggingMasksMultipleParties() throws Exception {
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

        ServiceHearingRequest request = ServiceHearingRequest.builder()
                .caseId(String.valueOf(CASE_ID))
                .build();

        when(serviceHearingsService.getServiceHearingValues(request)).thenReturn(
                ServiceHearingValues.builder()
                        .hmctsInternalCaseName("hmctsInternalCaseNameTest")
                        .publicCaseName("publicCaseNameTest")
                        .listingComments("listingCommentsTest")
                        .parties(parties)
                        .build()
        );

        serviceHearingsController = new ServiceHearingsController(serviceHearingsService, objectMapper);

        var response = serviceHearingsController.serviceHearingValues(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        logCapture
                .assertLogContains("hmctsInternalCaseName=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("publicCaseName=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("listingComments=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("partyName=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("firstName=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("lastName=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("hearingChannelEmail=[" + getMaskedEmail("paddington.bear@example.com"), Level.INFO)
                .assertLogContains("hearingChannelPhone=[" + getMaskedPhone("07123456789"), Level.INFO)

                .assertLogContains("firstName=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("lastName=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("hearingChannelEmail=[" + getMaskedEmail("jane.smith@example.com"), Level.INFO)
                .assertLogContains("hearingChannelPhone=[" + getMaskedPhone("07987654321"), Level.INFO)

                .assertLogDoesNotContain("hmctsInternalCaseNameTest", Level.INFO)
                .assertLogDoesNotContain("publicCaseNameTest", Level.INFO)
                .assertLogDoesNotContain("listingCommentsTest", Level.INFO)
                .assertLogDoesNotContain("paddington.bear@example.com", Level.INFO)
                .assertLogDoesNotContain("07123456789", Level.INFO)
                .assertLogDoesNotContain("jane.smith@example.com", Level.INFO)
                .assertLogDoesNotContain("07987654321", Level.INFO);

        assertThat(response.getBody().getHmctsInternalCaseName()).isEqualTo("hmctsInternalCaseNameTest");
        assertThat(response.getBody().getPublicCaseName()).isEqualTo("publicCaseNameTest");
        assertThat(response.getBody().getListingComments()).isEqualTo("listingCommentsTest");
        assertThat(response.getBody().getParties()).isEqualTo(parties);
    }

    @Test
    void getServiceHearingValuesForNullOrEmptyIndividualDetails() throws Exception {
        PartyDetails partyDetails1 = PartyDetails.builder()
                .partyName("Paddington Bear")
                .individualDetails(null)
                .build();

        List<PartyDetails> parties = new ArrayList<>();
        parties.add(partyDetails1);

        ServiceHearingRequest request = ServiceHearingRequest.builder()
                .caseId(String.valueOf(CASE_ID))
                .build();

        when(serviceHearingsService.getServiceHearingValues(request)).thenReturn(
                ServiceHearingValues.builder()
                        .hmctsInternalCaseName(null)
                        .publicCaseName(null)
                        .listingComments("")
                        .parties(parties)
                        .build()
        );

        serviceHearingsController = new ServiceHearingsController(serviceHearingsService, objectMapper);

        serviceHearingsController.serviceHearingValues(request);

        logCapture
                .assertLogContains("hmctsInternalCaseName=null", Level.INFO)
                .assertLogContains("publicCaseName=null", Level.INFO)
                .assertLogContains("listingComments=,", Level.INFO)
                .assertLogContains("individualDetails=null", Level.INFO);

    }
}
