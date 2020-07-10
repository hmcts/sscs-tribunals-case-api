package uk.gov.hmcts.reform.sscs.ccd.presubmit.isScottish;

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

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;


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
        initMocks(this);
        handler = new IsScottishHandler();
    }

    @Test
    public void givenAHandleDwpLapseEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"Glasgow,Yes", "GLASGOW,Yes", "NotGlas,No"})
    public void setIsScottishCorrectly(@Nullable String dwpIssuingOffice, @Nullable String expectedIsScottish) {

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
}
