package uk.gov.hmcts.sscs.controller;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.sscs.service.SubmitAppealService;

@RunWith(MockitoJUnitRunner.class)
public class SyaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SubmitAppealService submitAppealService;


    @Before
    public void setUp() {
        SyaController controller = new SyaController(submitAppealService);
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    public void shouldReturnHttpStatusCode201ForTheSubmittedAppeal() throws Exception {
        doNothing().when(submitAppealService).submitAppeal(any(SyaCaseWrapper.class));

        String json = getSyaCaseWrapperJson();

        mockMvc.perform(post("/appeals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    public void shouldHandleErrorWhileSubmitAppeal() throws Exception {
        doThrow(new PdfGenerationException(new Exception("malformed html template")))
                .when(submitAppealService).submitAppeal(any(SyaCaseWrapper.class));
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