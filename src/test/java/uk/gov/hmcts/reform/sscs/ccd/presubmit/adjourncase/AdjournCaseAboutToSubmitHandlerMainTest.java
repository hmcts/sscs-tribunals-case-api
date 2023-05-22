package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCasePanelMembersExcluded;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberExclusions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;

class AdjournCaseAboutToSubmitHandlerMainTest extends AdjournCaseAboutToSubmitHandlerTestBase {

    @BeforeEach
    void setUpMocks() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @DisplayName("Given draft adjournment notice already exists on case, then overwrite existing draft")
    @Test
    void givenAdjournmentNoticeAlreadyExistsOnCase_thenOverwriteExistingDraft() {
        SscsDocument doc = SscsDocument.builder().value(
                SscsDocumentDetails.builder()
                    .documentFileName(OLD_DRAFT_DOC)
                    .documentType(DRAFT_ADJOURNMENT_NOTICE.getValue())
                    .build())
            .build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(doc);
        callback.getCaseDetails().getCaseData().setSscsDocument(docs);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(previewDocumentService, times(1)).writePreviewDocumentToSscsDocument(
            sscsCaseData, DRAFT_ADJOURNMENT_NOTICE, null);
    }

    @DisplayName("Given an adjournment event with language interpreter required and case has existing interpreter, "
        + "then overwrite existing interpreter in hearing options")
    @Test
    void givenAdjournmentEventWithLanguageInterpreterRequiredAndCaseHasExistingInterpreter_overwriteExistingInterpreter() {
        callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterRequired(YES);
        callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterLanguage(new DynamicList(SPANISH));
        callback.getCaseDetails().getCaseData().getAppeal().setHearingOptions(HearingOptions.builder()
            .languageInterpreter(NO.getValue())
            .languages("French")
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter()).isEqualTo(YES.getValue());
        assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isEqualTo(SPANISH);
    }

    @DisplayName("Given an adjournment event with language interpreter required and interpreter language set, "
        + "then do not display error")
    @Test
    void givenAdjournmentEventWithLanguageInterpreterRequiredAndLanguageSet_thenDoNotDisplayError() {
        callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterRequired(YES);
        callback.getCaseDetails().getCaseData().getAdjournment().setInterpreterLanguage(new DynamicList(SPANISH));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getAppeal().getHearingOptions().getLanguageInterpreter()).isEqualTo(YES.getValue());
        assertThat(response.getData().getAppeal().getHearingOptions().getLanguages()).isEqualTo(SPANISH);
    }

    @DisplayName("When a previous write adjournment notice in place and you call the event the second time the generated date needs to be updated so its reflected in the issue adjournment event")
    @Test
    void givenPreviousWritenAdjournCaseTriggerAnotherThenCheckIssueAdjournmentHasMostRecentDate() {
        sscsCaseData.getAdjournment().setGeneratedDate(LocalDate.parse("2023-01-01"));
        assertThat(sscsCaseData.getAdjournment().getGeneratedDate()).isEqualTo(LocalDate.parse("2023-01-01"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        LocalDate date = response.getData().getAdjournment().getGeneratedDate();
        assertThat(date).isEqualTo(LocalDate.now());
    }


    @DisplayName("When we have written an adjournment notice and excluded some panel members, add them "
        + "to the excluded panel members list")
    @Test
    void givenPanelMembersExcluded_thenAddPanelMembersToExclusionList() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);
        sscsCaseData.getSchedulingAndListingFields().setPanelMemberExclusions(PanelMemberExclusions.builder()
            .excludedPanelMembers(new ArrayList<>(Arrays.asList(
                new CcdValue<>(JudicialUserBase.builder().idamId("1").build()),
                new CcdValue<>(JudicialUserBase.builder().idamId("2").build())))).build());

        sscsCaseData.getAdjournment().setPanelMembersExcluded(AdjournCasePanelMembersExcluded.YES);
        sscsCaseData.getAdjournment().setPanelMember1(JudicialUserBase.builder().idamId("1").build());
        sscsCaseData.getAdjournment().setPanelMember3(JudicialUserBase.builder().idamId("3").build());
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(NO);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getSchedulingAndListingFields()
            .getPanelMemberExclusions().getExcludedPanelMembers()).hasSize(3);
    }

    @DisplayName("When we have written an adjournment notice and excluded some panel members, and there are no current "
        + "exclusions,  add them to the excluded panel members list")
    @Test
    void givenNoExistingPanelMembersExcluded_thenAddPanelMembersToExclusionList() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);
        sscsCaseData.getAdjournment().setPanelMembersExcluded(AdjournCasePanelMembersExcluded.YES);
        sscsCaseData.getAdjournment().setPanelMember1(JudicialUserBase.builder().idamId("1").build());
        sscsCaseData.getAdjournment().setPanelMember3(JudicialUserBase.builder().idamId("3").build());
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(NO);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getSchedulingAndListingFields()
            .getPanelMemberExclusions().getExcludedPanelMembers()).hasSize(2);
        assertThat(sscsCaseData.getSchedulingAndListingFields().getPanelMemberExclusions().getArePanelMembersExcluded())
            .isEqualTo(YES);
    }

}
