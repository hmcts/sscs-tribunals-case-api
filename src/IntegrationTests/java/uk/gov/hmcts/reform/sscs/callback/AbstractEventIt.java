package uk.gov.hmcts.reform.sscs.callback;

import static java.lang.Long.parseLong;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackDispatcher;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@TestPropertySource(locations = "classpath:config/application_it.properties")
public abstract class AbstractEventIt {

    protected static final String JURISDICTION = "Benefit";
    protected static final String AN_Test = "AN Test";

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    protected MockMvc mockMvc;

    @MockBean
    protected AuthorisationService authorisationService;

    @Autowired
    protected SscsCaseCallbackDeserializer deserializer;

    @Autowired
    protected PreSubmitCallbackDispatcher dispatcher;

    @Autowired
    protected ObjectMapper mapper;

    @Autowired
    protected WebApplicationContext context;

    public String json;

    void setup() throws IOException {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mapper.registerModule(new JavaTimeModule());
    }

    void setup(String jsonFile) throws IOException {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mapper.registerModule(new JavaTimeModule());
        json = getJson(jsonFile);
    }

    protected void setJsonAndReplace(String fileLocation, String replaceKey, String replaceValue) throws IOException {
        String result = getJson(fileLocation);
        json = result.replace(replaceKey, replaceValue);
    }

    protected void setJsonAndReplace(String fileLocation, List<String> replaceKeys, List<String> replaceValues) throws IOException {
        String result = getJson(fileLocation);
        for (int i = 0; i < replaceKeys.size(); i++) {
            result = result.replace(replaceKeys.get(i), replaceValues.get(i));
        }
        json = result;
    }

    protected void setJson(SscsCaseData sscsCaseData, EventType eventType) throws JsonProcessingException {
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(parseLong(sscsCaseData.getCcdCaseId()), JURISDICTION,
            sscsCaseData.getState(), sscsCaseData, LocalDateTime.now(), "Benefit");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(),
            eventType, false);
        json = mapper.writeValueAsString(callback);
    }

    protected String getJson(String fileLocation) throws IOException {
        String path = Objects.requireNonNull(getClass().getClassLoader()
            .getResource(fileLocation)).getFile();
        return FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
    }

    protected MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

    @NotNull
    protected PreSubmitCallbackResponse<SscsCaseData> assertResponseOkAndGetResult(CallbackType callbackType, String pageId) throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, callbackType, pageId));
        assertHttpStatus(response, HttpStatus.OK);
        return deserialize(response.getContentAsString());
    }

    @NotNull
    protected PreSubmitCallbackResponse<SscsCaseData> assertResponseOkAndGetResult(CallbackType callbackType) throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, callbackType));
        assertHttpStatus(response, HttpStatus.OK);
        return deserialize(response.getContentAsString());
    }

    public PreSubmitCallbackResponse<SscsCaseData> deserialize(String source) {
        try {
            return mapper.readValue(
                source,
                new TypeReference<>() {
                }
            );

        } catch (IOException e) {
            throw new IllegalArgumentException("Could not deserialize object", e);
        }
    }
}
