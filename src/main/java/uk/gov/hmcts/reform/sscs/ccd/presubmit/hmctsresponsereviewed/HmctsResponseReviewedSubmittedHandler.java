package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_SEND_TO_INTERLOC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import feign.FeignException;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Component
@Slf4j
public class HmctsResponseReviewedSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final UpdateCcdCaseService updateCcdCaseService;
    private final IdamService idamService;

    @Autowired
    public HmctsResponseReviewedSubmittedHandler(UpdateCcdCaseService updateCcdCaseService, IdamService idamService) {
        this.updateCcdCaseService = updateCcdCaseService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.HMCTS_RESPONSE_REVIEWED;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (sscsCaseData.getIsInterlocRequired() != null && sscsCaseData.getIsInterlocRequired().equals("Yes")) {
            updateCase(
                    callback.getCaseDetails().getId(),
                    VALID_SEND_TO_INTERLOC,
                    "Send to interloc",
                    String.join(" ", "Send a case to a",
                            sscsCaseData.getSelectWhoReviewsCase().getValue().getCode().equals("reviewByJudge") ? "Judge" : "TCW",
                            "for review"),
                    sscsCaseDetails -> {
                        SscsCaseData caseData = sscsCaseDetails.getData();
                        caseData.setSelectWhoReviewsCase(sscsCaseData.getSelectWhoReviewsCase());
                        caseData.setOriginalSender(sscsCaseData.getOriginalSender());
                    });
        } else {
            updateCase(
                    callback.getCaseDetails().getId(),
                    READY_TO_LIST,
                    "Ready to list",
                    "Makes an appeal ready to list",
                     sscsCaseDetails -> sscsCaseDetails.getData().setIgnoreCallbackWarnings(YES));
        }

        return preSubmitCallbackResponse;
    }

    private void updateCase(Long caseId, EventType eventType, String summary, String description, Consumer<SscsCaseDetails> mutator) {
        try {
            updateCcdCaseService.updateCaseV2(
                    caseId,
                    eventType.getCcdType(),
                    summary,
                    description,
                    idamService.getIdamTokens(),
                    mutator);
        } catch (FeignException e) {
            log.error(
                    "{}. CCD response: {}",
                    String.format("Could not update event %s for case %d", eventType, caseId),
                    // exception.contentUTF8() uses response body internally
                    e.responseBody().isPresent() ? e.contentUTF8() : e.getMessage()
            );
            throw e;
        }
    }
}
