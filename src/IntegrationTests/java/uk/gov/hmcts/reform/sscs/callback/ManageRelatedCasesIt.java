package uk.gov.hmcts.reform.sscs.callback;

import static java.lang.Long.parseLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ASSOCIATE_CASE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_CASE_ONLY;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLinkDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.controller.CcdCallbackController;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@SpringBootTest
@AutoConfigureMockMvc
class ManageRelatedCasesIt extends AbstractEventIt {

    private static final String CASE_A_ID = "1111222233334444";
    private static final String CASE_B_ID = "5555666677778888";
    private static final String CASE_C_ID = "9999000011112222";

    @MockitoBean
    private CcdService ccdService;

    @MockitoBean
    private UpdateCcdCaseService updateCcdCaseService;

    @MockitoBean
    private IdamService idamService;

    @BeforeEach
    void setUp() {
        final CcdCallbackController controller = new CcdCallbackController(authorisationService, deserializer, dispatcher);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mapper.findAndRegisterModules();
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
    }

    @Test
    void givenCaseBAddedToCaseA_thenLinkToCaseAIsAddedToCaseB() throws Exception {
        final SscsCaseDetails caseB = sscsCaseDetails(CASE_B_ID, null);
        when(ccdService.getByCaseId(eq(parseLong(CASE_B_ID)), any())).thenReturn(caseB);
        setCallbackJson(caseData(CASE_A_ID, "Yes", caseLinks()), caseData(CASE_A_ID, "Yes", caseLinks(CASE_B_ID)));

        final PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(ABOUT_TO_SUBMIT);

        assertThat(response.getErrors()).isEmpty();
        assertThat(caseReferences(response.getData().getAssociatedCase())).containsExactly(CASE_B_ID);
        applyCaseUpdate(CASE_B_ID, caseB);
        assertThat(caseReferences(caseB.getData().getAssociatedCase())).containsExactly(CASE_A_ID);
        assertThat(caseB.getData().getLinkedCasesBoolean()).isEqualTo("Yes");
    }

    @Test
    void givenCaseBAddedToCaseA_andCaseBAlreadyLinkedToCaseA_thenCaseBIsNotUpdated() throws Exception {
        when(ccdService.getByCaseId(eq(parseLong(CASE_B_ID)), any()))
            .thenReturn(sscsCaseDetails(CASE_B_ID, caseLinks(CASE_A_ID)));
        setCallbackJson(caseData(CASE_A_ID, "Yes", caseLinks()), caseData(CASE_A_ID, "Yes", caseLinks(CASE_B_ID)));

        final PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(ABOUT_TO_SUBMIT);

        assertThat(response.getErrors()).isEmpty();
        verify(updateCcdCaseService, never()).updateCaseV2(anyLong(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void givenOnlyRelatedCaseRemovedFromCaseA_thenLinkToCaseAIsRemovedFromCaseB() throws Exception {
        final SscsCaseDetails caseB = sscsCaseDetails(CASE_B_ID, caseLinks(CASE_A_ID));
        when(ccdService.getByCaseId(eq(parseLong(CASE_B_ID)), any())).thenReturn(caseB);
        setCallbackJson(caseData(CASE_A_ID, "Yes", caseLinks(CASE_B_ID)), caseData(CASE_A_ID, "No", caseLinks()));

        final PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(ABOUT_TO_SUBMIT);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getAssociatedCase()).isNullOrEmpty();
        applyCaseUpdate(CASE_B_ID, caseB);
        assertThat(caseB.getData().getAssociatedCase()).isEmpty();
        assertThat(caseB.getData().getLinkedCasesBoolean()).isEqualTo("No");
    }

    @Test
    void givenOneOfTwoRelatedCasesRemovedFromCaseA_thenOnlyTheRemovedCaseIsUpdated() throws Exception {
        final SscsCaseDetails caseB = sscsCaseDetails(CASE_B_ID, caseLinks(CASE_A_ID, CASE_C_ID));
        when(ccdService.getByCaseId(eq(parseLong(CASE_B_ID)), any())).thenReturn(caseB);
        when(ccdService.getByCaseId(eq(parseLong(CASE_C_ID)), any()))
            .thenReturn(sscsCaseDetails(CASE_C_ID, caseLinks(CASE_A_ID)));
        setCallbackJson(caseData(CASE_A_ID, "Yes", caseLinks(CASE_B_ID, CASE_C_ID)),
            caseData(CASE_A_ID, "Yes", caseLinks(CASE_C_ID)));

        final PreSubmitCallbackResponse<SscsCaseData> response = assertResponseOkAndGetResult(ABOUT_TO_SUBMIT);

        assertThat(response.getErrors()).isEmpty();
        assertThat(caseReferences(response.getData().getAssociatedCase())).containsExactly(CASE_C_ID);
        applyCaseUpdate(CASE_B_ID, caseB);
        assertThat(caseReferences(caseB.getData().getAssociatedCase())).containsExactly(CASE_C_ID);
        assertThat(caseB.getData().getLinkedCasesBoolean()).isEqualTo("Yes");
        verify(updateCcdCaseService, never()).updateCaseV2(eq(parseLong(CASE_C_ID)), anyString(), anyString(), anyString(),
            any(), any());
    }


    private void applyCaseUpdate(final String caseId, final SscsCaseDetails caseDetails) {
        final ArgumentCaptor<Consumer<SscsCaseDetails>> mutatorCaptor = ArgumentCaptor.captor();
        verify(updateCcdCaseService).updateCaseV2(eq(parseLong(caseId)), eq(UPDATE_CASE_ONLY.getCcdType()), anyString(),
            anyString(), any(IdamTokens.class), mutatorCaptor.capture());
        mutatorCaptor.getValue().accept(caseDetails);
    }

    private void setCallbackJson(final SscsCaseData caseDataBefore, final SscsCaseData caseDataAfter) throws Exception {
        final Callback<SscsCaseData> callback = new Callback<>(caseDetails(caseDataAfter),
            Optional.of(caseDetails(caseDataBefore)), ASSOCIATE_CASE, false);
        json = mapper.writeValueAsString(callback);
    }

    private static CaseDetails<SscsCaseData> caseDetails(final SscsCaseData sscsCaseData) {
        return new CaseDetails<>(parseLong(sscsCaseData.getCcdCaseId()), JURISDICTION, State.WITH_DWP, sscsCaseData,
            LocalDateTime.now(), "Benefit");
    }

    private static SscsCaseData caseData(final String caseId, final String linkedCasesBoolean,
                                         final List<CaseLink> associatedCases) {
        return SscsCaseData.builder()
            .ccdCaseId(caseId)
            .state(State.WITH_DWP)
            .appeal(Appeal.builder().build())
            .linkedCasesBoolean(linkedCasesBoolean)
            .associatedCase(associatedCases)
            .build();
    }

    private static SscsCaseDetails sscsCaseDetails(final String caseId, final List<CaseLink> associatedCases) {
        return SscsCaseDetails.builder()
            .id(parseLong(caseId))
            .data(caseData(caseId, associatedCases == null ? null : "Yes", associatedCases))
            .build();
    }

    private static List<CaseLink> caseLinks(final String... caseReferences) {
        final List<CaseLink> caseLinks = new ArrayList<>();
        for (final String caseReference : caseReferences) {
            caseLinks.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference(caseReference).build()).build());
        }
        return caseLinks;
    }

    private static List<String> caseReferences(final List<CaseLink> caseLinks) {
        return caseLinks.stream().map(caseLink -> caseLink.getValue().getCaseReference()).toList();
    }
}
