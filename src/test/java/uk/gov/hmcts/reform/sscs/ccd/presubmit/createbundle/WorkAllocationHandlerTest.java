package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRole;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResource;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.WorkAllocationFields;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class WorkAllocationHandlerTest {

    private PreSubmitCallbackHandler<SscsCaseData> handler;

    @Mock
    private IdamService idamService;

    @Mock
    private CaseAssignmentApi caseAssignmentApi;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder()
                .idamOauth2Token("USER-TOKEN")
                .serviceAuthorization("SERVICE-TOKEN")
                .build());
        when(callback.getEvent()).thenReturn(EventType.CREATE_BUNDLE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getId()).thenReturn(1111222233334444L);
        when(caseDetails.getCaseData()).thenReturn(new SscsCaseData());
    }

    @Test
    public void givenFeatureDisabled_thenReturnFalse() {
        handler = new WorkAllocationHandler(caseAssignmentApi, idamService, false);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        handler = new WorkAllocationHandler(caseAssignmentApi, idamService, true);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonCreateBundleEvent_thenReturnFalse() {
        handler = new WorkAllocationHandler(caseAssignmentApi, idamService, true);
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenNullResponse_setAssignedCaseRolesFieldToNull() {
        when(caseAssignmentApi.getUserRoles(anyString(), anyString(), anyList())).thenReturn(null);

        handler = new WorkAllocationHandler(caseAssignmentApi, idamService, true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, null);

        assertNull(response.getData().getWorkAllocationFields().getAssignedCaseRoles());
    }

    @Test
    public void givenNoCaseRolesAllocated_setAssignedCaseRolesFieldToNull() {
        when(caseAssignmentApi.getUserRoles("USER-TOKEN", "SERVICE-TOKEN",
                Arrays.asList("1111222233334444"))).thenReturn(
                    CaseAssignmentUserRolesResource.builder().build());

        handler = new WorkAllocationHandler(caseAssignmentApi, idamService, true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, null);

        assertNull(response.getData().getWorkAllocationFields().getAssignedCaseRoles());
    }

    @Test
    public void givenCaseRolesAllocated_setAssignedCaseRolesField() {
        when(caseAssignmentApi.getUserRoles("USER-TOKEN", "SERVICE-TOKEN",
                Arrays.asList("1111222233334444"))).thenReturn(
                    CaseAssignmentUserRolesResource.builder()
                            .caseAssignmentUserRoles(Arrays.asList(
                                    CaseAssignmentUserRole.builder().caseRole("tribunal-member-1").build(),
                                    CaseAssignmentUserRole.builder().caseRole("appraiser-1").build()))
                            .build());

        handler = new WorkAllocationHandler(caseAssignmentApi, idamService, true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, null);

        assertEquals(Arrays.asList("tribunal-member-1", "appraiser-1"),
                response.getData().getWorkAllocationFields().getAssignedCaseRoles());
    }

    @Test
    public void givenCreateBundleHasRun_setAssignedCaseRolesField() {
        when(caseAssignmentApi.getUserRoles("USER-TOKEN", "SERVICE-TOKEN",
                Arrays.asList("1111222233334444"))).thenReturn(
                CaseAssignmentUserRolesResource.builder()
                        .caseAssignmentUserRoles(Arrays.asList(
                                CaseAssignmentUserRole.builder().caseRole("tribunal-member-1").build(),
                                CaseAssignmentUserRole.builder().caseRole("tribunal-member-2").build(),
                                CaseAssignmentUserRole.builder().caseRole("appraiser-1").build()))
                        .build());

        when(caseDetails.getCaseData()).thenReturn(SscsCaseData.builder().workAllocationFields(
                WorkAllocationFields.builder().assignedCaseRoles(Arrays.asList("tribunal-member-1")).build()
        ).build());

        handler = new WorkAllocationHandler(caseAssignmentApi, idamService, true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, null);

        assertEquals(Arrays.asList("tribunal-member-1"),
                response.getData().getWorkAllocationFields().getPreviouslyAssignedCaseRoles());
        assertEquals(Arrays.asList("tribunal-member-2", "appraiser-1"),
                response.getData().getWorkAllocationFields().getAssignedCaseRoles());
    }

    @Test
    public void canBeRunMultipleTimes_setAssignedCaseRolesField() {
        when(caseAssignmentApi.getUserRoles("USER-TOKEN", "SERVICE-TOKEN",
                Arrays.asList("1111222233334444"))).thenReturn(
                CaseAssignmentUserRolesResource.builder()
                        .caseAssignmentUserRoles(Arrays.asList(
                                CaseAssignmentUserRole.builder().caseRole("tribunal-member-1").build(),
                                CaseAssignmentUserRole.builder().caseRole("tribunal-member-2").build(),
                                CaseAssignmentUserRole.builder().caseRole("appraiser-1").build(),
                                CaseAssignmentUserRole.builder().caseRole("appraiser-2").build()))
                        .build());

        when(caseDetails.getCaseData()).thenReturn(SscsCaseData.builder().workAllocationFields(
                WorkAllocationFields.builder()
                        .assignedCaseRoles(Arrays.asList("tribunal-member-2"))
                        .previouslyAssignedCaseRoles(Arrays.asList("tribunal-member-1", "appraiser-1"))
                        .build()
        ).build());

        handler = new WorkAllocationHandler(caseAssignmentApi, idamService, true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, null);

        assertEquals(Arrays.asList("tribunal-member-1", "appraiser-1", "tribunal-member-2"),
                response.getData().getWorkAllocationFields().getPreviouslyAssignedCaseRoles());
        assertEquals(Arrays.asList("appraiser-2"),
                response.getData().getWorkAllocationFields().getAssignedCaseRoles());
    }
}
