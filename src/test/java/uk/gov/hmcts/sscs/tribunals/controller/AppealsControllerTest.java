package uk.gov.hmcts.sscs.tribunals.controller;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.sscs.TribunalsCaseApiApplication;
import uk.gov.hmcts.sscs.tribunals.service.CcdService;



@RunWith(SpringRunner.class)
@SpringBootTest(classes = TribunalsCaseApiApplication.class)
@WebAppConfiguration
public class AppealsControllerTest {


    private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));

    private final String appealJson = "{\n"
        +    "  BenefitsType_type: \"ESA\",\n"
        +    "  MRNDate_day: \"01\",\n"
        +    "  MRNDate_month: \"09\",\n"
        +    "  MRNDate_year: \"2017\",\n"
        +    "  Appointee_isappointee: \"?\",\n"
        +    "  AppellantDetails_firstName: \"Paul\",\n"
        +    "  AppellantDetails_lastName: \"Gain\",\n"
        +    "  AppellantDetails_nationalInsuranceNumber: \"NX123456789\",\n"
        +    "  AppellantDetails_addressLine1: \"33 Wharf Road\",\n"
        +    "  AppellantDetails_addressLine2: \"\",\n"
        +    "  AppellantDetails_townCity: \"Brentwood\",\n"
        +    "  AppellantDetails_postcode: \"CM15 6LG\",\n"
        +    "  AppellantDetails_phoneNumber: \"07455737993\",\n"
        +    "  AppellantDetails_emailAddress: \"mickeymouse@disney.com\",\n"
        +    "}";

    private MockMvc mockMvc;


    @Mock
    private CcdService ccdService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private AppealsController appealsController;

    @Before
    public void setUp() throws Exception {
        appealsController = new AppealsController(ccdService);
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }


    @Test
    public void shouldCreateNewAppeals() throws Exception {
        doNothing().when(ccdService).saveCase(appealJson);

        this.mockMvc.perform(post("/appeals").content(appealJson).contentType(contentType))
                .andExpect(status().isCreated());
    }


}