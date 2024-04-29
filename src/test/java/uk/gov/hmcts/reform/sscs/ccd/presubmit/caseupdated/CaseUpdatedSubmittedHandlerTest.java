package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated.CaseUpdatedSubmittedHandler.isANewJointParty;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;


public class CaseUpdatedSubmittedHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private CaseUpdatedSubmittedHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamService idamService;

    private SscsCaseData sscsCaseData;
    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new CaseUpdatedSubmittedHandler(idamService, updateCcdCaseService);
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        BenefitType benefitType = BenefitType.builder().code("UC").description("Universal credit").build();
        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1563382899630221").jointParty(JointParty.builder().hasJointParty(YES).build()).createdInGapsFrom(READY_TO_LIST.getId())
                .appeal(Appeal.builder().benefitType(benefitType).build())
                .build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

    }

    @Test
    public void givenANonCaseUpdatedEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenACaseUpdatedEventNotDigital_thenReturnFalse() {
        sscsCaseData.setCreatedInGapsFrom(VALID_APPEAL.getId());
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenACaseUpdatedEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @ParameterizedTest
    @EnumSource(names = {"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenANonCaseUpdatedCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenACaseUpdatedCallbackType_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenACaseUpdatedWithJointParty_runJointPartyAddedEvent() {
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getId()).thenReturn(1563382899630221L);
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        when(updateCcdCaseService.triggerCaseEventV2(any(), any(), any(),
                any(),
                any()))
                .thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(updateCcdCaseService).triggerCaseEventV2(Long.valueOf(callback.getCaseDetails().getCaseData().getCcdCaseId()),
                EventType.JOINT_PARTY_ADDED.getCcdType(), "Joint party added","", idamTokens);
    }

    @ParameterizedTest
    @CsvSource({
        "ESA,ESA",
        "PIP,Personal Independence Payment"
    })
    public void givenACaseUpdatedWithJointPartyNotUc_dontRunJointPartyAddedEvent(String benefitCode, String benefitDescription) {
        BenefitType benefitType = BenefitType.builder().code(benefitCode).description(benefitDescription).build();
        sscsCaseData.getAppeal().setBenefitType(benefitType);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getId()).thenReturn(1563382899630221L);
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyNoInteractions(updateCcdCaseService);
    }

    @Test
    public void givenACaseUpdatedWithNoJointPartyUc_dontRunJointPartyAddedEvent() {
        sscsCaseData.getJointParty().setHasJointParty(NO);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getId()).thenReturn(1563382899630221L);
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyNoInteractions(updateCcdCaseService);
    }

    @Test
    public void givenAJointPartyIsNew_thenReturnTrue() {
        SscsCaseData caseData = SscsCaseData.builder()
                .ccdCaseId("1563382899630221").jointParty(JointParty.builder().hasJointParty(YES).build())
                .appeal(Appeal.builder().build())
                .build();

        assertTrue(isANewJointParty(callback, caseData));
    }

    @Test
    public void givenAJointPartyIsExisting_thenReturnFalse() {
        SscsCaseData oldCaseData = SscsCaseData.builder()
                .ccdCaseId("1563382899630221").jointParty(JointParty.builder().hasJointParty(YES).build())
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetails));
        when(caseDetails.getCaseData()).thenReturn(oldCaseData);

        SscsCaseData caseData = SscsCaseData.builder()
                .ccdCaseId("1563382899630221").jointParty(JointParty.builder().hasJointParty(YES).build())
                .appeal(Appeal.builder().build())
                .build();


        assertFalse(isANewJointParty(callback, caseData));
    }

    @Test
    public void givenAJointPartyWasNoIsYes_thenReturnTrue() {
        SscsCaseData oldCaseData = SscsCaseData.builder()
                .ccdCaseId("1563382899630221").jointParty(JointParty.builder().hasJointParty(NO).build())
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetails));
        when(caseDetails.getCaseData()).thenReturn(oldCaseData);

        SscsCaseData caseData = SscsCaseData.builder()
                .ccdCaseId("1563382899630221").jointParty(JointParty.builder().hasJointParty(YES).build())
                .appeal(Appeal.builder().build())
                .build();

        assertTrue(isANewJointParty(callback, caseData));
    }
}
