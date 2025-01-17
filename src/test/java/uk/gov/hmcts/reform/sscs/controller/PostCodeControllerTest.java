package uk.gov.hmcts.reform.sscs.controller;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class PostCodeControllerTest {
    PostCodeController postCodeController;

    @Mock
    private AirLookupService airLookupServiceMock;

    @Before
    public void setUp() {
        postCodeController = new PostCodeController(airLookupServiceMock);
    }

    @Test
    void shouldReturnRegionalCentre() {
        when(airLookupServiceMock.lookupRegionalCentre("postCode")).thenReturn("regionalCentre");

        ResponseEntity<String> response = postCodeController.getRegionalCentre("postCode");
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    void shouldNotReturnRegionalCentre() {
        when(airLookupServiceMock.lookupRegionalCentre("postCode")).thenReturn(null);

        ResponseEntity<String> response = postCodeController.getRegionalCentre("postCode");
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }
}
