package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;


import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;



@Component
@Slf4j
public class DwpUploadResponseMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public static final String APPENDIX_12_DOC_NOT_FOR_SSCS5_CONFIDENTIALITY = "You cannot upload an Appendix 12 document when submitting Confidentiality documents";

    @Autowired
    public DwpUploadResponseMidEventHandler() {

    }

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

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse =
                new PreSubmitCallbackResponse<>(sscsCaseData);



        validatePostponementRequests(caseDetails, sscsCaseData, preSubmitCallbackResponse);


        return preSubmitCallbackResponse;
    }

    private void validatePostponementRequests(CaseDetails<SscsCaseData> caseDetails, SscsCaseData sscsCaseData,
                                              PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        if (Benefit.TAX_CREDIT.getShortName().equals(sscsCaseData.getAppeal().getBenefitType().getCode())
            || Benefit.GUARDIANS_ALLOWANCE.getShortName().equals(sscsCaseData.getAppeal().getBenefitType().getCode())
            || Benefit.TAX_FREE_CHILDCARE.getShortName().equals(sscsCaseData.getAppeal().getBenefitType().getCode())
            || Benefit.HOME_RESPONSIBILITIES_PROTECTION.getShortName().equals(sscsCaseData.getAppeal().getBenefitType().getCode())
            || Benefit.CHILD_BENEFIT.getShortName().equals(sscsCaseData.getAppeal().getBenefitType().getCode())
            || Benefit.THIRTY_HOURS_FREE_CHILDCARE.getShortName().equals(sscsCaseData.getAppeal().getBenefitType().getCode())
            || Benefit.GUARANTEED_MINIMUM_PENSION.getShortName().equals(sscsCaseData.getAppeal().getBenefitType().getCode())
            || Benefit.NATIONAL_INSURANCE_CREDITS.getShortName().equals(sscsCaseData.getAppeal().getBenefitType().getCode())) {
            if (StringUtils.equalsIgnoreCase(sscsCaseData.getDwpEditedEvidenceReason(), "childSupportConfidentiality")) {
                if (sscsCaseData.getAppendix12Doc() != null && sscsCaseData.getAppendix12Doc().getDocumentLink() != null) {
                    preSubmitCallbackResponse.addError(APPENDIX_12_DOC_NOT_FOR_SSCS5_CONFIDENTIALITY);
                }
            }
        }
    }
}
