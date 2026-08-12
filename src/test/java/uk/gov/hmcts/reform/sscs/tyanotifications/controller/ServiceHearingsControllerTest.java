package uk.gov.hmcts.reform.sscs.tyanotifications.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getMaskedEmail;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getMaskedPhoneOrString;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.sscs.config.SpringConfig;
import uk.gov.hmcts.reform.sscs.controller.ServiceHearingsController;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyDetails;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.ServiceHearingValues;
import uk.gov.hmcts.reform.sscs.model.single.hearing.IndividualDetails;
import uk.gov.hmcts.reform.sscs.service.ServiceHearingsService;

@ExtendWith(MockitoExtension.class)
public class ServiceHearingsControllerTest {

    @MockitoBean
    public ServiceHearingsService serviceHearingsService;

    private ObjectMapper objectMapper = SpringConfig.mapper();

    @DisplayName("getServiceHearingValuesForLogging should handle null parties")
    @Test
    void testGetServiceHearingValuesForLoggingWithNullParties() throws Exception {
        ServiceHearingValues serviceHearingValues = ServiceHearingValues.builder()
                .parties(null)
                .build();

        Method method = ServiceHearingsController.class.getDeclaredMethod("getServiceHearingValuesForLogging", ServiceHearingValues.class);
        method.setAccessible(true);
        String result = (String) method.invoke(new ServiceHearingsController(serviceHearingsService, objectMapper), serviceHearingValues);

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
        String result = (String) method.invoke(new ServiceHearingsController(serviceHearingsService, objectMapper), serviceHearingValues);

        assertThat(result).isNotNull();
        assertThat(result).contains("parties=[]");

    }

    @DisplayName("getServiceHearingValuesForLogging should mask multiple parties")
    @Test
    void testGetServiceHearingValuesForLoggingMasksMultipleParties() throws Exception {
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

        ServiceHearingValues serviceHearingValues = ServiceHearingValues.builder()
                .hmctsInternalCaseName("hmctsInternalCaseNameTest")
                .publicCaseName("publicCaseNameTest")
                .listingComments("listingCommentsTest")
                .parties(parties)
                .build();

        Method method = ServiceHearingsController.class.getDeclaredMethod("getServiceHearingValuesForLogging", ServiceHearingValues.class);
        method.setAccessible(true);
        String result = (String) method.invoke(new ServiceHearingsController(serviceHearingsService, objectMapper), serviceHearingValues);

        assertThat(result).isNotNull();
        assertThat(result).doesNotContain("Paddington");
        assertThat(result).contains(getMaskedPhoneOrString("Paddington"));
        assertThat(result).doesNotContain("Paddington Bear");
        assertThat(result).contains(getMaskedPhoneOrString("Paddington Bear"));
        assertThat(result).doesNotContain("paddington.bear@example.com");
        assertThat(result).contains(getMaskedEmail("paddington.bear@example.com"));
        assertThat(result).doesNotContain("07123456789");
        assertThat(result.contains(getMaskedPhoneOrString("07123456789")));
        assertThat(result).doesNotContain("Jane");
        assertThat(result).contains(getMaskedPhoneOrString("Jane"));
        assertThat(result).doesNotContain("Jane Smith");
        assertThat(result).contains(getMaskedPhoneOrString("Jane Smith"));
        assertThat(result).doesNotContain("jane.smith@example.com");
        assertThat(result).contains(getMaskedEmail("jane.smith@example.com"));
        assertThat(result).doesNotContain("07987654321");
        assertThat(result).contains(getMaskedPhoneOrString("07987654321"));
        assertThat(result).doesNotContain("hmctsInternalCaseNameTest");
        assertThat(result).contains(getMaskedPhoneOrString("hmctsInternalCaseNameTest"));
        assertThat(result).doesNotContain("publicCaseNameTest");
        assertThat(result).contains(getMaskedPhoneOrString("publicCaseNameTest"));
        assertThat(result).doesNotContain("listingCommentsTest");
        assertThat(result).contains(getMaskedPhoneOrString("listingCommentsTest"));
    }

    @DisplayName("getServiceHearingValuesForLogging should not set the original serviceHearingValues parties to null")
    @Test
    void testGetServiceHearingValuesForLoggingDoesNotNullOriginalParties() throws Exception {
        IndividualDetails individualDetails = IndividualDetails.builder()
                .firstName("Paddington")
                .lastName("Bear")
                .hearingChannelEmail(List.of("paddington.bear@example.com"))
                .hearingChannelPhone(List.of("07000000001"))
                .build();
        PartyDetails partyDetails = PartyDetails.builder()
                .partyName("Paddington Bear")
                .individualDetails(individualDetails)
                .build();
        List<PartyDetails> parties = new ArrayList<>();
        parties.add(partyDetails);

        ServiceHearingValues serviceHearingValues = ServiceHearingValues.builder()
                .hmctsInternalCaseName("hmctsInternalCaseNameTest")
                .publicCaseName("publicCaseNameTest")
                .listingComments("listingCommentsTest")
                .parties(parties)
                .build();

        Method method = ServiceHearingsController.class.getDeclaredMethod("getServiceHearingValuesForLogging", ServiceHearingValues.class);
        method.setAccessible(true);
        ServiceHearingsController controller = new ServiceHearingsController(serviceHearingsService, objectMapper);

        String result = (String) method.invoke(controller, serviceHearingValues);

        // Ensure the returned masked string is present
        assertThat(result).isNotNull();

        // Verify original object's parties are NOT set to null
        assertThat(serviceHearingValues.getParties()).isNotNull();
        assertThat(serviceHearingValues.getParties()).isNotEmpty();
        assertThat(serviceHearingValues.getHmctsInternalCaseName()).isEqualTo("hmctsInternalCaseNameTest");
        assertThat(serviceHearingValues.getPublicCaseName()).isEqualTo("publicCaseNameTest");
        assertThat(serviceHearingValues.getListingComments()).isEqualTo("listingCommentsTest");
        assertThat(serviceHearingValues.getParties().getFirst().getPartyName()).isEqualTo("Paddington Bear");
        assertThat(serviceHearingValues.getParties().getFirst().getIndividualDetails().getFirstName()).isEqualTo("Paddington");
        assertThat(serviceHearingValues.getParties().getFirst().getIndividualDetails().getLastName()).isEqualTo("Bear");
    }

}
