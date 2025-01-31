package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;

public class PostCodeControllerTest {
    PostCodeController postCodeController;

    private AirLookupService airLookupServiceMock;

    @Before
    public void setUp() {
        airLookupServiceMock = mock(AirLookupService.class);
        postCodeController = new PostCodeController(airLookupServiceMock);
    }

    @Test
    public void shouldReturnRegionalCentre() {
        when(airLookupServiceMock.lookupRegionalCentre("postCode")).thenReturn("regionalCentre");

        ResponseEntity<String> response = postCodeController.getRegionalCentre("postCode");
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void shouldNotReturnRegionalCentre() {
        when(airLookupServiceMock.lookupRegionalCentre("postCode")).thenReturn(null);

        ResponseEntity<String> response = postCodeController.getRegionalCentre("postCode");
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }
}
