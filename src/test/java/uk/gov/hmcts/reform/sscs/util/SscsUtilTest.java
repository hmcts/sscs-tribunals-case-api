package uk.gov.hmcts.reform.sscs.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.CORRECTION_GRANTED;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_CORRECTED_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.FINAL_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.NOT_ATTENDING;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.PAPER;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.TELEPHONE;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;

@ExtendWith(MockitoExtension.class)
class SscsUtilTest {
    public static final String UNEXPECTED_POST_HEARING_REVIEW_TYPE_AND_ACTION = "getting the document type has an unexpected postHearingReviewType and action";
  
    private SessionCategoryMapService categoryMapService = new SessionCategoryMapService();
    private PostHearing postHearing;
    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        postHearing = PostHearing.builder()
            .correction(Correction.builder()
                .isCorrectionFinalDecisionInProgress(YesNo.NO)
                .build())
            .build();

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
        SscsCaseData caseData = SscsCaseData.builder()
            .postHearing(postHearing)
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YesNo.YES)
                .build())
            .documentStaging(DocumentStaging.builder()
                .dateAdded(LocalDate.now())
                .build())
            .build();

        SscsUtil.clearPostHearingFields(caseData, false);

        assertThat(caseData.getPostHearing().getRequestType()).isNotNull();
        assertThat(caseData.getDocumentGeneration().getGenerateNotice()).isNull();
        assertThat(caseData.getDocumentStaging().getDateAdded()).isNull();
    }

    @Test
    void givenPostHearingsEnabledTrue_clearPostHearingsFieldClearsDocumentFields_andClearsPostHearing() {
        postHearing.setRequestType(PostHearingRequestType.SET_ASIDE);
        SscsCaseData caseData = SscsCaseData.builder()
            .postHearing(postHearing)
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YesNo.YES)
                .build())
            .documentStaging(DocumentStaging.builder()
                .dateAdded(LocalDate.now())
                .build())
            .build();

        SscsUtil.clearPostHearingFields(caseData, true);

        assertThat(caseData.getPostHearing().getRequestType()).isNull();
        assertThat(caseData.getDocumentGeneration().getGenerateNotice()).isNull();
        assertThat(caseData.getDocumentStaging().getDateAdded()).isNull();
    }

    @Test
    void givenPostponement_thenClearPostponementFieldsOn() {
        SscsCaseData caseData = SscsCaseData.builder()
            .postponement(Postponement.builder()
                .postponementEvent(READY_TO_LIST)
                .unprocessedPostponement(YesNo.YES)
                .build())
            .postponementRequest(PostponementRequest.builder()
                .unprocessedPostponementRequest(YesNo.YES)
                .actionPostponementRequestSelected("TEST")
                .build())
            .build();

        clearPostponementTransientFields(caseData);
        assertNull(caseData.getPostponement().getPostponementEvent());
        assertNull(caseData.getPostponement().getUnprocessedPostponement());
        assertNull(caseData.getPostponementRequest().getUnprocessedPostponementRequest());
        assertNull(caseData.getPostponementRequest().getActionPostponementRequestSelected());
    }

    @Test
    void givenCorrectIssueAndBenefitCode_dontAddErrorToResponse() {
        SscsCaseData caseData = SscsCaseData.builder().benefitCode("002").issueCode("DD").build();
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);
        validateBenefitIssueCode(caseData, response, categoryMapService);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenWrongIssueAndBenefitCode_addErrorToResponse() {
        SscsCaseData caseData = SscsCaseData.builder().benefitCode("002").issueCode("XA").build();
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);
        validateBenefitIssueCode(caseData, response, categoryMapService);

        assertThat(response.getErrors().size()).isEqualTo(1);
        assertThat(response.getErrors()).contains(INVALID_BENEFIT_ISSUE_CODE);
    }

    @Test
    void givenLegacyBenefitCode_addErrorToResponse() {
        SscsCaseData caseData = SscsCaseData.builder().benefitCode("032").issueCode("CR").build();
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);
        validateBenefitIssueCode(caseData, response, categoryMapService);

        assertThat(response.getErrors().size()).isEqualTo(1);
        assertThat(response.getErrors()).contains(BENEFIT_CODE_NOT_IN_USE);
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
}
