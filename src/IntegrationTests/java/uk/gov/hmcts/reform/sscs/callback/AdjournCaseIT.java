package uk.gov.hmcts.reform.sscs.callback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.time.LocalDate;
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
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
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
public class AdjournCaseIT extends AbstractEventIt {

    public static final String DIRECTIONS_DUE_DATE_PLACEHOLDER = "DIRECTIONS_DUE_DATE_PLACEHOLDER";
    public static final String NOT_LISTED_STRAIGHT_AWAY_WITH_DIRECTIONS_MADE_JSON =
        "callback/adjournCaseGeneratedVideoWhenCaseNotListedStraightAwayWithDirectionsMade.json";
    public static final String CCD_ABOUT_TO_SUBMIT = "/ccdAboutToSubmit";
    public static final String AN_TEST = "An Test";
    public static final String CASE_ID = "12345656789";
    public static final String NINO = "JT 12 34 56 D";
    public static final String DRAFT_ADJOURNMENT_NOTICE1 = "DRAFT ADJOURNMENT NOTICE";
    public static final String CHESTER_MAGISTRATE_S_COURT = "Chester Magistrate's Court";
    public static final String FIRST_IN_THE_MORNING_SESSION_ON_A_DATE_TO_BE_FIXED = "It will be first in the morning session on a date to be fixed";
    public static final String FACE_TO_FACE_HEARING = "face to face hearing";
    public static final String A_STANDARD_TIME_SLOT = "a standard time slot";
    public static final String JUDGE_FULL_NAME = "Judge Full Name";
    public static final String FACE_TO_FACE = "faceToFace";
    public static final String SOMETHING_ELSE = "something else";
    public static final String REASONS_1 = "Reasons 1";
    public static final String DATE_2017 = "2017-07-17";
    public static final String DATE_2019 = "2019-10-10";
    @MockBean
    private IdamClient idamClient;
    @MockBean
    private GenerateFile generateFile;

    @MockBean
    private UserDetails userDetails;

    @DisplayName("Call to mid event callback when path is YES NO YES will validate the data when due date in past")
    @Test
    public void testMidEventCallbackWithDueDateInPast() throws Exception {
        setup();
        setJsonAndReplace(NOT_LISTED_STRAIGHT_AWAY_WITH_DIRECTIONS_MADE_JSON,
            List.of(DIRECTIONS_DUE_DATE_PLACEHOLDER),
            List.of(DATE_2019));

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors()).containsOnly("Directions due date must be in the future");
    }

    @DisplayName("Call to mid event callback when path is YES NO YES will validate the data when due date in future")
    @Test
    public void testMidEventCallbackWithDueDateInFuture() throws Exception {
        setup();
        setJsonAndReplace(NOT_LISTED_STRAIGHT_AWAY_WITH_DIRECTIONS_MADE_JSON,
            List.of(DIRECTIONS_DUE_DATE_PLACEHOLDER),
            List.of(LocalDate.now().plus(1, ChronoUnit.DAYS).toString()));

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors()).isEmpty();
    }

    @DisplayName("Call to about to submit handler will write adjourn notice to case")
    @Test
    public void testAdjournNotice() throws Exception {
        setup();
        setJsonAndReplace(NOT_LISTED_STRAIGHT_AWAY_WITH_DIRECTIONS_MADE_JSON, DIRECTIONS_DUE_DATE_PLACEHOLDER, DATE_2019);

        noticeGeneratedWithExpectedDetails();
    }

    @DisplayName("Call to about to submit handler will write adjourn notice to case with generated date as set")
    @Test
    public void testDateSet() throws Exception {
        setup();
        setJsonAndReplace(
            "callback/adjournCaseValidSubmissionWithSetGeneratedDate.json", DIRECTIONS_DUE_DATE_PLACEHOLDER, DATE_2019);

        PreSubmitCallbackResponse<SscsCaseData> result = noticeGeneratedWithExpectedDetails();
        assertThat(result.getData().getAdjournCaseGeneratedDate()).isEqualTo("2018-01-01");
    }

    @DisplayName("Call to about to submit handler will write manually uploaded adjourn notice to case")
    @Test
    //FIXME: Might need to improve the data for this test once manual route has been fully implemented
    public void testManuallyUploadedAdjournNotice() throws Exception {
        setup("callback/adjournCaseManuallyGenerated.json");
        
        noticeGeneratedWithExpectedDetails();
    }

    @DisplayName("Call to mid event preview adjourn case callback will preview the document for face to face")
    @Test
    public void testFaceToFace() throws Exception {
        setup();
        json = getJson(
            "callback/adjournCaseGeneratedFaceToFaceWhenCaseNotListedStraightAwayWithoutDirectionsMade.json");

        checkStandardDocumentNoInterpreter();
    }

    @DisplayName("Call to mid event preview adjourn case callback will preview the document for face to face "
        + "when interpreter required and language set")
    @Test
    public void testFaceToFaceWithInterpreterAndLanguage() throws Exception {
        setup();
        json =  getJson(
            "callback/adjournCaseGeneratedFaceToFaceWithInterpreterRequiredAndLanguageSet.json");

        checkDocumentNoErrors();

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        checkPayloadDetailsAtVenue(parentPayload,
            payload,
            "It will be first in the afternoon session on a date to be fixed",
            "an interpreter in French"
        );
    }

    @DisplayName("Call to mid event preview adjourn case callback will not preview the document for face to face "
        + "when interpreter required and language not set")
    @Test
    public void testFaceToFaceWithInterpreterAndNoLanguage() throws Exception {
        setup();
        json =  getJson(
            "callback/adjournCaseGeneratedFaceToFaceWithInterpreterRequiredAndLanguageNotSet.json");

        String documentUrl = "document.url";
        PreSubmitCallbackResponse<SscsCaseData> result =
            getPreSubmitCallbackResponse(documentUrl);

        assertThat(result.getData().getAdjournCasePreviewDocument()).isNull();

        assertThat(result.getErrors()).hasSize(1);

        String error = result.getErrors().stream().findFirst().orElse("");
        assertThat(error).isEqualTo("An interpreter is required but no language is set");
    }

    @DisplayName("Call to mid event preview adjourn case callback will preview the document for face to face "
        + "when interpreter not required and language set")
    @Test
    public void testFaceToFaceWithNoInterpreterAndLanguage() throws Exception {
        setup();
        json = getJson(
            "callback/adjournCaseGeneratedFaceToFaceWithInterpreterNotRequiredAndLanguageSet.json");

        checkStandardDocumentNoInterpreter();
    }

    @DisplayName("Call to mid event preview adjourn case callback will preview the document for telephone")
    @Test
    public void testTelephone() throws Exception {
        setup();
        json = getJson(
            "callback/adjournCaseGeneratedTelephoneWhenCaseNotListedStraightAwayWithoutDirectionsMade.json");

        checkDocumentNoErrors();

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        checkPayloadDetailsNoVenue(parentPayload,
            payload,
            FIRST_IN_THE_MORNING_SESSION_ON_A_DATE_TO_BE_FIXED,
            "telephone hearing",
            A_STANDARD_TIME_SLOT,
            "telephone"
        );
    }

    @DisplayName("Call to mid event preview adjourn case callback will preview the document for video")
    @Test
    public void testVideo() throws Exception {
        setup();
        json = getJson(
            "callback/adjournCaseGeneratedVideoWhenCaseNotListedStraightAwayWithoutDirectionsMade.json");

        checkDocumentNoErrors();

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        checkPayloadDetailsNoVenue(parentPayload,
            payload,
            "It will be first in the session on a date to be fixed",
            "video hearing",
            A_STANDARD_TIME_SLOT,
            "video"
        );
    }

    @DisplayName("Call to mid event preview adjourn case callback will preview the document for paper")
    @Test
    public void testPaper() throws Exception {
        setup();
        json = getJson(
            "callback/adjournCaseGeneratedPaperWhenCaseNotListedStraightAwayWithoutDirectionsMade.json");

        checkDocumentNoErrors();

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        checkPayloadDetailsNoVenue(parentPayload,
            payload,
            FIRST_IN_THE_MORNING_SESSION_ON_A_DATE_TO_BE_FIXED,
            "decision on the papers",
            null,
            "paper"
        );
    }

    @DisplayName("Call to populate venue dropdown will populate next hearing venue selected list")
    @Test
    public void testPopulateVenueDropdown() throws Exception {
        setup();
        String nextHearingDateSpecificDate = "2020-07-01";
        setJsonAndReplace("callback/adjournCaseGeneratedFaceToFaceWhenCaseNotListedStraightAwayWithoutDirectionsMade.json",
            "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER",
            nextHearingDateSpecificDate);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventAdjournCasePopulateVenueDropdown"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        assertThat(result.getErrors()).isEmpty();
        DynamicList results = result.getData().getAdjournCaseNextHearingVenueSelected();
        assertThat(results.getListItems()).isNotEmpty();
    }

    private void checkStandardDocumentNoInterpreter() throws Exception {
        checkDocumentNoErrors();

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        checkPayloadDetailsAtVenue(
            parentPayload,
            payload,
            FIRST_IN_THE_MORNING_SESSION_ON_A_DATE_TO_BE_FIXED,
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
        assertThat(document.getDocumentType()).isEqualTo(DRAFT_ADJOURNMENT_NOTICE.getValue());
        assertThat(document.getDocumentDateAdded()).isEqualTo(LocalDate.now().toString());
        assertThat(document.getDocumentFileName()).containsPattern(
            Pattern.compile("Draft Adjournment Notice generated on \\d{1,2}-\\d{1,2}-\\d{4}\\.pdf"));
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
        checkPayloadDetails(parentPayload, payload, nextHearingDate, FACE_TO_FACE_HEARING,
            A_STANDARD_TIME_SLOT, FACE_TO_FACE, interpreterDescription);
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
        assertThat(parentPayload.getAppellantFullName()).isEqualTo(AN_TEST);
        assertThat(parentPayload.getCaseId()).isEqualTo(CASE_ID);
        assertThat(parentPayload.getNino()).isEqualTo(NINO);
        assertThat(parentPayload.getAppointeeFullName()).isEqualTo(null);
        assertThat(parentPayload.getNoticeType()).isEqualTo(DRAFT_ADJOURNMENT_NOTICE1);
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
        assertThat(payload.getAdditionalDirections().get(0)).isEqualTo(SOMETHING_ELSE);
        assertThat(payload.getReasonsForDecision().get(0)).isEqualTo(REASONS_1);
        assertThat(payload.getPanelMembersExcluded()).isEqualTo("yes");
        assertThat(payload.getAppellantName()).isEqualTo(AN_TEST);
        assertThat(payload.getInterpreterDescription()).isEqualTo(interpreterDescription);
    }

    private void checkDocumentNoErrors() throws Exception {
        String documentUrl = "document.url";
        PreSubmitCallbackResponse<SscsCaseData> result = getPreSubmitCallbackResponse(documentUrl);

        assertThat(result.getErrors()).isEmpty();

        assertThat(result.getData().getAdjournCasePreviewDocument().getDocumentUrl()).isEqualTo(documentUrl);
    }

    private PreSubmitCallbackResponse<SscsCaseData> getPreSubmitCallbackResponse(String documentUrl) throws Exception {
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn(JUDGE_FULL_NAME);

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewAdjournCase"));
        assertHttpStatus(response, HttpStatus.OK);
        return deserialize(response.getContentAsString());
    }

}
