package uk.gov.hmcts.reform.sscs.controller;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INTERLOC_INFORMATION_RECEIVED;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackDispatcher;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@RunWith(SpringRunner.class)
@WebMvcTest(CcdCallbackController.class)
public class CcdCallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("PMD.UnusedPrivateField")
    @MockBean
    private AuthorisationService authorisationService;

    @MockBean
    private SscsCaseCallbackDeserializer deserializer;

    @MockBean
    private Callback<SscsCaseData> caseDataCallback;

    @MockBean
    private PreSubmitCallbackDispatcher dispatcher;


    @Test
    public void handleCcdAboutToSubmitCallbackAndUpdateCaseData() throws Exception {
        String path = getClass().getClassLoader().getResource("sya/allDetailsForGeneratePdf.json").getFile();
        String content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();
        when(deserializer.deserialize(content)).thenReturn(new Callback<>(
                new CaseDetails<>(1234L, "SSCS", State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now()),
                Optional.empty(),
                INTERLOC_INFORMATION_RECEIVED));

        PreSubmitCallbackResponse response = new PreSubmitCallbackResponse(SscsCaseData.builder().interlocReviewState("new_state").build());
        when(dispatcher.handle(any()))
                .thenReturn(response);

        mockMvc.perform(post("/ccdAboutToSubmit")
                .contentType(MediaType.APPLICATION_JSON)
                .header("ServiceAuthorization", "")
                .content(content))
                .andExpect(status().isOk())
                .andExpect(content().json("{'data': {'interlocReviewState': 'new_state'}}"));
    }

}