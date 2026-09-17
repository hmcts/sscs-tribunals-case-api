package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ASSOCIATE_CASE;
import static uk.gov.hmcts.reform.sscs.utility.StringUtils.getMaskedNino;

import ch.qos.logback.classic.Level;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.util.LogCaptureExtension;

@ExtendWith(MockitoExtension.class)
class AssociatedCaseLinkHelperTest {

    @Mock
    private CcdService ccdService;
    @Mock
    private IdamService idamService;
    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    private AssociatedCaseLinkHelper associatedCaseLinkHelper;
    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> caseDetailsCaptor;

    @RegisterExtension
    private final LogCaptureExtension logCapture =
            new LogCaptureExtension(AssociatedCaseLinkHelper.class);

    @BeforeEach
    void setUp() {
        associatedCaseLinkHelper = new AssociatedCaseLinkHelper(ccdService, idamService, updateCcdCaseService);

        lenient().when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
    }

    @Test
    void shouldLinkCaseByNinoIfPreviousNinoNotPresentV2() {

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

        assertThat(result.getAssociatedCase()).hasSize(2);
        assertThat(result.getLinkedCasesBoolean()).isEqualTo("Yes");
        assertThat(result.getAssociatedCase().get(0).getValue().getCaseReference()).isEqualTo("56765676");
        assertThat(result.getAssociatedCase().get(1).getValue().getCaseReference()).isEqualTo("12345678");

        verify(updateCcdCaseService).updateCaseV2(eq(12345678L), eq(EventType.UPDATE_CASE_ONLY.getCcdType()), any(), any(), any(IdamTokens.class), caseDetailsCaptor.capture());
        caseDetailsCaptor.getValue().accept(matchingCase1);

        verify(updateCcdCaseService).updateCaseV2(eq(56765676L), eq(EventType.UPDATE_CASE_ONLY.getCcdType()), any(), any(), any(IdamTokens.class), caseDetailsCaptor.capture());
        caseDetailsCaptor.getValue().accept(matchingCase2);

        assertThat(matchingCase1.getData().getLinkedCasesBoolean()).isEqualTo("Yes");
        assertThat(matchingCase1.getData().getAssociatedCase().getFirst().getValue().getCaseReference()).isEqualTo("33333333");

        assertThat(matchingCase2.getData().getLinkedCasesBoolean()).isEqualTo("Yes");
        assertThat(matchingCase2.getData().getAssociatedCase().getFirst().getValue().getCaseReference()).isEqualTo("33333333");
        logCapture
                .assertLogContains(getMaskedNino("AB223344B"), Level.INFO)
                .assertLogDoesNotContain("AB223344B", Level.INFO);
    }

    @Test
    void shouldNotLinkCaseByNinoIfPreviousNinoIsPresent() {
        Appellant appellant = Appellant.builder().identity(Identity.builder().nino("AB223344B").build()).build();

        SscsCaseDetails matchingCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().ccdCaseId("12345678").appeal(Appeal.builder().appellant(appellant).build()).build()).build();
        SscsCaseDetails matchingCase2 = SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().ccdCaseId("56765676").appeal(Appeal.builder().appellant(appellant).build()).build()).build();
        SscsCaseData previousCaseData  = SscsCaseData.builder().appeal(Appeal.builder().appellant(appellant).build()).ccdCaseId("33333333").build();
        Optional<CaseDetails<SscsCaseData>> previousSscsCaseDataCaseDetails = Optional.of(new CaseDetails<SscsCaseData>(33333333L, "", State.APPEAL_CREATED, previousCaseData, LocalDateTime.now(), "Benefit"));

        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().appellant(appellant).build()).ccdCaseId("33333333").build();
        SscsCaseData result = associatedCaseLinkHelper.linkCaseByNino(caseData, previousSscsCaseDataCaseDetails);

        assertThat(result.getAssociatedCase()).isNull();
        assertThat(result.getLinkedCasesBoolean()).isNull();

        assertThat(matchingCase1.getData().getLinkedCasesBoolean()).isNull();
        assertThat(matchingCase1.getData().getAssociatedCase()).isNull();

        assertThat(matchingCase2.getData().getLinkedCasesBoolean()).isNull();
        assertThat(matchingCase2.getData().getAssociatedCase()).isNull();
    }

    @Test
    void shouldNotLinkCaseByNinoIfNinoIsInvalid() {
        Appellant appellant = Appellant.builder().identity(Identity.builder().nino("INVALID1").build()).build();

        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().appellant(appellant).build()).ccdCaseId("33333333").build();
        SscsCaseData result = associatedCaseLinkHelper.linkCaseByNino(caseData, Optional.empty());

        assertThat(result.getAssociatedCase()).isNull();
        assertThat(result.getLinkedCasesBoolean()).isNull();
        verify(ccdService, times(0)).findCaseBy(any(), any(), any());
    }

    @Test
    void shouldLinkCaseByNinoButNotToOthersIfCaseIdIsNull() {
        Appellant appellant = Appellant.builder().identity(Identity.builder().nino("AB223344B").build()).build();
        SscsCaseDetails matchingCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().ccdCaseId("12345678").appeal(Appeal.builder().appellant(appellant).build()).build()).build();
        SscsCaseDetails matchingCase2 = SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().ccdCaseId("56765676").appeal(Appeal.builder().appellant(appellant).build()).build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase1);
        matchedByNinoCases.add(matchingCase2);
        when(ccdService.findCaseBy(anyString(), anyString(), any())).thenReturn(matchedByNinoCases);

        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().appellant(appellant).build()).build();
        SscsCaseData result = associatedCaseLinkHelper.linkCaseByNino(caseData, Optional.empty());

        assertThat(result.getAssociatedCase()).hasSize(2);
        assertThat(result.getLinkedCasesBoolean()).isEqualTo("Yes");
        assertThat(result.getAssociatedCase().get(0).getValue().getCaseReference()).isEqualTo("56765676");
        assertThat(result.getAssociatedCase().get(1).getValue().getCaseReference()).isEqualTo("12345678");

        assertThat(matchingCase1.getData().getLinkedCasesBoolean()).isNull();
        assertThat(matchingCase1.getData().getAssociatedCase()).isNull();

        assertThat(matchingCase2.getData().getLinkedCasesBoolean()).isNull();
        assertThat(matchingCase2.getData().getAssociatedCase()).isNull();
    }

    @Test
    void givenMultipleAssociatedCases_thenAddAllAssociatedCaseLinksToCaseV2() {
        SscsCaseDetails matchingCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().ccdCaseId("12345678").build()).build();
        SscsCaseDetails matchingCase2 = SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().ccdCaseId("56765676").build()).build();
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();
        matchedByNinoCases.add(matchingCase1);
        matchedByNinoCases.add(matchingCase2);

        SscsCaseData caseData = associatedCaseLinkHelper.addAssociatedCases(
                SscsCaseData.builder().ccdCaseId("33333333").build(),
                matchedByNinoCases);

        assertThat(caseData.getAssociatedCase()).hasSize(2);
        assertThat(caseData.getLinkedCasesBoolean()).isEqualTo("Yes");
        assertThat(caseData.getAssociatedCase().get(0).getValue().getCaseReference()).isEqualTo("56765676");
        assertThat(caseData.getAssociatedCase().get(1).getValue().getCaseReference()).isEqualTo("12345678");

        verify(updateCcdCaseService).updateCaseV2(eq(12345678L), eq(EventType.UPDATE_CASE_ONLY.getCcdType()), any(), any(), any(IdamTokens.class), caseDetailsCaptor.capture());
        caseDetailsCaptor.getValue().accept(matchingCase1);

        verify(updateCcdCaseService).updateCaseV2(eq(56765676L), eq(EventType.UPDATE_CASE_ONLY.getCcdType()), any(), any(), any(IdamTokens.class), caseDetailsCaptor.capture());
        caseDetailsCaptor.getValue().accept(matchingCase2);

        assertThat(matchingCase1.getData().getLinkedCasesBoolean()).isEqualTo("Yes");
        assertThat(matchingCase1.getData().getAssociatedCase().getFirst().getValue().getCaseReference()).isEqualTo("33333333");

        assertThat(matchingCase2.getData().getLinkedCasesBoolean()).isEqualTo("Yes");
        assertThat(matchingCase2.getData().getAssociatedCase().getFirst().getValue().getCaseReference()).isEqualTo("33333333");
    }

    @Test
    void addNoAssociatedCases() {
        List<SscsCaseDetails> matchedByNinoCases = new ArrayList<>();

        SscsCaseData caseData = associatedCaseLinkHelper.addAssociatedCases(
                SscsCaseData.builder().ccdCaseId("00000000").build(),
                matchedByNinoCases);

        assertThat(caseData.getAssociatedCase()).isNull();
        assertThat(caseData.getLinkedCasesBoolean()).isEqualTo("No");
        verify(ccdService, times(0)).updateCase(any(), any(), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());
    }

    @Test
    void getMatchedCases() {
        given(ccdService.findCaseBy(any(), any(), any())).willReturn(Collections.singletonList(
                SscsCaseDetails.builder().id(12345678L).build()
        ));
        List<SscsCaseDetails> matchedCases = associatedCaseLinkHelper.getMatchedCases("AB123456C", idamService.getIdamTokens());

        assertThat(matchedCases).hasSize(1);
    }

    @Test
    void getMatchedCasesReturnsEmptyListWhenNinoIsInvalid() {
        List<SscsCaseDetails> matchedCases = associatedCaseLinkHelper.getMatchedCases("INVALID1", idamService.getIdamTokens());

        assertThat(matchedCases).isEmpty();
        verify(ccdService, times(0)).findCaseBy(any(), any(), any());
    }
}
