package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
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

    @MockBean
    private IdamClient idamClient;
    @MockBean
    private GenerateFile generateFile;

    @MockBean
    private UserDetails userDetails;

    @Test
    public void callToMidEventCallback_whenPathIsYesNoYes_willValidateTheData_WhenDueDateInPast() throws Exception {
        setup();
        setJsonAndReplace("callback/adjournCaseGeneratedVideoWhenCaseNotListedStraightAwayWithDirectionsMade.json", Arrays.asList("DIRECTIONS_DUE_DATE_PLACEHOLDER"),
            Arrays.asList("2019-10-10"));

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("Directions due date must be in the future", result.getErrors().toArray()[0]);
    }

    @Test
    public void callToMidEventCallback_whenPathIsYesNoYes_willValidateTheData_WhenDueDateInFuture() throws Exception {
        setup();
        setJsonAndReplace("callback/adjournCaseGeneratedVideoWhenCaseNotListedStraightAwayWithDirectionsMade.json", Arrays.asList("DIRECTIONS_DUE_DATE_PLACEHOLDER"), Arrays
            .asList(LocalDate.now().plus(1, ChronoUnit.DAYS).toString()));

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void callToAboutToSubmitHandler_willWriteAdjournNoticeToCase() throws Exception {
        setup();
        setJsonAndReplace("callback/adjournCaseGeneratedVideoWhenCaseNotListedStraightAwayWithDirectionsMade.json", "DIRECTIONS_DUE_DATE_PLACEHOLDER", "2019-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertNull(result.getData().getOutcome());

        assertEquals(DRAFT_ADJOURNMENT_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(LocalDate.now().toString(), result.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Draft Adjournment Notice generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
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
    @Test
    public void callToAboutToSubmitHandler_willWriteAdjournNoticeToCaseWithGeneratedDateAsNowWhenGeneratedDateNotSet() throws Exception {
        setup();
        setJsonAndReplace(
            "callback/adjournCaseValidSubmissionWithNullGeneratedDate.json", "DIRECTIONS_DUE_DATE_PLACEHOLDER", "2019-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertNull(result.getData().getOutcome());

        assertEquals(DRAFT_ADJOURNMENT_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(LocalDate.now().toString(), result.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Draft Adjournment Notice generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());


        assertEquals(LocalDate.now().toString(), result.getData().getAdjournCaseGeneratedDate());
    }

    /**
     * This test asserts that whatever the value of the existing generated date from CCD
     * submitted as part of the payload to the AdjournCaseAboutToSubmitHandler,
     * then that date is updated to now() after the AdjournCaseAboutToSubmitHandler is called.
     * This is due to a workaround we have implemented in the AdjournCaseAboutToSubmitHandler
     *
     */
    @Test
    public void callToAboutToSubmitHandler_willWriteAdjournNoticeToCaseWithGeneratedDateOfNowWhenGeneratedDateSet() throws Exception {
        setup();
        setJsonAndReplace(
            "callback/adjournCaseValidSubmissionWithSetGeneratedDate.json", "DIRECTIONS_DUE_DATE_PLACEHOLDER", "2019-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertNull(result.getData().getOutcome());

        assertEquals(DRAFT_ADJOURNMENT_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(LocalDate.now().toString(), result.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Draft Adjournment Notice generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());


        assertEquals(LocalDate.now().toString(), result.getData().getAdjournCaseGeneratedDate());
    }

    @Test
    //FIXME: Might need to improve the data for this test once manual route has been fully implemented
    public void callToAboutToSubmitHandler_willWriteManuallyUploadedAdjournNoticeToCase() throws Exception {
        setup("callback/adjournCaseManuallyGenerated.json");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertNull(result.getData().getOutcome());

        assertEquals(DRAFT_ADJOURNMENT_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(LocalDate.now().toString(), result.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Draft Adjournment Notice generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willPreviewTheDocumentForFaceToFace() throws Exception {
        setup();
        json = getJson("callback/adjournCaseGeneratedFaceToFaceWhenCaseNotListedStraightAwayWithoutDirectionsMade.json");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewAdjournCase"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getAdjournCasePreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        assertEquals("An Test", parentPayload.getAppellantFullName());
        assertEquals("12345656789", parentPayload.getCaseId());
        assertEquals("JT 12 34 56 D", parentPayload.getNino());
        assertNull(parentPayload.getAppointeeFullName());
        assertEquals("DRAFT ADJOURNMENT NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name", payload.getHeldBefore());
        assertEquals("It will be first in the morning session on a date to be fixed", payload.getNextHearingDate());
        assertEquals("face to face hearing", payload.getNextHearingType());
        assertEquals("Chester Magistrate's Court", payload.getNextHearingVenue());
        assertEquals("a standard time slot", payload.getNextHearingTimeslot());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name", payload.getHeldBefore());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("faceToFace", payload.getHearingType());
        assertEquals("something else", payload.getAdditionalDirections().get(0));
        assertEquals("Reasons 1", payload.getReasonsForDecision().get(0));
        assertEquals(YES.getValue(), payload.getPanelMembersExcluded());
        assertEquals("An Test", payload.getAppellantName());
        assertEquals(true, payload.isNextHearingAtVenue());
        assertNull(payload.getInterpreterDescription());
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willPreviewTheDocumentForFaceToFace_WhenInterpreterRequiredAndLanguageSet() throws Exception {
        setup();
        json =  getJson(
            "callback/adjournCaseGeneratedFaceToFaceWithInterpreterRequiredAndLanguageSet.json");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewAdjournCase"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getAdjournCasePreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        assertEquals("An Test", parentPayload.getAppellantFullName());
        assertEquals("12345656789", parentPayload.getCaseId());
        assertEquals("JT 12 34 56 D", parentPayload.getNino());
        assertNull(parentPayload.getAppointeeFullName());
        assertEquals("DRAFT ADJOURNMENT NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name", payload.getHeldBefore());
        assertEquals("It will be first in the afternoon session on a date to be fixed", payload.getNextHearingDate());
        assertEquals("face to face hearing", payload.getNextHearingType());
        assertEquals("Chester Magistrate's Court", payload.getNextHearingVenue());
        assertEquals("a standard time slot", payload.getNextHearingTimeslot());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name", payload.getHeldBefore());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("faceToFace", payload.getHearingType());
        assertEquals("something else", payload.getAdditionalDirections().get(0));
        assertEquals("Reasons 1", payload.getReasonsForDecision().get(0));
        assertEquals(YES.getValue(), payload.getPanelMembersExcluded());
        assertEquals("An Test", payload.getAppellantName());
        assertEquals(true, payload.isNextHearingAtVenue());
        assertEquals("an interpreter in French", payload.getInterpreterDescription());
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willNotPreviewTheDocumentForFaceToFace_WhenInterpreterRequiredAndLanguageNotSet() throws Exception {
        setup();
        json =  getJson(
            "callback/adjournCaseGeneratedFaceToFaceWithInterpreterRequiredAndLanguageNotSet.json");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewAdjournCase"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertNull(result.getData().getAdjournCasePreviewDocument());

        assertEquals(1, result.getErrors().size());

        String error = result.getErrors().stream().findFirst().orElse("");
        assertEquals("An interpreter is required but no language is set", error);
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willPreviewTheDocumentForFaceToFace_WhenInterpreterNotRequiredAndLanguageSet() throws Exception {
        setup();
        json = getJson(
            "callback/adjournCaseGeneratedFaceToFaceWithInterpreterNotRequiredAndLanguageSet.json");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewAdjournCase"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getAdjournCasePreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        assertEquals("An Test", parentPayload.getAppellantFullName());
        assertEquals("12345656789", parentPayload.getCaseId());
        assertEquals("JT 12 34 56 D", parentPayload.getNino());
        assertNull(parentPayload.getAppointeeFullName());
        assertEquals("DRAFT ADJOURNMENT NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name", payload.getHeldBefore());
        assertEquals("It will be first in the morning session on a date to be fixed", payload.getNextHearingDate());
        assertEquals("face to face hearing", payload.getNextHearingType());
        assertEquals("Chester Magistrate's Court", payload.getNextHearingVenue());
        assertEquals("a standard time slot", payload.getNextHearingTimeslot());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name", payload.getHeldBefore());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("faceToFace", payload.getHearingType());
        assertEquals("something else", payload.getAdditionalDirections().get(0));
        assertEquals("Reasons 1", payload.getReasonsForDecision().get(0));
        assertEquals(YES.getValue(), payload.getPanelMembersExcluded());
        assertEquals("An Test", payload.getAppellantName());
        assertEquals(true, payload.isNextHearingAtVenue());
        assertNull(payload.getInterpreterDescription());
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willPreviewTheDocumentForTelephone() throws Exception {
        setup();
        json = getJson(
            "callback/adjournCaseGeneratedTelephoneWhenCaseNotListedStraightAwayWithoutDirectionsMade.json");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewAdjournCase"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getAdjournCasePreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        assertEquals("An Test", parentPayload.getAppellantFullName());
        assertEquals("12345656789", parentPayload.getCaseId());
        assertEquals("JT 12 34 56 D", parentPayload.getNino());
        assertNull(parentPayload.getAppointeeFullName());
        assertEquals("DRAFT ADJOURNMENT NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name", payload.getHeldBefore());
        assertEquals("It will be first in the morning session on a date to be fixed", payload.getNextHearingDate());
        assertEquals("telephone hearing", payload.getNextHearingType());
        assertNull(payload.getNextHearingVenue());
        assertEquals("a standard time slot", payload.getNextHearingTimeslot());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name", payload.getHeldBefore());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("telephone", payload.getHearingType());
        assertEquals("something else", payload.getAdditionalDirections().get(0));
        assertEquals("Reasons 1", payload.getReasonsForDecision().get(0));
        assertEquals(YES.getValue(), payload.getPanelMembersExcluded());
        assertEquals("An Test", payload.getAppellantName());
        assertEquals(false, payload.isNextHearingAtVenue());
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willPreviewTheDocumentForVideo() throws Exception {
        setup();
        json = getJson(
            "callback/adjournCaseGeneratedVideoWhenCaseNotListedStraightAwayWithoutDirectionsMade.json");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewAdjournCase"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getAdjournCasePreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        assertEquals("An Test", parentPayload.getAppellantFullName());
        assertEquals("12345656789", parentPayload.getCaseId());
        assertEquals("JT 12 34 56 D", parentPayload.getNino());
        assertNull(parentPayload.getAppointeeFullName());
        assertEquals("DRAFT ADJOURNMENT NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name", payload.getHeldBefore());
        assertEquals("It will be first in the session on a date to be fixed", payload.getNextHearingDate());
        assertEquals("video hearing", payload.getNextHearingType());
        assertNull(payload.getNextHearingVenue());
        assertEquals("a standard time slot", payload.getNextHearingTimeslot());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name", payload.getHeldBefore());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("video", payload.getHearingType());
        assertEquals("something else", payload.getAdditionalDirections().get(0));
        assertEquals("Reasons 1", payload.getReasonsForDecision().get(0));
        assertEquals(YES.getValue(), payload.getPanelMembersExcluded());
        assertEquals("An Test", payload.getAppellantName());
        assertEquals(false, payload.isNextHearingAtVenue());
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willPreviewTheDocumentForPaper() throws Exception {
        setup();
        json = getJson(
            "callback/adjournCaseGeneratedPaperWhenCaseNotListedStraightAwayWithoutDirectionsMade.json");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewAdjournCase"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getAdjournCasePreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final AdjournCaseTemplateBody payload = parentPayload.getAdjournCaseTemplateBody();

        assertEquals("An Test", parentPayload.getAppellantFullName());
        assertEquals("12345656789", parentPayload.getCaseId());
        assertEquals("JT 12 34 56 D", parentPayload.getNino());
        assertNull(parentPayload.getAppointeeFullName());
        assertEquals("DRAFT ADJOURNMENT NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name", payload.getHeldBefore());
        assertEquals("It will be first in the morning session on a date to be fixed", payload.getNextHearingDate());
        assertEquals("decision on the papers", payload.getNextHearingType());
        assertNull(payload.getNextHearingVenue());
        assertNull(payload.getNextHearingTimeslot());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name", payload.getHeldBefore());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("paper", payload.getHearingType());
        assertEquals("something else", payload.getAdditionalDirections().get(0));
        assertEquals("Reasons 1", payload.getReasonsForDecision().get(0));
        assertEquals(YES.getValue(), payload.getPanelMembersExcluded());
        assertEquals("An Test", payload.getAppellantName());
        assertEquals(false, payload.isNextHearingAtVenue());

    }

    @Test
    public void callToPopulateVenueDropdown_willPopulateNextHearingVenueSelectedList() throws Exception {
        setup();
        String nextHearingDateSpecificDate = "2020-07-01";
        final String expectedNextHearingDateSpecificDateInDocument = "01/07/2020";
        setJsonAndReplace(
            "callback/adjournCaseGeneratedFaceToFaceWhenCaseNotListedStraightAwayWithoutDirectionsMade.json", "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER", nextHearingDateSpecificDate);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventAdjournCasePopulateVenueDropdown"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        assertEquals(0, result.getErrors().size());
        DynamicList results = result.getData().getAdjournCaseNextHearingVenueSelected();
        Assert.assertFalse(results.getListItems().isEmpty());
    }

}
