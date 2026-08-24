package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADMIN_AMEND_LOCATION_DETAILS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.VenueService;

@ExtendWith(MockitoExtension.class)
class AdminAmendLocationDetailsAboutToStartTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String RPC_NAME = "Bradford";
    private static final String VENUE_LABEL = "Bradford Venue";
    private static final String EPIMS_ID = "123456";

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private VenueService venueService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @InjectMocks
    private AdminAmendLocationDetailsAboutToStart handler;

    private SscsCaseData caseData;
    private ExtendedSscsCaseData extendedSscsCaseData;

    @BeforeEach
    void setUp() {
        extendedSscsCaseData = new ExtendedSscsCaseData();

        caseData = new SscsCaseData();
        caseData.setExtendedSscsCaseData(extendedSscsCaseData);
        caseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name(RPC_NAME).build());
        caseData.setProcessingVenue(VENUE_LABEL);
    }

    @Test
    void canHandle_shouldReturnTrue_whenValidEventAndCallbackType() {
        when(callback.getEvent()).thenReturn(ADMIN_AMEND_LOCATION_DETAILS);
        boolean result = handler.canHandle(ABOUT_TO_START, callback);

        assertThat(result).isTrue();
    }

    @Test
    void canHandle_shouldReturnFalse_whenInvalidCallbackType() {
        boolean result = handler.canHandle(ABOUT_TO_SUBMIT, callback);

        assertThat(result).isFalse();
    }

    @Test
    void canHandle_shouldReturnFalse_whenInvalidEvent() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        boolean result = handler.canHandle(ABOUT_TO_START, callback);

        assertThat(result).isFalse();
    }

    @Test
    void canHandle_shouldThrowException_whenCallbackIsNull() {
        assertThatThrownBy(() -> handler.canHandle(ABOUT_TO_START, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("callback must not be null");
    }

    @Test
    void canHandle_shouldThrowException_whenCallbackTypeIsNull() {
        assertThatThrownBy(() -> handler.canHandle(null, callback))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("callbacktype must not be null");
    }

    @Test
    void handle_shouldThrowException_whenCannotHandle() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);

        assertThatThrownBy(() -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot handle callback");
    }

    @Test
    void handle_shouldPopulateDynamicLists_whenValidDataProvided() {
        caseData.setHearings(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                        .hearingDate(LocalDate.now().toString())
                        .venue(Venue.builder().name("venue name").build())
                        .venueId("123")
                        .build())
                .build()));
        when(callback.getEvent()).thenReturn(ADMIN_AMEND_LOCATION_DETAILS);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        caseData.getLatestHearing().getValue().setEpimsId(EPIMS_ID);

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder().name(RPC_NAME).build();
        when(regionalProcessingCenterService.getRegionalProcessingCenterMap())
                .thenReturn(Map.of("rpcKey", rpc));

        VenueDetails venueDetails = VenueDetails.builder().epimsId(EPIMS_ID).gapsVenName(VENUE_LABEL).build();
        when(venueService.getAllVenuesMap())
                .thenReturn(Map.of("venueKey", venueDetails));

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        ExtendedSscsCaseData extendedData = response.getData().getExtendedSscsCaseData();

        assertThat(extendedData.getLocationDetailsRpc().getValue().getCode()).isEqualTo(RPC_NAME);
        assertThat(extendedData.getLocationDetailsRpc().getListItems()).hasSize(2);
        assertThat(extendedData.getLocationDetailsProcessingVenue().getValue().getLabel()).isEqualTo(VENUE_LABEL);
        assertThat(extendedData.getLocationDetailsProcessingVenue().getListItems()).hasSize(2);
        assertThat(extendedData.getLocationDetailsHearingVenue().getValue().getCode()).isEqualTo(EPIMS_ID);
        assertThat(extendedData.getLocationDetailsHearingVenue().getListItems()).hasSize(2);
    }

    @Test
    void handle_shouldAddErrors_whenDropdownListsAreEmpty() {
        when(callback.getEvent()).thenReturn(ADMIN_AMEND_LOCATION_DETAILS);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(regionalProcessingCenterService.getRegionalProcessingCenterMap()).thenReturn(Collections.emptyMap());
        when(venueService.getAllVenuesMap()).thenReturn(Collections.emptyMap());
        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        ExtendedSscsCaseData extendedData = response.getData().getExtendedSscsCaseData();

        assertThat(response.getErrors()).isEmpty();
        assertThat(extendedData.getLocationDetailsRpc().getValue().getCode()).isEqualTo("null");
        assertThat(extendedData.getLocationDetailsProcessingVenue().getValue().getCode()).isEqualTo("null");
        assertThat(extendedData.getLocationDetailsHearingVenue().getValue().getCode()).isEqualTo("null");
    }

    @Test
    void handle_shouldHandleNullLatestHearing() {
        when(callback.getEvent()).thenReturn(ADMIN_AMEND_LOCATION_DETAILS);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(regionalProcessingCenterService.getRegionalProcessingCenterMap()).thenReturn(Collections.emptyMap());
        when(venueService.getAllVenuesMap()).thenReturn(Collections.emptyMap());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        ExtendedSscsCaseData extendedData = response.getData().getExtendedSscsCaseData();

        assertThat(extendedData.getLocationDetailsHearingVenue().getValue().getCode()).isEqualTo("null");
        assertThat(extendedData.getLocationDetailsHearingVenue().getValue().getLabel()).isEqualTo("Choose a Venue");
    }
}