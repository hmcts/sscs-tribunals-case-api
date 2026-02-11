package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.dwpuploadresponse;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.IBCA_BENEFIT_CODE;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isSscs2Case;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.BENEFIT_CODE_NOT_IN_USE;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.INVALID_BENEFIT_ISSUE_CODE;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.callback.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsType;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;

@Component
@Slf4j
@AllArgsConstructor
public class DwpUploadResponseMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public static final String APPENDIX_12_DOC_NOT_FOR_SSCS5_CONFIDENTIALITY =
            "An Appendix 12 document cannot be uploaded with Confidentiality documents";

    private final PanelCompositionService panelCompositionService;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
                && callback.getEvent() == EventType.DWP_UPLOAD_RESPONSE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        var response = new PreSubmitCallbackResponse<>(sscsCaseData);
        response.addErrors(validateBenefitIssueCodeV2(sscsCaseData));
        validatePostponementRequests(sscsCaseData, response);
        forceToAddOtherPartyOnSscs2Case(sscsCaseData, response);
        validateBenefitCode(sscsCaseData, response);
        validateDwpFurtherInfo(sscsCaseData, response);

        return response;
    }

    private void validatePostponementRequests(SscsCaseData sscsCaseData,
                                              PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        Benefit benefit = Benefit.getBenefitByCodeOrThrowException(sscsCaseData.getAppeal().getBenefitType().getCode());
        if (benefit.getSscsType().equals(SscsType.SSCS5)
                && equalsIgnoreCase(sscsCaseData.getDwpEditedEvidenceReason(), "childSupportConfidentiality")
                && nonNull(sscsCaseData.getAppendix12Doc())
                && nonNull(sscsCaseData.getAppendix12Doc().getDocumentLink())) {
            preSubmitCallbackResponse.addError(APPENDIX_12_DOC_NOT_FOR_SSCS5_CONFIDENTIALITY);
        }
    }

    private void forceToAddOtherPartyOnSscs2Case(SscsCaseData sscsCaseData,
                                                 PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (isSscs2Case(sscsCaseData.getAppeal().getBenefitType().getCode())
                && nonNull(sscsCaseData.getOtherParties()) && sscsCaseData.getOtherParties().isEmpty()) {
            preSubmitCallbackResponse.addError("Please provide other party details");
        }
    }

    private void validateBenefitCode(SscsCaseData sscsCaseData,
                                     PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        Benefit benefit = Benefit.getBenefitByCodeOrThrowException(sscsCaseData.getAppeal().getBenefitType().getCode());
        if (!IBCA_BENEFIT_CODE.equals(benefit.getBenefitCode()) && IBCA_BENEFIT_CODE.equals(sscsCaseData.getBenefitCode())) {
            preSubmitCallbackResponse.addError("Please choose a valid benefit code");
        }
    }

    private List<String> validateBenefitIssueCodeV2(SscsCaseData caseData) {
        List<String> errors = new ArrayList<>();
        if (isNull(Benefit.getBenefitFromBenefitCode(caseData.getBenefitCode()))) {
            errors.add(BENEFIT_CODE_NOT_IN_USE);
        }
        if (!panelCompositionService.isBenefitIssueCodeValid(caseData.getBenefitCode(), caseData.getIssueCode())) {
            errors.add(INVALID_BENEFIT_ISSUE_CODE);
            log.info("invalid benefit issue code {} for case id {}",
                    caseData.getBenefitCode() + caseData.getIssueCode(), caseData.getCcdCaseId());
        }
        return errors;
    }

    private static void validateDwpFurtherInfo(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {
        if (YesNo.YES.getValue().equals(sscsCaseData.getDwpFurtherInfo())) {
            response.addWarning("Are you sure you want HMCTS to review the case?");
        }
    }
}
