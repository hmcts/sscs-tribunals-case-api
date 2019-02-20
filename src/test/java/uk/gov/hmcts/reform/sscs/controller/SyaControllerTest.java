package uk.gov.hmcts.reform.sscs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
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
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.service.SubmitAppealService;

public class SyaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SubmitAppealService submitAppealService;


    @Before
    public void setUp() {
        initMocks(this);
        SyaController controller = new SyaController(submitAppealService);
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    public void shouldReturnHttpStatusCode201ForTheSubmittedAppeal() throws Exception {
        when(submitAppealService.submitAppeal(any(SyaCaseWrapper.class))).thenReturn(1L);

        String json = getSyaCaseWrapperJson();

        mockMvc.perform(post("/appeals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    public void shouldHandleErrorWhileSubmitAppeal() throws Exception {
        doThrow(new PdfGenerationException("malformed html template", new Exception()))
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