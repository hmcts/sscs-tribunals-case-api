package uk.gov.hmcts.reform.sscs.service.citizenrequest;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CITIZEN_REQUEST_HEARING_RECORDING;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.model.tya.HearingRecordingResponse;
import uk.gov.hmcts.reform.sscs.service.OnlineHearingService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CitizenRequestServiceTest {
    private static final String IDENTIFIER = "1234";
    private static final String E_MAIL = "user@test.com";
    private static final String AUTHORISATION = "Bearer 12jkas";

    @Mock
    private OnlineHearingService onlineHearingService;
    @Mock
    private IdamService idamService;
    @Mock
    private SscsCaseDetails caseDetails;
    @Mock
    private SscsCaseData caseData;
    @Mock
    private IdamTokens idamTokens;
    @Mock
    private CcdService ccdService;
    @Mock
    private UpdateCcdCaseService updateCcdCaseService;
    @Captor
    private ArgumentCaptor<Consumer<SscsCaseData>> sscsCaseDataCaptor;

    @InjectMocks
    private CitizenRequestService citizenRequestService;

    @BeforeEach
    void setup() {
        UserDetails user = new UserDetails("id", E_MAIL, "first last", "first", "last", List.of("citizen"));
        citizenRequestService = new CitizenRequestService(onlineHearingService, ccdService, idamService, updateCcdCaseService);

        when(onlineHearingService.getCcdCaseByIdentifier(IDENTIFIER)).thenReturn(Optional.of(caseDetails));
        when(idamService.getUserDetails(AUTHORISATION)).thenReturn(user);
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        when(caseDetails.getData()).thenReturn(caseData);
        when(caseDetails.getId()).thenReturn(Long.parseLong(IDENTIFIER));
    }

    @Test
    void testFindHearingRecordings_forEmptyCaseDetails_returnsEmptyResponse() {
        when(onlineHearingService.getCcdCaseByIdentifier(IDENTIFIER)).thenReturn(Optional.empty());

        Optional<HearingRecordingResponse> response = citizenRequestService.findHearingRecordings(IDENTIFIER, AUTHORISATION);
        assertThat(response.isEmpty(), is(true));
    }

    @Test
    void testFindHearingRecordings_forNullHearings_returnsEmptyHearingRecordingResponse() {
        when(caseData.getHearings()).thenReturn(null);

        Optional<HearingRecordingResponse> response = citizenRequestService.findHearingRecordings(IDENTIFIER, AUTHORISATION);
        assertThat(response.isEmpty(), is(false));
        assertThat(response.get(), any(HearingRecordingResponse.class));
        assertThat(response.get().getOutstandingHearingRecordings(), nullValue());
        assertThat(response.get().getReleasedHearingRecordings(), nullValue());
        assertThat(response.get().getRequestableHearingRecordings(), nullValue());
    }

    @Test
    void testFindHearingRecordings_forEmptyHearings_returnsEmptyHearingRecordingResponse() {
        when(caseData.getHearings()).thenReturn(List.of());

        Optional<HearingRecordingResponse> response = citizenRequestService.findHearingRecordings(IDENTIFIER, AUTHORISATION);
        assertThat(response.isEmpty(), is(false));
        assertThat(response.get(), any(HearingRecordingResponse.class));
        assertThat(response.get().getOutstandingHearingRecordings(), nullValue());
        assertThat(response.get().getReleasedHearingRecordings(), nullValue());
        assertThat(response.get().getRequestableHearingRecordings(), nullValue());

    }

    @Test
    void testFindHearingRecordings_forHearingWithoutRecording_returnsEmptyHearingRecordingResponse() {
        when(caseData.getHearings()).thenReturn(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                        .hearingId("id_1")
                        .hearingDate("2021-01-03")
                        .venue(Venue.builder().name("Town house").build())
                        .build())
                .build()));
        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .sscsHearingRecordings(List.of())
                .build());

        Optional<HearingRecordingResponse> response = citizenRequestService.findHearingRecordings(IDENTIFIER, AUTHORISATION);
        assertThat(response.isEmpty(), is(false));
        assertThat(response.get(), any(HearingRecordingResponse.class));
        assertThat(response.get().getOutstandingHearingRecordings().isEmpty(), is(true));
        assertThat(response.get().getReleasedHearingRecordings().isEmpty(), is(true));
        assertThat(response.get().getRequestableHearingRecordings().isEmpty(), is(true));
    }

    @Test
    void testFindHearingRecordings_returnsHearingWithRecording_asRequestableHearingRecordings() {
        when(caseData.getHearings()).thenReturn(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                        .hearingId("id_1")
                        .hearingDate("2021-01-03")
                        .venue(Venue.builder().name("Town house").build())
                        .build())
                .build()));
        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .sscsHearingRecordings(List.of(SscsHearingRecording.builder()
                        .value(SscsHearingRecordingDetails.builder().hearingId("id_1").build())
                        .build()))
                .build());

        Optional<HearingRecordingResponse> response = citizenRequestService.findHearingRecordings(IDENTIFIER, AUTHORISATION);
        assertThat(response.isEmpty(), is(false));
        assertThat(response.get(), any(HearingRecordingResponse.class));
        assertThat(response.get().getOutstandingHearingRecordings().isEmpty(), is(true));
        assertThat(response.get().getReleasedHearingRecordings().isEmpty(), is(true));
        assertThat(response.get().getRequestableHearingRecordings().isEmpty(), is(false));
        assertThat(response.get().getRequestableHearingRecordings().get(0).getHearingId(), is("id_1"));
        assertThat(response.get().getRequestableHearingRecordings().get(0).getHearingDate(), is("2021-01-03"));
        assertThat(response.get().getRequestableHearingRecordings().get(0).getVenue(), is("Town house"));
    }

    @Test
    void testFindHearingRecordings_returnsHearingWithRecording_asReleasedHearings() {
        when(caseData.getHearings()).thenReturn(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                        .hearingId("id_1")
                        .hearingDate("2021-01-03")
                        .venue(Venue.builder().name("Town house").build())
                        .build())
                .build()));
        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .citizenReleasedHearings(List.of(HearingRecordingRequest.builder()
                        .value(HearingRecordingRequestDetails.builder()
                                .requestingParty(PartyItemList.APPELLANT.getCode())
                                .sscsHearingRecording(SscsHearingRecordingDetails.builder()
                                        .hearingId("id_1")
                                        .hearingDate("2021-01-03")
                                        .venue("Town house")
                                        .recordings(List.of(HearingRecordingDetails.builder()
                                                .value(DocumentLink.builder()
                                                        .documentFilename("Test file.mp3")
                                                        .documentUrl("http://dm-store:5005")
                                                        .documentBinaryUrl("http://dm-store:5005/documents/document-id/binary")
                                                        .build())
                                                .build()))
                                        .build())
                                .build())
                        .build()))
                .build());

        Optional<HearingRecordingResponse> response = citizenRequestService.findHearingRecordings(IDENTIFIER, AUTHORISATION);
        assertThat(response.isEmpty(), is(false));
        assertThat(response.get(), any(HearingRecordingResponse.class));
        assertThat(response.get().getOutstandingHearingRecordings().isEmpty(), is(true));
        assertThat(response.get().getRequestableHearingRecordings().isEmpty(), is(true));
        assertThat(response.get().getReleasedHearingRecordings().isEmpty(), is(false));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getHearingId(), is("id_1"));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getHearingDate(), is("2021-01-03"));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getVenue(), is("Town house"));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getHearingRecordings().get(0).getDocumentUrl(), is("document-id"));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getHearingRecordings().get(0).getFileName(), is("Test file.mp3"));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getHearingRecordings().get(0).getFileType(), is("mp3"));
    }

    @Test
    void testFindHearingRecordings_returnsHearingWithRecording_asOutstandingHearings() {
        when(caseData.getHearings()).thenReturn(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                        .hearingId("id_1")
                        .hearingDate("2021-01-03")
                        .venue(Venue.builder().name("Town house").build())
                        .build())
                .build()));
        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .requestedHearings(List.of(HearingRecordingRequest.builder()
                        .value(HearingRecordingRequestDetails.builder()
                                .requestingParty(PartyItemList.APPELLANT.getCode())
                                .sscsHearingRecording(SscsHearingRecordingDetails.builder()
                                        .hearingId("id_1")
                                        .hearingDate("2021-01-03")
                                        .venue("Town house")
                                        .recordings(List.of(HearingRecordingDetails.builder()
                                                .value(DocumentLink.builder()
                                                        .documentFilename("Test file.mp3")
                                                        .documentUrl("http://dm-store:5005")
                                                        .documentBinaryUrl("http://dm-store:5005/documents/document-id/binary")
                                                        .build())
                                                .build()))
                                        .build())
                                .build())
                        .build()))
                .build());

        Optional<HearingRecordingResponse> response = citizenRequestService.findHearingRecordings(IDENTIFIER, AUTHORISATION);
        assertThat(response.isEmpty(), is(false));
        assertThat(response.get(), any(HearingRecordingResponse.class));
        assertThat(response.get().getReleasedHearingRecordings().isEmpty(), is(true));
        assertThat(response.get().getRequestableHearingRecordings().isEmpty(), is(true));
        assertThat(response.get().getOutstandingHearingRecordings().isEmpty(), is(false));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getHearingId(), is("id_1"));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getHearingDate(), is("2021-01-03"));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getVenue(), is("Town house"));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getHearingRecordings().get(0).getDocumentUrl(), is("document-id"));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getHearingRecordings().get(0).getFileName(), is("Test file.mp3"));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getHearingRecordings().get(0).getFileType(), is("mp3"));
    }

    @Test
    void testFindHearingRecordings_filterReleasedAndOutstandingHearingByRequestingParty() {
        when(caseData.getHearings()).thenReturn(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                        .hearingId("id_1")
                        .hearingDate("2021-01-03")
                        .venue(Venue.builder().name("Town house").build())
                        .build())
                .build()));
        when(caseData.getSubscriptions()).thenReturn(Subscriptions.builder()
                .jointPartySubscription(Subscription.builder().email(E_MAIL).build()).build());
        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .requestedHearings(List.of(HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.JOINT_PARTY.getCode())
                                        .sscsHearingRecording(SscsHearingRecordingDetails.builder()
                                                .hearingId("id_1")
                                                .hearingDate("2021-01-03")
                                                .venue("Town house")
                                                .recordings(List.of(HearingRecordingDetails.builder()
                                                        .value(DocumentLink.builder()
                                                                .documentFilename("Test file.mp3")
                                                                .documentUrl("http://dm-store:5005")
                                                                .documentBinaryUrl("http://dm-store:5005/documents/document-id/binary")
                                                                .build())
                                                        .build()))
                                                .build())
                                        .build())
                                .build(),
                        HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.APPELLANT.getCode()).build())
                                .build()))
                .citizenReleasedHearings(List.of(HearingRecordingRequest.builder()
                            .value(HearingRecordingRequestDetails.builder()
                                    .requestingParty(PartyItemList.JOINT_PARTY.getCode())
                                    .sscsHearingRecording(SscsHearingRecordingDetails.builder()
                                            .hearingId("id_1")
                                            .hearingDate("2021-01-03")
                                            .venue("Town house")
                                            .recordings(List.of(HearingRecordingDetails.builder()
                                                    .value(DocumentLink.builder()
                                                            .documentFilename("Test file.mp3")
                                                            .documentUrl("http://dm-store:5005")
                                                            .documentBinaryUrl("http://dm-store:5005/documents/document-id/binary")
                                                            .build())
                                                    .build()))
                                            .build())
                                    .build())
                            .build(),
                        HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.APPELLANT.getCode()).build())
                                .build()))
                .dwpReleasedHearings(List.of(HearingRecordingRequest.builder()
                        .value(HearingRecordingRequestDetails.builder()
                                .requestingParty(PartyItemList.JOINT_PARTY.getCode())
                                .sscsHearingRecording(SscsHearingRecordingDetails.builder()
                                        .hearingId("id_1")
                                        .hearingDate("2021-01-03")
                                        .venue("Town house")
                                        .recordings(List.of(HearingRecordingDetails.builder()
                                                .value(DocumentLink.builder()
                                                        .documentFilename("Test file.mp3")
                                                        .documentUrl("http://dm-store:5005")
                                                        .documentBinaryUrl("http://dm-store:5005/documents/document-id/binary")
                                                        .build())
                                                .build()))
                                        .build())
                                .build())
                        .build(),
                        HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.APPELLANT.getCode()).build())
                                .build()))
                .build());

        Optional<HearingRecordingResponse> response = citizenRequestService.findHearingRecordings(IDENTIFIER, AUTHORISATION);
        assertThat(response.isEmpty(), is(false));
        assertThat(response.get(), any(HearingRecordingResponse.class));
        assertThat(response.get().getOutstandingHearingRecordings().size(), is(1));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getHearingId(), is("id_1"));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getHearingDate(), is("2021-01-03"));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getVenue(), is("Town house"));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getHearingRecordings().get(0).getDocumentUrl(), is("document-id"));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getHearingRecordings().get(0).getFileName(), is("Test file.mp3"));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getHearingRecordings().get(0).getFileType(), is("mp3"));
        assertThat(response.get().getReleasedHearingRecordings().size(), is(1));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getHearingId(), is("id_1"));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getHearingDate(), is("2021-01-03"));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getVenue(), is("Town house"));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getHearingRecordings().get(0).getDocumentUrl(), is("document-id"));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getHearingRecordings().get(0).getFileName(), is("Test file.mp3"));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getHearingRecordings().get(0).getFileType(), is("mp3"));
    }

    @Test
    void testFindHearingRecordings_returnsHearingWithRecording_andFilterOutReleasedAndOutstandingFromRequestableHearingRecordings() {
        when(caseData.getHearings()).thenReturn(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                        .hearingId("id_1")
                        .hearingDate("2021-01-03")
                        .venue(Venue.builder().name("Town house").build())
                        .build())
                .build(),
                Hearing.builder()
                        .value(HearingDetails.builder()
                                .hearingId("id_2")
                                .hearingDate("2021-01-05")
                                .venue(Venue.builder().name("Crown house").build())
                                .build())
                        .build(),
                Hearing.builder()
                        .value(HearingDetails.builder()
                                .hearingId("id_3")
                                .hearingDate("2021-01-07")
                                .venue(Venue.builder().name("City Center").build())
                                .build())
                        .build()));

        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .sscsHearingRecordings(List.of(
                        SscsHearingRecording.builder()
                                .value(SscsHearingRecordingDetails.builder().hearingId("id_1").build())
                                .build(),
                        SscsHearingRecording.builder()
                                .value(SscsHearingRecordingDetails.builder().hearingId("id_2").build())
                                .build(),
                        SscsHearingRecording.builder()
                                .value(SscsHearingRecordingDetails.builder().hearingId("id_3").build())
                                .build()))
                .requestedHearings(List.of(HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.APPELLANT.getCode())
                                        .sscsHearingRecording(SscsHearingRecordingDetails.builder()
                                                .hearingId("id_1")
                                                .hearingDate("2021-01-03")
                                                .venue("Town house")
                                                .recordings(List.of(HearingRecordingDetails.builder()
                                                        .value(DocumentLink.builder()
                                                                .documentFilename("Test file.mp3")
                                                                .documentUrl("http://dm-store:5005")
                                                                .documentBinaryUrl("http://dm-store:5005/documents/document-id/binary")
                                                                .build())
                                                        .build()))
                                                .build())
                                        .build())
                                .build()))
                .citizenReleasedHearings(List.of(HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.APPELLANT.getCode())
                                        .sscsHearingRecording(SscsHearingRecordingDetails.builder()
                                                .hearingId("id_2")
                                                .hearingDate("2021-01-03")
                                                .venue("Town house")
                                                .recordings(List.of(HearingRecordingDetails.builder()
                                                        .value(DocumentLink.builder()
                                                                .documentFilename("Test file.mp3")
                                                                .documentUrl("http://dm-store:5005")
                                                                .documentBinaryUrl("http://dm-store:5005/documents/document-id/binary")
                                                                .build())
                                                        .build()))
                                                .build())
                                        .build())
                                .build()))
                .build());

        Optional<HearingRecordingResponse> response = citizenRequestService.findHearingRecordings(IDENTIFIER, AUTHORISATION);
        assertThat(response.isEmpty(), is(false));
        assertThat(response.get(), any(HearingRecordingResponse.class));
        assertThat(response.get().getOutstandingHearingRecordings().size(), is(1));
        assertThat(response.get().getReleasedHearingRecordings().size(), is(1));
        assertThat(response.get().getRequestableHearingRecordings().size(), is(1));
        assertThat(response.get().getRequestableHearingRecordings().get(0).getHearingId(), is("id_3"));
        assertThat(response.get().getRequestableHearingRecordings().get(0).getHearingDate(), is("2021-01-07"));
        assertThat(response.get().getRequestableHearingRecordings().get(0).getVenue(), is("City Center"));
    }

    @Test
    void testFindHearingRecordings_filterReleasedAndOutstandingHearingByRequestingParty_Rep() {
        when(caseData.getHearings()).thenReturn(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                        .hearingId("id_1")
                        .hearingDate("2021-01-03")
                        .venue(Venue.builder().name("Town house").build())
                        .build())
                .build()));
        when(caseData.getSubscriptions()).thenReturn(Subscriptions.builder()
                .representativeSubscription(Subscription.builder().email(E_MAIL).build()).build());
        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .requestedHearings(List.of(HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.REPRESENTATIVE.getCode())
                                        .sscsHearingRecording(SscsHearingRecordingDetails.builder()
                                                .hearingId("id_1")
                                                .hearingDate("2021-01-03")
                                                .venue("Town house")
                                                .recordings(List.of(HearingRecordingDetails.builder()
                                                        .value(DocumentLink.builder().build())
                                                        .build()))
                                                .build())
                                        .build())
                                .build(),
                        HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.APPELLANT.getCode()).build())
                                .build()))
                .build());

        Optional<HearingRecordingResponse> response = citizenRequestService.findHearingRecordings(IDENTIFIER, AUTHORISATION);
        assertThat(response.isEmpty(), is(false));
        assertThat(response.get(), any(HearingRecordingResponse.class));
        assertThat(response.get().getOutstandingHearingRecordings().size(), is(1));
    }

    @Test
    void testFindHearingRecordings_filterReleasedAndOutstandingHearingByRequestingParty_JointParty() {
        when(caseData.getHearings()).thenReturn(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                        .hearingId("id_1")
                        .hearingDate("2021-01-03")
                        .venue(Venue.builder().name("Town house").build())
                        .build())
                .build()));
        when(caseData.getSubscriptions()).thenReturn(Subscriptions.builder()
                .jointPartySubscription(Subscription.builder().email(E_MAIL).build()).build());
        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .requestedHearings(List.of(HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.JOINT_PARTY.getCode())
                                        .sscsHearingRecording(SscsHearingRecordingDetails.builder()
                                                .hearingId("id_1")
                                                .hearingDate("2021-01-03")
                                                .venue("Town house")
                                                .recordings(List.of(HearingRecordingDetails.builder()
                                                        .value(DocumentLink.builder().build())
                                                        .build()))
                                                .build())
                                        .build())
                                .build(),
                        HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.APPELLANT.getCode()).build())
                                .build()))
                .build());

        Optional<HearingRecordingResponse> response = citizenRequestService.findHearingRecordings(IDENTIFIER, AUTHORISATION);
        assertThat(response.isEmpty(), is(false));
        assertThat(response.get(), any(HearingRecordingResponse.class));
        assertThat(response.get().getOutstandingHearingRecordings().size(), is(1));
    }

    @Test
    void testFindHearingRecordings_returnsHearingWithRecording_asRequestableHearingRecordings_OtherParty() {
        when(caseData.getHearings()).thenReturn(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                        .hearingId("id_1")
                        .hearingDate("2021-01-03")
                        .venue(Venue.builder().name("Town house").build())
                        .build())
                .build()));
        when(caseData.getOtherParties()).thenReturn(List.of(CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id("1")
                        .otherPartySubscription(Subscription.builder().email(E_MAIL).build()).build())
                        .build()));
        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .sscsHearingRecordings(List.of(SscsHearingRecording.builder()
                        .value(SscsHearingRecordingDetails.builder().hearingId("id_1").build())
                        .build()))
                .build());

        Optional<HearingRecordingResponse> response = citizenRequestService.findHearingRecordings(IDENTIFIER, AUTHORISATION);
        assertThat(response.isEmpty(), is(false));
        assertThat(response.get(), any(HearingRecordingResponse.class));
        assertThat(response.get().getOutstandingHearingRecordings().isEmpty(), is(true));
        assertThat(response.get().getReleasedHearingRecordings().isEmpty(), is(true));
        assertThat(response.get().getRequestableHearingRecordings().isEmpty(), is(false));
        assertThat(response.get().getRequestableHearingRecordings().get(0).getHearingId(), is("id_1"));
        assertThat(response.get().getRequestableHearingRecordings().get(0).getHearingDate(), is("2021-01-03"));
        assertThat(response.get().getRequestableHearingRecordings().get(0).getVenue(), is("Town house"));
    }

    @Test
    void testFindHearingRecordings_returnsHearingWithRecording_asRequestableHearingRecordings_OtherPartyRep() {
        when(caseData.getHearings()).thenReturn(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                        .hearingId("id_1")
                        .hearingDate("2021-01-03")
                        .venue(Venue.builder().name("Town house").build())
                        .build())
                .build()));
        when(caseData.getOtherParties()).thenReturn(List.of(CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id("1")
                        .rep(Representative.builder().id("2").build())
                        .otherPartyRepresentativeSubscription(Subscription.builder().email(E_MAIL).build()).build())
                .build()));
        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .sscsHearingRecordings(List.of(SscsHearingRecording.builder()
                        .value(SscsHearingRecordingDetails.builder().hearingId("id_1").build())
                        .build()))
                .build());

        Optional<HearingRecordingResponse> response = citizenRequestService.findHearingRecordings(IDENTIFIER, AUTHORISATION);
        assertThat(response.isEmpty(), is(false));
        assertThat(response.get(), any(HearingRecordingResponse.class));
        assertThat(response.get().getOutstandingHearingRecordings().isEmpty(), is(true));
        assertThat(response.get().getReleasedHearingRecordings().isEmpty(), is(true));
        assertThat(response.get().getRequestableHearingRecordings().isEmpty(), is(false));
        assertThat(response.get().getRequestableHearingRecordings().get(0).getHearingId(), is("id_1"));
        assertThat(response.get().getRequestableHearingRecordings().get(0).getHearingDate(), is("2021-01-03"));
        assertThat(response.get().getRequestableHearingRecordings().get(0).getVenue(), is("Town house"));
    }

    @Test
    void testFindHearingRecordings_returnsHearingWithRecording_asRequestableHearingRecordings_OtherPartyAppointee() {
        when(caseData.getHearings()).thenReturn(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                        .hearingId("id_1")
                        .hearingDate("2021-01-03")
                        .venue(Venue.builder().name("Town house").build())
                        .build())
                .build()));
        when(caseData.getOtherParties()).thenReturn(List.of(CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id("1")
                        .appointee(Appointee.builder().id("2").build())
                        .otherPartyAppointeeSubscription(Subscription.builder().email(E_MAIL).build()).build())
                .build()));
        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .sscsHearingRecordings(List.of(SscsHearingRecording.builder()
                        .value(SscsHearingRecordingDetails.builder().hearingId("id_1").build())
                        .build()))
                .build());

        Optional<HearingRecordingResponse> response = citizenRequestService.findHearingRecordings(IDENTIFIER, AUTHORISATION);
        assertThat(response.isEmpty(), is(false));
        assertThat(response.get(), any(HearingRecordingResponse.class));
        assertThat(response.get().getOutstandingHearingRecordings().isEmpty(), is(true));
        assertThat(response.get().getReleasedHearingRecordings().isEmpty(), is(true));
        assertThat(response.get().getRequestableHearingRecordings().isEmpty(), is(false));
        assertThat(response.get().getRequestableHearingRecordings().get(0).getHearingId(), is("id_1"));
        assertThat(response.get().getRequestableHearingRecordings().get(0).getHearingDate(), is("2021-01-03"));
        assertThat(response.get().getRequestableHearingRecordings().get(0).getVenue(), is("Town house"));
    }

    @Test
    void testFindHearingRecordings_filterReleasedAndOutstandingHearingByRequestingParty_OtherParty() {
        when(caseData.getHearings()).thenReturn(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                        .hearingId("id_1")
                        .hearingDate("2021-01-03")
                        .venue(Venue.builder().name("Town house").build())
                        .build())
                .build()));
        when(caseData.getOtherParties()).thenReturn(List.of(CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id("1")
                        .otherPartySubscription(Subscription.builder().email(E_MAIL).build()).build())
                .build()));
        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .requestedHearings(List.of(HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.OTHER_PARTY.getCode())
                                        .otherPartyId("1")
                                        .sscsHearingRecording(SscsHearingRecordingDetails.builder()
                                                .hearingId("id_1")
                                                .hearingDate("2021-01-03")
                                                .venue("Town house")
                                                .recordings(List.of(HearingRecordingDetails.builder()
                                                        .value(DocumentLink.builder()
                                                                .documentFilename("Test file.mp3")
                                                                .documentUrl("http://dm-store:5005")
                                                                .documentBinaryUrl("http://dm-store:5005/documents/document-id/binary")
                                                                .build())
                                                        .build()))
                                                .build())
                                        .build())
                                .build(),
                        HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.OTHER_PARTY.getCode())
                                        .otherPartyId("2")
                                        .build())
                                .build(),
                        HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.APPELLANT.getCode()).build())
                                .build()))
                .citizenReleasedHearings(List.of(HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.OTHER_PARTY.getCode())
                                        .otherPartyId("1")
                                        .sscsHearingRecording(SscsHearingRecordingDetails.builder()
                                                .hearingId("id_1")
                                                .hearingDate("2021-01-03")
                                                .venue("Town house")
                                                .recordings(List.of(HearingRecordingDetails.builder()
                                                        .value(DocumentLink.builder()
                                                                .documentFilename("Test file.mp3")
                                                                .documentUrl("http://dm-store:5005")
                                                                .documentBinaryUrl("http://dm-store:5005/documents/document-id/binary")
                                                                .build())
                                                        .build()))
                                                .build())
                                        .build())
                                .build(),
                        HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.APPELLANT.getCode()).build())
                                .build(),
                        HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.OTHER_PARTY_REPRESENTATIVE.getCode()).build())
                                .build()))
                .dwpReleasedHearings(List.of(HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.OTHER_PARTY.getCode())
                                        .otherPartyId("1")
                                        .sscsHearingRecording(SscsHearingRecordingDetails.builder()
                                                .hearingId("id_1")
                                                .hearingDate("2021-01-03")
                                                .venue("Town house")
                                                .recordings(List.of(HearingRecordingDetails.builder()
                                                        .value(DocumentLink.builder()
                                                                .documentFilename("Test file.mp3")
                                                                .documentUrl("http://dm-store:5005")
                                                                .documentBinaryUrl("http://dm-store:5005/documents/document-id/binary")
                                                                .build())
                                                        .build()))
                                                .build())
                                        .build())
                                .build(),
                        HearingRecordingRequest.builder()
                                .value(HearingRecordingRequestDetails.builder()
                                        .requestingParty(PartyItemList.APPELLANT.getCode()).build())
                                .build()))
                .build());

        Optional<HearingRecordingResponse> response = citizenRequestService.findHearingRecordings(IDENTIFIER, AUTHORISATION);
        assertThat(response.isEmpty(), is(false));
        assertThat(response.get(), any(HearingRecordingResponse.class));
        assertThat(response.get().getOutstandingHearingRecordings().size(), is(1));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getHearingId(), is("id_1"));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getHearingDate(), is("2021-01-03"));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getVenue(), is("Town house"));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getHearingRecordings().get(0).getDocumentUrl(), is("document-id"));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getHearingRecordings().get(0).getFileName(), is("Test file.mp3"));
        assertThat(response.get().getOutstandingHearingRecordings().get(0).getHearingRecordings().get(0).getFileType(), is("mp3"));
        assertThat(response.get().getReleasedHearingRecordings().size(), is(1));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getHearingId(), is("id_1"));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getHearingDate(), is("2021-01-03"));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getVenue(), is("Town house"));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getHearingRecordings().get(0).getDocumentUrl(), is("document-id"));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getHearingRecordings().get(0).getFileName(), is("Test file.mp3"));
        assertThat(response.get().getReleasedHearingRecordings().get(0).getHearingRecordings().get(0).getFileType(), is("mp3"));
    }

    @Test
    void testRequestHearingRecordings_forEmptyCaseDetails_returnsFalse() {
        when(onlineHearingService.getCcdCaseByIdentifier(IDENTIFIER)).thenReturn(Optional.empty());

        boolean response = citizenRequestService.requestHearingRecordings(IDENTIFIER, anyList(), AUTHORISATION);
        assertThat(response, is(false));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testRequestHearingRecordings_addARequestForHearingId(boolean caseUpdateV2Enabled) {
        ReflectionTestUtils.setField(citizenRequestService, "hearingsRecordingReqCaseUpdateV2Enabled", caseUpdateV2Enabled);
        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .sscsHearingRecordings(List.of(
                        SscsHearingRecording.builder()
                        .value(SscsHearingRecordingDetails.builder()
                                .hearingId("id_1")
                                .venue("Town House")
                                .build())
                        .build(),
                        SscsHearingRecording.builder()
                        .value(SscsHearingRecordingDetails.builder()
                                .hearingId("id_2")
                                .venue("City Center")
                                .build())
                        .build()))
                .build());

        when(caseData.getSubscriptions()).thenReturn(Subscriptions.builder()
                .jointPartySubscription(Subscription.builder().email(E_MAIL).build()).build());

        boolean response = citizenRequestService.requestHearingRecordings(IDENTIFIER, List.of("id_1"), AUTHORISATION);
        assertThat(response, is(true));

        if (caseUpdateV2Enabled) {
            verifyUpdateCaseV2AndApplyCaseDataConsumerChanges(Long.parseLong(IDENTIFIER));
        } else {
            verify(ccdService).updateCase(
                    argThat(argument -> argument.getSscsHearingRecordingCaseData().getRequestedHearings().size() == 1),
                    eq(Long.parseLong(IDENTIFIER)),
                    eq(CITIZEN_REQUEST_HEARING_RECORDING.getCcdType()),
                    eq("SSCS - hearing recording request from MYA"),
                    eq("Requested hearing recordings"),
                    eq(idamTokens)
            );
        }

        assertThat(caseData.getSscsHearingRecordingCaseData().getRequestedHearings().size(), is(1));
        assertThat(caseData.getSscsHearingRecordingCaseData().getHearingRecordingRequestOutstanding(), is(YesNo.YES));
        HearingRecordingRequest hearingRecordingRequest = caseData.getSscsHearingRecordingCaseData().getRequestedHearings().get(0);
        assertThat(hearingRecordingRequest.getValue().getRequestingParty(), is(PartyItemList.JOINT_PARTY.getCode()));
        assertThat(hearingRecordingRequest.getValue().getDateRequested(), is(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        assertThat(hearingRecordingRequest.getValue().getSscsHearingRecording().getHearingId(), is("id_1"));
        assertThat(hearingRecordingRequest.getValue().getSscsHearingRecording().getVenue(), is("Town House"));
    }

    @ParameterizedTest
    @MethodSource("buildOtherParty")
    void testRequestHearingRecordings_addARequestForHearingId_OtherParty(CcdValue<OtherParty> otherParty, String otherPartyId, String code) {
        ReflectionTestUtils.setField(citizenRequestService, "hearingsRecordingReqCaseUpdateV2Enabled", false);
        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .sscsHearingRecordings(List.of(
                        SscsHearingRecording.builder()
                                .value(SscsHearingRecordingDetails.builder()
                                        .hearingId("id_1")
                                        .venue("Town House")
                                        .build())
                                .build(),
                        SscsHearingRecording.builder()
                                .value(SscsHearingRecordingDetails.builder()
                                        .hearingId("id_2")
                                        .venue("City Center")
                                        .build())
                                .build()))
                .build());

        when(caseData.getOtherParties()).thenReturn(List.of(otherParty));

        boolean response = citizenRequestService.requestHearingRecordings(IDENTIFIER, List.of("id_1"), AUTHORISATION);
        assertThat(response, is(true));
        assertThat(caseData.getSscsHearingRecordingCaseData().getRequestedHearings().size(), is(1));
        assertThat(caseData.getSscsHearingRecordingCaseData().getHearingRecordingRequestOutstanding(), is(YesNo.YES));
        HearingRecordingRequest hearingRecordingRequest = caseData.getSscsHearingRecordingCaseData().getRequestedHearings().get(0);
        assertThat(hearingRecordingRequest.getValue().getRequestingParty(), is(code));
        assertThat(hearingRecordingRequest.getValue().getOtherPartyId(), is(otherPartyId));
        assertThat(hearingRecordingRequest.getValue().getDateRequested(), is(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        assertThat(hearingRecordingRequest.getValue().getSscsHearingRecording().getHearingId(), is("id_1"));
        assertThat(hearingRecordingRequest.getValue().getSscsHearingRecording().getVenue(), is("Town House"));
    }

    @ParameterizedTest
    @MethodSource("buildOtherParty")
    void testRequestHearingRecordingsUpdateCaseV2_addARequestForHearingId_OtherParty(CcdValue<OtherParty> otherParty, String otherPartyId, String code) {
        ReflectionTestUtils.setField(citizenRequestService, "hearingsRecordingReqCaseUpdateV2Enabled", true);
        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .sscsHearingRecordings(List.of(
                        SscsHearingRecording.builder()
                                .value(SscsHearingRecordingDetails.builder()
                                        .hearingId("id_1")
                                        .venue("Town House")
                                        .build())
                                .build(),
                        SscsHearingRecording.builder()
                                .value(SscsHearingRecordingDetails.builder()
                                        .hearingId("id_2")
                                        .venue("City Center")
                                        .build())
                                .build()))
                .build());

        when(caseData.getOtherParties()).thenReturn(List.of(otherParty));

        boolean response = citizenRequestService.requestHearingRecordings(IDENTIFIER, List.of("id_1"), AUTHORISATION);
        assertThat(response, is(true));

        verifyUpdateCaseV2AndApplyCaseDataConsumerChanges(Long.parseLong(IDENTIFIER));

        assertThat(caseData.getSscsHearingRecordingCaseData().getRequestedHearings().size(), is(1));
        assertThat(caseData.getSscsHearingRecordingCaseData().getHearingRecordingRequestOutstanding(), is(YesNo.YES));
        HearingRecordingRequest hearingRecordingRequest = caseData.getSscsHearingRecordingCaseData().getRequestedHearings().get(0);
        assertThat(hearingRecordingRequest.getValue().getRequestingParty(), is(code));
        assertThat(hearingRecordingRequest.getValue().getOtherPartyId(), is(otherPartyId));
        assertThat(hearingRecordingRequest.getValue().getDateRequested(), is(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        assertThat(hearingRecordingRequest.getValue().getSscsHearingRecording().getHearingId(), is("id_1"));
        assertThat(hearingRecordingRequest.getValue().getSscsHearingRecording().getVenue(), is("Town House"));
    }

    private static Stream<Arguments> buildOtherParty() {
        return Stream.of(
            Arguments.of(CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                    .id("1")
                    .otherPartySubscription(Subscription.builder().email(E_MAIL).build()).build())
                .build(), "1", PartyItemList.OTHER_PARTY.getCode()),
            Arguments.of(CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                    .id("1")
                    .isAppointee(YesNo.YES.getValue())
                    .appointee(Appointee.builder().id("2").build())
                    .otherPartyAppointeeSubscription(Subscription.builder().email(E_MAIL).build()).build())
                .build(), "1", PartyItemList.OTHER_PARTY.getCode()),
            Arguments.of(CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                    .id("1")
                    .rep(Representative.builder().id("2").hasRepresentative(YesNo.YES.getValue()).build())
                    .otherPartyRepresentativeSubscription(Subscription.builder().email(E_MAIL).build()).build())
                .build(), "2", PartyItemList.OTHER_PARTY_REPRESENTATIVE.getCode())
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testRequestHearingRecordings_addARequestToExistingList(boolean caseUpdateV2Enabled) {
        ReflectionTestUtils.setField(citizenRequestService, "hearingsRecordingReqCaseUpdateV2Enabled", caseUpdateV2Enabled);
        List<HearingRecordingRequest> exitingRequests = new ArrayList<>();
        exitingRequests.add(HearingRecordingRequest.builder().build());
        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .sscsHearingRecordings(List.of(
                        SscsHearingRecording.builder()
                                .value(SscsHearingRecordingDetails.builder()
                                        .hearingId("id_1")
                                        .build())
                                .build()))
                .requestedHearings(exitingRequests)
                .build());

        boolean response = citizenRequestService.requestHearingRecordings(IDENTIFIER, List.of("id_1"), AUTHORISATION);
        assertThat(response, is(true));

        if (caseUpdateV2Enabled) {
            verifyUpdateCaseV2AndApplyCaseDataConsumerChanges(Long.parseLong(IDENTIFIER));
        } else {
            verify(ccdService).updateCase(
                    eq(caseData),
                    eq(Long.parseLong(IDENTIFIER)),
                    eq(CITIZEN_REQUEST_HEARING_RECORDING.getCcdType()),
                    eq("SSCS - hearing recording request from MYA"),
                    eq("Requested hearing recordings"),
                    eq(idamTokens)
            );
        }

        assertThat(exitingRequests.size(), is(2));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testRequestHearingRecordings_verifyUpdateCase(boolean caseUpdateV2Enabled) {
        ReflectionTestUtils.setField(citizenRequestService, "hearingsRecordingReqCaseUpdateV2Enabled", caseUpdateV2Enabled);
        when(caseDetails.getId()).thenReturn(1L);
        when(caseData.getSscsHearingRecordingCaseData()).thenReturn(SscsHearingRecordingCaseData.builder()
                .sscsHearingRecordings(List.of(
                        SscsHearingRecording.builder()
                                .value(SscsHearingRecordingDetails.builder()
                                        .hearingId("id_1")
                                        .build())
                                .build()))
                .build());

        boolean response = citizenRequestService.requestHearingRecordings(IDENTIFIER, List.of("id_1"), AUTHORISATION);
        assertThat(response, is(true));

        if (caseUpdateV2Enabled) {
            verifyUpdateCaseV2AndApplyCaseDataConsumerChanges(1L);
        } else {
            verify(ccdService).updateCase(
                    argThat(argument -> argument.getSscsHearingRecordingCaseData().getRequestedHearings().size() == 1),
                    eq(1L),
                    eq(CITIZEN_REQUEST_HEARING_RECORDING.getCcdType()),
                    eq("SSCS - hearing recording request from MYA"),
                    eq("Requested hearing recordings"),
                    eq(idamTokens)
            );
        }

        assertThat(caseData.getSscsHearingRecordingCaseData().getRequestedHearings().size(), is(1));
        assertThat(caseData.getSscsHearingRecordingCaseData().getHearingRecordingRequestOutstanding(), is(YesNo.YES));
    }

    private void verifyUpdateCaseV2AndApplyCaseDataConsumerChanges(long caseIdentifier) {
        verify(updateCcdCaseService).updateCaseV2(
                eq(caseIdentifier),
                eq(CITIZEN_REQUEST_HEARING_RECORDING.getCcdType()),
                eq("SSCS - hearing recording request from MYA"),
                eq("Requested hearing recordings"),
                eq(idamTokens),
                sscsCaseDataCaptor.capture()
        );

        Consumer<SscsCaseData> captorValue = sscsCaseDataCaptor.getValue();
        captorValue.accept(caseData);
    }
}
