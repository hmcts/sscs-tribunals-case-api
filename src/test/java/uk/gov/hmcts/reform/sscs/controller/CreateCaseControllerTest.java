package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.SubmitAppealService;

public class CreateCaseControllerTest {

    private CcdService ccdService;
    private IdamService idamService;
    private SubmitAppealService submitAppealService;
    private MockMvc mockMvc;
    private CreateCaseController controller;

    @Before
    public void setUp() {
        ccdService = mock(CcdService.class);
        submitAppealService = mock(SubmitAppealService.class);
        idamService = mock(IdamService.class);
        controller = new CreateCaseController(submitAppealService,ccdService, idamService);
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    public void createTestCase() throws URISyntaxException {
        Long caseId = 123L;
        String caseRef = "someCaseRef";
        String someTyaValue = "someTyaValue";
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().id(caseId).data(SscsCaseData.builder()
                .caseReference(caseRef)
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder()
                                .tya(someTyaValue)
                                .build())
                        .build())
                .build()
        ).build();
        when(ccdService.createCase(any(SscsCaseData.class), eq(EventType.CREATE_TEST_CASE.getCcdType()), eq("SSCS - create test case event"), eq("Created SSCS"), any(IdamTokens.class))).thenReturn(sscsCaseDetails);

        ResponseEntity<Map<String, String>> createCaseResponse = controller.createCase("someEmail", "someMobile", "oral");

        assertThat(createCaseResponse.getStatusCode(), is(HttpStatus.CREATED));
        assertThat(createCaseResponse.getBody().get("id"), is(caseId.toString()));
        assertThat(createCaseResponse.getBody().get("case_reference"), is(caseRef));
        assertThat(createCaseResponse.getBody().get("appellant_tya"), is(someTyaValue));
    }

    @Test
    public void shouldReturnHttpStatusCode201ForTheSubmittedTestAppeal() throws Exception {
        when(submitAppealService.submitAppeal(any(SyaCaseWrapper.class), any(String.class))).thenReturn(1L);

        String json = getSyaCaseWrapperJson("json/sya.json");

        mockMvc.perform(post("/api/appeals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    public void twoCallsToRandomNinoAreDifferent() {
        String nino1 = controller.getRandomNino();
        String nino2 = controller.getRandomNino();
        assertThat(nino1, not(nino2));
    }

    @Test
    public void twoCallsToRandomMrnDateHaveAGoodChanceOfBeingDifferent() {
        LocalDate mrnDate1 = controller.getRandomMrnDate();
        LocalDate mrnDate2 = controller.getRandomMrnDate();
        if (mrnDate2.equals(mrnDate1)) {
            mrnDate2 = controller.getRandomMrnDate();
        }
        assertThat(mrnDate1, not(mrnDate2));
    }

    private String getSyaCaseWrapperJson(String resourcePath) throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        return String.join("\n", Files.readAllLines(Paths.get(Objects.requireNonNull(resource).toURI())));
    }
}
