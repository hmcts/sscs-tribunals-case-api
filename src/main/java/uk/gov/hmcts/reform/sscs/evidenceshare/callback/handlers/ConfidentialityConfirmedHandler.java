package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SENT_TO_DWP;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.isBenefitTypeChildSupportOrUc;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Slf4j
@Service
public class ConfidentialityConfirmedHandler implements CallbackHandler<SscsCaseData> {

    private final UpdateCcdCaseService updateCcdCaseService;
    private final IdamService idamService;
    private final DispatchPriority dispatchPriority;

    @Autowired
    public ConfidentialityConfirmedHandler(UpdateCcdCaseService updateCcdCaseService,
                                           IdamService idamService) {
        this.updateCcdCaseService = updateCcdCaseService;
        this.idamService = idamService;
        this.dispatchPriority = DispatchPriority.LATEST;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        if (callbackType != CallbackType.SUBMITTED
            || callback.getEvent() != EventType.CONFIDENTIALITY_CONFIRMED) {
            return false;
        }

        return isBenefitTypeChildSupportOrUc(callback.getCaseDetails().getCaseData());
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        updateCcdCaseService.updateCaseV2(
                callback.getCaseDetails().getId(),
                SENT_TO_DWP.getCcdType(),
                "Case sent to FTA",
                "Case sent to FTA",
                idamService.getIdamTokens(),
                sscsCaseDetails -> {
                    SscsCaseData sscsCaseData = sscsCaseDetails.getData();
                    sscsCaseData.setDateSentToDwp(LocalDate.now().toString());
                    sscsCaseData.setHmctsDwpState(SENT_TO_DWP.getCcdType());
                });
    }

    @Override
    public DispatchPriority getPriority() {
        return this.dispatchPriority;
    }
}
