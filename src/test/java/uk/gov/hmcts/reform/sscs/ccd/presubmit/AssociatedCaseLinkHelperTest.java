package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ASSOCIATE_CASE;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
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
    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    private AssociatedCaseLinkHelper associatedCaseLinkHelper;
    @Captor
    private ArgumentCaptor<Consumer<SscsCaseData>> caseDetailsCaptor;

    @Before
    public void setUp() {
        associatedCaseLinkHelper = new AssociatedCaseLinkHelper(ccdService, idamService, updateCcdCaseService);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
    }

    @Test
    public void shouldLinkCaseByNinoIfPreviousNinoNotPresent() {
        ReflectionTestUtils.setField(associatedCaseLinkHelper, "addLinkToOtherAssociatedCasesV2Enabled", false);

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
        assertEquals("Yes", result.getLinkedCasesBoolean());
        assertEquals("56765676", result.getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("12345678", result.getAssociatedCase().get(1).getValue().getCaseReference());

        assertEquals("Yes", matchingCase1.getData().getLinkedCasesBoolean());
        assertEquals("33333333", matchingCase1.getData().getAssociatedCase().get(0).getValue().getCaseReference());

        assertEquals("Yes", matchingCase2.getData().getLinkedCasesBoolean());
        assertEquals("33333333", matchingCase2.getData().getAssociatedCase().get(0).getValue().getCaseReference());
    }

    @Test
    public void shouldLinkCaseByNinoIfPreviousNinoNotPresentV2() {
        ReflectionTestUtils.setField(associatedCaseLinkHelper, "addLinkToOtherAssociatedCasesV2Enabled", true);

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
        assertEquals("Yes", result.getLinkedCasesBoolean());
        assertEquals("56765676", result.getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("12345678", result.getAssociatedCase().get(1).getValue().getCaseReference());

        verify(updateCcdCaseService).updateCaseV2(eq(12345678L), eq(EventType.UPDATE_CASE_ONLY.getCcdType()), any(), any(), any(IdamTokens.class), caseDetailsCaptor.capture());
        caseDetailsCaptor.getValue().accept(matchingCase1.getData());

        verify(updateCcdCaseService).updateCaseV2(eq(56765676L), eq(EventType.UPDATE_CASE_ONLY.getCcdType()), any(), any(), any(IdamTokens.class), caseDetailsCaptor.capture());
        caseDetailsCaptor.getValue().accept(matchingCase2.getData());

        assertEquals("Yes", matchingCase1.getData().getLinkedCasesBoolean());
        assertEquals("33333333", matchingCase1.getData().getAssociatedCase().get(0).getValue().getCaseReference());

        assertEquals("Yes", matchingCase2.getData().getLinkedCasesBoolean());
        assertEquals("33333333", matchingCase2.getData().getAssociatedCase().get(0).getValue().getCaseReference());
    }

    @Test
    @Parameters({"true", "false"})
    public void shouldNotLinkCaseByNinoIfPreviousNinoIsPresent(String addLinkToOtherAssociatedCasesV2Enabled) {
        Boolean addLinkToOtherAssociatedCasesV2Boolean = Boolean.valueOf(addLinkToOtherAssociatedCasesV2Enabled);
        ReflectionTestUtils.setField(associatedCaseLinkHelper, "addLinkToOtherAssociatedCasesV2Enabled", addLinkToOtherAssociatedCasesV2Boolean);

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
    @Parameters({"true", "false"})
    public void shouldLinkCaseByNinoButNotToOthersIfCaseIdIsNull(String addLinkToOtherAssociatedCasesV2Enabled) {
        Boolean addLinkToOtherAssociatedCasesV2Boolean = Boolean.valueOf(addLinkToOtherAssociatedCasesV2Enabled);
        ReflectionTestUtils.setField(associatedCaseLinkHelper, "addLinkToOtherAssociatedCasesV2Enabled", addLinkToOtherAssociatedCasesV2Boolean);

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
        assertEquals("Yes", result.getLinkedCasesBoolean());
        assertEquals("56765676", result.getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("12345678", result.getAssociatedCase().get(1).getValue().getCaseReference());

        assertNull(matchingCase1.getData().getLinkedCasesBoolean());
        assertNull(matchingCase1.getData().getAssociatedCase());

        assertNull(matchingCase2.getData().getLinkedCasesBoolean());
        assertNull(matchingCase2.getData().getAssociatedCase());
    }

    @Test
    public void givenMultipleAssociatedCases_thenAddAllAssociatedCaseLinksToCase() {
        ReflectionTestUtils.setField(associatedCaseLinkHelper, "addLinkToOtherAssociatedCasesV2Enabled", false);

        SscsCaseDetails matchingCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().ccdCaseId("12345678").build()).build();
        SscsCaseDetails matchingCase2 = SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().ccdCaseId("56765676").build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase1);
        matchedByNinoCases.add(matchingCase2);

        SscsCaseData caseData = associatedCaseLinkHelper.addAssociatedCases(
                SscsCaseData.builder().ccdCaseId("33333333").build(),
                matchedByNinoCases);

        assertEquals(2, caseData.getAssociatedCase().size());
        assertEquals("Yes", caseData.getLinkedCasesBoolean());
        assertEquals("56765676", caseData.getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("12345678", caseData.getAssociatedCase().get(1).getValue().getCaseReference());

        assertEquals("Yes", matchingCase1.getData().getLinkedCasesBoolean());
        assertEquals("33333333", matchingCase1.getData().getAssociatedCase().get(0).getValue().getCaseReference());

        assertEquals("Yes", matchingCase2.getData().getLinkedCasesBoolean());
        assertEquals("33333333", matchingCase2.getData().getAssociatedCase().get(0).getValue().getCaseReference());
    }

    @Test
    public void givenMultipleAssociatedCases_thenAddAllAssociatedCaseLinksToCaseV2() {
        ReflectionTestUtils.setField(associatedCaseLinkHelper, "addLinkToOtherAssociatedCasesV2Enabled", true);

        SscsCaseDetails matchingCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().ccdCaseId("12345678").build()).build();
        SscsCaseDetails matchingCase2 = SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().ccdCaseId("56765676").build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase1);
        matchedByNinoCases.add(matchingCase2);

        SscsCaseData caseData = associatedCaseLinkHelper.addAssociatedCases(
                SscsCaseData.builder().ccdCaseId("33333333").build(),
                matchedByNinoCases);

        assertEquals(2, caseData.getAssociatedCase().size());
        assertEquals("Yes", caseData.getLinkedCasesBoolean());
        assertEquals("56765676", caseData.getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("12345678", caseData.getAssociatedCase().get(1).getValue().getCaseReference());

        verify(updateCcdCaseService).updateCaseV2(eq(12345678L), eq(EventType.UPDATE_CASE_ONLY.getCcdType()), any(), any(), any(IdamTokens.class), caseDetailsCaptor.capture());
        caseDetailsCaptor.getValue().accept(matchingCase1.getData());

        verify(updateCcdCaseService).updateCaseV2(eq(56765676L), eq(EventType.UPDATE_CASE_ONLY.getCcdType()), any(), any(), any(IdamTokens.class), caseDetailsCaptor.capture());
        caseDetailsCaptor.getValue().accept(matchingCase2.getData());

        assertEquals("Yes", matchingCase1.getData().getLinkedCasesBoolean());
        assertEquals("33333333", matchingCase1.getData().getAssociatedCase().get(0).getValue().getCaseReference());

        assertEquals("Yes", matchingCase2.getData().getLinkedCasesBoolean());
        assertEquals("33333333", matchingCase2.getData().getAssociatedCase().get(0).getValue().getCaseReference());
    }

    @Test
    @Parameters({"true", "false"})
    public void addNoAssociatedCases(String addLinkToOtherAssociatedCasesV2Enabled) {
        Boolean addLinkToOtherAssociatedCasesV2Boolean = Boolean.valueOf(addLinkToOtherAssociatedCasesV2Enabled);
        ReflectionTestUtils.setField(associatedCaseLinkHelper, "addLinkToOtherAssociatedCasesV2Enabled", addLinkToOtherAssociatedCasesV2Boolean);

        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();

        SscsCaseData caseData = associatedCaseLinkHelper.addAssociatedCases(
                SscsCaseData.builder().ccdCaseId("00000000").build(),
                matchedByNinoCases);

        assertNull(caseData.getAssociatedCase());
        assertEquals("No", caseData.getLinkedCasesBoolean());
        verify(ccdService, times(0)).updateCase(any(), any(), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());
    }

    @Test
    @Parameters({"true", "false"})
    public void getMatchedCases(String addLinkToOtherAssociatedCasesV2Enabled) {
        Boolean addLinkToOtherAssociatedCasesV2Boolean = Boolean.valueOf(addLinkToOtherAssociatedCasesV2Enabled);
        ReflectionTestUtils.setField(associatedCaseLinkHelper, "addLinkToOtherAssociatedCasesV2Enabled", addLinkToOtherAssociatedCasesV2Boolean);

        given(ccdService.findCaseBy(any(), any(), any())).willReturn(Collections.singletonList(
                SscsCaseDetails.builder().id(12345678L).build()
        ));
        List<SscsCaseDetails> matchedCases = associatedCaseLinkHelper.getMatchedCases("ABCDEFG", idamService.getIdamTokens());

        assertEquals(1, matchedCases.size());
    }
}