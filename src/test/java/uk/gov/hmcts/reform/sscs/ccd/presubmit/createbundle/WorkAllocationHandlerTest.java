package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;

public class WorkAllocationHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private PreSubmitCallbackHandler<SscsCaseData> handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @BeforeEach
    public void setUp() {
        openMocks(this);

        handler = new WorkAllocationHandler(new SessionCategoryMapService());

        when(callback.getEvent()).thenReturn(EventType.CREATE_BUNDLE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseTypeId()).thenReturn("Benefit");
        when(caseDetails.getJurisdiction()).thenReturn("SSCS");
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonCreateBundleEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    static Stream<Arguments> sessionCategoryScenarioProvider() {
        return Stream.of(
                Arguments.of("001", "US", null, null, 1),
                Arguments.of("026", "RB", YesNo.YES, null, 2),
                Arguments.of("037", "EX", null, null, 3),
                Arguments.of("051", "SG", null, null, 4),
                Arguments.of("031", "DQ", null, null, 5),
                Arguments.of("014", "DD", null, "Dr XYZ", 6)
        );
    }

    @ParameterizedTest(name = "event id: {0} post event state: {1} appeal type: {2}")
    @MethodSource("sessionCategoryScenarioProvider")
    void shouldSetSessionCategory(String benefitCode, String issueCode, YesNo isFqpmRequired, String secondPanelDoctorSpecialism, Integer expectedSessionCategory) {
        SscsCaseData.SscsCaseDataBuilder caseBuilder = SscsCaseData.builder()
                .benefitCode(benefitCode)
                .issueCode(issueCode)
                .isFqpmRequired(isFqpmRequired);

        if (secondPanelDoctorSpecialism != null) {
            caseBuilder.sscsIndustrialInjuriesData(
                    SscsIndustrialInjuriesData.builder()
                            .secondPanelDoctorSpecialism(secondPanelDoctorSpecialism)
                            .build());
        }

        when(caseDetails.getCaseData()).thenReturn(caseBuilder.build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(expectedSessionCategory, response.getData().getWorkAllocationFields().getSessionCategory());
    }

    static Stream<Arguments> panelCountScenarioProvider() {
        return Stream.of(
                Arguments.of(null, null, null, null),
                Arguments.of("JUDGE", null, null, 1),
                Arguments.of("JUDGE", "Member", null, 2),
                Arguments.of("JUDGE", "Member", "Member", 3)
        );
    }

    @ParameterizedTest(name = "event id: {0} post event state: {1} appeal type: {2}")
    @MethodSource("panelCountScenarioProvider")
    void shouldSetPanelCount(String assignedTo, String medicalMember, String disabilityQualifiedMember, Integer expectedCount) {
        SscsCaseData caseData = SscsCaseData.builder()
                .panel(Panel.builder()
                        .assignedTo(assignedTo)
                        .medicalMember(medicalMember)
                        .disabilityQualifiedMember(disabilityQualifiedMember)
                        .build())
                .build();
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(expectedCount, response.getData().getWorkAllocationFields().getPanelCount());
    }
}
