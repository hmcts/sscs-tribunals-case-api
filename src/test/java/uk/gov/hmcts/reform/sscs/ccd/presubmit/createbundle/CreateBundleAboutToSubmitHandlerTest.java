package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.bundling.BundlingHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.WorkAllocationFields;

@RunWith(JUnitParamsRunner.class)
public class CreateBundleAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private CreateBundleAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private BundlingHandler bundlingHandler;

    @Mock
    private WorkAllocationService workAllocationService;

    private SscsCaseData sscsCaseData;

    private PreSubmitCallbackResponse<SscsCaseData> bundlingResponse;

    private WorkAllocationFields workAllocationFields;

    @Before
    public void setUp() {
        openMocks(this);
        sscsCaseData = SscsCaseData.builder().build();
        bundlingResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        workAllocationFields = WorkAllocationFields.builder().assignedCaseRoles(Arrays.asList("role")).build();

        when(callback.getEvent()).thenReturn(EventType.CREATE_BUNDLE);
        when(bundlingHandler.handle(any())).thenReturn(bundlingResponse);
        when(workAllocationService.updateAssignedCaseRoles(any())).thenReturn(workAllocationFields);

        handler = new CreateBundleAboutToSubmitHandler(bundlingHandler, workAllocationService, true);
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    public void givenANonCreateBundleEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    public void shouldHandleBundling() {
        when(bundlingHandler.handle(any())).thenReturn(bundlingResponse);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response).isEqualTo(bundlingResponse);
    }

    @Test
    public void shouldUpdateWorkAllocationFields() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getWorkAllocationFields()).isEqualTo(workAllocationFields);
    }

    @Test
    public void shouldNotUpdateWorkAllocationFields_whenFeatureOff() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        handler = new CreateBundleAboutToSubmitHandler(bundlingHandler, workAllocationService, false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getWorkAllocationFields().getAssignedCaseRoles()).isNull();
        assertThat(response.getData().getWorkAllocationFields().getPreviouslyAssignedCaseRoles()).isNull();
    }
}
