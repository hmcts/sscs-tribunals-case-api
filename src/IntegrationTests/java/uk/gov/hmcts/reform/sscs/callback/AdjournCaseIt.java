package uk.gov.hmcts.reform.sscs.callback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;

@SpringBootTest
@AutoConfigureMockMvc
public class AdjournCaseIt extends AbstractEventIt {

    public static final String DIRECTIONS_DUE_DATE_PLACEHOLDER = "2020-01-01";
    public static final String GENERATED_VIDEO_WHEN_CASE_NOT_LISTED_STRAIGHT_AWAY_WITH_DIRECTIONS_MADE_JSON =
        "callback/adjournCaseGeneratedVideoWhenCaseNotListedStraightAwayWithDirectionsMade.json";
    public static final String GENERATED_PAPER_WHEN_CASE_NOT_LISTED_STRAIGHT_AWAY_WITHOUT_DIRECTIONS_MADE_JSON =
        "callback/adjournCaseGeneratedPaperWhenCaseNotListedStraightAwayWithoutDirectionsMade.json";
    public static final String GENERATED_FACE_TO_FACE_WHEN_CASE_NOT_LISTED_STRAIGHT_AWAY_WITHOUT_DIRECTIONS_MADE_JSON =
        "callback/adjournCaseGeneratedFaceToFaceWhenCaseNotListedStraightAwayWithoutDirectionsMade.json";
    public static final String MANUALLY_GENERATED_JSON = "callback/adjournCaseManuallyGenerated.json";
    public static final String GENERATED_FACE_TO_FACE_WITH_INTERPRETER_REQUIRED_AND_LANGUAGE_SET_JSON =
        "callback/adjournCaseGeneratedFaceToFaceWithInterpreterRequiredAndLanguageSet.json";
    public static final String GENERATED_FACE_TO_FACE_WITH_INTERPRETER_REQUIRED_AND_LANGUAGE_NOT_SET_JSON =
        "callback/adjournCaseGeneratedFaceToFaceWithInterpreterRequiredAndLanguageNotSet.json";
    public static final String GENERATED_FACE_TO_FACE_WITH_INTERPRETER_NOT_REQUIRED_AND_LANGUAGE_SET_JSON =
        "callback/adjournCaseGeneratedFaceToFaceWithInterpreterNotRequiredAndLanguageSet.json";
    public static final String GENERATED_TELEPHONE_WHEN_CASE_NOT_LISTED_STRAIGHT_AWAY_WITHOUT_DIRECTIONS_MADE_JSON =
        "callback/adjournCaseGeneratedTelephoneWhenCaseNotListedStraightAwayWithoutDirectionsMade.json";
    public static final String GENERATED_VIDEO_WHEN_CASE_NOT_LISTED_STRAIGHT_AWAY_WITHOUT_DIRECTIONS_MADE_JSON =
        "callback/adjournCaseGeneratedVideoWhenCaseNotListedStraightAwayWithoutDirectionsMade.json";
    public static final String CCD_ABOUT_TO_SUBMIT = "/ccdAboutToSubmit";
    public static final String CCD_MID_EVENT_PREVIEW_ADJOURN_CASE = "/ccdMidEventPreviewAdjournCase";
    public static final String CCD_MID_EVENT_ADJOURN_CASE_POPULATE_VENUE_DROPDOWN = "/ccdMidEventAdjournCasePopulateVenueDropdown";
    public static final String CCD_MID_EVENT = "/ccdMidEvent";
    public static final String TEST_NAME = "AN Test";
    public static final String CHESTER_MAGISTRATE_S_COURT = "Chester Magistrate's Court";
    public static final String FIRST_MORNING_SESSION_ON_A_DATE_TO_BE_FIXED =
        "It will be first in the morning session on a date to be fixed";
    public static final String A_STANDARD_TIME_SLOT = "a standard time slot";
    public static final String JUDGE_GIVEN_NAME = "Judge";
    public static final String JUDGE_FAMILY_NAME = "Family Name";
    public static final String JUDGE_FULL_NAME = "Judge Family Name";
    public static final String DATE_2017 = "2017-07-17";
    public static final String DATE_2019 = "2019-10-10";
    public static final String DOCUMENT_URL = "document.url";
    @MockBean
    private IdamClient idamClient;
    @MockBean
    private GenerateFile generateFile;
    @MockBean
    private UserInfo userInfo;

    @DisplayName("Call to mid event callback when path is YES NO YES will validate the data when due date in past")
    @Test
    public void givenCallToMidEventCallbackWithPathYesNoYesThenValidatesDataWithDueDateInPast() throws Exception {
        setup();
        setJsonAndReplace(GENERATED_VIDEO_WHEN_CASE_NOT_LISTED_STRAIGHT_AWAY_WITH_DIRECTIONS_MADE_JSON,
            List.of(DIRECTIONS_DUE_DATE_PLACEHOLDER),
            List.of(DATE_2019));

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, CCD_MID_EVENT));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors())
            .hasSize(1)
            .containsOnly("Directions due date must be in the future");
    }

    @DisplayName("Call to mid event callback when path is YES NO YES will validate the data when due date in future")
    @Test
    public void givenCallToMidEventCallbackWithPathYesNoYesThenValidatesDataWhenDueDateInFuture() throws Exception {
        setup();
        setJsonAndReplace(GENERATED_VIDEO_WHEN_CASE_NOT_LISTED_STRAIGHT_AWAY_WITH_DIRECTIONS_MADE_JSON,
            List.of(DIRECTIONS_DUE_DATE_PLACEHOLDER),
            List.of(LocalDate.now().plus(1, ChronoUnit.DAYS).toString()));

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, CCD_MID_EVENT));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors()).isEmpty();
    }

    @DisplayName("Call to about to submit handler will write adjourn notice to case")
    @Test
    public void givenCallToAboutToSubmitHandlerThenWritesAdjournNoticeToCase() throws Exception {
        setup();
        setJsonAndReplace(GENERATED_VIDEO_WHEN_CASE_NOT_LISTED_STRAIGHT_AWAY_WITH_DIRECTIONS_MADE_JSON,
            DIRECTIONS_DUE_DATE_PLACEHOLDER, DATE_2019);

        noticeGeneratedWithExpectedDetails();
    }

    @DisplayName("Call to about to submit handler will write manually uploaded adjourn notice to case")
    @Test
    //FIXME: Might need to improve the data for this test once manual route has been fully implemented
    public void givenCallToAboutToSubmitHandlerThenWritesManuallyUploadedAdjournNoticeToCase() throws Exception {
        setup(MANUALLY_GENERATED_JSON);

        noticeGeneratedWithExpectedDetails();
    }

    @DisplayName("Call to mid event preview adjourn case callback will preview the document for face to face")
    @Test
    public void givenCallToMidEventPreviewAdjournCaseCallbackThenPreviewsTheDocumentForFaceToFace() throws Exception {
        setup();
        json = getJson(GENERATED_FACE_TO_FACE_WHEN_CASE_NOT_LISTED_STRAIGHT_AWAY_WITHOUT_DIRECTIONS_MADE_JSON);

        checkStandardDocumentNoInterpreter();
    }

    @DisplayName("Call to mid event preview adjourn case callback will preview the document for face to face "
        + "when interpreter required and language set")
    @Test
    public void givenCallToMidEventPreviewAdjournCaseCallbackThenPreviewsTheDocumentForFaceToFaceWhenInterpreterRequiredAndLanguageSet()
        throws Exception {
        setup();
        json = getJson(GENERATED_FACE_TO_FACE_WITH_INTERPRETER_REQUIRED_AND_LANGUAGE_SET_JSON);

        checkDocumentNoErrors();

        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) getGenerateFileParamsArgumentCaptor().getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        checkPayloadDetailsAtVenue(
            parentPayload,
            payload,
            "It will be first in the afternoon session on a date to be fixed",
            "an interpreter in French"
        );
    }

    @NotNull
    private ArgumentCaptor<GenerateFileParams> getGenerateFileParamsArgumentCaptor() {
        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        return capture;
    }

    @DisplayName("Call to mid event preview adjourn case callback will not preview the document for face to face "
        + "when interpreter required and language not set")
    @Test
    public void givenCallToMidEventPreviewAdjournCaseCallbackThenPreviewsTheDocumentForFaceToFaceWhenInterpreterRequiredAndLanguageNotSet()
        throws Exception {
        setup();
        json =  getJson(GENERATED_FACE_TO_FACE_WITH_INTERPRETER_REQUIRED_AND_LANGUAGE_NOT_SET_JSON);

        PreSubmitCallbackResponse<SscsCaseData> result = getPreSubmitCallbackResponse();

        assertThat(result.getData().getAdjournment().getPreviewDocument()).isNull();

        assertThat(result.getErrors()).hasSize(1);

        String error = result.getErrors().stream().findFirst().orElse("");
        assertThat(error).isEqualTo("An interpreter is required but no language is set");
    }

    @DisplayName("Call to mid event preview adjourn case callback will preview the document for face to face "
        + "when interpreter not required and language set")
    @Test
    public void givenCallToMidEventPreviewAdjournCaseCallbackThenPreviewsTheDocumentForFaceToFaceWhenInterpreterNotRequiredAndLanguageSet()
        throws Exception {
        setup();
        json = getJson(GENERATED_FACE_TO_FACE_WITH_INTERPRETER_NOT_REQUIRED_AND_LANGUAGE_SET_JSON);

        checkStandardDocumentNoInterpreter();
    }

    @DisplayName("Call to mid event preview adjourn case callback will preview the document for telephone")
    @Test
    public void givenCallToMidEventPreviewAdjournCaseCallbackThenPreviewsTheDocumentForTelephone() throws Exception {
        setup();
        json = getJson(GENERATED_TELEPHONE_WHEN_CASE_NOT_LISTED_STRAIGHT_AWAY_WITHOUT_DIRECTIONS_MADE_JSON);

        checkDocumentNoErrors();

        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) getGenerateFileParamsArgumentCaptor().getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        checkPayloadDetailsNoVenue(
            parentPayload,
            payload,
            FIRST_MORNING_SESSION_ON_A_DATE_TO_BE_FIXED,
            "telephone hearing",
            A_STANDARD_TIME_SLOT,
            "telephone"
        );
    }

    @DisplayName("Call to mid event preview adjourn case callback will preview the document for video")
    @Test
    public void givenCallToMidEventPreviewAdjournCaseCallbackThenPreviewsTheDocumentForVideo() throws Exception {
        setup();
        json = getJson(GENERATED_VIDEO_WHEN_CASE_NOT_LISTED_STRAIGHT_AWAY_WITHOUT_DIRECTIONS_MADE_JSON);

        checkDocumentNoErrors();

        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) getGenerateFileParamsArgumentCaptor().getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        checkPayloadDetailsNoVenue(
            parentPayload,
            payload,
            "It will be first in the session on a date to be fixed",
            "video hearing",
            A_STANDARD_TIME_SLOT,
            "video"
        );
    }

    @DisplayName("Call to mid event preview adjourn case callback will preview the document for paper")
    @Test
    public void givenCallToMidEventPreviewAdjournCaseCallbackThenPreviewsTheDocumentForPaper() throws Exception {
        setup();
        json = getJson(GENERATED_PAPER_WHEN_CASE_NOT_LISTED_STRAIGHT_AWAY_WITHOUT_DIRECTIONS_MADE_JSON);

        checkDocumentNoErrors();

        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) getGenerateFileParamsArgumentCaptor().getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        checkPayloadDetailsNoVenue(
            parentPayload,
            payload,
            FIRST_MORNING_SESSION_ON_A_DATE_TO_BE_FIXED,
            "decision on the papers",
            null,
            "paper"
        );
    }

    @DisplayName("Call to populate venue dropdown will populate next hearing venue selected list")
    @Test
    public void givenCallToPopulateVenueDropdownThenPopulatesNextHearingVenueSelectedList() throws Exception {
        setup();
        String nextHearingDateSpecificDate = "2020-07-01";
        setJsonAndReplace(GENERATED_FACE_TO_FACE_WHEN_CASE_NOT_LISTED_STRAIGHT_AWAY_WITHOUT_DIRECTIONS_MADE_JSON,
            "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER",
            nextHearingDateSpecificDate);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, CCD_MID_EVENT_ADJOURN_CASE_POPULATE_VENUE_DROPDOWN));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        assertThat(result.getErrors()).isEmpty();
        DynamicList results = result.getData().getAdjournment().getNextHearingVenueSelected();
        assertThat(results.getListItems()).isNotEmpty();
    }

    private void checkStandardDocumentNoInterpreter() throws Exception {
        checkDocumentNoErrors();

        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) getGenerateFileParamsArgumentCaptor().getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        checkPayloadDetailsAtVenue(
            parentPayload,
            payload,
            FIRST_MORNING_SESSION_ON_A_DATE_TO_BE_FIXED,
            null
        );
    }

    @NotNull
    private PreSubmitCallbackResponse<SscsCaseData> noticeGeneratedWithExpectedDetails() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, CCD_ABOUT_TO_SUBMIT));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors()).isEmpty();

        assertThat(result.getData().getOutcome()).isNull();

        SscsDocumentDetails document = result.getData().getSscsDocument().get(0).getValue();
        assertThat(document.getDocumentType()).isEqualTo(DocumentType.DRAFT_ADJOURNMENT_NOTICE.getValue());
        assertThat(document.getDocumentDateAdded()).isEqualTo(LocalDate.now().toString());
        assertThat(document.getDocumentFileName()).containsPattern(
            Pattern.compile("Draft Adjournment Notice generated on \\d{1,2}-\\d{1,2}-\\d{4}\\.pdf"));
        assertThat(document.getDocumentFileName()).contains(
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        return result;
    }

    private static void checkPayloadDetailsNoVenue(
        NoticeIssuedTemplateBody parentPayload,
        AdjournCaseTemplateBody payload,
        String nextHearingDate,
        String nextHearingType,
        String nextHearingTimeslot,
        String hearingType
    ) {
        checkPayloadDetails(parentPayload, payload, nextHearingDate, nextHearingType,
            nextHearingTimeslot, hearingType, null);
        assertThat(payload.isNextHearingAtVenue()).isFalse();
        assertThat(payload.getNextHearingVenue()).isNull();
    }

    private static void checkPayloadDetailsAtVenue(
        NoticeIssuedTemplateBody parentPayload,
        AdjournCaseTemplateBody payload,
        String nextHearingDate,
        String interpreterDescription
    ) {
        checkPayloadDetails(parentPayload, payload, nextHearingDate, "face to face hearing",
            A_STANDARD_TIME_SLOT, "faceToFace", interpreterDescription);
        assertThat(payload.isNextHearingAtVenue()).isTrue();
        assertThat(payload.getNextHearingVenue()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
    }

    private static void checkPayloadDetails(
        NoticeIssuedTemplateBody parentPayload,
        AdjournCaseTemplateBody payload,
        String nextHearingDate,
        String nextHearingType,
        String nextHearingTimeslot,
        String hearingType,
        String interpreterDescription
    ) {
        assertThat(parentPayload.getAppellantFullName()).isEqualTo(TEST_NAME);
        assertThat(parentPayload.getCaseId()).isEqualTo("12345656789");
        assertThat(parentPayload.getNino()).isEqualTo("JT 12 34 56 D");
        assertThat(parentPayload.getAppointeeFullName()).isEqualTo(null);
        assertThat(parentPayload.getNoticeType()).isEqualTo("DRAFT ADJOURNMENT NOTICE");
        assertThat(parentPayload.getUserName()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getHeldOn()).isEqualTo(LocalDate.parse(DATE_2017));
        assertThat(payload.getHeldAt()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getHeldBefore()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getNextHearingDate()).isEqualTo(nextHearingDate);
        assertThat(payload.getNextHearingType()).isEqualTo(nextHearingType);
        assertThat(payload.getNextHearingTimeslot()).isEqualTo(nextHearingTimeslot);
        assertThat(payload.getHeldAt()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getHeldBefore()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getHeldOn()).isEqualTo(LocalDate.parse(DATE_2017));
        assertThat(payload.getHearingType()).isEqualTo(hearingType);
        assertThat(payload.getAdditionalDirections().get(0)).isEqualTo("something else");
        assertThat(payload.getReasonsForDecision().get(0)).isEqualTo("Reasons 1");
        assertThat(payload.getPanelMembersExcluded()).isEqualTo("Yes");
        assertThat(payload.getAppellantName()).isEqualTo(TEST_NAME);
        assertThat(payload.getInterpreterDescription()).isEqualTo(interpreterDescription);
    }

    private void checkDocumentNoErrors() throws Exception {
        PreSubmitCallbackResponse<SscsCaseData> result = getPreSubmitCallbackResponse();

        assertThat(result.getErrors()).isEmpty();

        assertThat(result.getData().getAdjournment().getPreviewDocument().getDocumentUrl()).isEqualTo(DOCUMENT_URL);
    }

    private PreSubmitCallbackResponse<SscsCaseData> getPreSubmitCallbackResponse() throws Exception {
        when(generateFile.assemble(any())).thenReturn(DOCUMENT_URL);

        when(userInfo.getGivenName()).thenReturn(JUDGE_GIVEN_NAME);
        when(userInfo.getFamilyName()).thenReturn(JUDGE_FAMILY_NAME);

        when(idamClient.getUserInfo("Bearer userToken")).thenReturn(userInfo);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, CCD_MID_EVENT_PREVIEW_ADJOURN_CASE));
        assertHttpStatus(response, HttpStatus.OK);
        return deserialize(response.getContentAsString());
    }

}
