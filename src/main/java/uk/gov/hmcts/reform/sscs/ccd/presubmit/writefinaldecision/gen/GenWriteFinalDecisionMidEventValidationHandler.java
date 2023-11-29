package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isOtherPartyPresent;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerBase;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;

@Slf4j
@Component
public class GenWriteFinalDecisionMidEventValidationHandler extends WriteFinalDecisionMidEventValidationHandlerBase {

    public GenWriteFinalDecisionMidEventValidationHandler(Validator validator,
                                                          DecisionNoticeService decisionNoticeService,
                                                          @Value("${feature.postHearings.enabled}") boolean isPostHearingsEnabled) {
        super(validator, decisionNoticeService, isPostHearingsEnabled);
    }

    @Override
    protected String getBenefitType() {
        return "GEN";
    }

    @Override
    protected void setDefaultFields(SscsCaseData sscsCaseData) {
        if (isOtherPartyPresent(sscsCaseData)) {
            final List<OtherPartyAttendedQuestion> otherPartyAttendedQuestionList = new ArrayList<>();
            sscsCaseData.getOtherParties().stream()
                     .filter(otherPartyCcdValue -> nonNull(otherPartyCcdValue.getValue()))
                     .map(CcdValue::getValue)
                     .forEach(otherParty -> otherPartyAttendedQuestionList.add(OtherPartyAttendedQuestion.builder()
                             .value(OtherPartyAttendedQuestionDetails.builder()
                                     .otherPartyName(otherParty.getName().getFullNameNoTitle()).build()).build()));
            sscsCaseData.getSscsFinalDecisionCaseData().setOtherPartyAttendedQuestions(otherPartyAttendedQuestionList);
        }
    }

    @Override
    protected void setShowPageFlags(SscsCaseData sscsCaseData) {
        // N/A for GEN
    }

    @Override
    protected void validateAwardTypes(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        // N/A for GEN
    }

    @Override
    protected void setShowSummaryOfOutcomePage(SscsCaseData sscsCaseData, String pageId) {
        if (isYes(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())) {
            sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(YesNo.YES);
            return;
        }
        sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(YesNo.NO);
    }

    @Override
    protected void setShowWorkCapabilityAssessmentPage(SscsCaseData sscsCaseData) {
        // N/A for GEN
    }

    @Override
    protected void setDwpReassessAwardPage(SscsCaseData sscsCaseData, String pageId) {
        sscsCaseData.setShowDwpReassessAwardPage(YesNo.NO);
    }
}
