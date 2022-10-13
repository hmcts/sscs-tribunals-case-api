package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionposthearingapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ACTION_POST_HEARING_APPLICATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionposthearingapplication.ActionPostHearingApplicationMidEventHandler.PAGE_ID_GENERATE_NOTICE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;

@RunWith(MockitoJUnitRunner.class)
public class ActionPostHearingApplicationMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private static final String URL = "http://dm-store/documents/123";
    private static final String TEMPLATE_ID = "TB-SCS-GNO-ENG-00091.docx";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    private ArgumentCaptor<GenerateFileParams> capture;

    @Mock
    private GenerateFile generateFile;

    @Mock
    private DocumentConfiguration documentConfiguration;

    @InjectMocks
    private ActionPostHearingApplicationMidEventHandler handler;


    @Before
    public void setUp() {
        ReflectionTestUtils.setField(handler, "isPostHearingsEnabled", true);

        when(documentConfiguration.getDocuments()).thenReturn(new HashMap<>(Map.of(
            LanguagePreference.ENGLISH,  new HashMap<>(Map.of(
                DECISION_ISSUED, TEMPLATE_ID)
            ))
        ));

        sscsCaseData = SscsCaseData.builder()
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YES)
                .build())
            .appeal(Appeal.builder().appellant(Appellant.builder()
                    .name(Name.builder().firstName("APPELLANT").lastName("LastNamE").build())
                    .identity(Identity.builder().build()).build()).build())
            .directionDueDate(LocalDate.now().plusDays(1).toString())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(LIST_ASSIST)
                .build())
            .build();

        capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(ACTION_POST_HEARING_APPLICATION);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(generateFile.assemble(any())).thenReturn(URL);
        when(callback.getPageId()).thenReturn(PAGE_ID_GENERATE_NOTICE);


    }

    @Test
    public void givenAValidMidEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(ACTION_POST_HEARING_APPLICATION);
        assertThat(handler.canHandle(MID_EVENT, callback)).isTrue();
    }

    @Test
    public void givenPostHearingsEnabledFalse_thenReturnFalse() {
        ReflectionTestUtils.setField(handler, "isPostHearingsEnabled", false);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    public void givenLanguagePreferenceIsEnglish_NoticeIsGeneratedAndPopulatedInPreviewDocumentField() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        DocumentLink previewDocument = response.getData().getDocumentStaging().getPreviewDocument();
        assertThat(previewDocument).isNotNull();

        String expectedFilename = String.format("Decision Notice issued on %s.pdf",
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        assertThat(previewDocument.getDocumentFilename()).isEqualTo(expectedFilename);
        assertThat(previewDocument.getDocumentBinaryUrl()).isEqualTo(URL + "/binary");
        assertThat(previewDocument.getDocumentUrl()).isEqualTo(URL);

        verify(generateFile, times(1)).assemble(any());

        verify(generateFile, atLeastOnce()).assemble(capture.capture());

        var value = capture.getValue();
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) value.getFormPayload();
        assertThat(payload.getImage()).isEqualTo(NoticeIssuedTemplateBody.ENGLISH_IMAGE);
        assertThat(payload.getNoticeType()).isEqualTo("DECISION NOTICE");
        assertThat(payload.getAppellantFullName()).isEqualTo("Appellant Lastname");
        assertThat(value.getTemplateId()).isEqualTo(TEMPLATE_ID);
    }

    @Test
    public void givenOtherPageId_doNothing() {
        when(callback.getPageId()).thenReturn("test page id");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        verifyNoInteractions(generateFile);
    }

    @Test
    public void givenNonLaCase_shouldReturnErrorWithCorrectMessage() {
        sscsCaseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder()
            .hearingRoute(GAPS)
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("Cannot process Action Post Hearing Application on non Scheduling & Listing Case");
    }

}
