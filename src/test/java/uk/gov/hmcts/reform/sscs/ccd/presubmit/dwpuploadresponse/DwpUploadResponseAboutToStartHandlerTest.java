package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_UPLOAD_RESPONSE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
public class DwpUploadResponseAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private SscsCaseData sscsCaseData;

    private Callback<SscsCaseData> callback;

    private DwpUploadResponseAboutToStartHandler dwpUploadResponseAboutToStartHandler;

    @BeforeEach
    public void setUp() {
        sscsCaseData = SscsCaseData.builder()
                .createdInGapsFrom(READY_TO_LIST.getId())
                .dynamicDwpState(new DynamicList(""))
                .build();
        CaseDetails<SscsCaseData> caseDetails =
                new CaseDetails<>(1234L, "SSCS", WITH_DWP, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.empty(), DWP_UPLOAD_RESPONSE, false);
        dwpUploadResponseAboutToStartHandler = new DwpUploadResponseAboutToStartHandler();
    }

    @Test
    public void givenADwpUploadResponseEvent_thenReturnTrue() {
        assertTrue(dwpUploadResponseAboutToStartHandler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenSetToReadyForList_NoError() {
        callback.getCaseDetails().getCaseData().getWorkAllocationFields().setFtaResponseReviewRequired(NO);
        var response = dwpUploadResponseAboutToStartHandler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertTrue(response.getErrors().isEmpty());
        assertEquals(YES, response.getData().getWorkAllocationFields().getFtaResponseReviewRequired());
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

    @Test
    public void givenIbcaCaseSetIbcaOnlyBenefitAndIssueCodeFields_NoError() {
        sscsCaseData.setBenefitCode("093");
        sscsCaseData.setIssueCode("DD");

        PreSubmitCallbackResponse<SscsCaseData> response = dwpUploadResponseAboutToStartHandler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals("093", sscsCaseData.getBenefitCodeIbcaOnly());
        assertEquals("DD", sscsCaseData.getIssueCodeIbcaOnly());
    }

    private void containsErrorMessage(PreSubmitCallbackResponse<SscsCaseData> response) {
        for (String error : response.getErrors()) {
            assertEquals("This case cannot be updated by DWP", error);
        }
    }
}
