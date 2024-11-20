package uk.gov.hmcts.reform.sscs.ccd.presubmit.managewelshdocuments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.util.AddedDocumentsUtil;

@RunWith(JUnitParamsRunner.class)
public class ManageWelshDocumentsAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private ManageWelshDocumentsAboutToSubmitHandler handler = new ManageWelshDocumentsAboutToSubmitHandler(
            new AddedDocumentsUtil(true), true);

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void givenAnInvalidEvent_thenReturnFalse() {
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, createCallBack(EventType.VALID_APPEAL_CREATED, null, null)));
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, createCallBack(null, null)));
    }

    @Test
    public void givenNewDocumentAddedNoPreviousCaseData_thenSetUploadedWelshDocumentTypes() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .sscsWelshDocuments(Arrays.asList(
                        SscsWelshDocument.builder().id("111-111").value(SscsWelshDocumentDetails.builder().documentType("confidentialityRequest").build()).build()
                )).build();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, createCallBack(null, sscsCaseData), USER_AUTHORISATION);

        assertEquals(Arrays.asList("confidentialityRequest"), response.getData().getWorkAllocationFields().getUploadedWelshDocumentTypes());
    }

    @Test
    public void givenCaseHasNoExistingDocumentsNewDocumentAdded_thenSetUploadedWelshDocumentTypes() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .sscsWelshDocuments(Arrays.asList(
                        SscsWelshDocument.builder().id("111-111").value(SscsWelshDocumentDetails.builder().documentType("confidentialityRequest").build()).build()
                )).build();

        SscsCaseData sscsCaseDataBefore = SscsCaseData.builder().build();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, createCallBack(sscsCaseDataBefore, sscsCaseData), USER_AUTHORISATION);

        assertEquals(Arrays.asList("confidentialityRequest"), response.getData().getWorkAllocationFields().getUploadedWelshDocumentTypes());
    }

    @Test
    public void givenCaseHasExistingDocumentsNewDocumentAdded_thenSetUploadedWelshDocumentTypes() {
        SscsCaseData sscsCaseDataBefore =  SscsCaseData.builder()
                .sscsWelshDocuments(Arrays.asList(
                        SscsWelshDocument.builder().id("111-111").value(SscsWelshDocumentDetails.builder().documentType("other").build()).build()
                )).build();
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .sscsWelshDocuments(Arrays.asList(
                        SscsWelshDocument.builder().id("111-111").value(SscsWelshDocumentDetails.builder().documentType("other").build()).build(),
                        SscsWelshDocument.builder().id("222-222").value(SscsWelshDocumentDetails.builder().documentType("confidentialityRequest").build()).build()
                )).build();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, createCallBack(sscsCaseDataBefore, sscsCaseData), USER_AUTHORISATION);

        assertEquals(Arrays.asList("confidentialityRequest"), response.getData().getWorkAllocationFields().getUploadedWelshDocumentTypes());
    }

    @Test
    @Disabled
    public void givenCaseHasExistingDocumentsWithNullTypeWhenTypeSet_thenSetUploadedWelshDocumentTypes() {
        SscsCaseData sscsCaseDataBefore =  SscsCaseData.builder()
                .sscsWelshDocuments(Arrays.asList(
                        SscsWelshDocument.builder().id("111-111").value(SscsWelshDocumentDetails.builder().build()).build()
                )).build();
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .sscsWelshDocuments(Arrays.asList(
                        SscsWelshDocument.builder().id("111-111").value(SscsWelshDocumentDetails.builder().documentType("confidentialityRequest").build()).build()
                )).build();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, createCallBack(sscsCaseDataBefore, sscsCaseData), USER_AUTHORISATION);

        assertEquals(Arrays.asList("confidentialityRequest"), response.getData().getWorkAllocationFields().getUploadedWelshDocumentTypes());
    }

    @Test
    public void givenExistingDocumentChangedType_thenSetUploadedWelshDocumentTypes() {
        SscsCaseData sscsCaseDataBefore = SscsCaseData.builder()
            .sscsWelshDocuments(Arrays.asList(
                SscsWelshDocument.builder().id("111-111").value(SscsWelshDocumentDetails.builder().documentType("other").build()).build()
            )).build();

        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .sscsWelshDocuments(Arrays.asList(
                SscsWelshDocument.builder().id("111-111").value(SscsWelshDocumentDetails.builder().documentType("confidentialityRequest").build()).build()
            )).build();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, createCallBack(sscsCaseDataBefore, sscsCaseData), USER_AUTHORISATION);

        assertEquals(Arrays.asList("confidentialityRequest"), response.getData().getWorkAllocationFields().getUploadedWelshDocumentTypes());
    }

    private Callback<SscsCaseData> createCallBack(SscsCaseData sscsCaseDataBefore, SscsCaseData sscsCaseData) {
        return createCallBack(EventType.MANAGE_WELSH_DOCUMENTS, sscsCaseDataBefore, sscsCaseData);
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
