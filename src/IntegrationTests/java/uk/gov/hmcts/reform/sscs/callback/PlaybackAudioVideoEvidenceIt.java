package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.controller.CcdCallbackController;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(JUnitParamsRunner.class)
public class PlaybackAudioVideoEvidenceIt extends AbstractEventIt {

    @MockBean
    private CcdService ccdService;

    @MockBean
    private IdamService idamService;

    @Before
    public void setup() throws IOException {
        CcdCallbackController controller = new CcdCallbackController(authorisationService, deserializer, dispatcher);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mapper.registerModule(new JavaTimeModule());
        json = getJson("callback/playbackAudioVideoEvidenceCallback.json");

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
    }

    @Test
    public void callAboutToStartHandler_willPopulateDropdowns() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));

        assertHttpStatus(response, HttpStatus.OK);

        DynamicListItem listItem = new DynamicListItem("http://www.myAudio.mp3", "myAudio.mp3");

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());
        assertEquals(2, result.getData().getSelectedAudioVideoEvidence().getListItems().size());
        assertEquals(listItem, result.getData().getSelectedAudioVideoEvidence().getValue());

    }

    @Test
    public void callMidEventHandler_willPopulateSelectedOption() throws Exception {
        json = getJson("callback/playbackAudioVideoEvidenceMideventCallback.json");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        AudioVideoEvidenceDetails expectedAudioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder()
                .partyUploaded(UploadParty.DWP)
                .documentType(DocumentType.VIDEO_DOCUMENT.getLabel())
                .dateAdded(LocalDate.of(2021, 5, 10))
                .build();

        assertEquals(expectedAudioVideoEvidenceDetails, result.getData().getSelectedAudioVideoEvidenceDetails());
        assertTrue(result.getData().getTempMediaUrl().contains("myVideo.mp4"));
    }

}
