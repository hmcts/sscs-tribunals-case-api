package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Slf4j
public abstract class WriteFinalDecisionMidEventValidationHandlerBase extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    public static final String CANT_UPLOAD_ERROR_MESSAGE = "Unable to generate the corrected decision notice due to the original being uploaded";
    private static final List<String> DEATH_OF_APPELLANT_WARNING_PAGES = Arrays.asList("typeOfAppeal", "previewDecisionNotice");
    private final Validator validator;

    protected final DecisionNoticeService decisionNoticeService;
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

    protected WriteFinalDecisionMidEventValidationHandlerBase(Validator validator,
                                                              DecisionNoticeService decisionNoticeService,
                                                              @Value("${feature.postHearings.enabled}") boolean isPostHearingsEnabled) {
        this.validator = validator;
        this.decisionNoticeService = decisionNoticeService;
        this.isPostHearingsEnabled = isPostHearingsEnabled;
    }

    protected abstract String getBenefitType();

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
            && callback.getEvent() == EventType.WRITE_FINAL_DECISION
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData())
            && getBenefitType().equals(getBenefitTypeFromCallback(callback));
    }

    private String getBenefitTypeFromCallback(Callback<SscsCaseData> callback) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        return WriteFinalDecisionBenefitTypeHelper.getBenefitType(caseData);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        Set<ConstraintViolation<SscsCaseData>> violations = validator.validate(sscsCaseData);
        for (ConstraintViolation<SscsCaseData> violation : violations) {
            preSubmitCallbackResponse.addError(violation.getMessage());
        }
        
        SscsUtil.setCorrectionInProgress(caseDetails, isPostHearingsEnabled);

        if (SscsUtil.isCorrectionInProgress(sscsCaseData, isPostHearingsEnabled)
                && isYes(sscsCaseData.getSscsFinalDecisionCaseData().getFinalDecisionWasOriginalDecisionUploaded())
                && isYes(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())) {
            preSubmitCallbackResponse.addError(CANT_UPLOAD_ERROR_MESSAGE);
        }

        if (isDecisionNoticeDatesInvalid(sscsCaseData)) {
            preSubmitCallbackResponse.addError("Decision notice end date must be after decision notice start date");
        }

        if (isYes(sscsCaseData.getIsAppellantDeceased()) && DEATH_OF_APPELLANT_WARNING_PAGES.contains(callback.getPageId()) && !callback.isIgnoreWarnings()) {
            preSubmitCallbackResponse.addWarning("Appellant is deceased. Copy the draft decision and amend offline, then upload the offline version.");
        }

        setShowSummaryOfOutcomePage(sscsCaseData, callback.getPageId());
        setShowWorkCapabilityAssessmentPage(sscsCaseData);
        setDwpReassessAwardPage(sscsCaseData, callback.getPageId());

        validateAwardTypes(sscsCaseData, preSubmitCallbackResponse);
        setShowPageFlags(sscsCaseData);
        setDefaultFields(sscsCaseData);

        return preSubmitCallbackResponse;
    }

    protected abstract void setDefaultFields(SscsCaseData sscsCaseData);

    protected abstract void setShowPageFlags(SscsCaseData sscsCaseData);

    protected abstract void validateAwardTypes(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse);

    protected abstract void setShowSummaryOfOutcomePage(SscsCaseData sscsCaseData, String pageId);

    protected abstract void setShowWorkCapabilityAssessmentPage(SscsCaseData sscsCaseData);

    protected abstract void setDwpReassessAwardPage(SscsCaseData sscsCaseData, String pageId);

    private boolean isDecisionNoticeDatesInvalid(SscsCaseData sscsCaseData) {
        if (isNotBlank(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate()) && isNotBlank(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate())) {
            LocalDate decisionNoticeStartDate = LocalDate.parse(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate());
            LocalDate decisionNoticeEndDate = LocalDate.parse(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate());
            return !decisionNoticeStartDate.isBefore(decisionNoticeEndDate);
        }
        return false;
    }

}
