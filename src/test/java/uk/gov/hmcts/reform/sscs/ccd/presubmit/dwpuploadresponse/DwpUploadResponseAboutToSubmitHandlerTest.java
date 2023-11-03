package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason.PHE_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse.DwpUploadResponseAboutToSubmitHandler.NEW_OTHER_PARTY_RESPONSE_DUE_DAYS;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_1;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_2;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_3;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_4;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidence;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpResponseDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.WorkAllocationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.model.AppConstants;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.util.AddedDocumentsUtil;
import uk.gov.hmcts.reform.sscs.util.DateTimeUtils;

@RunWith(JUnitParamsRunner.class)
public class DwpUploadResponseAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    public static final int UUID_SIZE = 36;

    private DwpUploadResponseAboutToSubmitHandler dwpUploadResponseAboutToSubmitHandler;
    private SscsCaseData sscsCaseData;
    private SscsCaseData sscsCaseDataBefore;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    @Mock
    private UserDetailsService userDetailsService;

    private DwpDocumentService dwpDocumentService;

    private AddedDocumentsUtil addedDocumentsUtil;

    @Before
    public void setUp() {
        addedDocumentsUtil = new AddedDocumentsUtil(false);

        openMocks(this);
        dwpDocumentService = new DwpDocumentService();
        AddNoteService addNoteService = new AddNoteService(userDetailsService);
        dwpUploadResponseAboutToSubmitHandler = new DwpUploadResponseAboutToSubmitHandler(dwpDocumentService,
            addNoteService, addedDocumentsUtil);

        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
                .forename("Chris").surname("Davis").build().getFullName());

        when(callback.getEvent()).thenReturn(EventType.DWP_UPLOAD_RESPONSE);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1234")
            .benefitCode("022")
            .issueCode("CC")
            .dwpFurtherInfo("Yes")
            .dynamicDwpState(new DynamicList(""))
            .dwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("a.pdf").documentFilename("a.pdf").build()).build())
            .dwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("b.pdf").documentFilename("b.pdf").build()).build())
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .build();

        sscsCaseDataBefore = SscsCaseData.builder().build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetails.getId()).thenReturn(Long.valueOf(sscsCaseData.getCcdCaseId()));
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);
    }

    @Test
    public void givenADwpUploadResponseEvent_thenReturnTrue() {
        assertTrue(dwpUploadResponseAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonDwpUploadResponseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(dwpUploadResponseAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callback));

    }

    @Test
    public void givenADwpUploadResponseEvent_thenSetCaseCode() {
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals("022CC", response.getData().getCaseCode());
        assertEquals(LocalDate.now().toString(), response.getData().getDwpResponseDate());
    }

    @Test
    public void givenADwpUploadResponseEventWithEmptyBenefitCode_displayAnError() {
        callback.getCaseDetails().getCaseData().setBenefitCode(null);
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Benefit code cannot be empty", error);
        }
    }

    @Test
    public void givenADwpUploadResponseEventWithEmptyIssueCode_displayAnError() {
        callback.getCaseDetails().getCaseData().setIssueCode(null);
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Issue code cannot be empty", error);
        }
    }

    @Test
    public void givenADwpUploadResponseEventWithEmptyDwpResponseDoc_displayAnError() {
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(null);
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("FTA response document cannot be empty.", response.getErrors().iterator().next());
    }

    @Test
    public void givenADwpUploadResponseEventWithEmptyDwpEvidenceBundle_displayAnError() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(null);
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("FTA evidence bundle cannot be empty.", response.getErrors().iterator().next());
    }

    @Test
    public void givenADwpUploadResponseEventWithDwpFurtherInfoIsNo_assertNoErrors() {
        callback.getCaseDetails().getCaseData().setDwpFurtherInfo("No");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(NO, response.getData().getHasUnprocessedAudioVideoEvidence());
    }

    @Test
    public void givenADwpUploadResponseEventWithExistingAudioVideoEvidence_assertFlagIsSet() {
        callback.getCaseDetails().getCaseData().setAudioVideoEvidence(List.of(AudioVideoEvidence.builder().build()));
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(YES, response.getData().getHasUnprocessedAudioVideoEvidence());
    }

    @Test
    public void givenADwpUploadResponseEventWithIssueCodeSetToDD_displayAnError() {
        callback.getCaseDetails().getCaseData().setIssueCode("DD");
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Issue code cannot be set to the default value of DD", error);
        }
    }

    @Test
    public void givenADwpUploadResponseEventWithResponseDocumentsAndDwpDocumentsBundleFeatureFlagOn_assertRenameFilenameAndMoveDocumentsToDwpDocumentsCollection() {

        List<DwpDocument> existingDwpDocuments = new ArrayList();

        existingDwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DwpDocumentType.DWP_RESPONSE.getValue()).documentDateAdded(LocalDate.now().minusDays(1).toString()).documentLink(DocumentLink.builder().documentUrl("existing.com").build()).build()).build());
        existingDwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue()).documentDateAdded(LocalDate.now().minusDays(1).toString()).documentLink(DocumentLink.builder().documentUrl("existing2.com").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(existingDwpDocuments);

        callback.getCaseDetails().getCaseData().setDwpAT38Document(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/abcd-0123-xyzabcdefgh/binary")
                                .documentUrl("http://dm-store:5005/documents/abcd-0123-xyzabcdefgh")
                                .documentFilename("testAT38Document.pdf")
                                .build()
                ).build());

        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-5678-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-5678-xyzabcmnop")
                                .documentFilename("testEvidenceBundleDocument.pdf")
                                .build()
                ).build());

        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("phme");
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedEvidenceBundleDocument.pdf")
                                .build()
                ).build());

        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw/binary")
                                .documentUrl("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw")
                                .documentFilename("testResponseDocument.pdf")
                                .build()
                ).build());


        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/efgh-4567-mnopqrstuvw/binary")
                                .documentUrl("http://dm-store:5005/documents/efgh-4567-mnopqrstuvw")
                                .documentFilename("testEditedResponseDocument.pdf")
                                .build()
                ).build());

        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("Edited reason");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getDwpEvidenceBundleDocument());
        assertNull(response.getData().getDwpEditedEvidenceBundleDocument());
        assertNull(response.getData().getDwpResponseDocument());
        assertNull(response.getData().getDwpEditedResponseDocument());

        assertEquals(3, response.getData().getDwpDocuments().size());

        String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        assertThat(response.getData().getDwpDocuments(), hasItem(
                hasProperty("value", allOf(
                        hasProperty("documentLink", allOf(
                                hasProperty("documentUrl", is("http://dm-store:5005/documents/defg-5678-xyzabcmnop")),
                                hasProperty("documentBinaryUrl", is("http://dm-store:5005/documents/defg-5678-xyzabcmnop/binary")),
                                hasProperty("documentFilename", is(AppConstants.DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX + " on " + todayDate + ".pdf"))
                        )),
                        hasProperty("editedDocumentLink", allOf(
                                hasProperty("documentUrl", is("http://dm-store:5005/documents/defg-6545-xyzabcmnop")),
                                hasProperty("documentBinaryUrl", is("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")),
                                hasProperty("documentFilename", is(AppConstants.DWP_DOCUMENT_EDITED_EVIDENCE_FILENAME_PREFIX + " on " + todayDate + ".pdf"))
                        )),
                        hasProperty("documentType", is(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue())),
                        hasProperty("dwpEditedEvidenceReason", is("Edited reason"))
                ))
        ));

        assertThat(response.getData().getDwpDocuments(), hasItem(
                hasProperty("value", allOf(
                        hasProperty("documentLink", allOf(
                                hasProperty("documentUrl", is("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw")),
                                hasProperty("documentBinaryUrl", is("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw/binary")),
                                hasProperty("documentFilename", is(AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX + " on " + todayDate + ".pdf"))
                        )),
                        hasProperty("editedDocumentLink", allOf(
                                hasProperty("documentUrl", is("http://dm-store:5005/documents/efgh-4567-mnopqrstuvw")),
                                hasProperty("documentBinaryUrl", is("http://dm-store:5005/documents/efgh-4567-mnopqrstuvw/binary")),
                                hasProperty("documentFilename", is(AppConstants.DWP_DOCUMENT_EDITED_RESPONSE_FILENAME_PREFIX + " on " + todayDate + ".pdf"))
                        )),
                        hasProperty("documentType", is(DwpDocumentType.DWP_RESPONSE.getValue())),
                        hasProperty("dwpEditedEvidenceReason", is("Edited reason"))
                ))
        ));

        assertThat(response.getData().getDwpDocuments(), hasItem(
                hasProperty("value", allOf(
                        hasProperty("documentLink", allOf(
                                hasProperty("documentUrl", is("http://dm-store:5005/documents/abcd-0123-xyzabcdefgh")),
                                hasProperty("documentBinaryUrl", is("http://dm-store:5005/documents/abcd-0123-xyzabcdefgh/binary")),
                                hasProperty("documentFilename", is(AppConstants.DWP_DOCUMENT_AT38_FILENAME_PREFIX + " on " + todayDate + ".pdf"))
                        )),
                        hasProperty("documentType", is(DwpDocumentType.AT_38.getValue()))
                ))
        ));

        assertEquals(REVIEW_BY_JUDGE, response.getData().getInterlocReviewState());
    }

    @Test
    public void givenAUcCaseWithSingleElementSelected_thenSetCaseCodeToUs() {
        List<String> elementList = new ArrayList<>();
        elementList.add("testElement");
        sscsCaseData.setElementsDisputedList(elementList);
        sscsCaseData.setCaseCode("001");
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("uc").build());

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("US", response.getData().getIssueCode());
        assertEquals("001", response.getData().getBenefitCode());
        assertEquals("001US", response.getData().getCaseCode());
        assertEquals(DwpState.RESPONSE_SUBMITTED_DWP, response.getData().getDwpState());
        assertNull(response.getData().getInterlocReviewState());
    }

    @Test
    public void givenAUcCaseWithMultipleElementSelected_thenSetCaseCodeToUm() {
        List<String> elementList = new ArrayList<>();
        elementList.add("testElement");
        elementList.add("testElement2");
        sscsCaseData.setElementsDisputedList(elementList);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("uc").build());

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("UM", response.getData().getIssueCode());
        assertEquals("001", response.getData().getBenefitCode());
        assertEquals("001UM", response.getData().getCaseCode());
        assertEquals(DwpState.RESPONSE_SUBMITTED_DWP, response.getData().getDwpState());
        assertNull(response.getData().getInterlocReviewState());
    }

    @Test
    public void givenUcCaseWithPheNoFurtherInfo_thenSetReviewByJudge() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("phme");
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedEvidenceBundleDocument.pdf")
                                .build()
                ).build());
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedResponseDocument.pdf")
                                .build()
                ).build());
        callback.getCaseDetails().getCaseData().setDwpFurtherInfo("No");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("reviewByJudge", response.getData().getSelectWhoReviewsCase().getValue().getCode());
        assertEquals(PHE_REQUEST, response.getData().getInterlocReferralReason());
        assertEquals(REVIEW_BY_JUDGE, response.getData().getInterlocReviewState());
        assertEquals(1, response.getData().getAppealNotePad().getNotesCollection().size());
        assertEquals("Referred to interloc for review by judge - PHE request", response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDetail());
        assertEquals(LocalDate.now(), response.getData().getInterlocReferralDate());

        dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenUcCaseWithPheNoFurtherInfo2ndCall_thenNoError() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("phme");
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedEvidenceBundleDocument.pdf")
                                .build()
                ).build());
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedResponseDocument.pdf")
                                .build()
                ).build());
        callback.getCaseDetails().getCaseData().setDwpFurtherInfo("No");

        DynamicListItem reviewByJudgeItem = new DynamicListItem("reviewByJudge", null);
        sscsCaseData.setSelectWhoReviewsCase(new DynamicList(reviewByJudgeItem, null));

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("reviewByJudge", response.getData().getSelectWhoReviewsCase().getValue().getCode());
        assertEquals(PHE_REQUEST, response.getData().getInterlocReferralReason());
        assertEquals(REVIEW_BY_JUDGE, response.getData().getInterlocReviewState());
        assertEquals(1, response.getData().getAppealNotePad().getNotesCollection().size());
        assertEquals("Referred to interloc for review by judge - PHE request", response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDetail());
        assertEquals(LocalDate.now(), response.getData().getInterlocReferralDate());
    }

    @Test
    public void givenUcCaseWithPheYesFurtherInfo_thenDontReviewByJudge() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("phme");
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedEvidenceBundleDocument.pdf")
                                .build()
                ).build());

        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedResponseDocument.pdf")
                                .build()
                ).build());

        callback.getCaseDetails().getCaseData().setDwpFurtherInfo("Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getSelectWhoReviewsCase());
        assertEquals(PHE_REQUEST, response.getData().getInterlocReferralReason());
        assertEquals(REVIEW_BY_JUDGE, response.getData().getInterlocReviewState());
        assertEquals(1, response.getData().getAppealNotePad().getNotesCollection().size());
        assertEquals("Referred to interloc for review by judge - PHE request", response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDetail());
        assertEquals(LocalDate.now(), response.getData().getInterlocReferralDate());
    }

    @Test
    public void givenUcCaseWithPhmeAndEditedEvidenceBundle_thenMustHaveEditedDwpResponseDoc() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("phme");
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedEvidenceBundleDocument.pdf")
                                .build()
                ).build());

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("You must upload an edited FTA response document", error);
        }
    }

    @Test
    public void givenUcCaseWithPhmeAndEditedResponse_thenMustHaveEditedDwpEvidenceBundle() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("phme");
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-6545-xyzabcmnop")
                                .documentFilename("testEditedEvidenceBundleDocument.pdf")
                                .build()
                ).build());

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("You must upload an edited FTA evidence bundle", error);
        }
    }

    @Test
    public void givenUcCaseWithAppendix12Document_thenMoveDocumentToDwpDocumentsCollection() {
        callback.getCaseDetails().getCaseData().setAppendix12Doc(DwpResponseDocument.builder().documentFileName("testA").documentLink(DocumentLink.builder().documentFilename("My document name.pdf").build()).build());
        List<DwpDocument> dwpResponseDocuments = new ArrayList<>();
        dwpResponseDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentFileName("existingDoc").documentDateAdded(LocalDate.now().minusDays(1).toString()).build()).build());

        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpResponseDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(4, response.getData().getDwpDocuments().size());

        String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        assertThat(response.getData().getDwpDocuments(), hasItem(
                hasProperty("value", allOf(
                        hasProperty("documentLink", allOf(
                                hasProperty("documentFilename", is("Appendix 12 received on " + todayDate + ".pdf"))
                        )),
                        hasProperty("documentFileName", is("Appendix 12 received on " + todayDate)),
                        hasProperty("documentType", is(DwpDocumentType.APPENDIX_12.getValue()))
                ))
        ));

        assertThat(response.getData().getDwpDocuments(), hasItem(
                hasProperty("value", allOf(
                        hasProperty("documentFileName", is("existingDoc"))
                ))
        ));
    }

    @Test
    @Parameters(method = "emptyAppendix12Documents")
    public void givenEmptyAppendix12Document_thenDoNotMoveDocumentToDwpDocumentsCollection(@Nullable DwpResponseDocument dwpResponseDocument) {
        callback.getCaseDetails().getCaseData().setAppendix12Doc(dwpResponseDocument);
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(dwpResponseDocument);
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(dwpResponseDocument);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getDwpDocuments());
    }

    public static Object[][] emptyAppendix12Documents() {
        return new Object[][] {
                {DwpResponseDocument.builder().build()},
                {null}
        };
    }

    @Test
    public void givenUcbSelectedAndNoUcbDocument_displayAnError() {
        sscsCaseData.setDwpUcb(YES.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(null);
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Please upload a UCB document"));
    }

    @Test
    public void givenUcbSelectedIsNo_thenTheFieldsAreCleared() {
        sscsCaseData.setDwpUcb(NO.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(getPdfDocument().getDocumentLink());
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(sscsCaseData.getDwpUcb(), is(nullValue()));
        assertThat(sscsCaseData.getDwpUcbEvidenceDocument(), is(nullValue()));
        assertThat(sscsCaseData.getDwpDocuments().size(), is(2));
    }

    @Test
    public void givenUcbSelectedAndUploadedUcbDoc_thenNoErrors() {
        sscsCaseData.setDwpUcb(YES.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(getPdfDocument().getDocumentLink());
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(sscsCaseData.getDwpUcb(), is(YES.getValue()));
        assertThat(sscsCaseData.getDwpUcbEvidenceDocument(), is(nullValue()));
        assertThat(sscsCaseData.getDwpDocuments().size(), is(3));
    }

    @Test
    public void givenHandleAudioVideoDocuments_thenItMovesToAudioVideoListAndFillsInFieldsSendToTcw() {
        AudioVideoEvidenceDetails audioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder().documentLink(DocumentLink.builder()
                .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("filename.mp4").build())
                .rip1Document(DocumentLink.builder()
                        .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("rip1").build()).build();

        sscsCaseData.setDwpUploadAudioVideoEvidence(Collections
                .singletonList(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build()));

        dwpUploadResponseAboutToSubmitHandler.handleAudioVideoDocuments(sscsCaseData);

        assertNull(callback.getCaseDetails().getCaseData().getDwpUploadAudioVideoEvidence());
        assertEquals(1, callback.getCaseDetails().getCaseData().getAudioVideoEvidence().size());
        AudioVideoEvidence audioVideoEvidence = callback.getCaseDetails().getCaseData().getAudioVideoEvidence().get(0);
        assertEquals("/url", audioVideoEvidence.getValue().getDocumentLink().getDocumentUrl());
        assertEquals("rip1", audioVideoEvidence.getValue().getRip1Document().getDocumentFilename());
        assertEquals("filename.mp4", audioVideoEvidence.getValue().getFileName());
        assertEquals(UploadParty.DWP, audioVideoEvidence.getValue().getPartyUploaded());
        assertNotNull(audioVideoEvidence.getValue().getDateAdded());
        assertEquals(InterlocReviewState.REVIEW_BY_TCW, callback.getCaseDetails().getCaseData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE, callback.getCaseDetails().getCaseData().getInterlocReferralReason());
    }

    @Test
    public void givenHandleAudioVideoDocumentsNoRip1_thenItMovesToAudioVideoListAndFillsInFieldsSendToTcw() {
        AudioVideoEvidenceDetails audioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder().documentLink(DocumentLink.builder()
            .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("filename.mp4").build()).build();

        sscsCaseData.setDwpUploadAudioVideoEvidence(Collections
                .singletonList(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build()));

        dwpUploadResponseAboutToSubmitHandler.handleAudioVideoDocuments(sscsCaseData);

        assertNull(callback.getCaseDetails().getCaseData().getDwpUploadAudioVideoEvidence());
        assertEquals(1, callback.getCaseDetails().getCaseData().getAudioVideoEvidence().size());
        AudioVideoEvidence audioVideoEvidence = callback.getCaseDetails().getCaseData().getAudioVideoEvidence().get(0);
        assertEquals("/url", audioVideoEvidence.getValue().getDocumentLink().getDocumentUrl());
        assertNull("rip1", audioVideoEvidence.getValue().getRip1Document());
        assertEquals("filename.mp4", audioVideoEvidence.getValue().getFileName());
        assertEquals(UploadParty.DWP, audioVideoEvidence.getValue().getPartyUploaded());
        assertNotNull(audioVideoEvidence.getValue().getDateAdded());
        assertEquals(InterlocReviewState.REVIEW_BY_TCW, callback.getCaseDetails().getCaseData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE, callback.getCaseDetails().getCaseData().getInterlocReferralReason());
    }

    @Test
    public void givenHandleNullAudioVideoDocuments_thenNoAudioVideoList() {
        dwpUploadResponseAboutToSubmitHandler.handleAudioVideoDocuments(sscsCaseData);
        assertNull(sscsCaseData.getAudioVideoEvidence());
    }

    @Test
    public void givenHandleEmptyAudioVideoDocuments_thenNoAudioVideoList() {
        sscsCaseData.setDwpUploadAudioVideoEvidence(Collections.emptyList());
        dwpUploadResponseAboutToSubmitHandler.handleAudioVideoDocuments(sscsCaseData);
        assertNull(sscsCaseData.getAudioVideoEvidence());
    }

    @Test
    public void givenExistingAudioVideo_thenItGetsAddedToListSendToTcw() {
        AudioVideoEvidenceDetails audioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder().documentLink(DocumentLink.builder()
                .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("filename.mp4").build())
                .rip1Document(DocumentLink.builder()
                        .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("surveillance").build()).build();

        List<AudioVideoEvidence> audioVideoList = new ArrayList<>();
        audioVideoList.add(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build());

        sscsCaseData.setAudioVideoEvidence(new ArrayList<>(
                Arrays.asList(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build())));

        sscsCaseData.setDwpUploadAudioVideoEvidence(new ArrayList<>(
                Arrays.asList(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build())));

        dwpUploadResponseAboutToSubmitHandler.handleAudioVideoDocuments(sscsCaseData);

        assertNull(callback.getCaseDetails().getCaseData().getDwpUploadAudioVideoEvidence());
        assertEquals(2, callback.getCaseDetails().getCaseData().getAudioVideoEvidence().size());
        assertEquals(InterlocReviewState.REVIEW_BY_TCW, callback.getCaseDetails().getCaseData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE, callback.getCaseDetails().getCaseData().getInterlocReferralReason());
    }

    @Test
    public void givenHandleAudioVideoDocumentsAndPhme_thenItMovesToAudioVideoListAndFillsInFieldsSendToJudge() {
        AudioVideoEvidenceDetails audioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder().documentLink(DocumentLink.builder()
                .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("filename.mp4").build())
                .rip1Document(DocumentLink.builder()
                        .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("rip1").build()).build();

        sscsCaseData.setDwpUploadAudioVideoEvidence(Collections
                .singletonList(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build()));

        sscsCaseData.setDwpEditedEvidenceReason("phme");

        dwpUploadResponseAboutToSubmitHandler.handleAudioVideoDocuments(sscsCaseData);

        assertNull(callback.getCaseDetails().getCaseData().getDwpUploadAudioVideoEvidence());
        assertEquals(1, callback.getCaseDetails().getCaseData().getAudioVideoEvidence().size());
        AudioVideoEvidence audioVideoEvidence = callback.getCaseDetails().getCaseData().getAudioVideoEvidence().get(0);
        assertEquals("/url", audioVideoEvidence.getValue().getDocumentLink().getDocumentUrl());
        assertEquals("rip1", audioVideoEvidence.getValue().getRip1Document().getDocumentFilename());
        assertEquals("filename.mp4", audioVideoEvidence.getValue().getFileName());
        assertEquals(UploadParty.DWP, audioVideoEvidence.getValue().getPartyUploaded());
        assertNotNull(audioVideoEvidence.getValue().getDateAdded());
        assertEquals(REVIEW_BY_JUDGE, callback.getCaseDetails().getCaseData().getInterlocReviewState());
        assertNull(callback.getCaseDetails().getCaseData().getInterlocReferralReason());
    }

    @Test
    public void givenHandleAudioVideoDocumentsAndInterlocReviewStateAlreadyReviewByJudge_thenLeaveAsReviewByJudge() {
        AudioVideoEvidenceDetails audioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder().documentLink(DocumentLink.builder()
                .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("filename.mp4").build())
                .rip1Document(DocumentLink.builder()
                        .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("surveillance").build()).build();

        List audioVideoList = new ArrayList<>();
        audioVideoList.add(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build());

        sscsCaseData.setAudioVideoEvidence(new ArrayList<>(
                Arrays.asList(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build())));

        sscsCaseData.setDwpUploadAudioVideoEvidence(new ArrayList<>(
                Arrays.asList(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build())));

        sscsCaseData.setInterlocReviewState(REVIEW_BY_JUDGE);

        dwpUploadResponseAboutToSubmitHandler.handleAudioVideoDocuments(sscsCaseData);

        assertNull(callback.getCaseDetails().getCaseData().getDwpUploadAudioVideoEvidence());
        assertEquals(2, callback.getCaseDetails().getCaseData().getAudioVideoEvidence().size());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, callback.getCaseDetails().getCaseData().getInterlocReviewState());
        assertEquals(REVIEW_AUDIO_VIDEO_EVIDENCE, callback.getCaseDetails().getCaseData().getInterlocReferralReason());
    }

    @Test
    public void givenAudioVideoDocuments_shouldComputeCorrectAudioVideoTotals() throws JsonProcessingException {
        dwpUploadResponseAboutToSubmitHandler = new DwpUploadResponseAboutToSubmitHandler(dwpDocumentService,
            new AddNoteService(userDetailsService), new AddedDocumentsUtil(true));

        List<AudioVideoEvidence> audioVideoEvidence = new ArrayList<>();

        audioVideoEvidence.add(AudioVideoEvidence.builder().value(AudioVideoEvidenceDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("/url")
                    .documentBinaryUrl("/url/binary")
                    .documentFilename("video.mp4")
                    .build())
                .rip1Document(DocumentLink.builder()
                    .documentUrl("/url")
                    .documentBinaryUrl("/url/binary")
                    .documentFilename("surveillance")
                    .build())
                .build())
            .build());

        audioVideoEvidence.add(AudioVideoEvidence.builder().value(AudioVideoEvidenceDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("/url")
                    .documentBinaryUrl("/url/binary")
                    .documentFilename("audio.mp3")
                    .build())
                .rip1Document(DocumentLink.builder()
                    .documentUrl("/url")
                    .documentBinaryUrl("/url/binary")
                    .documentFilename("surveillance")
                    .build())
                .build())
            .build());


        sscsCaseData.setDwpUploadAudioVideoEvidence(audioVideoEvidence);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT,
            callback, USER_AUTHORISATION);

        Map<String, Integer> addedDocuments = new ObjectMapper().readerFor(Map.class)
            .readValue(response.getData().getWorkAllocationFields().getAddedDocuments());

        org.assertj.core.api.Assertions.assertThat(addedDocuments)
            .as("One piece of audio and video evidence each have been added, should be reflected in the map.")
            .containsOnly(org.assertj.core.api.Assertions.entry("audioDocument", 1),
                org.assertj.core.api.Assertions.entry("videoDocument", 1));
    }

    @Test
    public void givenPreExistingAudioVideoDocuments_shouldComputeCorrectAudioVideoTotalsForAvAddedThisEvent() throws JsonProcessingException {
        dwpUploadResponseAboutToSubmitHandler = new DwpUploadResponseAboutToSubmitHandler(dwpDocumentService,
            new AddNoteService(userDetailsService), new AddedDocumentsUtil(true));

        List<AudioVideoEvidence> newAudioVideoEvidence = new ArrayList<>();

        newAudioVideoEvidence.add(AudioVideoEvidence.builder().value(AudioVideoEvidenceDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("/url")
                    .documentBinaryUrl("/url/binary")
                    .documentFilename("video.mp4")
                    .build())
                .rip1Document(DocumentLink.builder()
                    .documentUrl("/url")
                    .documentBinaryUrl("/url/binary")
                    .documentFilename("surveillance")
                    .build())
                .build())
            .build());

        List<AudioVideoEvidence> existingAudioVideoEvidence = new ArrayList<>();

        existingAudioVideoEvidence.add(AudioVideoEvidence.builder().value(AudioVideoEvidenceDetails.builder()
                .documentLink(DocumentLink.builder()
                    .documentUrl("/url")
                    .documentBinaryUrl("/url/binary")
                    .documentFilename("audio.mp3")
                    .build())
                .rip1Document(DocumentLink.builder()
                    .documentUrl("/url")
                    .documentBinaryUrl("/url/binary")
                    .documentFilename("surveillance")
                    .build())
                .build())
            .build());


        sscsCaseData.setDwpUploadAudioVideoEvidence(newAudioVideoEvidence);
        sscsCaseData.setAudioVideoEvidence(existingAudioVideoEvidence);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT,
            callback, USER_AUTHORISATION);

        Map<String, Integer> addedDocuments = new ObjectMapper().readerFor(Map.class)
            .readValue(response.getData().getWorkAllocationFields().getAddedDocuments());

        org.assertj.core.api.Assertions.assertThat(addedDocuments)
            .as("Only video evidence was added this event, audio should not be inserted into added documents.")
            .containsOnly(org.assertj.core.api.Assertions.entry("videoDocument", 1));
    }

    @Test
    public void givenNoNewAudioVideoDocuments_shouldStillClearAddedDocuments() {
        dwpUploadResponseAboutToSubmitHandler = new DwpUploadResponseAboutToSubmitHandler(dwpDocumentService,
            new AddNoteService(userDetailsService), new AddedDocumentsUtil(true));

        sscsCaseData.setDwpUploadAudioVideoEvidence(new ArrayList<>());
        sscsCaseData.setWorkAllocationFields(WorkAllocationFields.builder()
            .addedDocuments("{audioEvidence=1}")
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT,
            callback, USER_AUTHORISATION);

        org.assertj.core.api.Assertions.assertThat(response.getData().getWorkAllocationFields().getAddedDocuments())
            .as("Added documents should be cleared every event.")
            .isNull();
    }

    @Test
    public void givenADwpUploadResponseEventWithRemovedDwpDocumentsThenHandle() {

        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(new DwpResponseDocument(null, null));
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(new DwpResponseDocument(null, null));
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason(null);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenADwpUploadResponseEventWithRip1DocumentAndNoAvFile_displayAnError() {
        AudioVideoEvidenceDetails audioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder().documentLink(null)
                .rip1Document(DocumentLink.builder().documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("surveillance").build()).build();

        List<AudioVideoEvidence> audioVideoList = new ArrayList<>();
        audioVideoList.add(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build());

        sscsCaseData.setDwpUploadAudioVideoEvidence(audioVideoList);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("You must upload an audio/video document when submitting a RIP 1 document", error);
        }
    }

    @Test
    public void givenADwpUploadResponseEventWithRip1DocumentAndAvFile_thenDoNotDisplayAnError() {
        AudioVideoEvidenceDetails audioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder().documentLink(DocumentLink.builder()
                .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("filename.mp4").build())
                .rip1Document(DocumentLink.builder()
                        .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("surveillance").build()).build();

        List<AudioVideoEvidence> audioVideoList = new ArrayList<>();
        audioVideoList.add(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build());

        sscsCaseData.setDwpUploadAudioVideoEvidence(audioVideoList);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenADwpUploadResponseEventWithAvFileAndNoRip1Document_thenDoNotDisplayAnError() {
        AudioVideoEvidenceDetails audioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder().documentLink(DocumentLink.builder()
                .documentUrl("/url").documentBinaryUrl("/url/binary").documentFilename("filename.mp4").build())
                .rip1Document(null).build();

        List<AudioVideoEvidence> audioVideoList = new ArrayList<>();
        audioVideoList.add(AudioVideoEvidence.builder().value(audioVideoEvidenceDetails).build());

        sscsCaseData.setDwpUploadAudioVideoEvidence(audioVideoList);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenADwpUploadResponseEventWithEmptyDocument_thenHandleThisCorrectly() {

        callback.getCaseDetails().getCaseData().setDwpAT38Document(DwpResponseDocument.builder().build());

        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/defg-5678-xyzabcmnop/binary")
                                .documentUrl("http://dm-store:5005/documents/defg-5678-xyzabcmnop")
                                .documentFilename("testEvidenceBundleDocument.pdf")
                                .build()
                ).build());


        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder()
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw/binary")
                                .documentUrl("http://dm-store:5005/documents/efgh-7890-mnopqrstuvw")
                                .documentFilename("testResponseDocument.pdf")
                                .build()
                ).build());

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(2, response.getData().getDwpDocuments().size());
    }

    private DwpResponseDocument getMovieDocument() {
        return DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("0101").documentFilename("movie.mov").build()).build();
    }

    private DwpResponseDocument getPdfDocument() {
        return DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("0101").documentFilename("a.pdf").build()).build();
    }

    @Test
    public void dwpResponseDocument_mustBeAPdf() {
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(getMovieDocument());
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("FTA response document must be a PDF."));
    }

    @Test
    public void dwpEvidenceBundleDocument_mustBeAPdf() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(getMovieDocument());
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("FTA evidence bundle must be a PDF."));
    }

    @Test
    public void at38_mustBeAPdf() {
        callback.getCaseDetails().getCaseData().setDwpAT38Document(getMovieDocument());
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("FTA AT38 document must be a PDF."));
    }

    @Test
    public void dwpEditedEvidenceDocument_mustBeAPdf() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("phme");
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(getPdfDocument());
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(getMovieDocument());
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("FTA edited response document must be a PDF."));
    }

    @Test
    public void dwpEditedEvidenceBundle_mustBeAPdf() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("phme");
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(getPdfDocument());
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(getMovieDocument());
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("FTA edited evidence bundle must be a PDF."));
    }

    @Test
    public void appendix12_mustBeAPdf() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("phme");
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(getPdfDocument());
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(getPdfDocument());
        callback.getCaseDetails().getCaseData().setAppendix12Doc(getMovieDocument());
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Appendix 12 document must be a PDF."));
    }

    @Test
    public void ucbEvidenceDocument_mustBeAPdf() {
        sscsCaseData.setDwpUcb(YES.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(getMovieDocument().getDocumentLink());
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("UCB document must be a PDF."));
    }

    @Test
    public void givenADwpUploadResponseEventPhmeChildSupportThenErrorAdded() {

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName())
                .description(Benefit.CHILD_SUPPORT.getDescription()).build());
        sscsCaseData.setDwpEditedResponseDocument(getPdfDocument());
        sscsCaseData.setDwpEditedEvidenceBundleDocument(getPdfDocument());
        sscsCaseData.setDwpEditedEvidenceReason("phme");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler
                .handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().iterator().next(),
                is("Potential harmful evidence is not a valid selection for child support cases"));
    }

    @Test
    public void givenADwpUploadResponseEventChildSupConfNonChildSupportThenNoErrorAdded() {

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder()
                .code(Benefit.PENSION_CREDIT.getShortName())
                .description(Benefit.PENSION_CREDIT.getDescription()).build());
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(getPdfDocument());
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(getPdfDocument());
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("childSupportConfidentiality");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler
                .handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().iterator().next(),
                is("Child support - Confidentiality is not a valid selection for this case"));
    }

    @Test
    public void givenADwpUploadResponseEventPhmeNonChildSupportThenNoErrorAdded() {

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(Benefit.PENSION_CREDIT.getShortName())
                .description(Benefit.PENSION_CREDIT.getDescription()).build());
        sscsCaseData.setDwpEditedResponseDocument(getPdfDocument());
        sscsCaseData.setDwpEditedEvidenceBundleDocument(getPdfDocument());
        sscsCaseData.setDwpEditedEvidenceReason("phme");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler
                .handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenADwpUploadResponseEventChildSupConfChildSupportThenNoErrorAdded() {

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName())
                .description(Benefit.CHILD_SUPPORT.getDescription()).build());
        sscsCaseData.setDwpEditedResponseDocument(getPdfDocument());
        sscsCaseData.setDwpEditedEvidenceBundleDocument(getPdfDocument());
        sscsCaseData.setDwpEditedEvidenceReason("childSupportConfidentiality");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler
                .handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    public void givenChildSupportCaseAndCaseCodeIsChangedToNonChildSupportCodeAndCaseHasOtherParty_thenShowError() {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode("022");

        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder().build()).build();
        otherPartyList.add(ccdValue);
        sscsCaseData.setOtherParties(otherPartyList);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(response.getErrors().iterator().next(), is("Benefit code cannot be changed on cases with registered 'Other Party'"));
    }

    @Test
    public void givenChildSupportCaseAndCaseCodeIsChangedToNonChildSupportCodeAndCaseHasNoOtherParty_thenShowWarning() {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode("022");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(1));
        assertThat(response.getWarnings().iterator().next(), is("The benefit code will be changed to a non-child support benefit code"));
    }

    @Test
    @Parameters({"022", "023", "024", "025", "026", "028"})
    public void givenChildSupportCaseAndCaseCodeIsSetToChildSupportCode_thenNoWarningOrErrorIsShown(String childSupportBenefitCode) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode(childSupportBenefitCode);
        sscsCaseDataBefore.setBenefitCode("022");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    public void givenChildSupportCaseAndCaseCodeIsAlreadyANonChildSupportCase_thenShowErrorOrWarning() {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode("001");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    public void givenOtherPartiesUcbIsYes_thenUpdateCasedataOtherPartyUcb() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty(ID_2), buildOtherParty(ID_1)));
        sscsCaseData.getAppeal().getBenefitType().setCode("childSupport");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(YesNo.YES.getValue(), response.getData().getOtherPartyUcb());
    }

    @Test
    public void givenNewOtherPartyAdded_thenAssignAnId() {
        sscsCaseData.setOtherParties(Arrays.asList(
            buildOtherPartyWithAppointeeAndRep(null, null, null)));
        sscsCaseData.getAppeal().getBenefitType().setCode("childSupport");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertThat(response.getData().getOtherParties())
            .hasSize(1)
            .extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId()).hasSize(UUID_SIZE);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                Assertions.assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                Assertions.assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            });
    }

    @Test
    public void givenExistingOtherParties_thenNewOtherPartyAssignedNewId() {
        sscsCaseData.setOtherParties(Arrays.asList(
            buildOtherParty(ID_2),
            buildOtherParty(ID_1),
            buildOtherPartyWithAppointeeAndRep(null, null, null)));
        sscsCaseData.getAppeal().getBenefitType().setCode("childSupport");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertThat(response.getData().getOtherParties())
            .hasSize(3)
            .extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId()).isEqualTo(ID_1);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            })
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId()).isEqualTo(ID_2);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            })
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId())
                    .isNotEqualTo(ID_1)
                    .isNotEqualTo(ID_2)
                    .hasSize(UUID_SIZE);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                Assertions.assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                Assertions.assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            });

        Assertions.assertThat(sscsCaseData.getDirectionDueDate())
            .isEqualTo(DateTimeUtils.generateDwpResponseDueDate(NEW_OTHER_PARTY_RESPONSE_DUE_DAYS));
    }

    @Test
    public void givenExistingOtherPartiesWithAppointeeAndRep_thenNewOtherPartyAssignedNewId() {
        sscsCaseData.setOtherParties(Arrays.asList(
            buildOtherParty(ID_2),
            buildOtherPartyWithAppointeeAndRep(ID_1, ID_3, ID_4),
            buildOtherPartyWithAppointeeAndRep(null, null, null)));
        sscsCaseData.getAppeal().getBenefitType().setCode("childSupport");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertThat(response.getData().getOtherParties())
            .hasSize(3)
            .extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId()).isEqualTo(ID_1);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                Assertions.assertThat(otherParty.getAppointee().getId()).isEqualTo(ID_3);
                Assertions.assertThat(otherParty.getRep().getId()).isEqualTo(ID_4);
            })
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId()).isEqualTo(ID_2);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            })
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId())
                    .isNotEqualTo(ID_1)
                    .isNotEqualTo(ID_2)
                    .hasSize(UUID_SIZE);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                Assertions.assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                Assertions.assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            });
    }

    @Test
    public void givenExistingOtherParties_thenNewOtherPartyAppointeeAndRepAssignedNewId() {
        sscsCaseData.setOtherParties(Arrays.asList(
            buildOtherPartyWithAppointeeAndRep(ID_2, null, null),
            buildOtherPartyWithAppointeeAndRep(ID_1, ID_3, ID_4),
            buildOtherParty(null)));
        sscsCaseData.getAppeal().getBenefitType().setCode("childSupport");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertThat(response.getData().getOtherParties())
            .hasSize(3)
            .extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId()).isEqualTo(ID_1);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                Assertions.assertThat(otherParty.getAppointee().getId()).isEqualTo(ID_3);
                Assertions.assertThat(otherParty.getRep().getId()).isEqualTo(ID_4);
            })
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId()).isEqualTo(ID_2);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
                Assertions.assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                Assertions.assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            })
            .anySatisfy((OtherParty otherParty) -> {
                Assertions.assertThat(otherParty.getId())
                    .isNotEqualTo(ID_1)
                    .isNotEqualTo(ID_2)
                    .hasSize(UUID_SIZE);
                Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            });
    }

    private CcdValue<OtherParty> buildOtherParty(String id) {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id(id)
                    .unacceptableCustomerBehaviour(YES)
                    .build())
            .build();
    }

    private CcdValue<OtherParty> buildOtherPartyWithAppointeeAndRep(String id, String appointeeId, String repId) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .isAppointee(YES.getValue())
                        .appointee(Appointee.builder().id(appointeeId).build())
                        .rep(Representative.builder().id(repId).hasRepresentative(YES.getValue()).build())
                        .build())
                .build();
    }

    @Test
    public void givenChildSupportCaseAppellantWantsConfidentialWithEditedDocs_thenNoError() {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.getAppeal().setAppellant(Appellant.builder().confidentialityRequired(YES).build());
        sscsCaseData.setIsConfidentialCase(YES);
        sscsCaseData.setDwpEditedEvidenceReason("childSupportConfidentiality");
        sscsCaseData.setDwpEditedResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("a.pdf").documentFilename("a.pdf").build()).build());
        sscsCaseData.setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("a.pdf").documentFilename("a.pdf").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(YES, is(response.getData().getAppeal().getAppellant().getConfidentialityRequired()));
        assertThat(YES, is(response.getData().getIsConfidentialCase()));
    }

    @Test
    @Parameters({"childSupport", "taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
        "childBenefit","thirtyHoursFreeChildcare","guaranteedMinimumPension","nationalInsuranceCredits"})
    public void givenChildSupportCaseAppellantWantsConfidentialNoEditedDocs_thenShowError(String shortName) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(shortName).build());
        sscsCaseData.getAppeal().setAppellant(Appellant.builder().confidentialityRequired(YES).build());
        sscsCaseData.setIsConfidentialCase(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertTrue(response.getErrors().contains("Appellant requires confidentiality, upload edited and unedited responses"));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(YES, is(response.getData().getAppeal().getAppellant().getConfidentialityRequired()));
        assertThat(YES, is(response.getData().getIsConfidentialCase()));
    }

    @Test
    public void givenChildSupportCaseOtherPartyWantsConfidentialWithEditedDocs_thenNoError() {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.getAppeal().setAppellant(Appellant.builder().confidentialityRequired(NO).build());
        sscsCaseData.setIsConfidentialCase(NO);

        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder().confidentialityRequired(YES).build()).build();
        otherPartyList.add(ccdValue);
        sscsCaseData.setOtherParties(otherPartyList);

        sscsCaseData.setDwpEditedEvidenceReason("childSupportConfidentiality");
        sscsCaseData.setDwpEditedResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("a.pdf").documentFilename("a.pdf").build()).build());
        sscsCaseData.setDwpEditedEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("a.pdf").documentFilename("a.pdf").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(NO, is(response.getData().getAppeal().getAppellant().getConfidentialityRequired()));
        assertThat(YES, is(response.getData().getIsConfidentialCase()));
    }

    @Test
    @Parameters({"childSupport", "taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
        "childBenefit","thirtyHoursFreeChildcare","guaranteedMinimumPension","nationalInsuranceCredits"})
    public void givenChildSupportCaseOtherPartyWantsConfidentialNoEditedDocs_thenShowError(String shortName) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(shortName).build());
        sscsCaseData.getAppeal().setAppellant(Appellant.builder().confidentialityRequired(NO).build());
        sscsCaseData.setIsConfidentialCase(NO);

        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder().confidentialityRequired(YES).build()).build();
        otherPartyList.add(ccdValue);
        sscsCaseData.setOtherParties(otherPartyList);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertTrue(response.getErrors().contains("Other Party requires confidentiality, upload edited and unedited responses"));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(NO, is(response.getData().getAppeal().getAppellant().getConfidentialityRequired()));
        assertThat(YES, is(response.getData().getIsConfidentialCase()));
    }

    @Test
    @Parameters({"childSupport", "taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
        "childBenefit","thirtyHoursFreeChildcare","guaranteedMinimumPension","nationalInsuranceCredits"})
    public void givenChildSupportCaseAppellantAndOtherPartyWantsConfidentialNoEditedDocs_thenShow2Error(String shortName) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(shortName).build());
        sscsCaseData.getAppeal().setAppellant(Appellant.builder().confidentialityRequired(YES).build());
        sscsCaseData.setIsConfidentialCase(YES);

        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder().confidentialityRequired(YES).build()).build();
        otherPartyList.add(ccdValue);
        sscsCaseData.setOtherParties(otherPartyList);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(2));
        assertTrue(response.getErrors().contains("Appellant requires confidentiality, upload edited and unedited responses"));
        assertTrue(response.getErrors().contains("Other Party requires confidentiality, upload edited and unedited responses"));

        assertThat(response.getWarnings().size(), is(0));
        assertThat(YES, is(response.getData().getAppeal().getAppellant().getConfidentialityRequired()));
        assertThat(YES, is(response.getData().getIsConfidentialCase()));
    }

    @Test
    @Parameters({"childSupport", "taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection",
        "childBenefit","thirtyHoursFreeChildcare","guaranteedMinimumPension","nationalInsuranceCredits"})
    public void givenChildSupportCaseThatIsNotConfidentialNoEditedDocs_thenNoWarning(String shortName) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(shortName).build());
        sscsCaseData.getAppeal().setAppellant(Appellant.builder().confidentialityRequired(NO).build());

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(NO, is(response.getData().getAppeal().getAppellant().getConfidentialityRequired()));
        assertThat(null, is(response.getData().getIsConfidentialCase()));
    }

    @Test
    @Parameters({"015", "016", "030", "034", "050", "053", "054", "055", "057", "058"})
    public void givenSscs5CaseAndCaseCodeIsSetToSscs5Code_thenNoErrorIsShown(String sscs5BenefitCode) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("taxCredit").build());
        sscsCaseData.setBenefitCode(sscs5BenefitCode);
        sscsCaseDataBefore.setBenefitCode("022");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    @Parameters({"015", "016", "030", "034", "050", "053", "054", "055", "057", "058"})
    public void givenSscs5CaseAndCaseCodeIsChangedToNonSscs5_thenShowError(String sscs5BenefitCode) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("guardiansAllowance").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode(sscs5BenefitCode);

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getWarnings().size(), is(0));
        assertEquals("Benefit code cannot be changed to the selected code", response.getErrors().stream().findFirst().get());
    }

    @Test
    @Parameters({"015", "016", "030", "034", "050", "053", "054", "055", "057", "058"})
    public void givenNonSscs5CaseAndCaseCodeIsSetToSscs5Code_thenErrorIsShown(String sscs5BenefitCode) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode(sscs5BenefitCode);
        sscsCaseDataBefore.setBenefitCode("022");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getWarnings().size(), is(0));
        assertEquals("Benefit code cannot be changed to the selected code", response.getErrors().stream().findFirst().get());
    }

    @Test
    public void givenDynamicDwpStateHasBeenChosen_thenSetDwpState() {
        sscsCaseData.setDynamicDwpState(new DynamicList("Withdrawn"));

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState(), is(DwpState.WITHDRAWN));
        assertNull(response.getData().getDynamicDwpState());
    }
}
