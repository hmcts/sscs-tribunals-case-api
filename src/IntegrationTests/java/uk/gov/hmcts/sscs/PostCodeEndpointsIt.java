package uk.gov.hmcts.sscs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class PostCodeEndpointsIt {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldReturnRegionalCentreFromPostCode() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/regionalcentre/br2")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String result = mvcResult.getResponse().getContentAsString();

        assertEquals("{" + "\"regionalCentre\": \"Sutton\"" + "}", result);

        assertEquals(mvcResult.getResponse().getStatus(), 200);
    }

    @Test
    public void shouldReturnNotFoundFromPostCodeThatDoesNotExist() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/regionalcentre/aa1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

    }
}
