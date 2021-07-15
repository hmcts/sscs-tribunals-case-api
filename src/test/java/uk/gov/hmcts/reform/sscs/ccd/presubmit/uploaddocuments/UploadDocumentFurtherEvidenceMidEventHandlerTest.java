package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.DocumentType.REQUEST_FOR_HEARING_RECORDING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class UploadDocumentFurtherEvidenceMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private UploadDocumentFurtherEvidenceMidEventHandler handler;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new UploadDocumentFurtherEvidenceMidEventHandler();
        sscsCaseData = SscsCaseData.builder().appeal(
                Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAValidHandleAndEventType_thenReturnTrue() {
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "ABOUT_TO_START", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenNonUploadDocumentFurtherEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenUploadDocumentFurtherEvidenceEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE);
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenOneHearingRecordingRequestDocumentIsUploaded_thenShowHearingsPageAndSetPartiesListForAppellant() {
        SscsFurtherEvidenceDoc furtherEvidenceDoc = SscsFurtherEvidenceDoc.builder().value(SscsFurtherEvidenceDocDetails.builder().documentType(REQUEST_FOR_HEARING_RECORDING.getId()).build()).build();

        sscsCaseData.setDraftSscsFurtherEvidenceDocument(Collections.singletonList(furtherEvidenceDoc));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.YES, response.getData().getSscsHearingRecordingCaseData().getShowRequestingPartyPage());
        assertEquals(1, response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().size());
        assertEquals("appellant", response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().get(0).getCode());
    }

    @Test
    public void givenOneHearingRecordingRequestDocumentIsUploaded_thenShowHearingsPageAndSetPartiesListForAppellantAndRep() {
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative("yes").build());
        SscsFurtherEvidenceDoc furtherEvidenceDoc = SscsFurtherEvidenceDoc.builder().value(SscsFurtherEvidenceDocDetails.builder().documentType(REQUEST_FOR_HEARING_RECORDING.getId()).build()).build();

        sscsCaseData.setDraftSscsFurtherEvidenceDocument(Collections.singletonList(furtherEvidenceDoc));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.YES, response.getData().getSscsHearingRecordingCaseData().getShowRequestingPartyPage());
        assertEquals(2, response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().size());
        assertEquals("appellant", response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().get(0).getCode());
        assertEquals("representative", response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().get(1).getCode());
    }

    @Test
    public void givenOneHearingRecordingRequestDocumentIsUploaded_thenShowHearingsPageAndSetPartiesListForAppellantAndJointParty() {
        sscsCaseData.setJointParty("Yes");
        SscsFurtherEvidenceDoc furtherEvidenceDoc = SscsFurtherEvidenceDoc.builder().value(SscsFurtherEvidenceDocDetails.builder().documentType(REQUEST_FOR_HEARING_RECORDING.getId()).build()).build();

        sscsCaseData.setDraftSscsFurtherEvidenceDocument(Collections.singletonList(furtherEvidenceDoc));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.YES, response.getData().getSscsHearingRecordingCaseData().getShowRequestingPartyPage());
        assertEquals(2, response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().size());
        assertEquals("appellant", response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().get(0).getCode());
        assertEquals("jointParty", response.getData().getSscsHearingRecordingCaseData().getRequestingParty().getListItems().get(1).getCode());
    }

    @Test
    public void givenMoreThanOneHearingRecordingRequestDocumentIsUploaded_thenShowError() {
        SscsFurtherEvidenceDoc furtherEvidenceDoc1 = SscsFurtherEvidenceDoc.builder().value(SscsFurtherEvidenceDocDetails.builder().documentType(REQUEST_FOR_HEARING_RECORDING.getId()).build()).build();
        SscsFurtherEvidenceDoc furtherEvidenceDoc2 = SscsFurtherEvidenceDoc.builder().value(SscsFurtherEvidenceDocDetails.builder().documentType(REQUEST_FOR_HEARING_RECORDING.getId()).build()).build();

        List<SscsFurtherEvidenceDoc> sscsFurtherEvidenceDocList = new ArrayList<>();
        sscsFurtherEvidenceDocList.add(furtherEvidenceDoc1);
        sscsFurtherEvidenceDocList.add(furtherEvidenceDoc2);

        sscsCaseData.setDraftSscsFurtherEvidenceDocument(sscsFurtherEvidenceDocList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Only one request for hearing recording can be submitted at a time"));
    }

    @Test
    public void givenNoHearingRecordingRequestDocumentIsUploaded_thenDoNotShowHearingsPag() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getSscsHearingRecordingCaseData().getShowRequestingPartyPage());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }

}
