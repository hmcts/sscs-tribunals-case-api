package uk.gov.hmcts.reform.sscs.ccd.presubmit.addhearing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.model.PoDetails;

@RunWith(JUnitParamsRunner.class)
public class AddHearingAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private AddHearingAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new AddHearingAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.ADD_HEARING);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonAddHearingEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAddHearingEventWithValidTime_thenDontThrowError() {
        sscsCaseData.setPoAttendanceConfirmed(YesNo.YES);
        sscsCaseData.setPresentingOfficersDetails(PoDetails.builder().name(Name.builder().firstName("bane").build())
                .contact(Contact.builder().email("emails").build()).build());

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(sscsCaseData.getPoAttendanceConfirmed(), YesNo.NO);
        assertNull(sscsCaseData.getPresentingOfficersDetails().getName().getFirstName());
        assertNull(sscsCaseData.getPresentingOfficersDetails().getContact().getEmail());
    }
}
