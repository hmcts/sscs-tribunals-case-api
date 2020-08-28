package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class DwpUploadResponseAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private DwpUploadResponseAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new DwpUploadResponseAboutToStartHandler();

        when(callback.getEvent()).thenReturn(EventType.DWP_UPLOAD_RESPONSE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().build())
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonDwpUploadResponseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenCaseHasPipUpperCaseBenefitType_thenPopulateDwpUploadResponseDynamicBenefitTypeWithPip() {

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("PIP").build());

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType());
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue());
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems());
        assertEquals(1, sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().size());

        assertNotNull("pip", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue().getCode());
        assertNotNull("pip", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().get(0).getCode());

        assertNotNull("PIP", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue().getLabel());
        assertNotNull("PIP", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().get(0).getLabel());

    }

    @Test
    public void givenCaseHasPipLowerCaseBenefitType_thenPopulateDwpUploadResponseDynamicBenefitTypeWithPip() {

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("pip").build());

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType());
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue());
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems());
        assertEquals(1, sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().size());

        assertNotNull("pip", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue().getCode());
        assertNotNull("pip", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().get(0).getCode());

        assertNotNull("PIP", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue().getLabel());
        assertNotNull("PIP", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().get(0).getLabel());

    }

    @Test
    public void givenCaseHasEsaUpperCaseBenefitType_thenPopulateDwpUploadResponseDynamicBenefitTypeWithEsa() {

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("ESA").build());

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType());
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue());
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems());
        assertEquals(1, sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().size());

        assertNotNull("esa", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue().getCode());
        assertNotNull("esa", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().get(0).getCode());

        assertNotNull("ESA", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue().getLabel());
        assertNotNull("ESA", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().get(0).getLabel());

    }

    @Test
    public void givenCaseHasEsaLowerCaseBenefitType_thenPopulateDwpUploadResponseDynamicBenefitTypeWithEsa() {

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("esa").build());

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType());
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue());
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems());
        assertEquals(1, sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().size());

        assertNotNull("esa", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue().getCode());
        assertNotNull("esa", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().get(0).getCode());

        assertNotNull("ESA", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue().getLabel());
        assertNotNull("ESA", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().get(0).getLabel());

    }

    @Test
    public void givenCaseHasUcUpperCaseBenefitType_thenPopulateDwpUploadResponseDynamicBenefitTypeWithUc() {

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("UC").build());

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType());
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue());
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems());
        assertEquals(1, sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().size());

        assertNotNull("uc", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue().getCode());
        assertNotNull("uc", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().get(0).getCode());

        assertNotNull("UC", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue().getLabel());
        assertNotNull("UC", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().get(0).getLabel());

    }

    @Test
    public void givenCaseHasUcLowerCaseBenefitType_thenPopulateDwpUploadResponseDynamicBenefitTypeWithUc() {

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("uc").build());

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType());
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue());
        assertNotNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems());
        assertEquals(1, sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().size());

        assertNotNull("uc", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue().getCode());
        assertNotNull("uc", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().get(0).getCode());

        assertNotNull("UC", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getValue().getLabel());
        assertNotNull("UC", sscsCaseData.getDwpUploadResponseDynamicBenefitType().getListItems().get(0).getLabel());

    }

    @Test
    public void givenCaseHasBenefitTypeWithUnknownCode_thenDoNotPopulateDwpUploadResponseDynamicBenefitType() {

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("unknown").build());

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType());
    }

    @Test
    public void givenCaseHasBenefitTypeWithNoCode_thenDoNotPopulateDwpUploadResponseDynamicBenefitType() {

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().build());

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType());
    }

    @Test
    public void givenCaseHasNoBenefitType_thenDoNotPopulateDwpUploadResponseDynamicBenefitType() {
        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType());
    }

    @Test
    public void givenCaseHasNoAppeal_thenDoNotPopulateDwpUploadResponseDynamicBenefitType() {
        sscsCaseData.setAppeal(null);
        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertNull(sscsCaseData.getDwpUploadResponseDynamicBenefitType());
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
