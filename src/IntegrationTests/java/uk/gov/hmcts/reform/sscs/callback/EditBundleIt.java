package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.controller.CcdCallbackController;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(JUnitParamsRunner.class)
public class EditBundleIt extends AbstractEventIt {

    @MockBean
    private CcdService ccdService;

    @MockBean
    private IdamService idamService;

    @MockBean
    private RestTemplate restTemplate;

    @Mock
    private ResponseEntity responseEntity;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;


    @Before
    public void setup() throws IOException {
        CcdCallbackController controller = new CcdCallbackController(authorisationService, deserializer, dispatcher);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mapper.registerModule(new JavaTimeModule());
        json = getJson("callback/editBundleCallback.json");

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
    }

    @Test
    public void callToAboutToSubmitHandler_willCallExternalEditBundleService() throws Exception {
        SscsCaseData caseData = SscsCaseData.builder().build();

        when(restTemplate.exchange(eq("/api/stitch-ccd-bundles"), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);

        when(responseEntity.getBody()).thenReturn(new PreSubmitCallbackResponse<CaseData>(caseData));

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        verify(restTemplate).exchange(eq("/api/stitch-ccd-bundles"), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    public void callToAboutToSubmitHandlerWithNoBundleSelectedToAmend_willReturnWarningToCaseworker() throws Exception {
        json = getJson("callback/editBundleCallbackNoBundleToAmend.json");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertHttpStatus(response, HttpStatus.OK);

        String warning = result.getWarnings().stream()
                .findFirst()
                .orElse("");
        assertEquals("No bundle selected to be amended. The stitched PDF will not be updated. Are you sure you want to continue?", warning);

    }
}
