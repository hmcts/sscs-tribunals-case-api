package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.fenoaction;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FE_RECEIVED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.BaseHandlerTest;

@RunWith(JUnitParamsRunner.class)
class FeNoActionAboutToStartHandlerTest extends BaseHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private FeNoActionAboutToStartHandler handler = new FeNoActionAboutToStartHandler();
    private static final String FE_NO_ACTION_CALLBACK_JSON = "fenoaction/feNoActionAboutToStartCallback.json";
    @Mock private Callback<SscsCaseData> mockCallback;
    @Mock private CaseDetails<SscsCaseData> mockCaseDetails;
    @Mock private SscsCaseData mockCaseData;

    @Test
    @Parameters({
        "ABOUT_TO_START,FE_NO_ACTION,withDwp,true",
        "ABOUT_TO_SUBMIT,FE_NO_ACTION,withDwp,false",
        "ABOUT_TO_START,FE_NO_ACTION,appealCreated,true",
        "ABOUT_TO_START,APPEAL_RECEIVED,withDwp,false",
        "null,FE_NO_ACTION,withDwp,false",
        "ABOUT_TO_SUBMIT,null,withDwp,false"
    })
    public void canHandle(@Nullable CallbackType callbackType, @Nullable EventType eventType, String state,
                          boolean expectedResult) throws IOException {
        boolean actualResult = handler.canHandle(callbackType, buildTestCallbackGivenData(eventType, state,
            "appellantEvidence","", FE_NO_ACTION_CALLBACK_JSON));

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void handle() throws IOException {
        PreSubmitCallbackResponse<SscsCaseData> actualCaseData = handler.handle(CallbackType.ABOUT_TO_START,
            buildTestCallbackGivenData(EventType.FE_NO_ACTION, State.WITH_DWP.getId(),
                "appellantEvidence", "", FE_NO_ACTION_CALLBACK_JSON), USER_AUTHORISATION);

        String expectedCaseData = fetchData("fenoaction/expectedFeNoActionAboutToStartCallbackResponse.json");
        assertThatJson(actualCaseData)
            .whenIgnoringPaths(
                "data.jointPartyId",
                "data.appeal.appellant.appointee.id",
                "data.appeal.appellant.id",
                "data.appeal.rep.id",
                "data.sscsDocument[0].id")
            .when(Option.TREATING_NULL_AS_ABSENT)
            .isEqualTo(expectedCaseData);
        assertEquals(FE_RECEIVED, actualCaseData.getData().getDwpState());

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(DwpState.FE_ACTIONED_NR.getCcdDefinition(), DwpState.FE_ACTIONED_NR.getDescription()));
        listOptions.add(new DynamicListItem(DwpState.FE_ACTIONED_NA.getCcdDefinition(), DwpState.FE_ACTIONED_NA.getDescription()));
        assertEquals(listOptions, actualCaseData.getData().getDwpStateFeNoAction().getListItems());
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({
        "ABOUT_TO_SUBMIT,FE_NO_ACTION,withDwp",
        "ABOUT_TO_START,UPLOAD_DOCUMENT,appealCreated",
        "ABOUT_TO_SUBMIT,null,withDwp",
        "null,UPLOAD_DOCUMENT,withDwp"
    })
    public void handleCornerCaseScenarios(@Nullable CallbackType callbackType, @Nullable EventType eventType,
                                          @Nullable String state)
        throws IOException {
        handler.handle(callbackType, buildTestCallbackGivenData(eventType, state,
            "appellantEvidence", "", FE_NO_ACTION_CALLBACK_JSON), USER_AUTHORISATION);
    }

    @ParameterizedTest
    @EnumSource(value = DwpState.class, mode = EnumSource.Mode.EXCLUDE, names = {"FE_RECEIVED"})
    void returns_dwp_error_for_not_FE_received_non_ibca(DwpState dwpState) {
        MockitoAnnotations.openMocks(this);
        when(mockCallback.getEvent()).thenReturn(EventType.FE_NO_ACTION);
        when(mockCallback.getCaseDetails()).thenReturn(mockCaseDetails);
        when(mockCaseDetails.getCaseData()).thenReturn(mockCaseData);
        when(mockCaseData.getDwpState()).thenReturn(dwpState);
        when(mockCaseData.getBenefitCode()).thenReturn("000");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START, mockCallback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains("The dwp state value has to be 'FE received' in order to run this event"));
    }

    @ParameterizedTest
    @EnumSource(value = DwpState.class, mode = EnumSource.Mode.EXCLUDE, names = {"FE_RECEIVED"})
    void returns_fta_error_for_not_FE_received_ibca(DwpState dwpState) {
        MockitoAnnotations.openMocks(this);
        when(mockCallback.getEvent()).thenReturn(EventType.FE_NO_ACTION);
        when(mockCallback.getCaseDetails()).thenReturn(mockCaseDetails);
        when(mockCaseDetails.getCaseData()).thenReturn(mockCaseData);
        when(mockCaseData.getDwpState()).thenReturn(dwpState);
        when(mockCaseData.isIbcCase()).thenReturn(true);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_START, mockCallback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains("The FTA state value has to be 'FE received' in order to run this event"));
    }
}
