package uk.gov.hmcts.reform.sscs.jms.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMappingBase.BENEFIT_CODE;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMappingBase.ISSUE_CODE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.LISTED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import jakarta.jms.JMSException;
import java.time.LocalDateTime;
import org.apache.qpid.jms.message.JmsBytesMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.exception.CaseException;
import uk.gov.hmcts.reform.sscs.exception.HmcEventProcessingException;
import uk.gov.hmcts.reform.sscs.exception.MessageProcessingException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HearingUpdate;
import uk.gov.hmcts.reform.sscs.model.hmc.message.HmcMessage;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.ListingStatus;
import uk.gov.hmcts.reform.sscs.model.single.hearing.CaseDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDaySchedule;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.PartyDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.RequestDetails;
import uk.gov.hmcts.reform.sscs.service.CcdCaseService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApi;
import uk.gov.hmcts.reform.sscs.service.VenueService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.ProcessHmcMessageServiceV2;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integration")
@TestPropertySource(locations = "classpath:config/application_it_updateCaseV2.properties")
public class HmcHearingsEventTopicListenerV2ItTest {

    private static final String CASE_ID = "1234123412341234";
    private static final String PROCESSING_VENUE_1 = "Cardiff";

    private HmcHearingsEventTopicListener hmcHearingsEventTopicListener;

    @MockitoBean
    private ObjectMapper mapper;

    @Autowired
    private ProcessHmcMessageServiceV2 processHmcMessageServiceV2;

    @MockitoBean
    private JmsBytesMessage bytesMessage;

    @MockitoBean
    private IdamService idamService;

    @MockitoBean
    private CcdCaseService ccdCaseService;

    @MockitoBean
    private VenueService venueService;

    @MockitoBean
    private HmcHearingApi hearingApi;

    @MockitoBean
    private UpdateCcdCaseService updateCcdCaseService;

    @Test
    public void testHearingsUpdateV2() throws JMSException, HmcEventProcessingException, JsonProcessingException, CaseException, MessageProcessingException {

        hmcHearingsEventTopicListener = new HmcHearingsEventTopicListener(processHmcMessageServiceV2);
        ReflectionTestUtils.setField(hmcHearingsEventTopicListener, "objectMapper", mapper);

        HmcMessage hmcMessage = createHmcMessage("BBA3");
        IdamTokens idamTokens = IdamTokens.builder().build();

        HearingGetResponse hearingGetResponse = getHearingGetResponse();

        when(bytesMessage.getStringProperty("hmctsDeploymentId")).thenReturn("test");
        when(bytesMessage.getBodyLength()).thenReturn(20L);

        when(mapper.readValue(any(String.class), eq(HmcMessage.class))).thenReturn(hmcMessage);
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        when(ccdCaseService.getCaseDetails(anyLong())).thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());
        when(venueService.getVenueDetailsForActiveVenueByEpimsId(any())).thenReturn(VenueDetails.builder().build());

        when(hearingApi.getHearingRequest(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(hearingGetResponse);

        hmcHearingsEventTopicListener.onMessage(bytesMessage);

        verify(idamService, times(3)).getIdamTokens();

        verify(hearingApi).getHearingRequest(any(), any(), any(), any(), any(), any(), any());

        verify(ccdCaseService, never()).updateCaseData(any(), any(), any());
        verify(updateCcdCaseService).updateCaseV2DynamicEvent(eq(Long.parseLong(CASE_ID)), eq(idamTokens), any());

    }

    private static SscsCaseDetails getSscsCaseDetails() {
        return SscsCaseDetails.builder().data(
            SscsCaseData.builder()
                .ccdCaseId(CASE_ID)
                .adjournment(Adjournment.builder().nextHearingDateType(FIRST_AVAILABLE_DATE).build())
                .processingVenue(PROCESSING_VENUE_1)
                .benefitCode(BENEFIT_CODE)
                .issueCode(ISSUE_CODE)
                .appeal(
                    Appeal.builder().appellant(
                            Appellant.builder()
                                .id("11")
                                .name(Name.builder().firstName("first").lastName("last").build())
                                .build())
                        .hearingOptions(HearingOptions.builder().languageInterpreter("Yes").build())
                        .build())
                .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Bristol Magistrates").postcode(
                    "BA1 1AA").build())
                .build()).build();
    }

    private HmcMessage createHmcMessage(String messageServiceCode) {
        return HmcMessage.builder()
            .hmctsServiceCode(messageServiceCode)
            .caseId(Long.parseLong(CASE_ID))
            .hearingId("1")
            .hearingUpdate(HearingUpdate.builder()
                               .hmcStatus(LISTED)
                               .build())
            .build();
    }

    private HearingGetResponse getHearingGetResponse() {
        return HearingGetResponse.builder()
            .hearingResponse(HearingResponse.builder()
                                 .hearingSessions(Lists.newArrayList(HearingDaySchedule.builder()
                                                                         .hearingVenueEpimsId("8001")
                                                                         .hearingStartDateTime(LocalDateTime.now())
                                                                         .hearingEndDateTime(LocalDateTime.now().plusDays(2))
                                                                         .build()))
                                 .listingStatus(ListingStatus.FIXED).build())
            .hearingDetails(HearingDetails.builder().build())
            .caseDetails(CaseDetails.builder().build())
            .partyDetails(Lists.newArrayList(PartyDetails.builder().build()))
            .requestDetails(RequestDetails.builder().hearingRequestId("12").build())
            .build();
    }

    private VenueDetails getVenueDetails() {
        return VenueDetails.builder()
            .venueId("8001")
            .venName("venueName")
            .url("http://test.com")
            .venAddressLine1("adrLine1")
            .venAddressLine2("adrLine2")
            .venAddressTown("adrTown")
            .venAddressCounty("adrCounty")
            .venAddressPostcode("adrPostcode")
            .regionalProcessingCentre("regionalProcessingCentre")
            .build();
    }
}
