package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.reform.sscs.service.TribunalsService;


public class TyaControllerTest {

    private static final Long CASE_ID = 123456789L;

    @Mock
    private TribunalsService tribunalsService;

    private TyaController controller;

    @Before
    public void setUp() {
        openMocks(this);
        controller = new TyaController(tribunalsService);
    }

    @Test
    public void testToReturnAppealForGivenCaseReference() throws CcdException {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        //Given
        when(tribunalsService.findAppeal(CASE_ID, false)).thenReturn(node);

        //When
        ResponseEntity<String> receivedAppeal = controller.getAppealByCaseId(CASE_ID, false);

        //Then
        assertThat(receivedAppeal.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedAppeal.getBody(), equalTo(node.toString()));
    }

    @Test
    public void testToReturnMyaAppealForGivenCaseReference() throws CcdException {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        //Given
        when(tribunalsService.findAppeal(CASE_ID, true)).thenReturn(node);

        //When
        ResponseEntity<String> receivedAppeal = controller.getAppealByCaseId(CASE_ID, true);

        //Then
        assertThat(receivedAppeal.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedAppeal.getBody(), equalTo(node.toString()));
    }

    @Test(expected = AppealNotFoundException.class)
    public void testToThrowAppealNotFoundExceptionIfAppealNotFound() throws CcdException {
        //Given
        when(tribunalsService.findAppeal(CASE_ID, true)).thenThrow(
            new AppealNotFoundException(CASE_ID));

        //When
        controller.getAppealByCaseId(CASE_ID, true);
    }

}
