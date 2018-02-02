package uk.gov.hmcts.sscs.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.sscs.service.SubmitAppealService;
import uk.gov.hmcts.sscs.service.TribunalsService;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class SyaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TribunalsService tribunalsService;

    @Mock
    private SubmitAppealService submitAppealService;


    @Before
    public void setUp() {
        SyaController controller = new SyaController(tribunalsService, submitAppealService);
        mockMvc = standaloneSetup(controller).build();
    }


    @Test
    public void shouldReturnHttpStatusCode201ForTheSubmittedAppeal() throws Exception {
        doNothing().when(submitAppealService).submitAppeal(any(Map.class),any(String.class));
        when(tribunalsService.submitAppeal(any(SyaCaseWrapper.class))).thenReturn(HttpStatus.OK);

        String json = getSyaCaseWrapperJson();

        mockMvc.perform(post("/appeals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldHandleErrorWhileSubmitAppeal() throws Exception {
        doThrow(new PdfGenerationException("malformed html template"))
                .when(submitAppealService).submitAppeal(any(Map.class),any(String.class));
        String json = getSyaCaseWrapperJson();

        mockMvc.perform(post("/appeals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .accept(APPLICATION_PDF_VALUE))
                .andExpect(status().is5xxServerError());
    }

    private String getSyaCaseWrapperJson() throws IOException, URISyntaxException {

        URL resource = getClass().getClassLoader().getResource("json/sya.json");
        return Files.readAllLines(Paths.get(resource.toURI())).stream().collect(Collectors.joining("\n"));
    }

}