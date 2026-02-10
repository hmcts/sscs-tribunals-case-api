package uk.gov.hmcts.reform.sscs.ccd.presubmit.managedocuments;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.managedocuments.UploadDocumentAboutToStartHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentTabChoice;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InternalCaseDocumentData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

class UploadDocumentAboutToStartHandlerTest {

    private UploadDocumentAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @BeforeEach
    void setUp() {
        openMocks(this);
        handler = new UploadDocumentAboutToStartHandler(false);
        SscsCaseData sscsCaseData = SscsCaseData.builder().internalCaseDocumentData(
            InternalCaseDocumentData.builder()
                .moveDocumentTo(DocumentTabChoice.INTERNAL)
                .shouldBeIssued(YesNo.YES)
                .uploadRemoveOrMoveDocument("move")
                .build()
        ).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_DOCUMENT);
    }

    @Test
    void canHandleWithCorrectEventAndCallbackType() {
        assertTrue(handler.canHandle(CallbackType.ABOUT_TO_START, callback));
    }

    @Test
    void canHandleThrowsNullCallback() {
        assertThrows(NullPointerException.class, () -> handler.canHandle(CallbackType.ABOUT_TO_START, null));
    }

    @Test
    void canHandleThrowsNullCallbackType() {
        assertThrows(NullPointerException.class, () -> handler.canHandle(null, callback));
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {"UPLOAD_DOCUMENT"})
    void cannotHandleWithIncorrectEventAndCorrectCallbackType(EventType event) {
        when(callback.getEvent()).thenReturn(event);
        assertFalse(handler.canHandle(CallbackType.ABOUT_TO_START, callback));
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, mode = EnumSource.Mode.EXCLUDE, names = {"ABOUT_TO_START"})
    void cannotHandleWithCorrectEventAndIncorrectCallbackType(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    void handleThrowsIfCannotHandle() {
        assertThrows(IllegalStateException.class, () -> handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, ""), "Cannot handle callback");
    }

    @Test
    void tribunalInternalDocumentsFlagOffDoesNothing() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START, callback, "");
        assertNotNull(response.getData().getInternalCaseDocumentData());
        assertNotNull(response.getData().getInternalCaseDocumentData().getMoveDocumentTo());
        assertNotNull(response.getData().getInternalCaseDocumentData().getShouldBeIssued());
        assertNotNull(response.getData().getInternalCaseDocumentData().getUploadRemoveOrMoveDocument());
    }

    @Test
    void tribunalInternalDocumentsFlagOnSetsFieldsNull() {
        handler = new UploadDocumentAboutToStartHandler(true);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START, callback, "");
        assertNotNull(response.getData().getInternalCaseDocumentData());
        assertNull(response.getData().getInternalCaseDocumentData().getMoveDocumentTo());
        assertNull(response.getData().getInternalCaseDocumentData().getShouldBeIssued());
        assertNotNull(response.getData().getInternalCaseDocumentData().getUploadRemoveOrMoveDocument());
    }

}
