package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class DwpUploadResponseAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private DwpUploadResponseAboutToStartHandler dwpUploadResponseAboutToStartHandler;
    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Before
    public void setUp() {
        openMocks(this);
        dwpUploadResponseAboutToStartHandler = new DwpUploadResponseAboutToStartHandler();
        sscsCaseData = SscsCaseData.builder()
                .createdInGapsFrom(READY_TO_LIST.getId())
                .dynamicDwpState(new DynamicList(""))
                .build();
        when(callback.getEvent()).thenReturn(EventType.DWP_UPLOAD_RESPONSE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenADwpUploadResponseEvent_thenReturnTrue() {
        assertTrue(dwpUploadResponseAboutToStartHandler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenSetToReadyForList_NoError() {
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToStartHandler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenDwpStateIsPostHearing_thenSetDynamicDwpStateValueToNull() {
        sscsCaseData.setDwpState(DwpState.CORRECTION_REFUSED);
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToStartHandler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertNull(response.getData().getDynamicDwpState().getValue());
    }

    @Test
    public void givenDwpStateIsWithdrawn_thenSetDynamicDwpStateValueToWithdrawn() {
        sscsCaseData.setDwpState(DwpState.WITHDRAWN);
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToStartHandler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(response.getData().getDynamicDwpState().getValue().getCode(), DwpState.WITHDRAWN.getCcdDefinition());
    }

    @Test
    public void givenSetToValidAppeal_ReturnsError() {
        callback.getCaseDetails().getCaseData().setCreatedInGapsFrom(VALID_APPEAL.getId());
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToStartHandler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        containsErrorMessage(response);
    }

    @Test
    public void givenSetToNull_ReturnsError() {
        callback.getCaseDetails().getCaseData().setCreatedInGapsFrom(null);
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToStartHandler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        containsErrorMessage(response);
    }

    @Test
    public void givenSetToEmptyString_ReturnsError() {
        callback.getCaseDetails().getCaseData().setCreatedInGapsFrom("");
        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToStartHandler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        containsErrorMessage(response);
    }

    private void containsErrorMessage(PreSubmitCallbackResponse<SscsCaseData> response) {
        for (String error : response.getErrors()) {
            assertEquals("This case cannot be updated by DWP", error);
        }
    }
}
