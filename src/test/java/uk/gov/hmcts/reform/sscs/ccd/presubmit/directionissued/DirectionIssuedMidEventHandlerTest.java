package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.DirectionOrDecisionIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;


@RunWith(JUnitParamsRunner.class)
public class DirectionIssuedMidEventHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String TEMPLATE_ID = "nuts.docx";
    private static final String URL = "http://dm-store/documents/123";

    private DirectionIssuedMidEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private GenerateFile generateFile;

    private SscsCaseData sscsCaseData;

    private ArgumentCaptor<GenerateFileParams> capture;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new DirectionIssuedMidEventHandler(generateFile, TEMPLATE_ID);

        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);

        sscsCaseData = SscsCaseData.builder()
                .generateNotice("Yes")
                .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("APPELLANT")
                                        .lastName("LastNamE")
                                        .build())
                                .identity(Identity.builder().build())
                                .build())
                        .build()).build();

        capture = ArgumentCaptor.forClass(GenerateFileParams.class);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(generateFile.assemble(any())).thenReturn(URL);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenGenerateNoticeIsNo_thenReturnFalse() {
        sscsCaseData.setGenerateNotice("No");
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenGenerateNoticeIsYes_thenReturnTrue() {
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void willSetPreviewFile() {

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getPreviewDocument());
        assertEquals(DocumentLink.builder()
                .documentFilename(String.format("Direction Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .documentBinaryUrl(URL + "/binary")
                .documentUrl(URL)
                .build(), response.getData().getPreviewDocument());

        verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE);
    }

    @Test
    public void scottishRpcWillShowAScottishImage() {
        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.SCOTTISH_IMAGE);
    }

    private void verifyTemplateBody(String image) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());
        DirectionOrDecisionIssuedTemplateBody payload = (DirectionOrDecisionIssuedTemplateBody) capture.getValue().getFormPayload();
        assertEquals(image, payload.getImage());
        assertEquals("DIRECTION NOTICE", payload.getNoticeType());
        assertEquals("Appellant Lastname", payload.getAppellantFullName());
    }
}

