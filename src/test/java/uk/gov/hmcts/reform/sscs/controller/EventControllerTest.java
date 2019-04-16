package uk.gov.hmcts.reform.sscs.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.EventService;


@RunWith(SpringRunner.class)
@WebMvcTest(EventController.class)
public class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private AuthorisationService authorisationService;

    @MockBean
    private SscsCaseCallbackDeserializer deserializer;

    @MockBean
    private Callback<SscsCaseData> caseDataCallback;

    @Test
    public void checkCreatePdfEvent() throws Exception {

        CaseDetails<SscsCaseData> caseDataCaseDetails = mock(CaseDetails.class);
        SscsCaseData sscsCaseData = mock(SscsCaseData.class);

        String content = "{\n"
                + "  \"case_id\": \"1546942528346226\",\n"
                + "  \"online_hearing_id\":\"13f8480c-fca4-4549-b4e4-17bef753d3ef\",\n"
                + "  \"event_type\":\"createAppealPDF\",\n"
                + "  \"expiry_date\":\"2018-08-12T23:59:59Z\",\n"
                + "  \"reason\":\"foo\"\n"
                + "}";

        when(deserializer.deserialize(content)).thenReturn(caseDataCallback);
        when(caseDataCallback.getCaseDetails()).thenReturn(caseDataCaseDetails);
        when(caseDataCaseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDataCallback.getEvent()).thenReturn(EventType.CREATE_APPEAL_PDF);
        mockMvc.perform(post("/send")
                .contentType(MediaType.APPLICATION_JSON)
                .header("ServiceAuthorization", "")
                .content(content)).andExpect(status().isOk());
    }

}