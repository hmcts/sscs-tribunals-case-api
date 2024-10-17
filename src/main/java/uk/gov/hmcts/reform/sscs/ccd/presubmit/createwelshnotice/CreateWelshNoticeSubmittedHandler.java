package uk.gov.hmcts.reform.sscs.ccd.presubmit.createwelshnotice;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

@Service
@Slf4j
public class CreateWelshNoticeSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final UpdateCcdCaseService updateCcdCaseService;
    private final IdamService idamService;

    @Autowired
    public CreateWelshNoticeSubmittedHandler(UpdateCcdCaseService updateCcdCaseService, IdamService idamService) {
        this.updateCcdCaseService = updateCcdCaseService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
                && callback.getEvent().equals(EventType.CREATE_WELSH_NOTICE)
                && StringUtils.isNotEmpty(callback.getCaseDetails().getCaseData().getSscsWelshPreviewNextEvent());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        final String nextEvent = callback.getCaseDetails().getCaseData().getSscsWelshPreviewNextEvent();
        log.info("Next event to submit  {}", nextEvent);
        callback.getCaseDetails().getCaseData().setSscsWelshPreviewNextEvent(null);
        log.info("Pre calling Welsh Notice Submitted Handler to trigger Case Event V2");
        SscsCaseDetails sscsCaseDetails = updateCcdCaseService.triggerCaseEventV2(callback.getCaseDetails().getId(),
            nextEvent,
            "Create Welsh notice",
            "Create Welsh notice",
            idamService.getIdamTokens());
        log.info("Triggered case event V2 calling Welsh Notice Submitted Handler");
        return new PreSubmitCallbackResponse<>(sscsCaseDetails.getData());
    }
}
