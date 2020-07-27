package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
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
        setJsonAndReplace("callback/adjournCaseGeneratedVideoWhenCaseNotListedStraightAwayWithDirectionsMade.json", Arrays.asList("DIRECTIONS_DUE_DATE_PLACEHOLDER", "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER"),
            Arrays.asList("2019-10-10", LocalDate.now().plus(1, ChronoUnit.DAYS).toString()));

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("Directions due date must be in the future", result.getErrors().toArray()[0]);
    }

    @Test
    public void callToMidEventCallback_whenPathIsYesNoYes_willValidateTheData_WhenDueDateInFuture() throws Exception {
        setup();
        setJsonAndReplace("callback/adjournCaseGeneratedVideoWhenCaseNotListedStraightAwayWithDirectionsMade.json", Arrays.asList("DIRECTIONS_DUE_DATE_PLACEHOLDER",
            "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER"), Arrays
            .asList(LocalDate.now().plus(1, ChronoUnit.DAYS).toString(),
            LocalDate.now().plus(1, ChronoUnit.DAYS).toString()));

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void callToMidEventCallback_whenPathIsGeneratedVideoWhenCaseNotListedStraightAwayWithoutDirectionsMade_willValidateTheNextHearingDate_WhenDateInFuture() throws Exception {
        setup();
        setJsonAndReplace(
            "callback/adjournCaseGeneratedVideoWhenCaseNotListedStraightAwayWithoutDirectionsMade.json", "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER", LocalDate.now().plus(1, ChronoUnit.DAYS).toString());

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void callToMidEventCallback_whenPathIsGeneratedVideoWhenCaseNotListedStraightAwayWithoutDirectionsMade_willValidateTheNextHearingDate_WhenDateInPast() throws Exception {
        setup();
        setJsonAndReplace(
            "callback/adjournCaseGeneratedVideoWhenCaseNotListedStraightAwayWithoutDirectionsMade.json", "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER", LocalDate.now().plus(-1, ChronoUnit.DAYS).toString());

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("Specified date cannot be in the past", result.getErrors().toArray()[0]);
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
        String nextHearingDateSpecificDate = "2020-07-01";
        final String expectedNextHearingDateSpecificDateInDocument = "01/07/2020";
        setJsonAndReplace(
            "callback/adjournCaseGeneratedFaceToFaceWhenCaseNotListedStraightAwayWithoutDirectionsMade.json", "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER", nextHearingDateSpecificDate);

        
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
        assertEquals(expectedNextHearingDateSpecificDateInDocument, payload.getNextHearingDate());
        assertEquals("am", payload.getNextHearingTime());
        assertEquals("face to face", payload.getNextHearingType());
        assertEquals("Chester Magistrate's Court", payload.getNextHearingVenue());
        assertEquals("a standard time slot", payload.getNextHearingTimeslot());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name", payload.getHeldBefore());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("faceToFace", payload.getHearingType());
        assertEquals("something else", payload.getAnythingElse());
        assertEquals("Reasons 1", payload.getReasonsForDecision().get(0));
        assertEquals("yes", payload.getPanelMembersExcluded());
        assertEquals("An Test", payload.getAppellantName());
        assertEquals(true, payload.isNextHearingAtVenue());
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willPreviewTheDocumentForTelephone() throws Exception {
        setup();
        String nextHearingDateSpecificDate = "2020-07-01";
        final String expectedNextHearingDateSpecificDateInDocument = "01/07/2020";
        setJsonAndReplace(
            "callback/adjournCaseGeneratedTelephoneWhenCaseNotListedStraightAwayWithoutDirectionsMade.json", "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER", nextHearingDateSpecificDate);

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
        assertEquals(expectedNextHearingDateSpecificDateInDocument, payload.getNextHearingDate());
        assertEquals("am", payload.getNextHearingTime());
        assertEquals("telephone", payload.getNextHearingType());
        assertNull(payload.getNextHearingVenue());
        assertEquals("a standard time slot", payload.getNextHearingTimeslot());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name", payload.getHeldBefore());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("telephone", payload.getHearingType());
        assertEquals("something else", payload.getAnythingElse());
        assertEquals("Reasons 1", payload.getReasonsForDecision().get(0));
        assertEquals("yes", payload.getPanelMembersExcluded());
        assertEquals("An Test", payload.getAppellantName());
        assertEquals(false, payload.isNextHearingAtVenue());
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willPreviewTheDocumentForVideo() throws Exception {
        setup();
        String nextHearingDateSpecificDate = "2020-07-01";
        final String expectedNextHearingDateSpecificDateInDocument = "01/07/2020";
        setJsonAndReplace(
            "callback/adjournCaseGeneratedVideoWhenCaseNotListedStraightAwayWithoutDirectionsMade.json", "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER", nextHearingDateSpecificDate);

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
        assertEquals(expectedNextHearingDateSpecificDateInDocument, payload.getNextHearingDate());
        assertEquals("am", payload.getNextHearingTime());
        assertEquals("video", payload.getNextHearingType());
        assertNull(payload.getNextHearingVenue());
        assertEquals("a standard time slot", payload.getNextHearingTimeslot());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name", payload.getHeldBefore());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("video", payload.getHearingType());
        assertEquals("something else", payload.getAnythingElse());
        assertEquals("Reasons 1", payload.getReasonsForDecision().get(0));
        assertEquals("yes", payload.getPanelMembersExcluded());
        assertEquals("An Test", payload.getAppellantName());
        assertEquals(false, payload.isNextHearingAtVenue());
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willPreviewTheDocumentForPaper() throws Exception {
        setup();
        String nextHearingDateSpecificDate = "2020-07-01";
        final String expectedNextHearingDateSpecificDateInDocument = "01/07/2020";
        setJsonAndReplace(
            "callback/adjournCaseGeneratedPaperWhenCaseNotListedStraightAwayWithoutDirectionsMade.json", "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER", nextHearingDateSpecificDate);

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
        assertEquals(expectedNextHearingDateSpecificDateInDocument, payload.getNextHearingDate());
        assertEquals("am", payload.getNextHearingTime());
        assertEquals("paper", payload.getNextHearingType());
        assertNull(payload.getNextHearingVenue());
        assertNull(payload.getNextHearingTimeslot());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name", payload.getHeldBefore());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("paper", payload.getHearingType());
        assertEquals("something else", payload.getAnythingElse());
        assertEquals("Reasons 1", payload.getReasonsForDecision().get(0));
        assertEquals("yes", payload.getPanelMembersExcluded());
        assertEquals("An Test", payload.getAppellantName());
        assertEquals(false, payload.isNextHearingAtVenue());

    }

}
