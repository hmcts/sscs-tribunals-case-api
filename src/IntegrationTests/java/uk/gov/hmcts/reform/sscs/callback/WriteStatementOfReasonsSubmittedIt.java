package uk.gov.hmcts.reform.sscs.callback;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.Collections;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdCallbackMap;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentStaging;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.controller.CcdCallbackController;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@SpringBootTest(properties = {
    "feature.postHearings.enabled=true",
    "feature.handle-ccd-callbackMap-v2.enabled=true"
})
@AutoConfigureMockMvc
public class WriteStatementOfReasonsSubmittedIt  extends AbstractEventIt {

    @SpyBean
    private CcdCallbackMapService ccdCallbackMapService;

    @SpyBean
    private SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer;

    @MockBean
    private IdamService idamService;

    @MockBean
    private CcdClient ccdClient;

    @MockBean
    private SscsCcdConvertService sscsCcdConvertService;

    @BeforeEach
    public void setup() throws IOException {
        CcdCallbackController controller = new CcdCallbackController(authorisationService, deserializer, dispatcher);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mapper.registerModule(new JavaTimeModule());
        json = getJson("callback/postHearingRequest.json");
        json = json.replaceFirst("invoking_event", "sORWrite");
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
    }

    @Test
    public void callToSubmittedHandler_willTriggerWriteStatementOfReasonsSubmittedHandler() throws Exception {
        CaseDetails caseDetails = CaseDetails.builder().data(Collections.EMPTY_MAP).build();

        Callback<SscsCaseData> callback = sscsCaseCallbackDeserializer.deserialize(json);
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().id(123L).data(caseData).build();
        StartEventResponse startEventResponse = StartEventResponse.builder().caseDetails(caseDetails).build();

        when(ccdClient.startEvent(any(), anyLong(), anyString()))
                .thenReturn(startEventResponse);
        when(ccdClient.submitEventForCaseworker(any(), any(), any()))
                .thenReturn(caseDetails);
        when(sscsCcdConvertService.getCaseData(anyMap()))
                .thenReturn(sscsCaseDetails.getData());

        when(sscsCcdConvertService.getCaseDataContent(
                eq(sscsCaseDetails.getData()),
                any(),
                anyString(),
                anyString()))
                .thenReturn(CaseDataContent.builder()
                        .data(sscsCaseDetails.getData())
                        .build());
        when(sscsCcdConvertService.getCaseDetails(caseDetails))
                .thenReturn(sscsCaseDetails);

        assertThat(sscsCaseDetails.getData().getPostHearing().getSetAside())
                .isNotNull();
        assertThat(sscsCaseDetails.getData().getDocumentStaging().getPreviewDocument())
                .isNotNull();

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdSubmittedEvent"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        verify(ccdCallbackMapService).handleCcdCallbackMapV2(isA(CcdCallbackMap.class), anyLong(), any(Consumer.class));

        assertThat(result.getErrors())
                .isEmpty();
        assertThat(result.getData())
                .isNotNull();
        assertThat(result.getData().getPostHearing())
                .isEqualTo(PostHearing.builder().build());
        assertThat(result.getData().getDocumentGeneration())
                .isEqualTo(DocumentGeneration.builder().build());
        assertThat(result.getData().getDocumentStaging())
                .isEqualTo(DocumentStaging.builder().build());
    }
}
