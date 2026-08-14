package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.MASKED_STRING_VALUE;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getMaskedEmail;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.exception.GetHearingException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.single.hearing.CaseDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingCancelRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.IndividualDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.PartyDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.RequestDetails;
import uk.gov.hmcts.reform.sscs.util.LogCaptureExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
class HmcHearingApiServiceTest {

    private static final String IDAM_OAUTH2_TOKEN = "TestOauth2Token";
    private static final String SERVICE_AUTHORIZATION = "TestServiceAuthorization";
    private static final long VERSION = 1;
    private static final long CASE_ID = 1625080769409918L;
    private static final String HEARING_ID = "12345";
    private static final long HEARING_REQUEST_ID = 12345;

    @Mock
    private HmcHearingApi hmcHearingApi;

    @Mock
    private IdamService idamService;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private HmcHearingApiService hmcHearingsService;

    @RegisterExtension
    private final LogCaptureExtension logCapture =
            new LogCaptureExtension(HmcHearingApiService.class);

    @BeforeEach
    void setUp() {
        lenient().when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder()
                .serviceAuthorization(SERVICE_AUTHORIZATION)
                .idamOauth2Token(IDAM_OAUTH2_TOKEN)
                .build());
    }

    @DisplayName("When getHearingRequest is given the correct parameters it returns a valid response without error")
    @Test
    void testGetHearingRequest() throws GetHearingException {
        HearingGetResponse response = HearingGetResponse.builder()
                .hearingDetails(HearingDetails.builder().build())
                .partyDetails(new ArrayList<>())
                .hearingResponse(HearingResponse.builder().build())
                .caseDetails(CaseDetails.builder().build())
                .requestDetails(RequestDetails.builder().build())
                .build();

        given(hmcHearingApi.getHearingRequest(IDAM_OAUTH2_TOKEN, SERVICE_AUTHORIZATION, null, null, null, HEARING_ID, null))
                .willReturn(response);

        HearingGetResponse result = hmcHearingsService.getHearingRequest(HEARING_ID);

        assertThat(result)
                .isNotNull()
                .isEqualTo(response);
    }

    @DisplayName("When the api getHearingRequest returns a null the correct error and message is thrown")
    @Test
    void testGetHearingRequestNullResponse() {
        given(hmcHearingApi.getHearingRequest(IDAM_OAUTH2_TOKEN, SERVICE_AUTHORIZATION, null, null, null,  HEARING_ID, null))
                .willReturn(null);

        assertThatExceptionOfType(GetHearingException.class)
                .isThrownBy(() -> hmcHearingsService.getHearingRequest(HEARING_ID))
                .withMessageContaining("Failed to retrieve hearing");
    }

    @DisplayName("sendCreateHearingRequest should send request successfully")
    @Test
    void testSendCreateHearingRequest() {
        PartyDetails partyDetails = PartyDetails.builder()
                .individualDetails(IndividualDetails.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .hearingChannelEmail(List.of("john.doe@example.com"))
                        .hearingChannelPhone(List.of("07123456789"))
                        .build())
                .build();

        HearingRequestPayload payload = HearingRequestPayload.builder()
                .caseDetails(CaseDetails.builder()
                        .caseId(String.valueOf(CASE_ID))
                        .hmctsInternalCaseName("hmctsInternalCaseName")
                        .publicCaseName("publicCaseName")
                        .build())
                .partiesDetails(List.of(partyDetails))
                .build();

        HmcUpdateResponse response = HmcUpdateResponse.builder()
                .hearingRequestId(HEARING_REQUEST_ID)
                .versionNumber(VERSION)
                .build();

        given(hmcHearingApi.createHearingRequest(IDAM_OAUTH2_TOKEN, SERVICE_AUTHORIZATION, null, null, null,  payload)).willReturn(response);

        HmcUpdateResponse result = hmcHearingsService.sendCreateHearingRequest(payload);
        assertThat(result)
                .isNotNull()
                .isEqualTo(response);
        assertThat(payload.getCaseDetails().getHmctsInternalCaseName()).isEqualTo("hmctsInternalCaseName");
        assertThat(payload.getCaseDetails().getPublicCaseName()).isEqualTo("publicCaseName");
        assertThat(payload.getPartiesDetails().getFirst()).isEqualTo(partyDetails);
    }

    @DisplayName("sendDeleteHearingRequest should send request successfully")
    @Test
    void testSendUpdateHearingRequest() {
        HearingRequestPayload payload = HearingRequestPayload.builder()
                .caseDetails(CaseDetails.builder()
                        .caseId(String.valueOf(CASE_ID))
                        .build())
                .build();

        HmcUpdateResponse response = HmcUpdateResponse.builder()
                .hearingRequestId(HEARING_REQUEST_ID)
                .versionNumber(VERSION)
                .build();

        given(hmcHearingApi.updateHearingRequest(IDAM_OAUTH2_TOKEN, SERVICE_AUTHORIZATION, null, null, null, String.valueOf(HEARING_REQUEST_ID), payload)).willReturn(response);

        HmcUpdateResponse result = hmcHearingsService.sendUpdateHearingRequest(payload, String.valueOf(HEARING_REQUEST_ID));

        assertThat(result)
                .isNotNull()
                .isEqualTo(response);
    }


    @DisplayName("sendDeleteHearingRequest should send request successfully")
    @Test
    void testSendDeleteHearingRequest() {
        HearingCancelRequestPayload payload = HearingCancelRequestPayload.builder().build();

        HmcUpdateResponse response = HmcUpdateResponse.builder()
                .hearingRequestId(HEARING_REQUEST_ID)
                .versionNumber(VERSION)
                .build();

        given(hmcHearingApi.cancelHearingRequest(IDAM_OAUTH2_TOKEN, SERVICE_AUTHORIZATION, null, null, null, String.valueOf(HEARING_REQUEST_ID), payload)).willReturn(response);

        HmcUpdateResponse result = hmcHearingsService.sendCancelHearingRequest(payload, String.valueOf(HEARING_REQUEST_ID));

        assertThat(result)
                .isNotNull()
                .isEqualTo(response);
    }

    @DisplayName("getMaskedHearingPayload should mask sensitive fields in string output")
    @Test
    void testGetMaskedHearingPayloadMasksCaseNames() throws Exception {
        IndividualDetails individualDetails = IndividualDetails.builder()
                .firstName("Paddington")
                .lastName("Bear")
                .hearingChannelEmail(List.of("paddington.bear@example.com"))
                .hearingChannelPhone(null)
                .build();
        PartyDetails partyDetails = PartyDetails.builder()
                .individualDetails(individualDetails)
                .build();
        List<PartyDetails> parties = new ArrayList<>();
        parties.add(partyDetails);

        HearingRequestPayload payload = HearingRequestPayload.builder()
               .caseDetails(CaseDetails.builder()
                       .caseId(String.valueOf(CASE_ID))
                       .hmctsInternalCaseName("HMCTS Internal Name")
                       .publicCaseName("Public Case Name")
                       .build())
               .partiesDetails(parties)
               .hearingDetails(HearingDetails.builder().listingComments("Listing Comments").build())
               .build();

        hmcHearingsService.sendCreateHearingRequest(payload);

        logCapture
                .assertLogContains("hmctsInternalCaseName=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("publicCaseName=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("firstName=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("lastName=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("listingComments=" + MASKED_STRING_VALUE, Level.INFO)
                .assertLogContains("hearingChannelEmail=[" + getMaskedEmail("paddington.bear@example.com"), Level.INFO)

                .assertLogDoesNotContain("HMCTS Internal Name", Level.INFO)
                .assertLogDoesNotContain("Public Case Name", Level.INFO)
                .assertLogDoesNotContain("Paddington", Level.INFO)
                .assertLogDoesNotContain("Bear", Level.INFO)
                .assertLogDoesNotContain("Listing Comments", Level.INFO)
                .assertLogDoesNotContain("paddington.bear@example.com", Level.INFO);
    }

    @DisplayName("getMaskedHearingPayload should handle null party individual details")
    @Test
    void testGetMaskedHearingPayloadWithNullIndividualDetails() throws Exception {
        PartyDetails partyDetails = PartyDetails.builder()
               .individualDetails(null)
               .build();
        List<PartyDetails> parties = new ArrayList<>();
        parties.add(partyDetails);

        HearingRequestPayload payload = HearingRequestPayload.builder()
               .caseDetails(CaseDetails.builder()
                       .caseId(String.valueOf(CASE_ID))
                       .build())
               .partiesDetails(parties)
               .hearingDetails(null)
               .build();

        hmcHearingsService.sendCreateHearingRequest(payload);

        logCapture
                .assertLogContains("hmctsInternalCaseName=null", Level.INFO)
                .assertLogContains("publicCaseName=null", Level.INFO)
                .assertLogContains("individualDetails=null", Level.INFO);
    }
}
