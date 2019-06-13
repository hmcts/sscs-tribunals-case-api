package uk.gov.hmcts.reform.sscs.controller;

import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.mock;
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
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.EventService;
import uk.gov.hmcts.reform.sscs.service.InterlocService;


@RunWith(SpringRunner.class)
@WebMvcTest(EventController.class)
public class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("PMD.UnusedPrivateField")
    @MockBean
    private EventService eventService;

    @SuppressWarnings("PMD.UnusedPrivateField")
    @MockBean
    private AuthorisationService authorisationService;

    @MockBean
    private SscsCaseCallbackDeserializer deserializer;

    @MockBean
    private Callback<SscsCaseData> caseDataCallback;

    @MockBean
    private InterlocService interlocService;

    @MockBean
    private SscsCaseDataSerializer sscsCaseDataSerializer;

    @Test
    public void checkCreatePdfEvent() throws Exception {

        CaseDetails<SscsCaseData> caseDataCaseDetails = mock(CaseDetails.class);
        SscsCaseData sscsCaseData = mock(SscsCaseData.class);

        String path = getClass().getClassLoader().getResource("sya/allDetailsForGeneratePdf.json").getFile();
        String content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        when(deserializer.deserialize(content)).thenReturn(caseDataCallback);
        when(caseDataCallback.getCaseDetails()).thenReturn(caseDataCaseDetails);
        when(caseDataCaseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDataCallback.getEvent()).thenReturn(EventType.CREATE_APPEAL_PDF);
        mockMvc.perform(post("/send")
                .contentType(MediaType.APPLICATION_JSON)
                .header("ServiceAuthorization", "")
                .content(content)).andExpect(status().isOk());
    }

    @Test
    public void updateInterlocSecondaryState() throws Exception {
        String path = getClass().getClassLoader().getResource("sya/allDetailsForGeneratePdf.json").getFile();
        String content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();
        when(deserializer.deserialize(content)).thenReturn(new Callback<>(
                new CaseDetails<>(1234L, "SSCS", State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now()),
                Optional.empty(),
                INTERLOC_INFORMATION_RECEIVED));

        SscsCaseData updatedSscsCaseData = SscsCaseData.builder().interlocSecondaryState("new_state").build();
        when(interlocService.setInterlocSecondaryState(INTERLOC_INFORMATION_RECEIVED, sscsCaseData))
                .thenReturn(updatedSscsCaseData);

        when(sscsCaseDataSerializer.serialize(updatedSscsCaseData))
                .thenReturn(singletonMap("interlocSecondaryState", "new_state"));

        mockMvc.perform(post("/ccdAboutToSubmit")
                .contentType(MediaType.APPLICATION_JSON)
                .header("ServiceAuthorization", "")
                .content(content))
                .andExpect(status().isOk())
                .andExpect(content().json("{'data': {'interlocSecondaryState': 'new_state'}}"))
        ;
    }
}