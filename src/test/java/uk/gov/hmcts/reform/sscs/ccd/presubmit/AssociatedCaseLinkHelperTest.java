package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ASSOCIATE_CASE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class AssociatedCaseLinkHelperTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

    @Mock
    private CcdService ccdService;
    @Mock
    private IdamService idamService;

    private AssociatedCaseLinkHelper associatedCaseLinkHelper;

    @Before
    public void setUp() {

        associatedCaseLinkHelper = new AssociatedCaseLinkHelper(ccdService, idamService);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());

    }

    @Test
    public void shouldLinkCaseByNino() {
        Appellant appellant = Appellant.builder().identity(Identity.builder().nino("AB223344B").build()).build();
        SscsCaseDetails matchingCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().appeal(Appeal.builder().appellant(appellant).build()).build()).build();
        SscsCaseDetails matchingCase2 = SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().appeal(Appeal.builder().appellant(appellant).build()).build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase1);
        matchedByNinoCases.add(matchingCase2);
        when(ccdService.findCaseBy(anyMap(),any())).thenReturn(matchedByNinoCases);

        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().appellant(appellant).build()).ccdCaseId("00000000").build();
        associatedCaseLinkHelper.linkCaseByNino(caseData);

        assertEquals(2, caseData.getAssociatedCase().size());
        assertEquals("Yes", caseData.getLinkedCasesBoolean());
        assertEquals("12345678", caseData.getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("56765676", caseData.getAssociatedCase().get(1).getValue().getCaseReference());
    }

    @Test
    public void givenMultipleAssociatedCases_thenAddAllAssociatedCaseLinksToCase() {
        SscsCaseDetails matchingCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().build()).build();
        SscsCaseDetails matchingCase2 = SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase1);
        matchedByNinoCases.add(matchingCase2);

        SscsCaseData caseData = associatedCaseLinkHelper.addAssociatedCases(
                SscsCaseData.builder().ccdCaseId("00000000").build(),
                matchedByNinoCases);

        assertEquals(2, caseData.getAssociatedCase().size());
        assertEquals("Yes", caseData.getLinkedCasesBoolean());
        assertEquals("12345678", caseData.getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("56765676", caseData.getAssociatedCase().get(1).getValue().getCaseReference());
    }

    @Test
    public void addNoAssociatedCases() {
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();

        SscsCaseData caseData = associatedCaseLinkHelper.addAssociatedCases(
                SscsCaseData.builder().ccdCaseId("00000000").build(),
                matchedByNinoCases);

        assertNull(caseData.getAssociatedCase());
        assertEquals("No", caseData.getLinkedCasesBoolean());
        verify(ccdService, times(0)).updateCase(any(), any(), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());
    }

    @Test
    public void getMatchedCases() {
        given(ccdService.findCaseBy(any(), any())).willReturn(Collections.singletonList(
                SscsCaseDetails.builder().id(12345678L).build()
        ));
        List<SscsCaseDetails> matchedCases = associatedCaseLinkHelper.getMatchedCases("ABCDEFG", idamService.getIdamTokens());

        assertEquals(1, matchedCases.size());
    }
}