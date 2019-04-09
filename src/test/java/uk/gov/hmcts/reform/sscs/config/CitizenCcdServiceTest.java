package uk.gov.hmcts.reform.sscs.config;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class CitizenCcdServiceTest {

    private static final String DRAFT = "draft";
    private static final IdamTokens IDAM_TOKENS = IdamTokens.builder().build();

    private CitizenCcdService citizenCcdService;

    @Mock
    private CitizenCcdClient citizenCcdClient;

    @Mock
    private SscsCcdConvertService sscsCcdConvertService;

    @Before
    public void setup() {
        initMocks(this);
        citizenCcdService = new CitizenCcdService(citizenCcdClient, sscsCcdConvertService);
    }

    @Test
    public void shouldInvokeCoreCaseDataApiWhenCreatingADraftCase() {
        CaseDataContent caseDataContent = CaseDataContent.builder().build();
        when(sscsCcdConvertService.getCaseDataContent(any(), any(), any(), any())).thenReturn(caseDataContent);
        when(citizenCcdClient.searchForCitizen(eq(IDAM_TOKENS))).thenReturn(Collections.emptyList());
        when(citizenCcdClient.startCaseForCitizen(eq(IDAM_TOKENS), eq(DRAFT))).thenReturn(StartEventResponse.builder().eventId(DRAFT).build());
        when(citizenCcdClient.submitForCitizen(eq(IDAM_TOKENS), eq(caseDataContent))).thenReturn(CaseDetails.builder().build());
        when(sscsCcdConvertService.getCaseDetails(any())).thenReturn(SscsCaseDetails.builder().build());
        SscsCaseDetails caseDetails = citizenCcdService.saveCase(null, DRAFT, "summary", "description", IDAM_TOKENS);
        assertNotNull(caseDetails);
        verify(citizenCcdClient).searchForCitizen(eq(IDAM_TOKENS));
        verify(citizenCcdClient).startCaseForCitizen(eq(IDAM_TOKENS), eq(DRAFT));
        verify(citizenCcdClient).submitForCitizen(eq(IDAM_TOKENS), eq(caseDataContent));
        verifyNoMoreInteractions(citizenCcdClient);
    }

    @Test
    public void shouldInvokeCoreCaseDataApiWhenUpdatingADraftCase() {
        Long caseId = 1L;
        CaseDataContent caseDataContent = CaseDataContent.builder().build();
        when(sscsCcdConvertService.getCaseDataContent(any(), any(), any(), any())).thenReturn(caseDataContent);
        when(citizenCcdClient.searchForCitizen(eq(IDAM_TOKENS))).thenReturn(Collections.singletonList(CaseDetails.builder().id(caseId).build()));
        when(citizenCcdClient.startEventForCitizen(eq(IDAM_TOKENS), eq(caseId.toString()), eq(DRAFT))).thenReturn(StartEventResponse.builder().eventId(DRAFT).build());
        when(citizenCcdClient.submitEventForCitizen(eq(IDAM_TOKENS), eq(caseId.toString()), eq(caseDataContent))).thenReturn(CaseDetails.builder().build());
        when(sscsCcdConvertService.getCaseDetails(any())).thenReturn(SscsCaseDetails.builder().build());
        SscsCaseDetails caseDetails = citizenCcdService.saveCase(null, DRAFT, "summary", "description", IDAM_TOKENS);
        assertNotNull(caseDetails);
        verify(citizenCcdClient).searchForCitizen(eq(IDAM_TOKENS));
        verify(citizenCcdClient).startEventForCitizen(eq(IDAM_TOKENS), eq(caseId.toString()), eq(DRAFT));
        verify(citizenCcdClient).submitEventForCitizen(eq(IDAM_TOKENS), eq(caseId.toString()), eq(caseDataContent));
        verifyNoMoreInteractions(citizenCcdClient);
    }
}
