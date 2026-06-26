package uk.gov.hmcts.reform.sscs.ccd.presubmit.amendspecialism;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.AMEND_SPECIALISM;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;

@ExtendWith(MockitoExtension.class)
class AmendSpecialismAboutToSubmitHandlerTest {

    @Mock
    private PanelCompositionService panelCompositionService;

    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData caseData;

    private AmendSpecialismAboutToSubmitHandler handler;

    @BeforeEach
    public void setUp() {
        caseData = SscsCaseData.builder().ccdCaseId("ccdId").build();
        caseDetails =
                new CaseDetails<>(1234L, "SSCS", VALID_APPEAL, caseData, now(), "Benefit");
        handler = new AmendSpecialismAboutToSubmitHandler(panelCompositionService);
    }

    @Test
    public void givenAValidAboutToSubmitEvent_thenCanHandleReturnsTrue() {
        var callback = new Callback<>(caseDetails, empty(), AMEND_SPECIALISM, false);

        assertTrue(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAWrongEvent_thenCanHandleReturnsFalse() {
        var callback = new Callback<>(caseDetails, empty(), READY_TO_LIST, false);

        assertFalse(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void shouldResetPanelComposition() {
        var callback = new Callback<>(caseDetails, Optional.of(caseDetails), AMEND_SPECIALISM, false);
        var panelComposition = new PanelMemberComposition(List.of("84"));
        when(panelCompositionService.resetPanelCompositionIfStale(eq(caseData), eq(Optional.of(caseDetails))))
                .thenReturn(panelComposition);

        var response =
                handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, "Bearer token");

        assertNotNull(response);
        assertEquals(panelComposition, response.getData().getPanelMemberComposition());
    }
}
