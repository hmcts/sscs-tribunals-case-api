package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.DIRECTION_ACTION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_INFORMATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.servlet.http.HttpServletResponse;
import junitparams.JUnitParamsRunner;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.ProcessedAction;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.controller.CcdCallbackController;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(JUnitParamsRunner.class)
public class ProcessAudioVideoIt extends AbstractEventIt {

    @MockBean
    private CcdService ccdService;

    @MockBean
    private IdamService idamService;

    @MockBean
    private PdfStoreService pdfStoreService;

    @MockBean
    private GenerateFile generateFile;

    @MockBean
    private UserDetailsService userDetailsService;

    @Before
    public void setup() throws IOException {
        when(generateFile.assemble(any())).thenReturn("document.url");

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(pdfStoreService.download(anyString())).thenReturn(pdfBytes);

        when(userDetailsService.buildLoggedInUserName(any())).thenReturn("Logged in user");

        CcdCallbackController controller = new CcdCallbackController(authorisationService, deserializer, dispatcher);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mapper.registerModule(new JavaTimeModule());

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
    }

    @Test
    public void shouldHandleProcessAudioVideoEventCallbackWithAdmitEvidenceSelected() throws Exception {
        setJsonAndReplace("callback/processAudioVideoEvidenceCallback.json", "SELECTED_AUDIO_VIDEO_ACTION_PLACEHOLDER", "admitEvidence");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());
        SscsCaseData caseData = result.getData();

        assertNull(caseData.getInterlocReviewState());
        assertEquals(InterlocReferralReason.NONE, caseData.getInterlocReferralReason());
        assertEquals(DIRECTION_ACTION_REQUIRED, caseData.getDwpState());
        assertEquals(2, caseData.getSscsDocument().size());
        assertEquals("Addition A - Audio/Video evidence direction notice issued on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".pdf", caseData.getSscsDocument().get(1).getValue().getDocumentFileName());
        assertEquals("A", caseData.getSscsDocument().get(1).getValue().getBundleAddition());
        assertEquals("Addition B - Appellant - Statement for A/V file: evidence.mp3", caseData.getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("evidence.mp3", caseData.getSscsDocument().get(0).getValue().getAvDocumentLink().getDocumentFilename());
        assertEquals("B", caseData.getSscsDocument().get(0).getValue().getBundleAddition());

        assertNull(caseData.getAudioVideoEvidence());
    }

    @Test
    public void shouldHandleProcessAudioVideoEventCallbackWithAdmitEvidenceSelectedAndRip1Document() throws Exception {
        setJsonAndReplace("callback/processAudioVideoEvidenceWithRip1DocumentCallback.json", "SELECTED_AUDIO_VIDEO_ACTION_PLACEHOLDER", "admitEvidence");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());
        SscsCaseData caseData = result.getData();

        assertNull(caseData.getInterlocReviewState());
        assertEquals(InterlocReferralReason.NONE, caseData.getInterlocReferralReason());
        assertEquals(DIRECTION_ACTION_REQUIRED, caseData.getDwpState());
        assertEquals(2, caseData.getSscsDocument().size());
        assertEquals("Addition A - Audio/Video evidence direction notice issued on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".pdf", caseData.getSscsDocument().get(1).getValue().getDocumentFileName());
        assertEquals("A", caseData.getSscsDocument().get(1).getValue().getBundleAddition());
        assertEquals("Addition B - DWP - RIP 1 document for A/V file: evidence.mp3", caseData.getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("B", caseData.getSscsDocument().get(0).getValue().getBundleAddition());
        assertEquals("dwp", caseData.getSscsDocument().get(0).getValue().getPartyUploaded().getValue());
        assertEquals("evidence.mp3", caseData.getDwpDocuments().get(0).getValue().getDocumentFileName());
        assertEquals("evidence.mp3", caseData.getDwpDocuments().get(0).getValue().getAvDocumentLink().getDocumentFilename());
        assertEquals("dwp", caseData.getDwpDocuments().get(0).getValue().getPartyUploaded().getValue());

        assertNull(caseData.getAudioVideoEvidence());
    }

    @Test
    public void shouldHandleProcessAudioVideoEventCallbackWithExcludeEvidenceSelected() throws Exception {
        setJsonAndReplace("callback/processAudioVideoEvidenceCallback.json", "SELECTED_AUDIO_VIDEO_ACTION_PLACEHOLDER", "excludeEvidence");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());
        SscsCaseData caseData = result.getData();

        assertNull(caseData.getInterlocReviewState());
        assertEquals(InterlocReferralReason.NONE, caseData.getInterlocReferralReason());
        assertEquals(DIRECTION_ACTION_REQUIRED, caseData.getDwpState());
        assertEquals(1, caseData.getSscsDocument().size());
        assertNull(caseData.getAudioVideoEvidence());
    }

    @Test
    public void shouldHandleProcessAudioVideoEventCallbackWithSendToJudgeSelected() throws Exception {
        setJsonAndReplace("callback/processAudioVideoEvidenceCallback.json", "SELECTED_AUDIO_VIDEO_ACTION_PLACEHOLDER", "sendToJudge");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());
        SscsCaseData caseData = result.getData();

        assertEquals(REVIEW_BY_JUDGE, caseData.getInterlocReviewState());
        assertEquals(InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE, caseData.getInterlocReferralReason());
        assertEquals(LocalDate.now(), caseData.getInterlocReferralDate());
        assertEquals(1, caseData.getAppealNotePad().getNotesCollection().size());
        assertEquals(ProcessedAction.SENT_TO_JUDGE.getValue(), caseData.getAudioVideoEvidence().get(0).getValue().getProcessedAction().getValue());
    }

    @Test
    public void shouldHandleProcessAudioVideoEventCallbackWithSendToAdminSelected() throws Exception {
        setJsonAndReplace("callback/processAudioVideoEvidenceCallback.json", "SELECTED_AUDIO_VIDEO_ACTION_PLACEHOLDER", "sendToAdmin");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());
        SscsCaseData caseData = result.getData();

        assertEquals(AWAITING_ADMIN_ACTION, caseData.getInterlocReviewState());
        assertEquals(InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE, caseData.getInterlocReferralReason());
        assertEquals(1, caseData.getAppealNotePad().getNotesCollection().size());
        assertEquals(ProcessedAction.SENT_TO_ADMIN.getValue(), caseData.getAudioVideoEvidence().get(0).getValue().getProcessedAction().getValue());
    }

    @Test
    public void shouldHandleProcessAudioVideoEventCallbackWithIssueDirectionsNoticeSelected() throws Exception {
        setJsonAndReplace("callback/processAudioVideoEvidenceWithDueDateCallback.json", "SELECTED_AUDIO_VIDEO_ACTION_PLACEHOLDER", "issueDirectionsNotice");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());
        SscsCaseData caseData = result.getData();

        assertEquals(AWAITING_INFORMATION, caseData.getInterlocReviewState());
        assertEquals(InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE, caseData.getInterlocReferralReason());
        assertEquals(DIRECTION_ACTION_REQUIRED, caseData.getDwpState());
        assertEquals(1, caseData.getSscsDocument().size());
        assertEquals(InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE, caseData.getInterlocReferralReason());
        assertEquals(LocalDate.now(), caseData.getInterlocReferralDate());
        assertEquals(ProcessedAction.DIRECTION_ISSUED.getValue(), caseData.getAudioVideoEvidence().get(0).getValue().getProcessedAction().getValue());
        assertEquals("2120-10-10", caseData.getDirectionDueDate());
    }
}
