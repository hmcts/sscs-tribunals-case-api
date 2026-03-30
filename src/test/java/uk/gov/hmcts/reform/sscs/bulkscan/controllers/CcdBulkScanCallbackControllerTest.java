package uk.gov.hmcts.reform.sscs.bulkscan.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.hmcts.reform.sscs.bulkscan.common.TestHelper.TEST_SERVICE_AUTH_TOKEN;
import static uk.gov.hmcts.reform.sscs.bulkscan.common.TestHelper.TEST_USER_AUTH_TOKEN;
import static uk.gov.hmcts.reform.sscs.bulkscan.common.TestHelper.TEST_USER_ID;
import static uk.gov.hmcts.reform.sscs.bulkscan.common.TestHelper.exceptionRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.handlers.CcdCallbackHandler;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptionhandlers.ResponseExceptionHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@ExtendWith(MockitoExtension.class)
public class CcdBulkScanCallbackControllerTest {

    public static final long ID = 1234L;

    private MockMvc mockMvc;

    @Mock
    private AuthorisationService authorisationService;

    @Mock
    private SscsCaseCallbackDeserializer deserializer;

    @Mock
    private CcdCallbackHandler ccdCallbackHandler;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private Callback<SscsCaseData> callback;

    @BeforeEach
    public void setUp() {
        CcdBulkScanCallbackController controller =
                new CcdBulkScanCallbackController(authorisationService, deserializer, ccdCallbackHandler);
        mockMvc = standaloneSetup(controller)
            .setControllerAdvice(new ResponseExceptionHandler())
            .setMessageConverters(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(),
                new ResourceHttpMessageConverter(false), new SourceHttpMessageConverter<>(),
                new AllEncompassingFormHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter())
            .build();
    }

    @Test
    public void should_successfully_handle_callback_and_return_validate_response() throws Exception {
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(deserializer.deserialize(anyString())).willReturn(callback);
        given(ccdCallbackHandler.handleValidationAndUpdate(
            ArgumentMatchers.any(),
            eq(IdamTokens.builder().idamOauth2Token(TEST_USER_AUTH_TOKEN)
                    .serviceAuthorization(TEST_SERVICE_AUTH_TOKEN).userId(TEST_USER_ID).build()))
        ).willReturn(new PreSubmitCallbackResponse<>(SscsCaseData.builder().state(State.WITH_DWP).build()));

        doNothing().when(authorisationService).assertIsAllowedToHandleCallback(TEST_SERVICE_AUTH_TOKEN);

        mockMvc.perform(post("/validate-record")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", TEST_USER_AUTH_TOKEN)
                .header("ServiceAuthorization", TEST_SERVICE_AUTH_TOKEN)
                .header("user-id", TEST_USER_ID)
                .content(exceptionRecord("validation/validate-appeal-created-case-request.json")))
            .andExpect(jsonPath("$['data'].state", is("withDwp")));
    }

    @Test
    public void should_throw_exception_when_handler_fails() throws Exception {
        given(callback.getCaseDetails()).willReturn(caseDetails);
        given(deserializer.deserialize(anyString())).willReturn(callback);
        given(ccdCallbackHandler.handleValidationAndUpdate(
            ArgumentMatchers.any(),
            eq(IdamTokens.builder().idamOauth2Token(TEST_USER_AUTH_TOKEN)
                    .serviceAuthorization(TEST_SERVICE_AUTH_TOKEN).userId(TEST_USER_ID).build()))
        ).willThrow(RuntimeException.class);

        doNothing().when(authorisationService).assertIsAllowedToHandleCallback(TEST_SERVICE_AUTH_TOKEN);

        MvcResult result =  mockMvc.perform(post("/validate-record")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", TEST_USER_AUTH_TOKEN)
                .header("ServiceAuthorization", TEST_SERVICE_AUTH_TOKEN)
                .header("user-id", TEST_USER_ID)
                .content(exceptionRecord("validation/validate-appeal-created-case-request.json")))
            .andReturn();

        String content = result.getResponse().getContentAsString();

        assertThat(content, containsString("There was an unknown error when processing the case. "
                + "If the error persists, please contact the Bulk Scan development team"));
    }
}
