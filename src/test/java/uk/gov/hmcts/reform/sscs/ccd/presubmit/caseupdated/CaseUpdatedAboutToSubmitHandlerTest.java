package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ASSOCIATE_CASE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@RunWith(JUnitParamsRunner.class)
public class CaseUpdatedAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private CaseUpdatedAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;
    @Mock
    private CcdService ccdService;
    @Mock
    private IdamService idamService;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new CaseUpdatedAboutToSubmitHandler(regionalProcessingCenterService, ccdService, idamService);

        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder()
                .appellant(Appellant.builder().address(Address.builder().postcode("CM120NS").build()).build()).build())
                .benefitCode("002")
                .issueCode("DD")
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
    }

    @Test
    public void givenANonCaseUpdatedEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenACaseUpdatedEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCaseUpdatedCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenACaseUpdatedCallbackType_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenACaseUpdatedEvent_thenSetCaseCode() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002DD", response.getData().getCaseCode());
    }

    @Test
    public void givenACaseUpdatedEventWithEmptyBenefitCodeAndCaseCode_thenDoNotOverrideCaseCode() {
        callback.getCaseDetails().getCaseData().setBenefitCode(null);
        callback.getCaseDetails().getCaseData().setIssueCode(null);
        callback.getCaseDetails().getCaseData().setCaseCode("002DD");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002DD", response.getData().getCaseCode());
    }

    @Test
    public void givenAnAppealWithPostcode_updateRpc() {
        when(regionalProcessingCenterService.getByPostcode("CM120NS")).thenReturn(RegionalProcessingCenter.builder().name("Region1").address1("Line1").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Line1", response.getData().getRegionalProcessingCenter().getAddress1());
        assertEquals("Region1", response.getData().getRegion());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenMultipleAssociatedCases_thenAddAllAssociatedCaseLinksToCase() {
        SscsCaseDetails matchingCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().build()).build();
        SscsCaseDetails matchingCase2 = SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase1);
        matchedByNinoCases.add(matchingCase2);

        SscsCaseData caseData = handler.addAssociatedCases(
                SscsCaseData.builder().ccdCaseId("00000000").build(),
                matchedByNinoCases);

        assertEquals(2, caseData.getAssociatedCase().size());
        assertEquals("Yes", caseData.getLinkedCasesBoolean());
        assertEquals("12345678", caseData.getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("56765676", caseData.getAssociatedCase().get(1).getValue().getCaseReference());
    }

    @Test
    public void getMatchedCases() {
        given(ccdService.findCaseBy(any(), any())).willReturn(Collections.singletonList(
                SscsCaseDetails.builder().id(12345678L).build()
        ));
        List<SscsCaseDetails> matchedCases = handler.getMatchedCases("ABCDEFG", idamService.getIdamTokens());

        assertEquals(1, matchedCases.size());
    }

    @Test
    public void addNoAssociatedCases() {
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();

        SscsCaseData caseData = handler.addAssociatedCases(
                SscsCaseData.builder().ccdCaseId("00000000").build(),
                matchedByNinoCases);

        assertNull(caseData.getAssociatedCase());
        assertEquals("No", caseData.getLinkedCasesBoolean());
        verify(ccdService, times(0)).updateCase(any(), any(), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());
    }
}
