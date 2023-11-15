package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isSscs2Case;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.validateBenefitIssueCode;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;


@Component
@Slf4j
@AllArgsConstructor
public class DwpUploadResponseMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public static final String APPENDIX_12_DOC_NOT_FOR_SSCS5_CONFIDENTIALITY = "An Appendix 12 document cannot be uploaded with Confidentiality documents";

    protected final SessionCategoryMapService categoryMapService;


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

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        validateBenefitIssueCode(sscsCaseData, response, categoryMapService);
        validatePostponementRequests(sscsCaseData, response);
        forceToAddOtherPartyOnSscs2Case(sscsCaseData, response);

        return response;
    }

    private void validatePostponementRequests(SscsCaseData sscsCaseData,
                                              PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        Benefit benefit = Benefit.getBenefitByCodeOrThrowException(sscsCaseData.getAppeal().getBenefitType().getCode());
        if (benefit.getSscsType().equals(SscsType.SSCS5)) {
            if (StringUtils.equalsIgnoreCase(sscsCaseData.getDwpEditedEvidenceReason(), "childSupportConfidentiality")) {
                if (sscsCaseData.getAppendix12Doc() != null && sscsCaseData.getAppendix12Doc().getDocumentLink() != null) {
                    preSubmitCallbackResponse.addError(APPENDIX_12_DOC_NOT_FOR_SSCS5_CONFIDENTIALITY);
                }
            }
        }
    }

    private void forceToAddOtherPartyOnSscs2Case(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (isSscs2Case(sscsCaseData.getAppeal().getBenefitType().getCode())
                && sscsCaseData.getOtherParties() != null 
                && sscsCaseData.getOtherParties().size() == 0) {
            preSubmitCallbackResponse.addError("Please provide other party details");
        }
    }
}
