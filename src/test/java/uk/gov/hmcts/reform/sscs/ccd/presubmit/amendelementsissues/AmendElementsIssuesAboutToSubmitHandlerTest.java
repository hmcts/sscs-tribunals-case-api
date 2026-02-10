package uk.gov.hmcts.reform.sscs.ccd.presubmit.amendelementsissues;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.AMEND_ELEMENTS_ISSUES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.amendelementsissues.AmendElementsIssuesAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;

@ExtendWith(MockitoExtension.class)
public class AmendElementsIssuesAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private PanelCompositionService panelCompositionService;

    private SscsCaseData sscsCaseData;
    private CaseDetails<SscsCaseData> caseDetails;
    private Callback<SscsCaseData> callback;

    private AmendElementsIssuesAboutToSubmitHandler handler;

    @BeforeEach
    public void setUp() {
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("UC").build()).build()).build();
        caseDetails =
                new CaseDetails<>(1234L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, empty(), AMEND_ELEMENTS_ISSUES, false);

        handler = new AmendElementsIssuesAboutToSubmitHandler(panelCompositionService);
    }

    @Test
    public void givenANonAmendElementsIssuesEvent_thenReturnFalse() {
        callback = new Callback<>(caseDetails, empty(), APPEAL_RECEIVED, false);

        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @ParameterizedTest
    @CsvSource({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenAUcCaseWithSingleElementSelected_thenSetCaseCodeToUs() {
        List<String> elementList = new ArrayList<>();
        elementList.add("testElement");
        sscsCaseData.setElementsDisputedList(elementList);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("US", response.getData().getIssueCode());
        assertEquals("001", response.getData().getBenefitCode());
        assertEquals("001US", response.getData().getCaseCode());
    }

    @Test
    public void givenUcCaseWithMultipleElementSelected_thenSetCaseCodeToUm() {
        List<String> elementList = new ArrayList<>();
        elementList.add("testElement");
        elementList.add("testElement2");
        sscsCaseData.setElementsDisputedList(elementList);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("UM", response.getData().getIssueCode());
        assertEquals("001", response.getData().getBenefitCode());
        assertEquals("001UM", response.getData().getCaseCode());
        assertNull(response.getData().getPanelMemberComposition());
    }

    @Test
    public void givenCaseDetailsBeforeExist_thenSetPanelComposition() {
        var panelComposition = new PanelMemberComposition(List.of("84"));
        when(panelCompositionService.resetPanelCompIfElementsChanged(eq(sscsCaseData), eq(Optional.of(caseDetails))))
                .thenReturn(panelComposition);
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), AMEND_ELEMENTS_ISSUES, false);
        handler = new AmendElementsIssuesAboutToSubmitHandler(panelCompositionService);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(panelComposition, response.getData().getPanelMemberComposition());
    }

    @Test
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        callback = new Callback<>(caseDetails, empty(), APPEAL_RECEIVED, false);

        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }
}
