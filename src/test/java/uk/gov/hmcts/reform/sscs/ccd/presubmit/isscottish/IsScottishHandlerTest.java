package uk.gov.hmcts.reform.sscs.ccd.presubmit.isscottish;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class IsScottishHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private IsScottishHandler handler;
    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new IsScottishHandler();
    }

    @Test
    public void givenAHandledEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL_CREATED);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenNonAHandledEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.ADD_NOTE);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"Glasgow,Yes", "GLASGOW,Yes", "NotGlas,No"})
    public void setIsScottishCorrectlyWhenExistingIsNull(@Nullable String dwpIssuingOffice, @Nullable String expectedIsScottish) {

        RegionalProcessingCenter regionalProcessingCenter = getRegionalProcessingCenter();

        RegionalProcessingCenter updatedRpc = regionalProcessingCenter.toBuilder().name(dwpIssuingOffice).build();

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .regionalProcessingCenter(updatedRpc)
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData = sscsCaseData.toBuilder().state(State.WITH_DWP).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(expectedIsScottish, response.getData().getIsScottishCase());
    }

    @Test
    public void setIsScottishCorrectlyWhenNullRpc() {

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .regionalProcessingCenter(null)
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData = sscsCaseData.toBuilder().state(State.WITH_DWP).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("No", response.getData().getIsScottishCase());
    }

    @Test
    @Parameters({"Glasgow,Yes,No", "Glasgow,Yes,Yes", "NotGlas,No,No", "NotGlas,No,Yes"})
    public void changeIsScottishCorrectlyWhenAlreadySet(@Nullable String dwpIssuingOffice,
                                                                             @Nullable String expectedIsScottish,
                                                                             @Nullable String existingIsScottish) {

        RegionalProcessingCenter regionalProcessingCenter = getRegionalProcessingCenter();

        RegionalProcessingCenter updatedRpc = regionalProcessingCenter.toBuilder().name(dwpIssuingOffice).build();

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .isScottishCase(existingIsScottish)
                .regionalProcessingCenter(updatedRpc)
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData = sscsCaseData.toBuilder().state(State.WITH_DWP).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(expectedIsScottish, response.getData().getIsScottishCase());
    }
}
