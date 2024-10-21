package uk.gov.hmcts.reform.sscs.ccd.presubmit.validappeal;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.validappeal.SyaAppealValidatorTest.PROCESSING_VENUE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.validappeal.SyaAppealValidatorTest.REGION_ID;

import java.time.LocalDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseEvent;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseManagementLocation;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.validation.helper.AppealPostcodeHelper;
import uk.gov.hmcts.reform.sscs.ccd.validation.helper.SscsDataHelper;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.CaseManagementLocationService;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;

@RunWith(JUnitParamsRunner.class)
public class ValidateAppealAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String EPIMMS_ID = "1";


    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData bulkScanResponseData;
    private PreSubmitCallbackResponse<SscsCaseData> response;
    @Mock
    private DwpAddressLookupService dwpAddressLookupService;
    @Mock
    private CaseManagementLocationService caseManagementLocationService;
    @Mock
    private AirLookupService airLookupService;
    @Mock
    private AppealPostcodeHelper appealPostcodeHelper;
    @Mock
    private SyaAppealValidator appealValidator;
    private ValidateAppealAboutToSubmitHandler handler;

    @Before
    public void setUp() {
        openMocks(this);

        caseDetails = new CaseDetails<>(123L, "sscs",
                State.INTERLOCUTORY_REVIEW_STATE, SscsCaseData.builder().build(), LocalDateTime.now(), "Benefit");
        callback = new Callback<>(caseDetails, empty(), EventType.VALID_APPEAL, false);

        given(dwpAddressLookupService.getDwpRegionalCenterByBenefitTypeAndOffice("PIP", "3")).willReturn("Springburn");
        given(dwpAddressLookupService.getDwpRegionalCenterByBenefitTypeAndOffice("ESA", "Balham DRT")).willReturn("Balham");
        given(airLookupService.lookupAirVenueNameByPostCode(anyString(), any(BenefitType.class))).willReturn(PROCESSING_VENUE);
        given(caseManagementLocationService.retrieveCaseManagementLocation(eq(PROCESSING_VENUE), any())).willReturn(
                Optional.of(CaseManagementLocation.builder().baseLocation(EPIMMS_ID).region(REGION_ID).build()));

        SscsDataHelper sscsDataHelper =
                new SscsDataHelper(
                        new CaseEvent(null, "validAppealCreated", null, null),
                        airLookupService,
                        dwpAddressLookupService);
        handler = new ValidateAppealAboutToSubmitHandler(appealValidator, appealPostcodeHelper, sscsDataHelper, dwpAddressLookupService, caseManagementLocationService);
    }

    @Test
    public void givenANonValidAppealCaseEvent_thenReturnFalse() {
        callback = new Callback<>(caseDetails, empty(), EventType.APPEAL_RECEIVED, false);

        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        callback = new Callback<>(caseDetails, empty(), EventType.APPEAL_RECEIVED, false);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenValidateAppealForPreValidCase_thenSetNonDigitalToDigitalCase() {
        caseDetails = new CaseDetails<>(123L, "sscs",
                State.INCOMPLETE_APPLICATION, SscsCaseData.builder().build(), LocalDateTime.now(), "Benefit");
        callback = new Callback<>(caseDetails, empty(), EventType.VALID_APPEAL, false);
        bulkScanResponseData = SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build();
        response = new PreSubmitCallbackResponse<>(bulkScanResponseData);
        when(appealValidator.validateAppeal(eq(caseDetails), anyMap(), eq(false))).thenReturn(response);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getCreatedInGapsFrom(), is(READY_TO_LIST.getId()));
    }

    @Test
    public void givenValidateAppealForPreValidCase_thenSetNoDigitalToDigitalCase() {
        caseDetails = new CaseDetails<>(123L, "sscs",
                State.INCOMPLETE_APPLICATION,
                SscsCaseData.builder().createdInGapsFrom(VALID_APPEAL.getId()).build(), LocalDateTime.now(),
                "Benefit");
        callback = new Callback<>(caseDetails, empty(), EventType.VALID_APPEAL, false);
        bulkScanResponseData = SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build();
        response = new PreSubmitCallbackResponse<>(bulkScanResponseData);
        when(appealValidator.validateAppeal(eq(caseDetails), anyMap(), eq(false))).thenReturn(response);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getCreatedInGapsFrom(), is(READY_TO_LIST.getId()));
    }

    @Test
    public void givenValidateAppealForPreValidCase_thenSetDwpRegionalCenterToCase() {
        caseDetails = new CaseDetails<>(123L, "sscs",
                State.INCOMPLETE_APPLICATION, SscsCaseData.builder().build(), LocalDateTime.now(), "Benefit");
        callback = new Callback<>(caseDetails, empty(), EventType.VALID_APPEAL, false);
        bulkScanResponseData = SscsCaseData.builder().dwpRegionalCentre("PIP Newcastle").build();
        response = new PreSubmitCallbackResponse<>(bulkScanResponseData);
        when(appealValidator.validateAppeal(eq(caseDetails), anyMap(), eq(false))).thenReturn(response);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("PIP Newcastle", response.getData().getDwpRegionalCentre());
    }
}
