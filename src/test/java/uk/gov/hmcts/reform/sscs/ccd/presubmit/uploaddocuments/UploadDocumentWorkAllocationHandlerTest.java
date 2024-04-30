package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.util.AddedDocumentsUtil;

@RunWith(JUnitParamsRunner.class)
public class UploadDocumentWorkAllocationHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private UploadDocumentWorkAllocationHandler handler = new UploadDocumentWorkAllocationHandler(
            new AddedDocumentsUtil(true), true);

    @Before
    public void setUp() {
    }

    @Test
    public void givenAnInvalidEvent_thenReturnFalse() {
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, createCallBack(EventType.VALID_APPEAL_CREATED, null, null)));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, createCallBack(null, null)));
    }

    @Test
    public void givenNewDocumentAddedNoPreviousCaseData_thenSetScannedDocumentTypes() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .sscsDocument(Arrays.asList(
                        SscsDocument.builder().id("111-111").value(SscsDocumentDetails.builder().documentType("confidentialityRequest").build()).build()
                )).build();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, createCallBack(null, sscsCaseData), USER_AUTHORISATION);

        assertEquals(Arrays.asList("confidentialityRequest"), response.getData().getWorkAllocationFields().getScannedDocumentTypes());
    }

    @Test
    public void givenCaseHasNoExistingDocumentsNewDocumentAdded_thenSetScannedDocumentTypes() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .sscsDocument(Arrays.asList(
                        SscsDocument.builder().id("111-111").value(SscsDocumentDetails.builder().documentType("confidentialityRequest").build()).build()
                )).build();

        SscsCaseData sscsCaseDataBefore = SscsCaseData.builder().build();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, createCallBack(sscsCaseDataBefore, sscsCaseData), USER_AUTHORISATION);

        assertEquals(Arrays.asList("confidentialityRequest"), response.getData().getWorkAllocationFields().getScannedDocumentTypes());
    }

    @Test
    public void givenCaseHasExistingDocumentsNewDocumentAdded_thenSetScannedDocumentTypes() {
        SscsCaseData sscsCaseDataBefore =  SscsCaseData.builder()
                .sscsDocument(Arrays.asList(
                        SscsDocument.builder().id("111-111").value(SscsDocumentDetails.builder().documentType("other").build()).build()
                )).build();
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .sscsDocument(Arrays.asList(
                        SscsDocument.builder().id("111-111").value(SscsDocumentDetails.builder().documentType("other").build()).build(),
                        SscsDocument.builder().id("222-222").value(SscsDocumentDetails.builder().documentType("confidentialityRequest").build()).build()
                )).build();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, createCallBack(sscsCaseDataBefore, sscsCaseData), USER_AUTHORISATION);

        assertEquals(Arrays.asList("confidentialityRequest"), response.getData().getWorkAllocationFields().getScannedDocumentTypes());
    }

    @Test
    public void givenExistingDocumentChangedType_thenSetScannedDocumentTypes() {
        SscsCaseData sscsCaseDataBefore = SscsCaseData.builder()
                .sscsDocument(Arrays.asList(
                        SscsDocument.builder().id("111-111").value(SscsDocumentDetails.builder().documentType("other").build()).build()
                )).build();

        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .sscsDocument(Arrays.asList(
                        SscsDocument.builder().id("111-111").value(SscsDocumentDetails.builder().documentType("confidentialityRequest").build()).build()
                )).build();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, createCallBack(sscsCaseDataBefore, sscsCaseData), USER_AUTHORISATION);

        assertEquals(Arrays.asList("confidentialityRequest"), response.getData().getWorkAllocationFields().getScannedDocumentTypes());
    }

    @Test
    public void givenDocumentWithNullTypeAdded_thenSetScannedDocumentTypes() {
        SscsCaseData sscsCaseDataBefore = SscsCaseData.builder().build();

        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .sscsDocument(Arrays.asList(
                        SscsDocument.builder().id("111-111").value(SscsDocumentDetails.builder().build()).build()
                )).build();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, createCallBack(sscsCaseDataBefore, sscsCaseData), USER_AUTHORISATION);

        assertEquals(new ArrayList<String>(), response.getData().getWorkAllocationFields().getScannedDocumentTypes());
    }

    private Callback<SscsCaseData> createCallBack(SscsCaseData sscsCaseDataBefore, SscsCaseData sscsCaseData) {
        return createCallBack(EventType.UPLOAD_DOCUMENT, sscsCaseDataBefore, sscsCaseData);
    }

    private Callback<SscsCaseData> createCallBack(EventType event, SscsCaseData sscsCaseDataBefore, SscsCaseData sscsCaseData) {
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
                State.VALID_APPEAL, sscsCaseData, LocalDateTime.now(), "Benefit");

        if (sscsCaseDataBefore != null) {
            CaseDetails<SscsCaseData> caseDetailsBefore = new CaseDetails<>(123L, "sscs",
                    State.VALID_APPEAL, sscsCaseDataBefore, LocalDateTime.now(), "Benefit");

            return new Callback<>(caseDetails, Optional.of(caseDetailsBefore), event, false);
        }

        return new Callback<>(caseDetails, Optional.empty(), event, false);
    }
}
