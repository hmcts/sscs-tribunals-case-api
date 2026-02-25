package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT_SEND_TO_INTERLOC;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

class NonCompliantSendToInterlocAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private NonCompliantSendToInterlocAboutToStartHandler handler;
    private Callback<SscsCaseData> callback;

    @BeforeEach
    void setUp() {
        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        CaseDetails<SscsCaseData> caseDetails =
                new CaseDetails<>(1234L, "SSCS", State.WITH_DWP, caseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), NON_COMPLIANT_SEND_TO_INTERLOC, false);
        handler = new NonCompliantSendToInterlocAboutToStartHandler(false);
    }

    @Test
    void canHandleOnlyAboutToStartForNonCompliantSendToInterloc() {
        assertEquals(true, handler.canHandle(ABOUT_TO_START, callback));
        assertFalse(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback));
    }

    @Test
    void setsFirstPartyAsDefaultWhenFlagOff() {
        var response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        DynamicList originalSender = response.getData().getOriginalSender();
        assertEquals("appellant", originalSender.getValue().getCode());
    }

    @Test
    void clearsDefaultSelectionWhenFlagOnForChildSupport() {
        handler = new NonCompliantSendToInterlocAboutToStartHandler(true);
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());

        var response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        DynamicListItem selectedValue = response.getData().getOriginalSender().getValue();
        assertEquals("", selectedValue.getCode());
    }

    @Test
    void throwsIfCannotHandle() {
        Callback<SscsCaseData> wrongCallback =
                new Callback<>(callback.getCaseDetails(), Optional.of(callback.getCaseDetails()), EventType.APPEAL_RECEIVED, false);
        assertThrows(IllegalStateException.class,
                () -> handler.handle(ABOUT_TO_START, wrongCallback, USER_AUTHORISATION));
    }
}
