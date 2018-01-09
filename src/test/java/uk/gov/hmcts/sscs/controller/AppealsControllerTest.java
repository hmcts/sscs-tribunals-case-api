package uk.gov.hmcts.sscs.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.sscs.service.CcdService;
import uk.gov.hmcts.sscs.service.SubmitAppealService;



@RunWith(MockitoJUnitRunner.class)
public class AppealsControllerTest {

    public static final String APPEAL_ID = "appeal-id";
    public static final String NOT_FOUND_APPEAL_ID = "not-found-appeal-id";

    private MockMvc mockMvc;

    @Mock
    private CcdService ccdService;

    @Mock
    private SubmitAppealService submitAppealService;


    private AppealsController controller;

    @Before
    public void setUp() {
        controller = new AppealsController(ccdService, submitAppealService);
        mockMvc = standaloneSetup(controller).build();
    }


    @Test
    public void shouldReturnHttpStatusCode201ForTheSubmittedAppeal() throws Exception {
        doNothing().when(submitAppealService).submitAppeal(any(Map.class),any(String.class));
        when(ccdService.submitAppeal(any(SyaCaseWrapper.class))).thenReturn(HttpStatus.OK);

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

    @Test(expected = AppealNotFoundException.class)
    public void testToThrowAppealNotFoundExceptionIfAppealNotFound() throws IOException {
        //Given
        String appealId = NOT_FOUND_APPEAL_ID;
        when(ccdService.generateResponse(appealId, null))
                .thenThrow(new AppealNotFoundException(appealId));

        //When
        controller.getAppeal(appealId, null);
    }

    @Test
    public void testToReturnAppealForGivenAppealNumber() throws Exception {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        //Given
        when(ccdService.generateResponse(APPEAL_ID, null)).thenReturn(node);

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




    private String getSyaCaseWrapperJson() {
        return  "{\n"
                + "  \"benefitType\": \"Personal Independence Payment (PIP)\",\n"
                + "  \"postCodeCheck\": \"B14 7SP\",\n"
                + "  \"mrn\": {\n"
                + "    \"dwpIssuingOffice\": \"8\",\n"
                + "    \"date\": \"01-12-2017\"\n"
                + "  },\n"
                + "  \"isAppointee\": false,\n"
                +  "  \"appellant\": {\n"
                +  "    \"title\": \"Mr\",\n"
                + "    \"firstName\": \"Harry\",\n"
                + "    \"lastName\": \"Potter\",\n"
                +  "    \"dob\": \"01-05-1990\",\n"
                +  "    \"nino\": \"AB877533C\",\n"
                +   "    \"contactDetails\": {\n"
                +   "      \"addressLine1\": \"123 Hairy Lane\",\n"
                +   "      \"addressLine2\": \"Off Hairy Park\",\n"
                +   "      \"townCity\": \"Hairyfield\",\n"
                +   "      \"county\": \"Kent\",\n"
                +  "      \"postCode\": \"TN32 6PL\",\n"
                + "      \"phoneNumber\": \"07411222222\",\n"
                + "      \"emailAddress\": \"harry.potter@wizards.com\"\n"
                + "    }\n"
                + "  },\n"
                + "  \"smsNotify\": {\n"
                + "    \"wantsSMSNotifications\": true,\n"
                + "    \"useSameNumber\": true,\n"
                + "    \"smsNumber\": \"07411222222\"\n"
                + "  },\n"
                + "  \"hasRepresentative\": true,\n"
                +  "  \"representative\": {\n"
                +  "    \"firstName\": \"Hermione\",\n"
                + "    \"lastName\": \"Granger\",\n"
                + "    \"organisation\": \"Harry Potter Entertainments Ltd\",\n"
                +  "    \"contactDetails\": {\n"
                +  "      \"addressLine1\": \"991 Harlow Road\",\n"
                +  "      \"addressLine2\": \"Off Jam Park\",\n"
                +  "      \"townCity\": \"Tunbridge Wells\",\n"
                +  "      \"county\": \"Kent\",\n"
                +  "      \"postCode\": \"TN32 6PL\",\n"
                +  "      \"phoneNumber\": \"07411666666\",\n"
                +  "      \"emailAddress\": \"hermione.granger@wizards.com\"\n"
                +  "    }\n"
                +  "  },\n"
                +  "  \"reasonsForAppealing\": {\n"
                +  "    \"reasons\": \"Here are my reasons for appealing...\",\n"
                +  "    \"otherReasons\": \"Nope, not today anyway!\"\n"
                +  "  },\n"
                +  "  \"emailAddress\": \"harry.potter@wizards.com\",\n"
                +  "  \"hearing\": {\n"
                +  "    \"wantsToAttend\": true,\n"
                +  "    \"wantsSupport\": true,\n"
                +  "    \"arrangements\": {\n"
                +   "      \"languageInterpreter\": true,\n"
                +   "      \"signLanguageInterpreter\": true,\n"
                +   "      \"hearingLoop\": true,\n"
                +   "      \"disabledAccess\" : true\n"
                +   "    },\n"
                +    "    \"anythingElse\": \"Nothing else today.\",\n"
                +    "    \"scheduleHearing\": true,\n"
                +   "    \"datesCantAttend\": [\n"
                +   "      \"25/01/1972\"\n"
                +    "    ]\n"
                +     "  },\n"
                +  "  \"statementOfTruth\": true\n"
                +  "}";
    }

}