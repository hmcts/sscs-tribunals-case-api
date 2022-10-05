package uk.gov.hmcts.reform.sscs.callback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
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
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;

@SpringBootTest
@AutoConfigureMockMvc
public class AdjournCaseIt extends AbstractEventIt {

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
    public static final String DATE = "2017-07-17";
    @MockBean
    private IdamClient idamClient;
    @MockBean
    private GenerateFile generateFile;

    @MockBean
    private UserDetails userDetails;

    @Test
    public void callToMidEventCallback_whenPathIsYesNoYes_willValidateTheData_WhenDueDateInPast() throws Exception {
        setup();
        setJsonAndReplace(NOT_LISTED_STRAIGHT_AWAY_WITH_DIRECTIONS_MADE_JSON,
            List.of(DIRECTIONS_DUE_DATE_PLACEHOLDER),
            List.of("2019-10-10"));

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors()).containsOnly("Directions due date must be in the future");
    }

    @Test
    public void callToMidEventCallback_whenPathIsYesNoYes_willValidateTheData_WhenDueDateInFuture() throws Exception {
        setup();
        setJsonAndReplace(NOT_LISTED_STRAIGHT_AWAY_WITH_DIRECTIONS_MADE_JSON,
            List.of("DIRECTIONS_DUE_DATE_PLACEHOLDER"),
            List.of(LocalDate.now().plus(1, ChronoUnit.DAYS).toString()));

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    public void callToAboutToSubmitHandler_willWriteAdjournNoticeToCase() throws Exception {
        setup();
        setJsonAndReplace(NOT_LISTED_STRAIGHT_AWAY_WITH_DIRECTIONS_MADE_JSON, "DIRECTIONS_DUE_DATE_PLACEHOLDER", "2019-10-10");

        assertNoticeGeneratedWithExpectedDetails();
    }

    private void assertNoticeGeneratedWithExpectedDetails() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, CCD_ABOUT_TO_SUBMIT));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors()).isEmpty();

        assertThat(result.getData().getOutcome()).isNull();

        assertThat(result.getData().getSscsDocument().get(0).getValue().getDocumentType()).isEqualTo(DRAFT_ADJOURNMENT_NOTICE.getValue());
        assertThat(result.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded()).isEqualTo(LocalDate.now().toString());
        assertThat(result.getData().getSscsDocument().get(0).getValue().getDocumentFileName()).isEqualTo("Draft Adjournment Notice generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")) + ".pdf");
    }

    /**
     * Due to a CCD bug ( https://tools.hmcts.net/jira/browse/RDM-8200 ) we have had
     * to implement a workaround in AdjournCaseAboutToSubmitHandler to set
     * the generated date to now, even though it is already being determined by the
     * preview document handler.  This is because on submission, the correct generated date
     * (the one referenced in the preview document) is being overwritten to a null value.
     * Once RDM-8200 is fixed and we remove the workaround, this test should be changed
     * to assert that a "something has gone wrong" error is displayed instead of
     * previewing the document, as a null generated date would indicate that the
     * date in the preview document hasn't been set.
     *
     */
    @Disabled
    @Test
    public void callToAboutToSubmitHandler_willWriteAdjournNoticeToCaseWithGeneratedDateAsNowWhenGeneratedDateNotSet() throws Exception {
        setup();
        setJsonAndReplace(
            "callback/adjournCaseValidSubmissionWithNullGeneratedDate.json", "DIRECTIONS_DUE_DATE_PLACEHOLDER", "2019-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors()).isEmpty();

        assertThat(result.getData().getOutcome()).isNull();

        assertThat(result.getData().getSscsDocument().get(0).getValue().getDocumentType()).isEqualTo(DRAFT_ADJOURNMENT_NOTICE.getValue());
        assertThat(result.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded()).isEqualTo(LocalDate.now().toString());
        assertThat(result.getData().getSscsDocument().get(0).getValue().getDocumentFileName()).isEqualTo("Draft Adjournment Notice generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")) + ".pdf");

        assertThat(result.getData().getAdjournCaseGeneratedDate()).isEqualTo(LocalDate.now().toString());
    }

    /**
     * This test asserts that whatever the value of the existing generated date from CCD
     * submitted as part of the payload to the AdjournCaseAboutToSubmitHandler,
     * then that date is updated to now() after the AdjournCaseAboutToSubmitHandler is called.
     * This is due to a workaround we have implemented in the AdjournCaseAboutToSubmitHandler
     *
     */
    @Disabled
    @Test
    public void callToAboutToSubmitHandler_willWriteAdjournNoticeToCaseWithGeneratedDateOfNowWhenGeneratedDateSet() throws Exception {
        setup();
        setJsonAndReplace(
            "callback/adjournCaseValidSubmissionWithSetGeneratedDate.json", "DIRECTIONS_DUE_DATE_PLACEHOLDER", "2019-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors()).isEmpty();

        assertThat(result.getData().getOutcome()).isNull();

        assertThat(result.getData().getSscsDocument().get(0).getValue().getDocumentType()).isEqualTo(DRAFT_ADJOURNMENT_NOTICE.getValue());
        assertThat(result.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded()).isEqualTo(LocalDate.now().toString());
        assertThat(result.getData().getSscsDocument().get(0).getValue().getDocumentFileName()).isEqualTo("Draft Adjournment Notice generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")) + ".pdf");

        assertThat(result.getData().getAdjournCaseGeneratedDate()).isEqualTo(LocalDate.now().toString());
    }

    @Test
    //FIXME: Might need to improve the data for this test once manual route has been fully implemented
    public void callToAboutToSubmitHandler_willWriteManuallyUploadedAdjournNoticeToCase() throws Exception {
        setup("callback/adjournCaseManuallyGenerated.json");
        
        assertNoticeGeneratedWithExpectedDetails();
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willPreviewTheDocumentForFaceToFace() throws Exception {
        setup();
        json = getJson("callback/adjournCaseGeneratedFaceToFaceWhenCaseNotListedStraightAwayWithoutDirectionsMade.json");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn(JUDGE_FULL_NAME);

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewAdjournCase"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors()).isEmpty();

        assertThat(result.getData().getAdjournCasePreviewDocument().getDocumentUrl()).isEqualTo(documentUrl);

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        assertThat(parentPayload.getAppellantFullName()).isEqualTo(AN_TEST);
        assertThat(parentPayload.getCaseId()).isEqualTo(CASE_ID);
        assertThat(parentPayload.getNino()).isEqualTo(NINO);
        assertThat(parentPayload.getAppointeeFullName()).isNull();
        assertThat(parentPayload.getNoticeType()).isEqualTo(DRAFT_ADJOURNMENT_NOTICE1);
        assertThat(parentPayload.getUserName()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getHeldOn()).isEqualTo(LocalDate.parse(DATE));
        assertThat(payload.getHeldAt()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getHeldBefore()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getNextHearingDate()).isEqualTo(FIRST_IN_THE_MORNING_SESSION_ON_A_DATE_TO_BE_FIXED);
        assertThat(payload.getNextHearingType()).isEqualTo(FACE_TO_FACE_HEARING);
        assertThat(payload.getNextHearingVenue()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getNextHearingTimeslot()).isEqualTo(A_STANDARD_TIME_SLOT);
        assertThat(payload.getHeldAt()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getHeldBefore()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getHeldOn()).isEqualTo(LocalDate.parse(DATE));
        assertThat(payload.getHearingType()).isEqualTo(FACE_TO_FACE);
        assertThat(payload.getAdditionalDirections().get(0)).isEqualTo(SOMETHING_ELSE);
        assertThat(payload.getReasonsForDecision().get(0)).isEqualTo(REASONS_1);
        assertThat(payload.getPanelMembersExcluded()).isEqualTo("yes");
        assertThat(payload.getAppellantName()).isEqualTo(AN_TEST);
        assertThat(payload.isNextHearingAtVenue()).isTrue();
        assertThat(payload.getInterpreterDescription()).isNull();
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willPreviewTheDocumentForFaceToFace_WhenInterpreterRequiredAndLanguageSet() throws Exception {
        setup();
        json =  getJson(
            "callback/adjournCaseGeneratedFaceToFaceWithInterpreterRequiredAndLanguageSet.json");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn(JUDGE_FULL_NAME);

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewAdjournCase"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors()).isEmpty();

        assertThat(result.getData().getAdjournCasePreviewDocument().getDocumentUrl()).isEqualTo(documentUrl);

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        assertThat(parentPayload.getAppellantFullName()).isEqualTo(AN_TEST);
        assertThat(parentPayload.getCaseId()).isEqualTo(CASE_ID);
        assertThat(parentPayload.getNino()).isEqualTo(NINO);
        assertThat(parentPayload.getAppointeeFullName()).isNull();
        assertThat(parentPayload.getNoticeType()).isEqualTo(DRAFT_ADJOURNMENT_NOTICE1);
        assertThat(parentPayload.getUserName()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getHeldOn()).isEqualTo(LocalDate.parse(DATE));
        assertThat(payload.getHeldAt()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getHeldBefore()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getNextHearingDate()).isEqualTo("It will be first in the afternoon session on a date to be fixed");
        assertThat(payload.getNextHearingType()).isEqualTo(FACE_TO_FACE_HEARING);
        assertThat(payload.getNextHearingVenue()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getNextHearingTimeslot()).isEqualTo(A_STANDARD_TIME_SLOT);
        assertThat(payload.getHeldAt()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getHeldBefore()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getHeldOn()).isEqualTo(LocalDate.parse(DATE));
        assertThat(payload.getHearingType()).isEqualTo(FACE_TO_FACE);
        assertThat(payload.getAdditionalDirections()).containsOnly(SOMETHING_ELSE);
        assertThat(payload.getReasonsForDecision()).containsOnly(REASONS_1);
        assertThat(payload.getPanelMembersExcluded()).isEqualTo("yes");
        assertThat(payload.getAppellantName()).isEqualTo(AN_TEST);
        assertThat(payload.isNextHearingAtVenue()).isTrue();
        assertThat(payload.getInterpreterDescription()).isEqualTo("an interpreter in French");
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willNotPreviewTheDocumentForFaceToFace_WhenInterpreterRequiredAndLanguageNotSet() throws Exception {
        setup();
        json =  getJson(
            "callback/adjournCaseGeneratedFaceToFaceWithInterpreterRequiredAndLanguageNotSet.json");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn(JUDGE_FULL_NAME);

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewAdjournCase"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getData().getAdjournCasePreviewDocument()).isNull();

        assertThat(result.getErrors()).hasSize(1);

        String error = result.getErrors().stream().findFirst().orElse("");
        assertThat(error).isEqualTo("An interpreter is required but no language is set");
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willPreviewTheDocumentForFaceToFace_WhenInterpreterNotRequiredAndLanguageSet() throws Exception {
        setup();
        json = getJson(
            "callback/adjournCaseGeneratedFaceToFaceWithInterpreterNotRequiredAndLanguageSet.json");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn(JUDGE_FULL_NAME);

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewAdjournCase"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors()).isEmpty();

        assertThat(result.getData().getAdjournCasePreviewDocument().getDocumentUrl()).isEqualTo(documentUrl);

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        assertThat(parentPayload.getAppellantFullName()).isEqualTo(AN_TEST);
        assertThat(parentPayload.getCaseId()).isEqualTo(CASE_ID);
        assertThat(parentPayload.getNino()).isEqualTo(NINO);
        assertThat(parentPayload.getAppointeeFullName()).isNull();
        assertThat(parentPayload.getNoticeType()).isEqualTo(DRAFT_ADJOURNMENT_NOTICE1);
        assertThat(parentPayload.getUserName()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getHeldOn()).isEqualTo(LocalDate.parse(DATE));
        assertThat(payload.getHeldAt()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getHeldBefore()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getNextHearingDate()).isEqualTo(FIRST_IN_THE_MORNING_SESSION_ON_A_DATE_TO_BE_FIXED);
        assertThat(payload.getNextHearingType()).isEqualTo(FACE_TO_FACE_HEARING);
        assertThat(payload.getNextHearingVenue()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getNextHearingTimeslot()).isEqualTo(A_STANDARD_TIME_SLOT);
        assertThat(payload.getHeldAt()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getHeldBefore()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getHeldOn()).isEqualTo(LocalDate.parse(DATE));
        assertThat(payload.getHearingType()).isEqualTo(FACE_TO_FACE);
        assertThat(payload.getAdditionalDirections()).containsOnly(SOMETHING_ELSE);
        assertThat(payload.getReasonsForDecision()).containsOnly(REASONS_1);
        assertThat(payload.getPanelMembersExcluded()).isEqualTo("yes");
        assertThat(payload.getAppellantName()).isEqualTo(AN_TEST);
        assertThat(payload.isNextHearingAtVenue()).isTrue();
        assertThat(payload.getInterpreterDescription()).isNull();
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willPreviewTheDocumentForTelephone() throws Exception {
        setup();
        json = getJson(
            "callback/adjournCaseGeneratedTelephoneWhenCaseNotListedStraightAwayWithoutDirectionsMade.json");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn(JUDGE_FULL_NAME);

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewAdjournCase"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors()).isEmpty();

        assertThat(result.getData().getAdjournCasePreviewDocument().getDocumentUrl()).isEqualTo(documentUrl);

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        assertThat(parentPayload.getAppellantFullName()).isEqualTo(AN_TEST);
        assertThat(parentPayload.getCaseId()).isEqualTo(CASE_ID);
        assertThat(parentPayload.getNino()).isEqualTo(NINO);
        assertThat(parentPayload.getAppointeeFullName()).isNull();
        assertThat(parentPayload.getNoticeType()).isEqualTo(DRAFT_ADJOURNMENT_NOTICE1);
        assertThat(parentPayload.getUserName()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getHeldOn()).isEqualTo(LocalDate.parse(DATE));
        assertThat(payload.getHeldAt()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getHeldBefore()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getNextHearingDate()).isEqualTo(FIRST_IN_THE_MORNING_SESSION_ON_A_DATE_TO_BE_FIXED);
        assertThat(payload.getNextHearingType()).isEqualTo("telephone hearing");
        assertThat(payload.getNextHearingVenue()).isNull();
        assertThat(payload.getNextHearingTimeslot()).isEqualTo(A_STANDARD_TIME_SLOT);
        assertThat(payload.getHeldAt()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getHeldBefore()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getHeldOn()).isEqualTo(LocalDate.parse(DATE));
        assertThat(payload.getHearingType()).isEqualTo("telephone");
        assertThat(payload.getAdditionalDirections()).containsOnly(SOMETHING_ELSE);
        assertThat(payload.getReasonsForDecision()).containsOnly(REASONS_1);
        assertThat(payload.getPanelMembersExcluded()).isEqualTo("yes");
        assertThat(payload.getAppellantName()).isEqualTo(AN_TEST);
        assertThat(payload.isNextHearingAtVenue()).isFalse();
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willPreviewTheDocumentForVideo() throws Exception {
        setup();
        json = getJson(
            "callback/adjournCaseGeneratedVideoWhenCaseNotListedStraightAwayWithoutDirectionsMade.json");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn(JUDGE_FULL_NAME);

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewAdjournCase"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors()).isEmpty();

        assertThat(result.getData().getAdjournCasePreviewDocument().getDocumentUrl()).isEqualTo(documentUrl);

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        assertThat(parentPayload.getAppellantFullName()).isEqualTo(AN_TEST);
        assertThat(parentPayload.getCaseId()).isEqualTo(CASE_ID);
        assertThat(parentPayload.getNino()).isEqualTo(NINO);
        assertThat(parentPayload.getAppointeeFullName()).isNull();
        assertThat(parentPayload.getNoticeType()).isEqualTo(DRAFT_ADJOURNMENT_NOTICE1);
        assertThat(parentPayload.getUserName()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getHeldOn()).isEqualTo(LocalDate.parse(DATE));
        assertThat(payload.getHeldAt()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getHeldBefore()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getNextHearingDate()).isEqualTo("It will be first in the session on a date to be fixed");
        assertThat(payload.getNextHearingType()).isEqualTo("video hearing");
        assertThat(payload.getNextHearingVenue()).isNull();
        assertThat(payload.getNextHearingTimeslot()).isEqualTo(A_STANDARD_TIME_SLOT);
        assertThat(payload.getHeldAt()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getHeldBefore()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getHeldOn()).isEqualTo(LocalDate.parse(DATE));
        assertThat(payload.getHearingType()).isEqualTo("video");
        assertThat(payload.getAdditionalDirections()).containsOnly(SOMETHING_ELSE);
        assertThat(payload.getReasonsForDecision()).containsOnly(REASONS_1);
        assertThat(payload.getPanelMembersExcluded()).isEqualTo("yes");
        assertThat(payload.getAppellantName()).isEqualTo(AN_TEST);
        assertThat(payload.isNextHearingAtVenue()).isFalse();
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willPreviewTheDocumentForPaper() throws Exception {
        setup();
        json = getJson(
            "callback/adjournCaseGeneratedPaperWhenCaseNotListedStraightAwayWithoutDirectionsMade.json");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn(JUDGE_FULL_NAME);

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewAdjournCase"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertThat(result.getErrors()).isEmpty();

        assertThat(result.getData().getAdjournCasePreviewDocument().getDocumentUrl()).isEqualTo(documentUrl);

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        assertThat(parentPayload.getAppellantFullName()).isEqualTo(AN_TEST);
        assertThat(parentPayload.getCaseId()).isEqualTo(CASE_ID);
        assertThat(parentPayload.getNino()).isEqualTo(NINO);
        assertThat(parentPayload.getAppointeeFullName()).isNull();
        assertThat(parentPayload.getNoticeType()).isEqualTo(DRAFT_ADJOURNMENT_NOTICE1);
        assertThat(parentPayload.getUserName()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getHeldOn()).isEqualTo(LocalDate.parse(DATE));
        assertThat(payload.getHeldAt()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getHeldBefore()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getNextHearingDate()).isEqualTo(FIRST_IN_THE_MORNING_SESSION_ON_A_DATE_TO_BE_FIXED);
        assertThat(payload.getNextHearingType()).isEqualTo("decision on the papers");
        assertThat(payload.getNextHearingVenue()).isNull();
        assertThat(payload.getNextHearingTimeslot()).isNull();
        assertThat(payload.getHeldAt()).isEqualTo(CHESTER_MAGISTRATE_S_COURT);
        assertThat(payload.getHeldBefore()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getHeldOn()).isEqualTo(LocalDate.parse(DATE));
        assertThat(payload.getHearingType()).isEqualTo("paper");
        assertThat(payload.getAdditionalDirections()).containsOnly(SOMETHING_ELSE);
        assertThat(payload.getReasonsForDecision()).containsOnly(REASONS_1);
        assertThat(payload.getPanelMembersExcluded()).isEqualTo("yes");
        assertThat(payload.getAppellantName()).isEqualTo(AN_TEST);
        assertThat(payload.isNextHearingAtVenue()).isFalse();

    }

    @Test
    public void callToPopulateVenueDropdown_willPopulateNextHearingVenueSelectedList() throws Exception {
        setup();
        String nextHearingDateSpecificDate = "2020-07-01";
        final String expectedNextHearingDateSpecificDateInDocument = "01/07/2020";
        setJsonAndReplace("callback/adjournCaseGeneratedFaceToFaceWhenCaseNotListedStraightAwayWithoutDirectionsMade.json",
            "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER",
            nextHearingDateSpecificDate);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventAdjournCasePopulateVenueDropdown"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        assertThat(result.getErrors()).isEmpty();
        DynamicList results = result.getData().getAdjournCaseNextHearingVenueSelected();
        Assert.assertFalse(results.getListItems().isEmpty());
    }

}
