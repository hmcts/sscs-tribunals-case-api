package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.OriginalSenderItemList.*;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackDispatcher;
import uk.gov.hmcts.reform.sscs.controller.CcdCallbackController;
import uk.gov.hmcts.reform.sscs.idam.IdamApiClient;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@SpringBootTest
@AutoConfigureMockMvc
public class ReissueFurtherEvidenceIt {

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private MockMvc mockMvc;

    @MockBean
    private AuthorisationService authorisationService;

    @Autowired
    private SscsCaseCallbackDeserializer deserializer;

    @Autowired
    private PreSubmitCallbackDispatcher dispatcher;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockBean
    private IdamApiClient idamApiClient;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    private String json;

    private String midEventPartialJson;

    private String aboutToSubmitPartialJson;

    @Before
    public void setup() throws IOException {
        CcdCallbackController controller = new CcdCallbackController(authorisationService, deserializer, dispatcher);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mapper.registerModule(new JavaTimeModule());
        String path = Objects.requireNonNull(getClass().getClassLoader()
                .getResource("callback/reissueFurtherEvidenceCallback.json")).getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        String path2 = Objects.requireNonNull(getClass().getClassLoader()
                .getResource("callback/reissueFurtherEvidenceDocumentPartial.json")).getFile();
        midEventPartialJson = FileUtils.readFileToString(new File(path2), StandardCharsets.UTF_8.name());

        String path3 = Objects.requireNonNull(getClass().getClassLoader()
                .getResource("callback/reissueFurtherEvidenceOriginalSenderPartial.json")).getFile();
        aboutToSubmitPartialJson = FileUtils.readFileToString(new File(path3), StandardCharsets.UTF_8.name());

    }


    @Test
    public void callAboutToStartHandler_willPopulateDocumentsToReissue() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));

        assertHttpStatus(response, HttpStatus.OK);

        DynamicListItem listItem1 = new DynamicListItem("http://www.bbc.com", "11111.pdf -  Appellant evidence");
        DynamicListItem listItem2 = new DynamicListItem("http://www.itv.com", "22222.pdf -  Representative evidence");
        DynamicList expected = new DynamicList(listItem1, Arrays.asList(listItem1, listItem2));

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());
        assertEquals(expected, result.getData().getReissueFurtherEvidenceDocument());
    }

    @Test
    public void callToMidEventHandler_willPopulateOriginalSender() throws Exception {
        json = json.replaceFirst("\"reissueFurtherEvidenceDocument\": \\{\\}", "\"reissueFurtherEvidenceDocument\": " + midEventPartialJson);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        DynamicListItem appellantListItem = new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel());
        DynamicListItem repListItem = new DynamicListItem(REPRESENTATIVE.getCode(), REPRESENTATIVE.getLabel());
        DynamicListItem dwpListItem = new DynamicListItem(DWP.getCode(), DWP.getLabel());

        DynamicList expected = new DynamicList(repListItem, Arrays.asList(appellantListItem, repListItem, dwpListItem));
        assertEquals(expected, result.getData().getOriginalSender());
    }

    @Test
    public void callToAboutToSubmitHandler_willResetEvidenceHandledAndUpdateDocumentType() throws Exception {
        json = json.replaceFirst("\"reissueFurtherEvidenceDocument\": \\{\\}", "\"reissueFurtherEvidenceDocument\": " + midEventPartialJson);
        json = json.replaceFirst("\"originalSender\": \\{\\}", "\"originalSender\": " + aboutToSubmitPartialJson);
        json = json.replaceFirst("\"resendToAppellant\": \"NO\"", "\"resendToAppellant\": \"YES\"");
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        assertEquals(Collections.EMPTY_SET, result.getErrors());

        SscsDocument document1 = result.getData().getSscsDocument().get(0);
        assertEquals("Yes", document1.getValue().getEvidenceIssued());
        assertEquals("appellantEvidence", document1.getValue().getDocumentType());

        SscsDocument document2 = result.getData().getSscsDocument().get(1);
        assertEquals("No", document2.getValue().getEvidenceIssued());
        assertEquals("appellantEvidence", document2.getValue().getDocumentType());
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

    private PreSubmitCallbackResponse<SscsCaseData> deserialize(String source) {
        try {
            return mapper.readValue(
                    source,
                    new TypeReference<PreSubmitCallbackResponse<SscsCaseData>>() {
                    }
            );

        } catch (IOException e) {
            throw new IllegalArgumentException("Could not deserialize object", e);
        }
    }

}
