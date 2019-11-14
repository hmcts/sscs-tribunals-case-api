package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import java.util.Collections;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;

@RunWith(JUnitParamsRunner.class)
public class HmctsResponseReviewedAboutToStartTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private HmctsResponseReviewedAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private DwpAddressLookupService dwpAddressLookupService;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        initMocks(this);
        dwpAddressLookupService = new DwpAddressLookupService();
        handler = new HmctsResponseReviewedAboutToStartHandler(dwpAddressLookupService);

        when(callback.getEvent()).thenReturn(EventType.HMCTS_RESPONSE_REVIEWED);

        sscsCaseData = SscsCaseData.builder().createdInGapsFrom("readyToList").appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"HMCTS_RESPONSE_REVIEWED"})
    public void givenAValidEvent_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenANonResponseReviewedEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void populateOriginatingAndPresentingOfficeDropdownsWhenHandlerFires_withCorrectSelectedOffice() {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("PIP").build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("DWP PIP (3)", response.getData().getDwpOriginatingOffice().getValue().getCode());
        assertEquals("DWP PIP (3)", response.getData().getDwpPresentingOffice().getValue().getCode());
    }

    @Test
    public void givenMrnIsNull_populateOriginatingAndPresentingOfficeDropdownsWhenHandlerFires_withNoDefaultedSelectedOffice() {
        callback.getCaseDetails().getCaseData().getAppeal().setMrnDetails(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertNull(response.getData().getDwpOriginatingOffice().getValue().getCode());
        assertNull(response.getData().getDwpPresentingOffice().getValue().getCode());
    }

    @Test
    public void givenOriginatingAndPresentingOfficeHavePreviouslyBeenSet_thenDefaultToTheseOfficesAndNotTheOneSetInMrn() {
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("PIP").build());

        DynamicListItem value = new DynamicListItem("DWP PIP (4)", "DWP PIP (4)");
        DynamicList originatingOfficeList = new DynamicList(value, Collections.singletonList(value));

        DynamicListItem value2 = new DynamicListItem("DWP PIP (5)", "DWP PIP (5)");
        DynamicList presentingOfficeList = new DynamicList(value2, Collections.singletonList(value2));

        callback.getCaseDetails().getCaseData().setDwpOriginatingOffice(originatingOfficeList);
        callback.getCaseDetails().getCaseData().setDwpPresentingOffice(presentingOfficeList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("DWP PIP (4)", response.getData().getDwpOriginatingOffice().getValue().getCode());
        assertEquals("DWP PIP (5)", response.getData().getDwpPresentingOffice().getValue().getCode());
    }

    @Test
    public void defaultTheDwpOptionsToNo() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("No", response.getData().getDwpIsOfficerAttending());
        assertEquals("No", response.getData().getDwpUcb());
        assertEquals("No", response.getData().getDwpPhme());
        assertEquals("No", response.getData().getDwpComplexAppeal());
    }

    @Test
    public void givenHmctsResponseReviewedEventIsTriggeredNonDigitalCase_thenDisplayError() {
        callback.getCaseDetails().getCaseData().setCreatedInGapsFrom("validAppeal");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("This event cannot be run for cases created in GAPS at valid appeal", response.getErrors().toArray()[0]);
    }

}