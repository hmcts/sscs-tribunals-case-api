package uk.gov.hmcts.reform.sscs.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CITIZEN_REQUEST_HEARING_RECORDING;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.HEARING_RECORDING_CCD;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.HEARING_RECORDING_MYA;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.service.OnlineHearingService;


@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class CitizenRequestControllerIt {

    private static final String CASE_ID = "1625080769409918";
    private static final String AUTHORIZATION = "Bearer 1203912-39012-=391231";
    private static final String E_MAIL = "sscs-citizen2@hmcts.net";

    @MockBean
    private OnlineHearingService onlineHearingService;
    @MockBean
    private CcdService ccdService;
    @MockBean
    private IdamService idamService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    protected ObjectMapper mapper;

    private SscsCaseData caseData;
    private IdamTokens idamTokens;

    @Before
    public void setUp() {
        idamTokens = IdamTokens.builder().build();
        caseData = HEARING_RECORDING_CCD.getDeserializeMessage();
        when(onlineHearingService.getCcdCaseByIdentifier(CASE_ID))
                .thenReturn(Optional.ofNullable(SscsCaseDetails.builder().id(Long.parseLong(CASE_ID)).data(caseData).build()));
        UserDetails user = UserDetails.builder().email(E_MAIL).build();
        when(idamService.getUserDetails(AUTHORIZATION)).thenReturn(user);
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    public void testGetHearingRecording() throws Exception {
        mockMvc.perform(get("/api/request/" + CASE_ID + "/hearingrecording")
                .header("Authorization", AUTHORIZATION))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(HEARING_RECORDING_MYA.getSerializedMessage()));

    }

    @Test
    public void testRequestHearingRecording() throws Exception {
        mockMvc.perform(post("/api/request/" + CASE_ID + "/recordingrequest")
                        .header("Authorization", AUTHORIZATION)
                        .param("hearingIds","1290312038013"))
                .andDo(print())
                .andExpect(status().isNoContent());

        assertEquals(caseData.getSscsHearingRecordingCaseData().getRequestedHearings().size(), 2);
        verify(ccdService).updateCase(
                argThat(argument -> argument.getSscsHearingRecordingCaseData().getRequestedHearings().size() == 2),
                eq(Long.parseLong(CASE_ID)),
                eq(CITIZEN_REQUEST_HEARING_RECORDING.getCcdType()),
                eq("SSCS - hearing recording request from MYA"),
                eq("Requested hearing recordings"),
                eq(idamTokens)
        );
    }
}
