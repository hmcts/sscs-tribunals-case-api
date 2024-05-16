package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRole;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResource;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.WorkAllocationFields;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class WorkAllocationServiceTest {

    private WorkAllocationService handler;

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

        handler = new WorkAllocationService(caseAssignmentApi, idamService);
    }

    @Test
    public void givenNullResponse_setAssignedCaseRolesFieldToNull() {
        when(caseAssignmentApi.getUserRoles(anyString(), anyString(), anyList())).thenReturn(null);

        WorkAllocationFields response = handler.updateAssignedCaseRoles(caseDetails);

        assertNull(response.getAssignedCaseRoles());
    }

    @Test
    public void givenNoCaseRolesAllocated_setAssignedCaseRolesFieldToNull() {
        when(caseAssignmentApi.getUserRoles("USER-TOKEN", "SERVICE-TOKEN",
                Arrays.asList("1111222233334444"))).thenReturn(
                    CaseAssignmentUserRolesResource.builder().build());

        WorkAllocationFields response = handler.updateAssignedCaseRoles(caseDetails);

        assertNull(response.getAssignedCaseRoles());
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

        WorkAllocationFields response = handler.updateAssignedCaseRoles(caseDetails);

        assertEquals(
                Arrays.asList("tribunal-member-1", "appraiser-1"),
                response.getAssignedCaseRoles());
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

        WorkAllocationFields response = handler.updateAssignedCaseRoles(caseDetails);

        assertEquals(
                Arrays.asList("tribunal-member-1"),
                response.getPreviouslyAssignedCaseRoles());
        assertEquals(
                Arrays.asList("tribunal-member-2", "appraiser-1"),
                response.getAssignedCaseRoles());
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

        WorkAllocationFields response = handler.updateAssignedCaseRoles(caseDetails);

        assertEquals(
                Arrays.asList("tribunal-member-1", "appraiser-1", "tribunal-member-2"),
                response.getPreviouslyAssignedCaseRoles());
        assertEquals(
                Arrays.asList("appraiser-2"),
                response.getAssignedCaseRoles());
    }
}
