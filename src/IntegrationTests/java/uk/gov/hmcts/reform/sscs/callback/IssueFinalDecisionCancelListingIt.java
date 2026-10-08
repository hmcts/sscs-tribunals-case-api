package uk.gov.hmcts.reform.sscs.callback;

import static java.time.ZoneId.systemDefault;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.createUploadResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.hmc.topic.HearingRequestHandler;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integration")
@TestPropertySource(locations = "classpath:config/application_it.properties")
class IssueFinalDecisionCancelListingIt extends AbstractEventIt {

    private static final String CALLBACK_JSON = "callback/issueFinalDecisionDescriptorCallback.json";
    private static final String CASE_ID = "12345656789";
    private static final String HEARING_ID = "2000012345";

    @MockitoBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;

    @MockitoBean
    private EvidenceManagementService evidenceManagementService;

    @MockitoBean
    private HearingRequestHandler hearingRequestHandler;

    @BeforeEach
    void setUp() throws Exception {
        setup();
        final byte[] pdfBytes = toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(evidenceManagementService.download(any(), anyString())).thenReturn(pdfBytes);
        final UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(), anyString())).thenReturn(uploadResponse);
    }

    @Test
    void givenListAssistCaseAwaitingListingInReadyToList_whenIssueFinalDecision_thenListingRequestIsCancelled() throws Exception {
        json = buildCallback(State.READY_TO_LIST, hearing(awaitingListingHearingDetails().build()));

        final PreSubmitCallbackResponse<SscsCaseData> result = assertResponseOkAndGetResult(CallbackType.ABOUT_TO_SUBMIT);

        assertThat(result.getErrors()).isEmpty();
        assertThatCancelHearingRequestSent();
    }

    @Test
    void givenListAssistCaseWithBookedHearingInTheFuture_whenIssueFinalDecision_thenHearingIsCancelled() throws Exception {
        json = buildCallback(State.HEARING, futureListedHearing());

        final PreSubmitCallbackResponse<SscsCaseData> result = assertResponseOkAndGetResult(CallbackType.ABOUT_TO_SUBMIT);

        assertThat(result.getErrors()).isEmpty();
        assertThatCancelHearingRequestSent();
    }

    @Test
    void givenListAssistCaseWithBookedHearingToday_whenIssueFinalDecision_thenHearingIsNotCancelled() throws Exception {
        json = buildCallback(State.HEARING, listedHearing(LocalDate.now(systemDefault()), LocalTime.MIDNIGHT));

        final PreSubmitCallbackResponse<SscsCaseData> result = assertResponseOkAndGetResult(CallbackType.ABOUT_TO_SUBMIT);

        assertThat(result.getErrors()).isEmpty();
        verifyNoInteractions(hearingRequestHandler);
    }

    @Test
    void givenListAssistCaseInReadyToListWithCancelledListingRequest_whenIssueFinalDecision_thenNoCancellationSent() throws Exception {
        json = buildCallback(State.READY_TO_LIST,
            hearing(awaitingListingHearingDetails().hearingStatus(HearingStatus.CANCELLED).build()));

        final PreSubmitCallbackResponse<SscsCaseData> result = assertResponseOkAndGetResult(CallbackType.ABOUT_TO_SUBMIT);

        assertThat(result.getErrors()).isEmpty();
        verifyNoInteractions(hearingRequestHandler);
    }

    private void assertThatCancelHearingRequestSent() throws Exception {
        final ArgumentCaptor<HearingRequest> captor = ArgumentCaptor.forClass(HearingRequest.class);
        verify(hearingRequestHandler).handleHearingRequest(captor.capture());
        final HearingRequest hearingRequest = captor.getValue();
        assertThat(hearingRequest.getCcdCaseId()).isEqualTo(CASE_ID);
        assertThat(hearingRequest.getHearingRoute()).isEqualTo(HearingRoute.LIST_ASSIST);
        assertThat(hearingRequest.getHearingState()).isEqualTo(HearingState.CANCEL_HEARING);
        assertThat(hearingRequest.getCancellationReason()).isEqualTo(CancellationReason.OTHER);
    }

    private String buildCallback(final State stateBefore, final Hearing hearing) throws IOException {
        final Callback<SscsCaseData> callback = deserializer.deserialize(getJson(CALLBACK_JSON));
        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setCcdCaseId(CASE_ID);
        caseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder().hearingRoute(HearingRoute.LIST_ASSIST).build());
        caseData.setHearings(List.of(hearing));

        final Callback<SscsCaseData> updatedCallback = new Callback<>(withState(callback.getCaseDetails(), stateBefore),
            callback.getCaseDetailsBefore().map(caseDetailsBefore -> withState(caseDetailsBefore, stateBefore)),
            callback.getEvent(), callback.isIgnoreWarnings());
        return mapper.writeValueAsString(updatedCallback);
    }

    private static CaseDetails<SscsCaseData> withState(final CaseDetails<SscsCaseData> caseDetails, final State state) {
        return new CaseDetails<>(caseDetails.getId(), caseDetails.getJurisdiction(), state, caseDetails.getCaseData(),
            caseDetails.getCreatedDate(), caseDetails.getCaseTypeId());
    }

    private static Hearing hearing(final HearingDetails hearingDetails) {
        return Hearing.builder().value(hearingDetails).build();
    }

    private static HearingDetails.HearingDetailsBuilder awaitingListingHearingDetails() {
        return HearingDetails.builder()
            .hearingId(HEARING_ID)
            .hearingStatus(HearingStatus.AWAITING_LISTING);
    }

    private static Hearing futureListedHearing() {
        return listedHearing(LocalDate.now(systemDefault()).plusDays(30), LocalTime.of(10, 0));
    }

    private static Hearing listedHearing(final LocalDate hearingDate, final LocalTime hearingTime) {
        return hearing(awaitingListingHearingDetails()
            .hearingStatus(HearingStatus.LISTED)
            .hearingDate(hearingDate.toString())
            .time(hearingTime.toString())
            .venue(Venue.builder().name("Prudential House").build())
            .build());
    }
}
