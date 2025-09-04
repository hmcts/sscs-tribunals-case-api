package uk.gov.hmcts.reform.sscs.jms.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMappingBase.BENEFIT_CODE;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsMappingBase.ISSUE_CODE;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.exception.GetCaseException;
import uk.gov.hmcts.reform.sscs.exception.TribunalsEventProcessingException;
import uk.gov.hmcts.reform.sscs.exception.UpdateCaseException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingDuration;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;
import uk.gov.hmcts.reform.sscs.service.CcdCaseService;
import uk.gov.hmcts.reform.sscs.service.HearingsService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApi;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.VenueService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingRequestHandler;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integration")
@TestPropertySource(locations = "classpath:config/application_it_updateCaseV2.properties")
public class HearingRequestHandlerIT {
    private static final String CASE_ID = "1234123412341234";
    private static final String PROCESSING_VENUE_1 = "Cardiff";

    private HearingRequestHandler hearingRequestHandler;

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private HearingsService hearingsService;

    @MockitoBean
    private IdamService idamService;
    @MockitoBean
    private CcdCaseService ccdCaseService;
    @MockitoBean
    private ReferenceDataServiceHolder refData;
    @MockitoBean
    private HearingDurationsService hearingDurationsService;
    @MockitoBean
    private RegionalProcessingCenterService regionalProcessingCenterService;
    @MockitoBean
    private VenueService venueService;
    @MockitoBean
    private VerbalLanguagesService verbalLanguages;

    @MockitoBean
    private HmcHearingApi hearingApi;
    @MockitoBean
    private UpdateCcdCaseService updateCcdCaseService;
    @Autowired
    private SignLanguagesService signLanguagesService;

    @Test
    public void testHearingsUpdateCaseV2() throws UpdateCaseException, TribunalsEventProcessingException, GetCaseException {

        hearingRequestHandler = new HearingRequestHandler(hearingsService, updateCcdCaseService,
                idamService);
        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        when(ccdCaseService.getStartEventResponse(anyLong(), any())).thenReturn(createSscsCaseDetails());
        when(hearingApi.getHearingsRequest(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(HearingsGetResponse.builder().build());
        when(refData.getHearingDurations()).thenReturn(hearingDurationsService);
        when(refData.getRegionalProcessingCenterService()).thenReturn(regionalProcessingCenterService);
        when(regionalProcessingCenterService.getByPostcode(any(), anyBoolean())).thenReturn(RegionalProcessingCenter.builder().hearingRoute(
            HearingRoute.LIST_ASSIST).build());
        when(refData.getVenueService()).thenReturn(venueService);
        when(venueService.getActiveRegionalEpimsIdsForRpc(any())).thenReturn(List.of(VenueDetails.builder().epimsId("1").build()));
        when(venueService.getEpimsIdForVenue(any())).thenReturn("1");
        when(refData.getVerbalLanguages()).thenReturn(verbalLanguages);
        when(refData.getSignLanguages()).thenReturn(signLanguagesService);
        when(verbalLanguages.getVerbalLanguage(any())).thenReturn(Language.builder().reference("LANG").build());
        HmcUpdateResponse response = HmcUpdateResponse.builder().hearingRequestId(22L).build();
        when(hearingApi.createHearingRequest(any(), any(), any(), any(), any(), any())).thenReturn(response);
        HearingDuration hearingDuration = new HearingDuration();
        hearingDuration.setDurationFaceToFace(60);
        when(hearingDurationsService.getHearingDuration(anyString(), anyString())).thenReturn(hearingDuration);


        String message = "{\n"
            + "  \"ccdCaseId\": \"" + CASE_ID + "\",\n"
            + "  \"hearingRoute\": \"listAssist\",\n"
            + "  \"hearingState\": \"adjournCreateHearing\"\n"
            + "}\n";
        hearingRequestHandler.handleHearingRequest(deserialize(message));

        verify(ccdCaseService, never()).updateCaseData(any(), any(), any());

        verify(updateCcdCaseService).updateCaseV2(
            eq(Long.parseLong(CASE_ID)), any(), any(), any(), any(), any());
    }

    private SscsCaseDetails createSscsCaseDetails() {
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(
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
                .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Bristol Magistrates").postcode("BA1 1AA").build())
                .build()).build();
        return sscsCaseDetails;
    }

    private HearingRequest deserialize(String source) {
        try {
            HearingRequest hearingRequest = mapper.readValue(
                source,
                new TypeReference<HearingRequest>() {
                }
            );

            return hearingRequest;

        } catch (IOException e) {
            throw new IllegalArgumentException("Could not deserialize HearingRequest", e);
        }
    }

}
