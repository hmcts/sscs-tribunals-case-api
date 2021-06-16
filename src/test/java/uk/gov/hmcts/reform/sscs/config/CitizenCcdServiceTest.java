package uk.gov.hmcts.reform.sscs.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_ARCHIVED;

import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;

public class CitizenCcdServiceTest {

    private static final String CREATE_DRAFT = "createDraft";
    private static final String UPDATE_DRAFT = "updateDraft";
    private static final IdamTokens IDAM_TOKENS =
            IdamTokens.builder().email("dummy@email.com").roles(List.of("citizen")).build();

    private CitizenCcdService citizenCcdService;

    @Mock
    private CitizenCcdClient citizenCcdClient;

    @Mock
    private SscsCcdConvertService sscsCcdConvertService;

    @Mock
    private CcdService ccdService;

    @Before
    public void setup() {
        openMocks(this);
        citizenCcdService = new CitizenCcdService(citizenCcdClient, sscsCcdConvertService, ccdService);
    }

    @Test
    public void shouldInvokeCoreCaseDataApiWhenCreatingADraftCase() {
        CaseDataContent caseDataContent = CaseDataContent.builder().build();
        when(sscsCcdConvertService.getCaseDataContent(any(), any(), any(), any())).thenReturn(caseDataContent);
        when(citizenCcdClient.searchForCitizen(eq(IDAM_TOKENS))).thenReturn(Collections.emptyList());
        when(citizenCcdClient.startCaseForCitizen(eq(IDAM_TOKENS), eq(CREATE_DRAFT))).thenReturn(StartEventResponse.builder().eventId(CREATE_DRAFT).build());
        when(citizenCcdClient.submitForCitizen(eq(IDAM_TOKENS), eq(caseDataContent)))
            .thenReturn(CaseDetails.builder().id(123L).build());

        SaveCaseResult result = citizenCcdService.saveCase(null, IDAM_TOKENS);

        assertEquals(123L, result.getCaseDetailsId());
        assertEquals(SaveCaseOperation.CREATE, result.getSaveCaseOperation());
        verify(citizenCcdClient).searchForCitizen(eq(IDAM_TOKENS));
        verify(citizenCcdClient).startCaseForCitizen(eq(IDAM_TOKENS), eq(CREATE_DRAFT));
        verify(citizenCcdClient).submitForCitizen(eq(IDAM_TOKENS), eq(caseDataContent));
        verifyNoMoreInteractions(citizenCcdClient);
    }

    @Test
    public void shouldInvokeCoreCaseDataApiWhenUpdatingADraftCase() {
        Long caseId = 123L;
        CaseDataContent caseDataContent = CaseDataContent.builder().build();
        when(sscsCcdConvertService.getCaseDataContent(any(), any(), any(), any())).thenReturn(caseDataContent);
        when(citizenCcdClient.searchForCitizen(eq(IDAM_TOKENS))).thenReturn(Collections.singletonList(CaseDetails.builder().id(caseId).build()));
        when(citizenCcdClient.startEventForCitizen(eq(IDAM_TOKENS), eq(caseId.toString()), eq(UPDATE_DRAFT))).thenReturn(StartEventResponse.builder().eventId(UPDATE_DRAFT).build());
        when(citizenCcdClient.submitEventForCitizen(eq(IDAM_TOKENS), eq(caseId.toString()), eq(caseDataContent)))
            .thenReturn(CaseDetails.builder().id(caseId).build());

        SaveCaseResult result = citizenCcdService.saveCase(null, IDAM_TOKENS);

        assertEquals(123L, result.getCaseDetailsId());
        assertEquals(SaveCaseOperation.UPDATE, result.getSaveCaseOperation());
        verify(citizenCcdClient).searchForCitizen(eq(IDAM_TOKENS));
        verify(citizenCcdClient).startEventForCitizen(eq(IDAM_TOKENS), eq(caseId.toString()), eq(UPDATE_DRAFT));
        verify(citizenCcdClient).submitEventForCitizen(eq(IDAM_TOKENS), eq(caseId.toString()), eq(caseDataContent));
        verifyNoMoreInteractions(citizenCcdClient);
    }

    @Test
    public void shouldArchiveADraftCase() {
        SscsCaseData caseData = SscsCaseData.builder().build();
        Long caseId = 123L;

        StartEventResponse eventResponse = StartEventResponse.builder().build();
        when(citizenCcdClient.startEventForCitizen(eq(IDAM_TOKENS), eq(caseId.toString()), eq(DRAFT_ARCHIVED.getCcdType())))
                .thenReturn(eventResponse);

        when(citizenCcdClient.submitEventForCitizen(eq(IDAM_TOKENS), eq(caseId.toString()), any()))
                .thenReturn(CaseDetails.builder().build());

        CaseDetails caseDetails = citizenCcdService.archiveDraft(caseData, IDAM_TOKENS, caseId);

        assertNotNull(caseDetails);
    }

    @Test
    public void shouldNotAssociateCaseToCitizenWhenCaseDetailsIsNull() {
        Long caseId = 1234L;
        when(ccdService.getByCaseId(caseId,IDAM_TOKENS)).thenReturn(null);

        citizenCcdService.associateCaseToCitizen(IDAM_TOKENS, caseId, IDAM_TOKENS);

        verifyNoMoreInteractions(citizenCcdClient);
    }

    @Test
    public void shouldNotAssociateCaseToCitizenWhenCaseDataIsNull() {
        Long caseId = 1234L;
        when(ccdService.getByCaseId(caseId,IDAM_TOKENS)).thenReturn(SscsCaseDetails.builder().data(null).build());

        citizenCcdService.associateCaseToCitizen(IDAM_TOKENS, caseId, IDAM_TOKENS);

        verifyNoMoreInteractions(citizenCcdClient);
    }

    @Test
    public void shouldNotAssociateCaseToCitizenWhenPostcodeIsNull() {
        SscsCaseData caseData = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .address(Address.builder().postcode(null).build())
                                .build()).build())
                .build();
        Long caseId = 1234L;
        when(ccdService.getByCaseId(caseId,IDAM_TOKENS)).thenReturn(SscsCaseDetails.builder().data(caseData).build());

        citizenCcdService.associateCaseToCitizen(IDAM_TOKENS, caseId, IDAM_TOKENS);

        verifyNoMoreInteractions(citizenCcdClient);
    }

    @Test
    public void shouldNotAssociateCaseToCitizenWhenSubscriptionIsNull() {
        SscsCaseData caseData = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .address(Address.builder().postcode("TS1 1ST").build())
                                .build()).build())
                .subscriptions(null)
                .build();
        Long caseId = 1234L;
        when(ccdService.getByCaseId(caseId,IDAM_TOKENS)).thenReturn(SscsCaseDetails.builder().data(caseData).build());

        citizenCcdService.associateCaseToCitizen(IDAM_TOKENS, caseId, IDAM_TOKENS);

        verifyNoMoreInteractions(citizenCcdClient);
    }

    @Test
    public void shouldNotAssociateCaseToCitizenWhenEmailIsNotSame() {
        SscsCaseData caseData = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .address(Address.builder().postcode("TS1 1ST").build())
                                .build()).build())
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder()
                                .email("notmatching@email.com").build()).build())
                .build();
        Long caseId = 1234L;
        when(ccdService.getByCaseId(caseId,IDAM_TOKENS)).thenReturn(SscsCaseDetails.builder().data(caseData).build());

        citizenCcdService.associateCaseToCitizen(IDAM_TOKENS, caseId, IDAM_TOKENS);

        verifyNoMoreInteractions(citizenCcdClient);
    }

    @Test
    public void shouldAssociateCaseToCitizenWhenSubscriptionEmailIsSame() {
        SscsCaseData caseData = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .address(Address.builder().postcode("TS1 1ST").build())
                                .build()).build())
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder()
                                .email("dummy@email.com").build()).build())
                .build();
        Long caseId = 1234L;
        when(ccdService.getByCaseId(caseId,IDAM_TOKENS)).thenReturn(SscsCaseDetails.builder().data(caseData).build());

        citizenCcdService.associateCaseToCitizen(IDAM_TOKENS, caseId, IDAM_TOKENS);

        verify(citizenCcdClient).addUserToCase(eq(IDAM_TOKENS), eq(IDAM_TOKENS.getUserId()), eq(caseId));
    }
}
