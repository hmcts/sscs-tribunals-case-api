package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.sscs.model.tya.HearingRecordingResponse;
import uk.gov.hmcts.reform.sscs.service.citizenrequest.CitizenRequestService;

public class CitizenRequestControllerTest {
    private static final String IDENTIFIER = "1234";
    private static final String AUTHORISATION = "Bearer 12jkas";

    @Mock
    private CitizenRequestService citizenRequestService;

    private CitizenRequestController citizenRequestController;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        citizenRequestController = new CitizenRequestController(citizenRequestService);
    }

    @Test
    public void testGetHearingRecording_notFound() {
        when(citizenRequestService.findHearingRecordings(IDENTIFIER,AUTHORISATION)).thenReturn(Optional.empty());

        ResponseEntity<HearingRecordingResponse> response = citizenRequestController.getHearingRecording(AUTHORISATION,IDENTIFIER);
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }

    @Test
    public void testGetHearingRecording_returnsHearingRecordingResponse() {
        HearingRecordingResponse hearingRecordingResponse = HearingRecordingResponse.builder().build();
        when(citizenRequestService.findHearingRecordings(IDENTIFIER,AUTHORISATION)).thenReturn(Optional.of(hearingRecordingResponse));

        ResponseEntity<HearingRecordingResponse> response = citizenRequestController.getHearingRecording(AUTHORISATION,IDENTIFIER);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is(hearingRecordingResponse));
    }

    @Test
    public void testSubmitHearingRecordingRequest_notFound() {
        when(citizenRequestService.requestHearingRecordings(eq(IDENTIFIER),anyList(),eq(AUTHORISATION))).thenReturn(false);

        ResponseEntity<HearingRecordingResponse> response = citizenRequestController.submitHearingRecordingRequest(AUTHORISATION,IDENTIFIER, List.of());
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }

    @Test
    public void testSubmitHearingRecordingRequest_returnsHearingRecordingResponse() {
        when(citizenRequestService.requestHearingRecordings(eq(IDENTIFIER),anyList(),eq(AUTHORISATION))).thenReturn(true);

        ResponseEntity<HearingRecordingResponse> response = citizenRequestController.submitHearingRecordingRequest(AUTHORISATION,IDENTIFIER, List.of());
        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
    }
}
