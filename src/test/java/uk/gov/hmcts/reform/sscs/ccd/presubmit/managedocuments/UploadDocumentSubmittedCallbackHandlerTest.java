package uk.gov.hmcts.reform.sscs.ccd.presubmit.managedocuments;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DocumentTabChoice.INTERNAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DocumentTabChoice.REGULAR;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.managedocuments.UploadDocumentSubmittedCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InternalCaseDocumentData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

class UploadDocumentSubmittedCallbackHandlerTest {
    private UploadDocumentSubmittedCallbackHandler handler;
    private final String userAuthorisation = "user_auth";
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private SscsCaseDetails sscsCaseDetails;
    private SscsCaseData sscsCaseData;
    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;
    @Mock
    private UpdateCcdCaseService updateCcdCaseService;
    @Mock
    private IdamService idamService;

    @BeforeEach
    void setUp() {
        openMocks(this);
        handler = new UploadDocumentSubmittedCallbackHandler(updateCcdCaseService, idamService, true);
        sscsCaseData = SscsCaseData.builder().internalCaseDocumentData(InternalCaseDocumentData.builder().build()).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_DOCUMENT);
    }

    @Test
    void canHandleWithCorrectEventAndCallbackType() {
        assertTrue(handler.canHandle(CallbackType.SUBMITTED, callback));
    }

    @Test
    void canHandleThrowsNullCallback() {
        assertThrows(NullPointerException.class, () -> handler.canHandle(CallbackType.SUBMITTED, null));
    }

    @Test
    void canHandleThrowsNullCallbackType() {
        assertThrows(NullPointerException.class, () -> handler.canHandle(null, callback));
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {"UPLOAD_DOCUMENT"})
    void cannotHandleWithIncorrectEventAndCorrectCallbackType(EventType event) {
        when(callback.getEvent()).thenReturn(event);
        assertFalse(handler.canHandle(CallbackType.SUBMITTED, callback));
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, mode = EnumSource.Mode.EXCLUDE, names = {"SUBMITTED"})
    void cannotHandleWithCorrectEventAndIncorrectCallbackType(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    void handleThrowsIfCannotHandle() {
        assertThrows(IllegalStateException.class, () -> handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, userAuthorisation), "Cannot handle callback");
    }

    @Test
    void doesNothingIfInternalDocumentFlagOff() {
        handler = new UploadDocumentSubmittedCallbackHandler(updateCcdCaseService, idamService, false);
        when(callback.getPageId()).thenReturn("moveDocumentTo");
        handler.handle(CallbackType.SUBMITTED, callback, userAuthorisation);
        verify(updateCcdCaseService, never()).triggerCaseEventV2(any(), any(), any(), any(), any());
    }

    @Test
    void doesNothingIfFlagOnButMoveToInternal() {
        when(callback.getPageId()).thenReturn("moveDocumentTo");
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentTo(INTERNAL);
        handler.handle(CallbackType.SUBMITTED, callback, userAuthorisation);
        verify(updateCcdCaseService, never()).triggerCaseEventV2(any(), any(), any(), any(), any());
    }

    @Test
    void doesNothingIfFlagOnAndMoveToRegularButNoToShouldBeIssued() {
        when(callback.getPageId()).thenReturn("moveDocumentTo");
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentTo(REGULAR);
        sscsCaseData.getInternalCaseDocumentData().setShouldBeIssued(NO);
        handler.handle(CallbackType.SUBMITTED, callback, userAuthorisation);
        verify(updateCcdCaseService, never()).triggerCaseEventV2(any(), any(), any(), any(), any());
    }

    @Test
    void triggersIssueFeEventIfFlagOnAndMoveToRegularAndYesToShouldBeIssued() {
        when(callback.getPageId()).thenReturn("moveDocumentTo");
        when(caseDetails.getId()).thenReturn(1234L);
        when(updateCcdCaseService.triggerCaseEventV2(any(), any(), any(), any(), any())).thenReturn(sscsCaseDetails);
        sscsCaseData.getInternalCaseDocumentData().setMoveDocumentTo(REGULAR);
        sscsCaseData.getInternalCaseDocumentData().setShouldBeIssued(YES);
        handler.handle(CallbackType.SUBMITTED, callback, userAuthorisation);
        verify(updateCcdCaseService).triggerCaseEventV2(1234L, EventType.ISSUE_FURTHER_EVIDENCE.getCcdType(),
            "Issue to all parties", "Issue to all parties", idamService.getIdamTokens());
    }
}
