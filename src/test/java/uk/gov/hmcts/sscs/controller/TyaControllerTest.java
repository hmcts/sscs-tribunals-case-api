package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.sscs.model.tya.SurnameResponse;
import uk.gov.hmcts.sscs.service.TribunalsService;
import uk.gov.hmcts.sscs.service.exceptions.InvalidSurnameException;

public class TyaControllerTest {

    private static final String APPEAL_ID = "appeal-id";
    private static final String SURNAME = "surname";
    private static final String NOT_FOUND_APPEAL_ID = "not-found-appeal-id";
    public static final String CCD_CASE_ID = "ccd-case-id";

    @Mock
    private TribunalsService tribunalsService;

    private TyaController controller;

    @Before
    public void setUp() {
        initMocks(this);
        controller = new TyaController(tribunalsService);
    }

    @Test
    public void shouldReturnOkGivenValidAppealNumberSurnameCombination() throws CcdException {
        when(tribunalsService.validateSurname(APPEAL_ID, SURNAME)).thenReturn(new SurnameResponse(CCD_CASE_ID, APPEAL_ID, SURNAME));

        controller.validateSurname(APPEAL_ID, SURNAME);
    }

    @Test(expected = InvalidSurnameException.class)
    public void shouldReturn404GivenInvalidAppealNumberSurnameCombination() throws CcdException {
        when(tribunalsService.validateSurname(APPEAL_ID, SURNAME)).thenThrow(new InvalidSurnameException());

        controller.validateSurname(APPEAL_ID, SURNAME);
    }

    @Test
    public void testToReturnAppealForGivenAppealNumber() throws CcdException {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        //Given
        when(tribunalsService.findAppeal(APPEAL_ID)).thenReturn(node);

        //When
        ResponseEntity<String> receivedAppeal = controller.getAppeal(APPEAL_ID);

        //Then
        assertThat(receivedAppeal.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedAppeal.getBody(), equalTo(node.toString()));
    }

    @Test(expected = AppealNotFoundException.class)
    public void testToThrowAppealNotFoundExceptionIfAppealNotFound() throws CcdException {
        //Given
        String appealId = NOT_FOUND_APPEAL_ID;
        when(tribunalsService.findAppeal(appealId)).thenThrow(
            new AppealNotFoundException(appealId));

        //When
        controller.getAppeal(appealId);
    }

}