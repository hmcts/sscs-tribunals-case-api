package uk.gov.hmcts.sscs.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import uk.gov.hmcts.sscs.TribunalsCaseApiApplication;
import uk.gov.hmcts.sscs.domain.wrapper.SyaAppellant;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.sscs.service.TribunalsService;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TribunalsCaseApiApplication.class)
@WebAppConfiguration
public class AppealsControllerTest {

    public static final String APPEAL_ID = "appeal-id";
    public static final String TOKEN = "RTHWEGEG";
    public static final String SURNAME = "Smith";
    public static final String NOT_FOUND_APPEAL_ID = "not-found-appeal-id";

    private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));

    private SyaCaseWrapper syaCaseWrapper = new SyaCaseWrapper();

    private MockMvc mockMvc;

    @MockBean
    private TribunalsService tribunalsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private AppealsController controller;

    @Before
    public void setUp() {

        controller = new AppealsController(tribunalsService);

        SyaAppellant appellant = new SyaAppellant();
        appellant.setFirstName("Paul");
        appellant.setLastName("Gain");
        syaCaseWrapper.setAppellant(appellant);
    }

    @Test
    public void shouldCreateNewAppeals() throws Exception {
        when(tribunalsService.submitAppeal(syaCaseWrapper)).thenReturn(HttpStatus.CREATED);

        ResponseEntity<?> entity = controller.createAppeals(syaCaseWrapper);

        verify(tribunalsService).submitAppeal(syaCaseWrapper);

        assertEquals(HttpStatus.CREATED, entity.getStatusCode());
    }

    @Test(expected = AppealNotFoundException.class)
    public void testToThrowAppealNotFoundExceptionIfAppealNotFound() throws IOException {
        //Given
        String appealId = NOT_FOUND_APPEAL_ID;
        when(tribunalsService.generateResponse(appealId, null)).thenThrow(
                new AppealNotFoundException(appealId));

        //When
        controller.getAppeal(appealId, null);
    }

    @Test
    public void testToReturnAppealForGivenAppealNumber() throws Exception {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        //Given
        when(tribunalsService.generateResponse(APPEAL_ID, null)).thenReturn(node);

        //When
        ResponseEntity<String> receivedAppeal = controller.getAppeal(APPEAL_ID, null);

        //Then
        assertThat(receivedAppeal.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedAppeal.getBody(), equalTo(node.toString()));
    }

    @Test
    public void testToReturnNotFoundResponseCodeForRootContext() {
        //When
        ResponseEntity<?> responseEntity = controller.getRootContext();

        //Then
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

}