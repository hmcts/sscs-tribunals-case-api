package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_SEND_TO_INTERLOC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.controller.CcdCallbackController;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@SpringBootTest
@AutoConfigureMockMvc
public class HmctsResponseReviewedIt extends AbstractEventIt {

    @MockBean
    private UpdateCcdCaseService updateCcdCaseService;

    @MockBean
    private IdamService idamService;

    @Before
    public void setup() throws IOException {
        CcdCallbackController controller = new CcdCallbackController(authorisationService, deserializer, dispatcher);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mapper.findAndRegisterModules();
        json = getJson("callback/hmctsResponseReviewedCallback.json");

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
    }

    @Test
    public void callAboutToStartHandler_willPopulateDropdowns() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));

        assertHttpStatus(response, HttpStatus.OK);

        DynamicListItem listItem = new DynamicListItem("DWP PIP (2)", "DWP PIP (2)");

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());
        assertEquals(11, result.getData().getDwpPresentingOffice().getListItems().size());
        assertEquals(listItem, result.getData().getDwpPresentingOffice().getValue());
        assertEquals(11, result.getData().getDwpOriginatingOffice().getListItems().size());
        assertEquals(listItem, result.getData().getDwpOriginatingOffice().getValue());
    }

    @Test
    public void callToAboutToSubmitHandler_willSetCaseCode() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());
        assertEquals("002CC", result.getData().getCaseCode());
        assertEquals(LocalDate.now().toString(), result.getData().getDwpResponseDate());
    }


    @Test
    public void callToSubmittedHandler_willTriggerValidSendToInterlocEvent() throws Exception {
        json = json.replaceAll("IS_INTERLOC_REQUIRED_VALUE", YES.getValue());

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdSubmittedEvent"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());
        verify(updateCcdCaseService).updateCaseV2(
                eq(12345656789L),
                eq(VALID_SEND_TO_INTERLOC.getCcdType()),
                any(String.class),
                any(String.class),
                any(IdamTokens.class),
                any(Consumer.class));
    }

    @Test
    public void callToSubmittedHandler_willTriggerReadyToListEvent() throws Exception {
        json = json.replaceAll("IS_INTERLOC_REQUIRED_VALUE", NO.getValue());

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdSubmittedEvent"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());
        verify(updateCcdCaseService).updateCaseV2(
                eq(12345656789L),
                eq(READY_TO_LIST.getCcdType()),
                any(String.class),
                any(String.class),
                any(IdamTokens.class),
                any(Consumer.class));
    }
}
