package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.io.IOException;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.DirectionOrDecisionIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.IssueFinalDecisionTemplateBody;

@RunWith(JUnitParamsRunner.class)
public class WriteFinalDecisionMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String TEMPLATE_ID = "nuts.docx";
    private static final String URL = "http://dm-store/documents/123";
    private WriteFinalDecisionMidEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private GenerateFile generateFile;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private ArgumentCaptor<GenerateFileParams> capture;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        handler = new WriteFinalDecisionMidEventHandler(generateFile, TEMPLATE_ID);

        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);


        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
            .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("APPELLANT")
                        .lastName("LastNamE")
                        .build())
                    .identity(Identity.builder().build())
                    .build())
                .build()).build();


        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        capture = ArgumentCaptor.forClass(GenerateFileParams.class);

        when(generateFile.assemble(any())).thenReturn(URL);
    }

    @Test
    public void givenANonWriteFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenAnEndDateIsBeforeStartDate_thenDisplayAnError() {
        sscsCaseData.setPipWriteFinalDecisionStartDate("2020-01-01");
        sscsCaseData.setPipWriteFinalDecisionEndDate("2019-01-01");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Decision notice end date must be after decision notice start date", error);
    }

    @Test
    public void givenADecisionDateIsInFuture_thenDisplayAnError() {

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        sscsCaseData.setPipWriteFinalDecisionDateOfDecision(tomorrow.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Decision notice date of decision must not be in the future", error);
    }

    @Test
    public void givenADecisionDateIsToday_thenDoNotDisplayAnError() {

        LocalDate today = LocalDate.now();
        sscsCaseData.setPipWriteFinalDecisionDateOfDecision(today.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenADecisionDateIsInPast_thenDoNotDisplayAnError() {

        LocalDate yesterday = LocalDate.now().plusDays(-1);
        sscsCaseData.setPipWriteFinalDecisionDateOfDecision(yesterday.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnEndDateIsSameAsStartDate_thenDisplayAnError() {
        sscsCaseData.setPipWriteFinalDecisionStartDate("2020-01-01");
        sscsCaseData.setPipWriteFinalDecisionEndDate("2020-01-01");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Decision notice end date must be after decision notice start date", error);
    }

    @Test
    public void givenAnEndDateIsAfterStartDate_thenDoNotDisplayAnError() {
        sscsCaseData.setPipWriteFinalDecisionStartDate("2019-01-01");
        sscsCaseData.setPipWriteFinalDecisionEndDate("2020-01-01");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }

    @Test
    public void willSetPreviewFile() {

        sscsCaseData.setPipWriteFinalDecisionGenerateNotice("Yes");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getPreviewDocument());

        verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname");
    }

    @Test
    public void scottishRpcWillShowAScottishImage() {

        sscsCaseData.setPipWriteFinalDecisionGenerateNotice("Yes");

        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.SCOTTISH_IMAGE, "Appellant Lastname");
    }

    @Test
    public void givenCaseWithAppointee_thenCorrectlySetTheNoticeNameWithAppellantAndAppointeeAppended() {

        sscsCaseData.setPipWriteFinalDecisionGenerateNotice("Yes");

        sscsCaseData.getAppeal().getAppellant().setIsAppointee("Yes");
        sscsCaseData.getAppeal().getAppellant().setAppointee(Appointee.builder()
            .name(Name.builder().firstName("APPOINTEE")
                .lastName("SurNamE")
                .build())
            .identity(Identity.builder().build())
            .build());

        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appointee Surname, appointee for Appellant Lastname");
    }


    private void verifyTemplateBody(String image, String expectedName) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());
        IssueFinalDecisionTemplateBody payload = (IssueFinalDecisionTemplateBody) capture.getValue().getFormPayload();
        assertEquals(image, payload.getImage());
        assertEquals("DECISION NOTICE", payload.getNoticeType());
        assertEquals(expectedName, payload.getAppellantFullName());
    }
}
