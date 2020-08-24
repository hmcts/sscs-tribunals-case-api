package uk.gov.hmcts.reform.sscs.controller;

import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class RootControllerTest {

    MockMvc mockMvc;
    RootController controller;

    @Before
    public void setup() {
        openMocks(this);
        controller = new RootController();
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void root() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }
}
