package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpchallengevalidity;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;

@RunWith(JUnitParamsRunner.class)
public class DwpChallengeValidityAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    private DwpChallengeValidityAboutToSubmitHandler handler;

    private DwpDocumentService dwpDocumentService;

    @Before
    public void setUp() {
        openMocks(this);
        dwpDocumentService = new DwpDocumentService();

        handler = new DwpChallengeValidityAboutToSubmitHandler(dwpDocumentService);

        when(callback.getEvent()).thenReturn(EventType.DWP_CHALLENGE_VALIDITY);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

    }

    @Test
    public void givenANonChallengeValidityResponseReviewedEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenDwpChallengeValidityEventWithEmptyDwpDocuments_thenMoveChallengeValidityDocToDwpDocuments() {
        sscsCaseData = sscsCaseData.toBuilder().state(State.WITH_DWP)
                .dwpChallengeValidityDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("challengeValidityLink").build()).build())
                .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getDwpChallengeValidityDocument());
        assertEquals("challengeValidityLink", response.getData().getDwpDocuments().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals(DwpDocumentType.DWP_CHALLENGE_VALIDITY.getValue(), response.getData().getDwpDocuments().get(0).getValue().getDocumentType());
    }

    @Test
    public void givenDwpChallengeValidityEventWithExistingDwpDocuments_thenMoveDocsToDwpDocuments() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentDateAdded(LocalDate.now().minusDays(1).toString()).documentLink(DocumentLink.builder().documentUrl("existing.com").build()).build()).build());

        sscsCaseData = sscsCaseData.toBuilder().state(State.WITH_DWP)
                .dwpDocuments(dwpDocuments)
                .dwpChallengeValidityDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("challengeValidityLink").build()).build())
                .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getDwpChallengeValidityDocument());
        assertEquals("existing.com", sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals(2, response.getData().getDwpDocuments().size());
        assertEquals("challengeValidityLink", response.getData().getDwpDocuments().get(1).getValue().getDocumentLink().getDocumentUrl());
        assertEquals(DwpDocumentType.DWP_CHALLENGE_VALIDITY.getValue(), response.getData().getDwpDocuments().get(1).getValue().getDocumentType());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
