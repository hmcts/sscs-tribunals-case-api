package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ASSOCIATE_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
    public void shouldLinkCaseByNinoIfPreviousNinoNotPresent() {
        Appellant appellant = Appellant.builder().identity(Identity.builder().nino("AB223344B").build()).build();

        SscsCaseDetails matchingCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().ccdCaseId("12345678").appeal(Appeal.builder().appellant(appellant).build()).build()).build();
        SscsCaseDetails matchingCase2 = SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().ccdCaseId("56765676").appeal(Appeal.builder().appellant(appellant).build()).build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase1);
        matchedByNinoCases.add(matchingCase2);
        SscsCaseData previousCaseData  = SscsCaseData.builder().appeal(Appeal.builder().appellant(Appellant.builder().build()).build()).ccdCaseId("33333333").build();
        Optional<CaseDetails<SscsCaseData>> previousSscsCaseDataCaseDetails = Optional.of(new CaseDetails<SscsCaseData>(33333333L, "", State.APPEAL_CREATED, previousCaseData, LocalDateTime.now(), "Benefit"));
        when(ccdService.findCaseBy(anyString(), anyString(), any())).thenReturn(matchedByNinoCases);

        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().appellant(appellant).build()).ccdCaseId("33333333").build();
        SscsCaseData result = associatedCaseLinkHelper.linkCaseByNino(caseData, previousSscsCaseDataCaseDetails);

        assertEquals(2, result.getAssociatedCase().size());
        assertEquals(YES, result.getLinkedCasesBoolean());
        assertEquals("56765676", result.getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("12345678", result.getAssociatedCase().get(1).getValue().getCaseReference());

        assertEquals(YES, matchingCase1.getData().getLinkedCasesBoolean());
        assertEquals("33333333", matchingCase1.getData().getAssociatedCase().get(0).getValue().getCaseReference());

        assertEquals(YES, matchingCase2.getData().getLinkedCasesBoolean());
        assertEquals("33333333", matchingCase2.getData().getAssociatedCase().get(0).getValue().getCaseReference());
    }

    @Test
    public void shouldNotLinkCaseByNinoIfPreviousNinoIsPresent() {
        Appellant appellant = Appellant.builder().identity(Identity.builder().nino("AB223344B").build()).build();

        SscsCaseDetails matchingCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().ccdCaseId("12345678").appeal(Appeal.builder().appellant(appellant).build()).build()).build();
        SscsCaseDetails matchingCase2 = SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().ccdCaseId("56765676").appeal(Appeal.builder().appellant(appellant).build()).build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase1);
        matchedByNinoCases.add(matchingCase2);
        SscsCaseData previousCaseData  = SscsCaseData.builder().appeal(Appeal.builder().appellant(appellant).build()).ccdCaseId("33333333").build();
        Optional<CaseDetails<SscsCaseData>> previousSscsCaseDataCaseDetails = Optional.of(new CaseDetails<SscsCaseData>(33333333L, "", State.APPEAL_CREATED, previousCaseData, LocalDateTime.now(), "Benefit"));
        when(ccdService.findCaseBy(anyString(), anyString(), any())).thenReturn(matchedByNinoCases);

        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().appellant(appellant).build()).ccdCaseId("33333333").build();
        SscsCaseData result = associatedCaseLinkHelper.linkCaseByNino(caseData, previousSscsCaseDataCaseDetails);

        assertNull(result.getAssociatedCase());
        assertNull(result.getLinkedCasesBoolean());

        assertNull(matchingCase1.getData().getLinkedCasesBoolean());
        assertNull(matchingCase1.getData().getAssociatedCase());

        assertNull(matchingCase2.getData().getLinkedCasesBoolean());
        assertNull(matchingCase2.getData().getAssociatedCase());
    }

    @Test
    public void shouldLinkCaseByNinoButNotToOthersIfCaseIdIsNull() {
        Appellant appellant = Appellant.builder().identity(Identity.builder().nino("AB223344B").build()).build();
        SscsCaseDetails matchingCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().ccdCaseId("12345678").appeal(Appeal.builder().appellant(appellant).build()).build()).build();
        SscsCaseDetails matchingCase2 = SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().ccdCaseId("56765676").appeal(Appeal.builder().appellant(appellant).build()).build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase1);
        matchedByNinoCases.add(matchingCase2);
        when(ccdService.findCaseBy(anyString(), anyString(), any())).thenReturn(matchedByNinoCases);

        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().appellant(appellant).build()).build();
        SscsCaseData result = associatedCaseLinkHelper.linkCaseByNino(caseData, Optional.empty());

        assertEquals(2, result.getAssociatedCase().size());
        assertEquals(YES, result.getLinkedCasesBoolean());
        assertEquals("56765676", result.getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("12345678", result.getAssociatedCase().get(1).getValue().getCaseReference());

        assertNull(matchingCase1.getData().getLinkedCasesBoolean());
        assertNull(matchingCase1.getData().getAssociatedCase());

        assertNull(matchingCase2.getData().getLinkedCasesBoolean());
        assertNull(matchingCase2.getData().getAssociatedCase());
    }

    @Test
    public void givenMultipleAssociatedCases_thenAddAllAssociatedCaseLinksToCase() {
        SscsCaseDetails matchingCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().ccdCaseId("12345678").build()).build();
        SscsCaseDetails matchingCase2 = SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().ccdCaseId("56765676").build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase1);
        matchedByNinoCases.add(matchingCase2);

        SscsCaseData caseData = associatedCaseLinkHelper.addAssociatedCases(
                SscsCaseData.builder().ccdCaseId("33333333").build(),
                matchedByNinoCases);

        assertEquals(2, caseData.getAssociatedCase().size());
        assertEquals(YES, caseData.getLinkedCasesBoolean());
        assertEquals("56765676", caseData.getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("12345678", caseData.getAssociatedCase().get(1).getValue().getCaseReference());

        assertEquals(YES, matchingCase1.getData().getLinkedCasesBoolean());
        assertEquals("33333333", matchingCase1.getData().getAssociatedCase().get(0).getValue().getCaseReference());

        assertEquals(YES, matchingCase2.getData().getLinkedCasesBoolean());
        assertEquals("33333333", matchingCase2.getData().getAssociatedCase().get(0).getValue().getCaseReference());
    }

    @Test
    public void addNoAssociatedCases() {
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();

        SscsCaseData caseData = associatedCaseLinkHelper.addAssociatedCases(
                SscsCaseData.builder().ccdCaseId("00000000").build(),
                matchedByNinoCases);

        assertNull(caseData.getAssociatedCase());
        assertEquals(NO, caseData.getLinkedCasesBoolean());
        verify(ccdService, times(0)).updateCase(any(), any(), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());
    }

    @Test
    public void getMatchedCases() {
        given(ccdService.findCaseBy(any(), any(), any())).willReturn(Collections.singletonList(
                SscsCaseDetails.builder().id(12345678L).build()
        ));
        List<SscsCaseDetails> matchedCases = associatedCaseLinkHelper.getMatchedCases("ABCDEFG", idamService.getIdamTokens());

        assertEquals(1, matchedCases.size());
    }
}
