package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.reform.sscs.model.tya.SurnameResponse;
import uk.gov.hmcts.reform.sscs.service.TribunalsService;


public class TyaControllerTest {

    private static final String APPEAL_ID = "appeal-id";
    private static final Long CASE_ID = 123456789L;
    private static final String SURNAME = "surname";
    private static final String NOT_FOUND_APPEAL_ID = "not-found-appeal-id";
    public static final String CCD_CASE_ID = "ccd-case-id";

    @Mock
    private TribunalsService tribunalsService;

    private TyaController controller;

    @Before
    public void setUp() {
        openMocks(this);
        controller = new TyaController(tribunalsService);
    }

    @Test
    public void shouldReturnOkGivenValidAppealNumberSurnameCombination() throws CcdException {
        when(tribunalsService.validateSurname(APPEAL_ID, SURNAME)).thenReturn(Optional.of(new SurnameResponse(CCD_CASE_ID, APPEAL_ID, SURNAME)));

        controller.validateSurname(APPEAL_ID, SURNAME);
    }

    @Test
    public void shouldReturn404GivenInvalidAppealNumberSurnameCombination() throws CcdException {
        when(tribunalsService.validateSurname(APPEAL_ID, SURNAME)).thenReturn(Optional.empty());

        ResponseEntity<SurnameResponse> receivedAppeal = controller.validateSurname(APPEAL_ID, SURNAME);
        assertThat(receivedAppeal.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
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
        String appealId = NOT_FOUND_APPEAL_ID;
        when(tribunalsService.findAppeal(appealId)).thenThrow(
            new AppealNotFoundException(appealId));

        //When
        controller.getAppeal(appealId);
    }

}
