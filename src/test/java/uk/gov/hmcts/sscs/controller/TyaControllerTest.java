package uk.gov.hmcts.sscs.controller;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.service.TribunalsService;

@RunWith(MockitoJUnitRunner.class)
public class TyaControllerTest {

    public static final String APPEAL_ID = "appeal-id";
    public static final String NOT_FOUND_APPEAL_ID = "not-found-appeal-id";

    @Mock
    private TribunalsService tribunalsService;

    private TyaController controller;

    @Before
    public void setUp() {
        controller = new TyaController(tribunalsService);
    }

    @Test(expected = AppealNotFoundException.class)
    public void testToThrowAppealNotFoundExceptionIfAppealNotFound() throws CcdException {
        //Given
        String appealId = NOT_FOUND_APPEAL_ID;
        when(tribunalsService.findAppeal(appealId)).thenThrow(
            new AppealNotFoundException(appealId));

        //When
        controller.getAppeal(appealId, null);
    }

    @Test
    public void testToReturnAppealForGivenAppealNumber() throws CcdException {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        //Given
        when(tribunalsService.findAppeal(APPEAL_ID)).thenReturn(node);

        //When
        ResponseEntity<String> receivedAppeal = controller.getAppeal(APPEAL_ID, null);

        //Then
        assertThat(receivedAppeal.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedAppeal.getBody(), equalTo(node.toString()));
    }


    @Test
    public void unsubscribeForGivenAppealNumber() throws CcdException {
        // Given
        when(tribunalsService.unsubscribe(APPEAL_ID, "reason")).thenReturn("benefitTypeValue");

        // When
        ResponseEntity<String> benefitType = controller.unsubscribe(APPEAL_ID, "reason");

        // Then
        assertThat(benefitType.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(benefitType.getBody(), equalTo("{\"benefitType\":\"benefitTypeValue\"}"));
    }

}