package uk.gov.hmcts.reform.sscs.event;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import junitparams.JUnitParamsRunner;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackDispatcher;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.controller.CcdCallbackController;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
public class CcdCallbackEndpointIt {
    // Below rules are needed to use the junitParamsRunner together with SpringRunner
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private MockMvc mockMvc;

    @MockBean
    private AuthorisationService authorisationService;

    @Autowired
    private CcdService ccdService;

    @Autowired
    private SscsCaseCallbackDeserializer deserializer;

    @MockBean
    private IdamService idamService;

    private String json;

    @Autowired
    private PreSubmitCallbackDispatcher dispatcher;

    private ObjectMapper mapper;

    @Before
    public void setup() throws Exception {
        CcdCallbackController controller = new CcdCallbackController(authorisationService, deserializer, dispatcher);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        Jackson2ObjectMapperBuilder objectMapperBuilder =
                new Jackson2ObjectMapperBuilder()
                        .featuresToEnable(READ_ENUMS_USING_TO_STRING)
                        .featuresToEnable(READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                        .featuresToEnable(WRITE_ENUMS_USING_TO_STRING)
                        .serializationInclusion(JsonInclude.Include.NON_ABSENT);

        mapper = objectMapperBuilder.createXmlMapper(false).build();
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void shouldActionFurtherEvidenceEventCallback() throws Exception {
        String path = getClass().getClassLoader().getResource("callback/actionFurtherEvidenceCallback.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        List<SscsDocument> documentList = result.getData().getSscsDocument();
        assertEquals(1, documentList.size());
        assertNull(result.getData().getScannedDocuments());
        assertEquals("appellantEvidence", documentList.get(0).getValue().getDocumentType());
        assertEquals("3", documentList.get(0).getValue().getControlNumber());
        assertEquals("scanned.pdf", documentList.get(0).getValue().getDocumentFileName());
        assertEquals("http://localhost:4603/documents/f812db06-fd5a-476d-a603-bee44b2ecd49", documentList.get(0).getValue().getDocumentLink().getDocumentUrl());
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

    public PreSubmitCallbackResponse deserialize(String source) {
        try {
            PreSubmitCallbackResponse callback = mapper.readValue(
                source,
                new TypeReference<PreSubmitCallbackResponse<SscsCaseData>>() {
                }
            );

            return callback;

        } catch (IOException e) {
            throw new IllegalArgumentException("Could not deserialize object", e);
        }
    }


}
