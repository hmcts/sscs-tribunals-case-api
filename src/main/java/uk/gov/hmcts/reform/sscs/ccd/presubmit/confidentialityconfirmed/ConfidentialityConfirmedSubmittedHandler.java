package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentialityconfirmed;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SENT_TO_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;


@Slf4j
@Service
@AllArgsConstructor
class ConfidentialityConfirmedSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final UpdateCcdCaseService updateCcdCaseService;
    private final IdamService idamService;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        if (callbackType != CallbackType.SUBMITTED
            || callback.getEvent() != EventType.CONFIDENTIALITY_CONFIRMED) {
            return false;
        }

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        return caseData.isBenefitType(CHILD_SUPPORT) || caseData.isBenefitType(UC);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
        String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseDetails caseDetails = updateCcdCaseService.updateCaseV2(
                callback.getCaseDetails().getId(),
                SENT_TO_DWP.getCcdType(),
                "Case sent to FTA",
                "Case sent to FTA",
                idamService.getIdamTokens(),
                sscsCaseDetails -> {
                    SscsCaseData sscsCaseData = sscsCaseDetails.getData();
                    sscsCaseData.setDateSentToDwp(LocalDate.now().toString());
                    sscsCaseData.setHmctsDwpState("sentToDwp");
                    sscsCaseData.setState(WITH_DWP);
                });
        return new PreSubmitCallbackResponse<>(caseDetails.getData());
    }
}
