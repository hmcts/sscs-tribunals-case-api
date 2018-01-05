package uk.gov.hmcts.sscs.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

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
import uk.gov.hmcts.sscs.service.TribunalsService;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TribunalsCaseApiApplication.class)
@WebAppConfiguration
public class AppealsControllerTest {

    private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));

    private SyaCaseWrapper syaCaseWrapper = new SyaCaseWrapper();

    private MockMvc mockMvc;

    @MockBean
    private TribunalsService tribunalsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private AppealsController appealsController;

    @Before
    public void setUp() {
        appealsController = new AppealsController(tribunalsService);
        mockMvc = webAppContextSetup(webApplicationContext).build();

        SyaAppellant appellant = new SyaAppellant();
        appellant.setFirstName("Paul");
        appellant.setLastName("Gain");
        syaCaseWrapper.setAppellant(appellant);
    }

    @Test
    public void shouldCreateNewAppeals() throws Exception {
        when(tribunalsService.submitAppeal(syaCaseWrapper)).thenReturn(HttpStatus.CREATED);

        ResponseEntity<?> entity = appealsController.createApppeals(syaCaseWrapper);

        verify(tribunalsService).submitAppeal(syaCaseWrapper);

        assertEquals(HttpStatus.CREATED, entity.getStatusCode());
    }
}