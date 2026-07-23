package uk.gov.hmcts.reform.sscs.util;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.CORRECTION_GRANTED;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_CORRECTED_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.FINAL_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.NOT_ATTENDING;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.PAPER;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.TELEPHONE;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.clearPostponementTransientFields;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.generateUniqueIbcaId;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getIssueFinalDecisionDocumentType;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getPortOfEntryFromCode;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getPortsOfEntry;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getPostHearingReviewDocumentType;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getSscsType;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getWriteFinalDecisionDocumentType;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.handleIbcaCase;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.updateHearingChannel;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.updateHearingInterpreter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correction;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrectionActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentStaging;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingSubtype;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.InternalCaseDocumentData;
import uk.gov.hmcts.reform.sscs.ccd.domain.JudicialUserPanel;
import uk.gov.hmcts.reform.sscs.ccd.domain.LibertyToApplyActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberExclusions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PermissionToAppealActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Postponement;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SetAsideActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFinalDecisionCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.StatementOfReasonsActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.UkPortOfEntry;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@ExtendWith(MockitoExtension.class)
class SscsUtilTest {
    private PostHearing postHearing;
    private SscsCaseData caseData;

    @Mock
    private SscsFinalDecisionCaseData finalDecisionCaseData;
    @Mock
    private SscsCaseData mockedCaseData;

    @Mock
    private FooterService footerService;

    @BeforeEach
    void setUp() {
        postHearing = PostHearing.builder().correction(Correction.builder()
                .isCorrectionFinalDecisionInProgress(YesNo.NO).build()).build();
        caseData = new SscsCaseData();
        caseData.setPostHearing(postHearing);
    }

    @Test
    void givenPostHearingsEnabledFalse_returnDecisionNotice() {
        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, false);
        assertThat(documentType).isEqualTo(DocumentType.DECISION_NOTICE);
    }

    @Test
    void givenPostHearingsReviewTypeIsNull_returnDecisionNotice() {
        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, true);
        assertThat(documentType).isEqualTo(DocumentType.DECISION_NOTICE);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "GRANT,SET_ASIDE_GRANTED",
        "REFUSE,SET_ASIDE_REFUSED"
    })
    void givenActionTypeSetAside_shouldReturnSetAsideDocument(SetAsideActions action, DocumentType expectedDocumentType) {
        postHearing.setReviewType(PostHearingReviewType.SET_ASIDE);
        postHearing.getSetAside().setAction(action);

        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, true);

        assertThat(documentType).isEqualTo(expectedDocumentType);
    }

    @Test
    void givenActionTypeSetAsideGrantedSelected_shouldReturnSetAsideGrantedDocument() {
        postHearing.setReviewType(PostHearingReviewType.SET_ASIDE);
        postHearing.getSetAside().setAction(SetAsideActions.GRANT);

        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, true);

        assertThat(documentType).isEqualTo(DocumentType.SET_ASIDE_GRANTED);
    }

    @Test
    void givenActionTypeCorrectionGrantedSelected_shouldThrowError() {
        postHearing.setReviewType(PostHearingReviewType.CORRECTION);
        postHearing.getCorrection().setAction(CorrectionActions.GRANT);

        assertThatThrownBy(() -> getPostHearingReviewDocumentType(postHearing, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("getting the document type has an unexpected postHearingReviewType and action");
    }

    @Test
    void givenActionTypeCorrectionRefusedSelected_shouldReturnCorrectionRefusedDocument() {
        postHearing.setReviewType(PostHearingReviewType.CORRECTION);
        postHearing.getCorrection().setAction(CorrectionActions.REFUSE);

        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, true);

        assertThat(documentType).isEqualTo(DocumentType.CORRECTION_REFUSED);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "GRANT,STATEMENT_OF_REASONS_GRANTED",
        "REFUSE,STATEMENT_OF_REASONS_REFUSED"
    })
    void givenActionTypeSor_shouldReturnSorDocument(StatementOfReasonsActions action, DocumentType expectedDocumentType) {
        postHearing.setReviewType(PostHearingReviewType.STATEMENT_OF_REASONS);
        postHearing.getStatementOfReasons().setAction(action);

        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, true);

        assertThat(documentType).isEqualTo(expectedDocumentType);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "GRANT,PERMISSION_TO_APPEAL_GRANTED",
        "REFUSE,PERMISSION_TO_APPEAL_REFUSED",
        "REVIEW,REVIEW_AND_SET_ASIDE"
    })
    void givenActionTypePta_shouldReturnPtaDocument(PermissionToAppealActions action, DocumentType expectedDocumentType) {
        postHearing.setReviewType(PostHearingReviewType.PERMISSION_TO_APPEAL);
        postHearing.getPermissionToAppeal().setAction(action);

        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, true);

        assertThat(documentType).isEqualTo(expectedDocumentType);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "GRANT,LIBERTY_TO_APPLY_GRANTED",
        "REFUSE,LIBERTY_TO_APPLY_REFUSED"
    })
    void givenActionTypeLta_shouldReturnLtaDocument(LibertyToApplyActions action, DocumentType expectedDocumentType) {
        postHearing.setReviewType(PostHearingReviewType.LIBERTY_TO_APPLY);
        postHearing.getLibertyToApply().setAction(action);

        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, true);

        assertThat(documentType).isEqualTo(expectedDocumentType);
    }

    @Test
    void givenPostHearingsFlagIsTrueAndCorrectionNotInProgress_shouldReturnDraftDecisionNotice() {
        assertThat(getWriteFinalDecisionDocumentType(caseData, true)).isEqualTo(DRAFT_DECISION_NOTICE);
    }

    @Test
    void givenPostHearingsFlagIsTrueAndCorrectionInProgress_shouldReturnDraftCorrectedDecisionNotice() {
        postHearing.getCorrection().setIsCorrectionFinalDecisionInProgress(YesNo.YES);
        assertThat(getWriteFinalDecisionDocumentType(caseData, true)).isEqualTo(DRAFT_CORRECTED_NOTICE);
    }

    @Test
    void givenPostHearingsFlagIsFalseAndCorrectionInProgress_shouldReturnDraftDecisionNotice() {
        postHearing.getCorrection().setIsCorrectionFinalDecisionInProgress(YesNo.YES);
        assertThat(getWriteFinalDecisionDocumentType(caseData, false)).isEqualTo(DRAFT_DECISION_NOTICE);
    }

    @Test
    void givenPostHearingsFlagIsTrueAndCorrectionInProgress_shouldReturnCorrectionGranted() {
        postHearing.getCorrection().setIsCorrectionFinalDecisionInProgress(YesNo.YES);
        assertThat(getIssueFinalDecisionDocumentType(caseData, true)).isEqualTo(CORRECTION_GRANTED);
    }

    @Test
    void givenPostHearingsFlagIsTrueAndCorrectionNotInProgress_shouldReturnFinalDecisionNotice() {
        assertThat(getIssueFinalDecisionDocumentType(caseData, true)).isEqualTo(FINAL_DECISION_NOTICE);
    }

    @Test
    void givenPostHearingsFlagIsFalseAndCorrectionInProgress_shouldReturnFinalDecisionNotice() {
        postHearing.getCorrection().setIsCorrectionFinalDecisionInProgress(YesNo.YES);
        assertThat(getIssueFinalDecisionDocumentType(caseData, false)).isEqualTo(FINAL_DECISION_NOTICE);
    }

    @Test
    void givenPostHearingsEnabledFalse_clearPostHearingsFieldClearsDocumentFields_butDoesNotAlterPostHearing() {
        postHearing.setRequestType(PostHearingRequestType.SET_ASIDE);
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .postHearing(postHearing)
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YesNo.YES)
                .build())
            .documentStaging(DocumentStaging.builder()
                .dateAdded(LocalDate.now())
                .build())
            .build();

        SscsUtil.clearPostHearingFields(sscsCaseData, false);

        assertThat(sscsCaseData.getPostHearing().getRequestType()).isNotNull();
        assertThat(sscsCaseData.getDocumentGeneration().getGenerateNotice()).isNull();
        assertThat(sscsCaseData.getDocumentStaging().getDateAdded()).isNull();
    }

    @Test
    void givenPostHearingsEnabledTrue_clearPostHearingsFieldClearsDocumentFields_andClearsPostHearing() {
        postHearing.setRequestType(PostHearingRequestType.SET_ASIDE);
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .postHearing(postHearing)
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YesNo.YES)
                .build())
            .documentStaging(DocumentStaging.builder()
                .dateAdded(LocalDate.now())
                .build())
            .build();

        SscsUtil.clearPostHearingFields(sscsCaseData, true);

        assertThat(sscsCaseData.getPostHearing().getRequestType()).isNull();
        assertThat(sscsCaseData.getDocumentGeneration().getGenerateNotice()).isNull();
        assertThat(sscsCaseData.getDocumentStaging().getDateAdded()).isNull();
    }

    @Test
    void givenPostponement_thenClearPostponementFieldsOn() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .postponement(Postponement.builder()
                .postponementEvent(READY_TO_LIST)
                .unprocessedPostponement(YesNo.YES)
                .build())
            .postponementRequest(PostponementRequest.builder()
                .unprocessedPostponementRequest(YesNo.YES)
                .actionPostponementRequestSelected("TEST")
                .build())
            .build();

        clearPostponementTransientFields(sscsCaseData);
        assertNull(sscsCaseData.getPostponement().getPostponementEvent());
        assertNull(sscsCaseData.getPostponement().getUnprocessedPostponement());
        assertNull(sscsCaseData.getPostponementRequest().getUnprocessedPostponementRequest());
        assertNull(sscsCaseData.getPostponementRequest().getActionPostponementRequestSelected());
    }

    @Test
    void givenHearingChannelOfNotAttending_UpdateWantsToAttendToNoAndUpdateHearingSubtype() {
        caseData.setAppeal(Appeal.builder().build());
        caseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());

        updateHearingChannel(caseData, NOT_ATTENDING);

        assertThat(caseData.getSchedulingAndListingFields().getOverrideFields().getAppellantHearingChannel()).isEqualTo(NOT_ATTENDING);
        Appeal appeal = caseData.getAppeal();
        assertThat(appeal.getHearingType()).isEqualTo(HearingType.PAPER.getValue());
        assertThat(appeal.getHearingOptions().getWantsToAttend()).isEqualTo(YesNo.NO.getValue());

        HearingSubtype hearingSubtype = appeal.getHearingSubtype();
        assertThat(hearingSubtype.isWantsHearingTypeTelephone()).isFalse();
        assertThat(hearingSubtype.isWantsHearingTypeFaceToFace()).isFalse();
        assertThat(hearingSubtype.isWantsHearingTypeVideo()).isFalse();
    }

    @Test
    void givenHearingChannelOfAttending_UpdateWantsToAttendToYesAndUpdateHearingSubtype() {
        caseData.setAppeal(Appeal.builder().build());
        caseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());

        updateHearingChannel(caseData, TELEPHONE);

        assertThat(caseData.getSchedulingAndListingFields().getOverrideFields().getAppellantHearingChannel()).isEqualTo(TELEPHONE);

        Appeal appeal = caseData.getAppeal();
        assertThat(appeal.getHearingType()).isEqualTo(HearingType.ORAL.getValue());
        assertThat(appeal.getHearingOptions().getWantsToAttend()).isEqualTo(YesNo.YES.getValue());

        HearingSubtype hearingSubtype = appeal.getHearingSubtype();
        assertThat(hearingSubtype.isWantsHearingTypeTelephone()).isTrue();
    }

    @Test
    void givenHearingChannelIsPaper_UpdateHearingSubtypeToNo() {
        caseData.setAppeal(Appeal.builder().build());
        caseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());

        updateHearingChannel(caseData, PAPER);

        assertThat(caseData.getSchedulingAndListingFields().getOverrideFields().getAppellantHearingChannel()).isEqualTo(PAPER);

        Appeal appeal = caseData.getAppeal();
        assertThat(appeal.getHearingType()).isEqualTo(HearingType.PAPER.getValue());

        HearingSubtype hearingSubtype = appeal.getHearingSubtype();
        assertThat(hearingSubtype.isWantsHearingTypeTelephone()).isFalse();
        assertThat(hearingSubtype.isWantsHearingTypeFaceToFace()).isFalse();
        assertThat(hearingSubtype.isWantsHearingTypeVideo()).isFalse();
    }

    @Test
    void givenAppellantInterpreterIsSetToYesAndLanguageIsNotNull_UpdateCaseDataInterpreter() {
        caseData.setAppeal(Appeal.builder().build());
        caseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());

        DynamicListItem interpreterLanguageItem = new DynamicListItem("test", "spanish");
        DynamicList interpreterLanguage = new DynamicList(interpreterLanguageItem, List.of());

        HearingInterpreter appellantInterpreter = HearingInterpreter.builder()
            .isInterpreterWanted(YesNo.YES)
            .interpreterLanguage(interpreterLanguage)
            .build();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        updateHearingInterpreter(caseData, response, appellantInterpreter);

        Appeal appeal = caseData.getAppeal();

        HearingOptions hearingOptions = appeal.getHearingOptions();
        assertThat(hearingOptions.getLanguageInterpreter()).isEqualTo("Yes");
        assertThat(hearingOptions.getLanguages()).isNotNull();
        assertThat(hearingOptions.getLanguages()).isEqualTo("spanish");
    }

    @Test
    void givenAppellantInterpreterIsSetToNoAndLanguageFieldIsNotEmpty_ThenClearLanguageValueField() {
        caseData.setAppeal(Appeal.builder().build());
        caseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());

        DynamicListItem interpreterLanguageItem = new DynamicListItem("test1", "Welsh");
        DynamicList interpreterLanguage = new DynamicList(interpreterLanguageItem, List.of());

        HearingInterpreter appellantInterpreter = HearingInterpreter.builder()
            .isInterpreterWanted(YesNo.NO)
            .interpreterLanguage(interpreterLanguage)
            .build();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        updateHearingInterpreter(caseData, response, appellantInterpreter);

        assertEquals(0, response.getErrors().size());
        assertThat(response.getData().getSchedulingAndListingFields().getOverrideFields().getAppellantInterpreter().getInterpreterLanguage()).isNull();
        assertNull(response.getData().getAppeal().getHearingOptions().getLanguages());
    }

    @Test
    void givenAppellantInterpreterIsSetToNoAndLanguageFieldIsEmpty_ThenUpdateThisOnCaseData() {
        caseData.setAppeal(Appeal.builder().build());
        caseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());

        caseData.getAppeal().setHearingOptions(HearingOptions.builder()
            .languageInterpreter("Yes")
            .languages("French")
            .build());

        HearingInterpreter appellantInterpreter = HearingInterpreter.builder()
            .isInterpreterWanted(YesNo.NO)
            .build();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        updateHearingInterpreter(caseData, response, appellantInterpreter);

        Appeal appeal = caseData.getAppeal();

        HearingOptions hearingOptions = appeal.getHearingOptions();
        assertThat(hearingOptions.getLanguageInterpreter()).isEqualTo("No");
        assertNull(hearingOptions.getLanguages());
    }

    @Test
    void givenAppellantInterpreterIsSetToYesAndLanguageIsNull_ThrowAnError() {
        caseData.setAppeal(Appeal.builder().build());
        caseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());

        HearingInterpreter appellantInterpreter = HearingInterpreter.builder()
            .isInterpreterWanted(YesNo.YES)
            .interpreterLanguage(null)
            .build();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        updateHearingInterpreter(caseData, response, appellantInterpreter);

        assertEquals(1, response.getErrors().size());
        assertEquals("Interpreter language must be selected if an interpreter is wanted.", response.getErrors().toArray()[0]);
    }

    @Test
    void givenAppellantInterpreterIsSetToYesAndLanguageValueIsNull_ThrowAnError() {
        caseData.setAppeal(Appeal.builder().build());
        caseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());

        DynamicListItem interpreterLanguageItem2 = new DynamicListItem("test", "Italian");
        DynamicListItem interpreterLanguageItem3 = new DynamicListItem("test1", "Persian");
        DynamicList interpreterLanguage = new DynamicList(null, List.of(interpreterLanguageItem2, interpreterLanguageItem3));

        HearingInterpreter appellantInterpreter = HearingInterpreter.builder()
            .isInterpreterWanted(YesNo.YES)
            .interpreterLanguage(interpreterLanguage)
            .build();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        updateHearingInterpreter(caseData, response, appellantInterpreter);

        assertEquals(1, response.getErrors().size());
        assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isNull();
        assertEquals("Interpreter language must be selected if an interpreter is wanted.", response.getErrors().toArray()[0]);
    }

    @Test
    void givenEmploymentAndSupportAllowanceBenefitCodeThenReturnSscs1Type() {
        assertEquals("SSCS1", getSscsType(SscsCaseData.builder().benefitCode("051").build()));
    }

    @Test
    void givenChildSupportBenefitCodeThenReturnSscs2Type() {
        assertEquals("SSCS2", getSscsType(SscsCaseData.builder().benefitCode("022").build()));
    }

    @Test
    void givenGuardiansAllowanceBenefitCodeThenReturnSscs5Type() {
        assertEquals("SSCS5", getSscsType(SscsCaseData.builder().benefitCode("015").build()));
    }

    @Test
    void givenInfectedBloodCompensationBenefitCodeThenReturnSscs8Type() {
        assertEquals("SSCS8", getSscsType(SscsCaseData.builder().benefitCode("093").build()));
    }

    @Test
    void givenNullBenefitCodeThenReturnNull() {
        assertNull(getSscsType(SscsCaseData.builder().build()));
    }

    @Test
    void shouldReturnPortsOfEntry() {
        final DynamicList portsOfEntry = getPortsOfEntry();

        assertThat(portsOfEntry.getValue()).isNull();
        assertThat(portsOfEntry.getListItems()).hasSize(269);
    }


    @ParameterizedTest
    @EnumSource(value = UkPortOfEntry.class)
    void shouldReturnPortOfEntryFromCode(UkPortOfEntry portOfEntry) {
        final DynamicListItem portOfEntryItem = getPortOfEntryFromCode(portOfEntry.getLocationCode());
        assertThat(portOfEntryItem.getCode()).isEqualTo(portOfEntry.getLocationCode());
        assertThat(portOfEntryItem.getLabel()).isEqualTo(portOfEntry.getLabel());
    }

    @Test
    void shouldReturnNullPortOfEntryFromInvalidCode() {
        final DynamicListItem portOfEntryItem = getPortOfEntryFromCode("invalid-code");
        assertThat(portOfEntryItem.getCode()).isNull();
        assertThat(portOfEntryItem.getLabel()).isNull();
    }

    @Test
    void shouldPopulateIbcaFieldsOnHandleIbcaCaseWithNoHearingOptions() {
        final SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().build())
                .build()
            )
            .regionalProcessingCenter(RegionalProcessingCenter.builder().build())
            .build();

        handleIbcaCase(sscsCaseData);

        assertThat(sscsCaseData.getAppeal().getHearingOptions().getHearingRoute()).isEqualTo(LIST_ASSIST);
        assertThat(sscsCaseData.getAppeal().getMrnDetails().getDwpIssuingOffice()).isEqualTo("IBCA");
        assertThat(sscsCaseData.getRegionalProcessingCenter().getHearingRoute()).isEqualTo(LIST_ASSIST);
    }


    @Test
    void shouldPopulateIbcaFieldsOnHandleIbcaCaseWithMrnDetails() {
        final SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().build())
            .regionalProcessingCenter(RegionalProcessingCenter.builder().build())
            .build();

        handleIbcaCase(sscsCaseData);

        assertThat(sscsCaseData.getAppeal().getHearingOptions().getHearingRoute()).isEqualTo(LIST_ASSIST);
        assertThat(sscsCaseData.getAppeal().getMrnDetails().getDwpIssuingOffice()).isEqualTo("IBCA");
        assertThat(sscsCaseData.getRegionalProcessingCenter().getHearingRoute()).isEqualTo(LIST_ASSIST);
    }


    @Test
    void shouldPopulateIbcaFieldsOnHandleIbcaCaseWithHearingOptions() {
        final SscsCaseData sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().build())
                .hearingOptions(HearingOptions.builder().hearingRoute(GAPS).build())
                .build()
            )
            .regionalProcessingCenter(RegionalProcessingCenter.builder().build())
            .build();

        handleIbcaCase(sscsCaseData);

        assertThat(sscsCaseData.getAppeal().getHearingOptions().getHearingRoute()).isEqualTo(LIST_ASSIST);
        assertThat(sscsCaseData.getAppeal().getMrnDetails().getDwpIssuingOffice()).isEqualTo("IBCA");
        assertThat(sscsCaseData.getRegionalProcessingCenter().getHearingRoute()).isEqualTo(LIST_ASSIST);
    }

    @Test
    void shouldGenerateUniqueIbcaId() {
        final Appellant appellant = Appellant.builder()
            .name(Name.builder()
                .lastName("Test")
                .build()
            )
            .identity(Identity.builder()
                .ibcaReference("IBCA12345")
                .build()
            )
            .build();

        final String result = generateUniqueIbcaId(appellant);

        assertThat(result).isEqualTo("Test_IBCA12345");
    }

    @Test
    void shouldReturnTrueWhenIsIbcaCase() {
        final SscsCaseData sscsCaseData = SscsCaseData.builder()
            .benefitCode("093")
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .descriptionSelection(
                        new DynamicList(
                            new DynamicListItem(
                                "infectedBloodCompensation",
                                "infectedBloodCompensation"
                            ),
                            emptyList()
                        )
                    )
                    .build()
                )
                .build()
            )
            .build();
        assertTrue(sscsCaseData.isIbcCase());
    }

    @Test
    void shouldReturnFalseWhenNotIbcaCase() {
        final SscsCaseData sscsCaseData = SscsCaseData.builder()
            .benefitCode("037")
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .descriptionSelection(
                        new DynamicList(
                            new DynamicListItem(
                                "DLA",
                                "DLA"
                            ),
                            emptyList()
                        )
                    )
                    .build()
                )
                .build()
            )
            .build();
        assertFalse(sscsCaseData.isIbcCase());
    }

    @Test
    void testBuildWriteFinalDecisionHeldBefore_WithAllPanelMembers() {
        when(mockedCaseData.getSscsFinalDecisionCaseData()).thenReturn(finalDecisionCaseData);
        when(finalDecisionCaseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName()).thenReturn("Disability Member");
        when(finalDecisionCaseData.getWriteFinalDecisionOtherPanelMemberName()).thenReturn("Other Member");
        when(finalDecisionCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName()).thenReturn("Medical Member");
        when(finalDecisionCaseData.getWriteFinalDecisionFinanciallyQualifiedPanelMemberName()).thenReturn("Financial Member");

        String result = SscsUtil.buildWriteFinalDecisionHeldBefore(mockedCaseData, "Judge Name");

        assertEquals("Judge Name, Disability Member, Other Member, Medical Member and Financial Member", result);
    }

    @Test
    void testBuildWriteFinalDecisionHeldBefore_WithNoPanelMembers() {
        when(mockedCaseData.getSscsFinalDecisionCaseData()).thenReturn(finalDecisionCaseData);
        when(finalDecisionCaseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName()).thenReturn(null);
        when(finalDecisionCaseData.getWriteFinalDecisionOtherPanelMemberName()).thenReturn(null);
        when(finalDecisionCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName()).thenReturn(null);
        when(finalDecisionCaseData.getWriteFinalDecisionFinanciallyQualifiedPanelMemberName()).thenReturn(null);

        String result = SscsUtil.buildWriteFinalDecisionHeldBefore(mockedCaseData, "Judge Name");

        assertEquals("Judge Name", result);
    }

    @Test
    void testBuildWriteFinalDecisionHeldBefore_WithSomePanelMembers() {
        when(mockedCaseData.getSscsFinalDecisionCaseData()).thenReturn(finalDecisionCaseData);
        when(finalDecisionCaseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName()).thenReturn("Disability Member");
        when(finalDecisionCaseData.getWriteFinalDecisionOtherPanelMemberName()).thenReturn(null);
        when(finalDecisionCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName()).thenReturn("Medical Member");
        when(finalDecisionCaseData.getWriteFinalDecisionFinanciallyQualifiedPanelMemberName()).thenReturn(null);

        String result = SscsUtil.buildWriteFinalDecisionHeldBefore(mockedCaseData, "Judge Name");

        assertEquals("Judge Name, Disability Member and Medical Member", result);
    }

    @Test
    void testAddPanelMembersToExclusions_WithPanelMemberExclusions() {
        PanelMemberExclusions panelMemberExclusions = PanelMemberExclusions.builder().build();
        SchedulingAndListingFields schedulingAndListingFields = SchedulingAndListingFields.builder().panelMemberExclusions(panelMemberExclusions).build();
        when(mockedCaseData.getSchedulingAndListingFields()).thenReturn(schedulingAndListingFields);
        List<CollectionItem<JudicialUserBase>> panelMembers = List.of(
            new CollectionItem<>("1", JudicialUserBase.builder().idamId("3").personalCode("Panel Member 1").build()),
            new CollectionItem<>("2", JudicialUserBase.builder().idamId("4").personalCode("Panel Member 2").build())
        );
        JudicialUserPanel panel = JudicialUserPanel.builder().panelMembers(panelMembers).build();
        Hearing hearing = Hearing.builder()
            .value(HearingDetails.builder().panel(panel).build())
            .build();
        when(mockedCaseData.getLatestHearing()).thenReturn(hearing);
        assertNull(schedulingAndListingFields.getPanelMemberExclusions().getExcludedPanelMembers());
        assertNull(schedulingAndListingFields.getPanelMemberExclusions().getArePanelMembersExcluded());
        SscsUtil.addPanelMembersToExclusions(mockedCaseData, false);
        assertEquals(panelMembers, schedulingAndListingFields.getPanelMemberExclusions().getExcludedPanelMembers());
        assertEquals(YesNo.YES, schedulingAndListingFields.getPanelMemberExclusions().getArePanelMembersExcluded());
    }

    @Test
    void testAddPanelMembersToExclusions_WithNullPanelMemberExclusions() {
        SchedulingAndListingFields schedulingAndListingFields = SchedulingAndListingFields.builder().build();
        when(mockedCaseData.getSchedulingAndListingFields()).thenReturn(schedulingAndListingFields);
        List<CollectionItem<JudicialUserBase>> panelMembers = List.of(
            new CollectionItem<>("1", JudicialUserBase.builder().idamId("3").personalCode("Panel Member 1").build()),
            new CollectionItem<>("2", JudicialUserBase.builder().idamId("4").personalCode("Panel Member 2").build())
        );
        JudicialUserPanel panel = JudicialUserPanel.builder().panelMembers(panelMembers).build();
        Hearing hearing = Hearing.builder()
            .value(HearingDetails.builder().panel(panel).build())
            .build();
        when(mockedCaseData.getLatestHearing()).thenReturn(hearing);
        assertNull(schedulingAndListingFields.getPanelMemberExclusions());
        assertNull(schedulingAndListingFields.getPanelMemberExclusions());
        SscsUtil.addPanelMembersToExclusions(mockedCaseData, false);
        assertEquals(panelMembers, schedulingAndListingFields.getPanelMemberExclusions().getExcludedPanelMembers());
        assertEquals(YesNo.YES, schedulingAndListingFields.getPanelMemberExclusions().getArePanelMembersExcluded());
    }

    @Test
    void testAddPanelMembersToExclusions_DoNotAddWithNullPanel() {
        SchedulingAndListingFields schedulingAndListingFields = SchedulingAndListingFields.builder().build();
        when(mockedCaseData.getSchedulingAndListingFields()).thenReturn(schedulingAndListingFields);
        Hearing hearing = Hearing.builder().value(HearingDetails.builder().build()).build();
        when(mockedCaseData.getLatestHearing()).thenReturn(hearing);
        assertNull(schedulingAndListingFields.getPanelMemberExclusions());
        assertNull(schedulingAndListingFields.getPanelMemberExclusions());
        SscsUtil.addPanelMembersToExclusions(mockedCaseData, false);
        assertNull(schedulingAndListingFields.getPanelMemberExclusions().getArePanelMembersExcluded());
        assertNull(schedulingAndListingFields.getPanelMemberExclusions().getExcludedPanelMembers());
        assertNull(schedulingAndListingFields.getPanelMemberExclusions().getReservedPanelMembers());
        assertNull(schedulingAndListingFields.getPanelMemberExclusions().getArePanelMembersReserved());
    }

    @Test
    void testAddPanelMembersToExclusions_DoNotAddWithNullLatestHearing() {
        SchedulingAndListingFields schedulingAndListingFields = SchedulingAndListingFields.builder().build();
        when(mockedCaseData.getSchedulingAndListingFields()).thenReturn(schedulingAndListingFields);
        assertNull(schedulingAndListingFields.getPanelMemberExclusions());
        assertNull(schedulingAndListingFields.getPanelMemberExclusions());
        SscsUtil.addPanelMembersToExclusions(mockedCaseData, false);
        assertNull(schedulingAndListingFields.getPanelMemberExclusions().getArePanelMembersExcluded());
        assertNull(schedulingAndListingFields.getPanelMemberExclusions().getExcludedPanelMembers());
        assertNull(schedulingAndListingFields.getPanelMemberExclusions().getReservedPanelMembers());
        assertNull(schedulingAndListingFields.getPanelMemberExclusions().getArePanelMembersReserved());
    }

    @Test
    void setHearingRouteIfNotSet_shouldNotSetIfExisting() {
        SchedulingAndListingFields schedulingAndListingFields = SchedulingAndListingFields.builder()
            .hearingRoute(LIST_ASSIST).build();
        when(mockedCaseData.getSchedulingAndListingFields()).thenReturn(schedulingAndListingFields);
        SscsUtil.setHearingRouteIfNotSet(mockedCaseData);
        verify(mockedCaseData, never()).setSchedulingAndListingFields(any());
    }

    @ParameterizedTest
    @EnumSource(value = HearingRoute.class)
    void setHearingRouteIfNotSet_shouldSetIfNullSnlFields(HearingRoute hearingRoute) {
        RegionalProcessingCenter regionalProcessingCenter = RegionalProcessingCenter.builder()
            .hearingRoute(hearingRoute).build();
        caseData.setRegionalProcessingCenter(regionalProcessingCenter);
        SscsUtil.setHearingRouteIfNotSet(caseData);
        assertEquals(hearingRoute, caseData.getSchedulingAndListingFields().getHearingRoute());
    }

    @ParameterizedTest
    @EnumSource(value = HearingRoute.class)
    void setHearingRouteIfNotSet_shouldSetIfNullSnlFieldHearingRoute(HearingRoute hearingRoute) {
        SchedulingAndListingFields schedulingAndListingFields = SchedulingAndListingFields.builder().build();
        RegionalProcessingCenter regionalProcessingCenter = RegionalProcessingCenter.builder()
            .hearingRoute(hearingRoute).build();
        caseData.setSchedulingAndListingFields(schedulingAndListingFields);
        caseData.setRegionalProcessingCenter(regionalProcessingCenter);
        SscsUtil.setHearingRouteIfNotSet(caseData);
        assertEquals(hearingRoute, caseData.getSchedulingAndListingFields().getHearingRoute());
    }

    @ParameterizedTest
    @EnumSource(value = HearingRoute.class)
    void setHearingRouteIfNotSet_shouldSetToNullIfNoRpc(HearingRoute hearingRoute) {
        SscsUtil.setHearingRouteIfNotSet(caseData);
        assertNull(caseData.getSchedulingAndListingFields().getHearingRoute());
    }

    @Test
    void testAddDocumentToDocumentTabAndBundle() {
        SscsUtil.addDocumentToDocumentTabAndBundle(footerService, caseData, DocumentLink.builder().build(), DocumentType.DECISION_NOTICE);
        verify(footerService).createFooterAndAddDocToCase(any(DocumentLink.class), eq(caseData), eq(DocumentType.DECISION_NOTICE), any(),
            eq(null), eq(null), eq(null), eq(null), eq(false));
    }

    @Test
    void testAddDocumentToDocumentTabAndBundleWithEventType() {
        SscsUtil.addDocumentToDocumentTabAndBundle(footerService, caseData, DocumentLink.builder().build(), DocumentType.DECISION_NOTICE, READY_TO_LIST);
        verify(footerService).createFooterAndAddDocToCase(any(DocumentLink.class), eq(caseData), eq(DocumentType.DECISION_NOTICE), any(),
            eq(null), eq(null), eq(null), eq(READY_TO_LIST), eq(false));
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testAddDocumentToDocumentTabAndBundleWithIssueBool(boolean issue) {
        SscsUtil.addDocumentToDocumentTabAndBundle(footerService, caseData, DocumentLink.builder().build(), DocumentType.DECISION_NOTICE, null, issue);
        verify(footerService).createFooterAndAddDocToCase(any(DocumentLink.class), eq(caseData), eq(DocumentType.DECISION_NOTICE), any(),
            eq(null), eq(null), eq(null), eq(null), eq(issue));
    }

    @Test
    void testAddDocumentToCaseDataDocuments() {
        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build();
        SscsUtil.addDocumentToCaseDataDocuments(caseData, sscsDocument);

        List<SscsDocument> documents = caseData.getSscsDocument();
        assertNotNull(documents);
        assertEquals(1, documents.size());
        assertEquals(sscsDocument, documents.getFirst());
    }

    @Test
    void testRemoveDocumentFromCaseDataDocuments() {
        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentLink(DocumentLink.builder().documentUrl("some-url/1029103123").build()).build()).build();
        SscsDocument sscsDocument2 = SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentLink(DocumentLink.builder().documentUrl("some-url/1029103126").build()).build()).build();
        List<SscsDocument> documents = new ArrayList<>();
        documents.add(sscsDocument);
        documents.add(sscsDocument2);
        caseData.setSscsDocument(documents);

        SscsUtil.removeDocumentFromCaseDataDocuments(caseData, sscsDocument2);

        List<SscsDocument> updatedDocuments = caseData.getSscsDocument();
        assertNotNull(updatedDocuments);
        assertEquals(1, updatedDocuments.size());
        assertNotEquals(sscsDocument2.getValue().getDocumentLink().getDocumentUrl(), updatedDocuments.getFirst().getValue().getDocumentLink().getDocumentUrl());
        assertEquals(sscsDocument.getValue().getDocumentLink().getDocumentUrl(), updatedDocuments.getFirst().getValue().getDocumentLink().getDocumentUrl());
    }

    @Test
    void testAddDocumentToCaseDataInternalDocuments() {
        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build();
        SscsUtil.addDocumentToCaseDataInternalDocuments(caseData, sscsDocument);

        InternalCaseDocumentData internalCaseDocumentData = caseData.getInternalCaseDocumentData();
        assertNotNull(internalCaseDocumentData);
        List<SscsDocument> documents = internalCaseDocumentData.getSscsInternalDocument();
        assertNotNull(documents);
        assertEquals(1, documents.size());
        assertEquals(sscsDocument, documents.getFirst());
    }

    @Test
    void testAddNonAdditionNamedDocumentToCaseDataInternalDocuments() {
        String randomName = RandomStringUtils.secure().nextAlphabetic(10);
        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentFileName(randomName).build()).build();
        SscsUtil.addDocumentToCaseDataInternalDocuments(caseData, sscsDocument);

        InternalCaseDocumentData internalCaseDocumentData = caseData.getInternalCaseDocumentData();
        assertNotNull(internalCaseDocumentData);
        List<SscsDocument> documents = internalCaseDocumentData.getSscsInternalDocument();
        assertNotNull(documents);
        assertEquals(1, documents.size());
        assertEquals(randomName, documents.getFirst().getValue().getDocumentFileName());
    }

    @Test
    void testAddAdditionNamedDocumentToCaseDataInternalDocuments() {
        String randomName = RandomStringUtils.secure().nextAlphabetic(10);
        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentFileName("Addition A - " + randomName).build()).build();
        SscsUtil.addDocumentToCaseDataInternalDocuments(caseData, sscsDocument);

        InternalCaseDocumentData internalCaseDocumentData = caseData.getInternalCaseDocumentData();
        assertNotNull(internalCaseDocumentData);
        List<SscsDocument> documents = internalCaseDocumentData.getSscsInternalDocument();
        assertNotNull(documents);
        assertEquals(1, documents.size());
        assertEquals(randomName, documents.getFirst().getValue().getDocumentFileName());
    }

    @Test
    void testRemoveDocumentFromCaseDataInternalDocuments() {
        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentLink(DocumentLink.builder().documentUrl("some-url/1029103123").build()).build()).build();
        SscsDocument sscsDocument2 = SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentLink(DocumentLink.builder().documentUrl("some-url/1029103126").build()).build()).build();
        InternalCaseDocumentData internalCaseDocumentData = InternalCaseDocumentData.builder()
            .sscsInternalDocument(new ArrayList<>(List.of(sscsDocument, sscsDocument2)))
            .build();
        caseData.setInternalCaseDocumentData(internalCaseDocumentData);

        SscsUtil.removeDocumentFromCaseDataInternalDocuments(caseData, sscsDocument);

        InternalCaseDocumentData updatedInternalCaseDocumentData = caseData.getInternalCaseDocumentData();
        assertNotNull(updatedInternalCaseDocumentData);
        List<SscsDocument> updatedDocuments = updatedInternalCaseDocumentData.getSscsInternalDocument();
        assertEquals(1, updatedDocuments.size());
        assertEquals(sscsDocument2.getValue().getDocumentLink().getDocumentUrl(), updatedDocuments.getFirst().getValue().getDocumentLink().getDocumentUrl());
        assertNotEquals(sscsDocument.getValue().getDocumentLink().getDocumentUrl(), updatedDocuments.getFirst().getValue().getDocumentLink().getDocumentUrl());
    }

    @Test
    void testInterpreterChannelChange_ForGeneratedAdjournment() {
        Adjournment adjournment = Adjournment.builder().generateNotice(YesNo.YES)
                .typeOfHearing(AdjournCaseTypeOfHearing.PAPER)
                .typeOfNextHearing(AdjournCaseTypeOfHearing.FACE_TO_FACE).build();
        caseData.setAdjournment(adjournment);
        String hearingType = null;
        assertTrue(SscsUtil.hasChannelChangedForAdjournment(caseData, hearingType));
    }

    @Test
    void testNoInterpreterChannelChange_ForGeneratedAdjournment() {
        Adjournment adjournment = Adjournment.builder().generateNotice(YesNo.YES)
                .typeOfHearing(AdjournCaseTypeOfHearing.FACE_TO_FACE)
                .typeOfNextHearing(AdjournCaseTypeOfHearing.FACE_TO_FACE).build();
        caseData.setAdjournment(adjournment);
        String hearingType = null;
        assertFalse(SscsUtil.hasChannelChangedForAdjournment(caseData, hearingType));
    }

    @Test
    void testInterpreterChannelChangePaperToOral_ForNonGeneratedAdjournment() {
        Adjournment adjournment = Adjournment.builder().generateNotice(YesNo.NO).typeOfNextHearing(AdjournCaseTypeOfHearing.PAPER).build();
        caseData.setAdjournment(adjournment);
        String hearingType = "oral";
        assertTrue(SscsUtil.hasChannelChangedForAdjournment(caseData, hearingType));
    }

    @Test
    void testInterpreterChannelChangeInPersonToPaper_ForNonGeneratedAdjournment() {
        Adjournment adjournment = Adjournment.builder().generateNotice(YesNo.NO).typeOfNextHearing(AdjournCaseTypeOfHearing.FACE_TO_FACE).build();
        caseData.setAdjournment(adjournment);
        String hearingType = "paper";
        assertTrue(SscsUtil.hasChannelChangedForAdjournment(caseData, hearingType));
    }

    @Test
    void testNoInterpreterChannelChange_ForNonGeneratedAdjournment() {
        Adjournment adjournment = Adjournment.builder().generateNotice(YesNo.NO).typeOfNextHearing(AdjournCaseTypeOfHearing.FACE_TO_FACE).build();
        caseData.setAdjournment(adjournment);
        String hearingType = "oral";
        assertFalse(SscsUtil.hasChannelChangedForAdjournment(caseData, hearingType));
    }
}
