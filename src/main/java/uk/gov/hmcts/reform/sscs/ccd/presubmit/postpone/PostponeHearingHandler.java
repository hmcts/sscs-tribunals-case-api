package uk.gov.hmcts.reform.sscs.ccd.presubmit.postpone;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NOT_LISTABLE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Postponement;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostponeHearingHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final IdamService idamService;
    private final UpdateCcdCaseService updateCcdCaseService;

    @Value("${feature.snl.enabled}")
    private boolean isScheduleListingEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.POSTPONED
            && isScheduleListingEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData caseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        Long caseId = Long.valueOf(caseData.getCcdCaseId());

        EventType postponementEvent = caseData.getPostponement().getPostponementEvent();

        log.info("Action postponement request: handling event {} for case {}", postponementEvent, caseId);

        if (!SscsUtil.isSAndLCase(caseData)) {
            log.info("Hearing postponed: Cannot process non Scheduling & Listing Case for Case ID {}", caseId);
            response.addError("Cannot process hearing postponed on non Scheduling & Listing Case");
            return response;
        }

        if (isNoOrNull(caseData.getPostponement().getUnprocessedPostponement())) {
            log.info("Hearing postponed: Cannot process hearing postponed event on a case that "
                    + "has already been processed for Case ID {}", caseId);
            response.addError("Cannot process hearing postponed event on a case that has already been processed");
            return response;
        }

        String summary;
        String description;

        if (READY_TO_LIST  == postponementEvent) {
            summary = "Ready to List after Hearing Postponed";
            description = "Setting case to Ready to List after Hearing Postponed";
        } else if (NOT_LISTABLE == postponementEvent) {
            summary = "Not Listable after Hearing Postponed";
            description = "Setting case to Not Listable after Hearing Postponed";
        } else {
            response.addError(String.format("Invalid event type: %s for Hearing Postponed Event", postponementEvent));
            return response;
        }

        log.info("About to update case v2 with {} event for id {}", postponementEvent, caseId);
        SscsCaseDetails updatedSscsCaseDetails = updateCcdCaseService.updateCaseV2(
                caseId,
                postponementEvent.getCcdType(),
                summary,
                description,
                idamService.getIdamTokens(),
                sscsCaseDetails -> {
                    SscsCaseData sscsCaseData = sscsCaseDetails.getData();
                    sscsCaseData.setDwpState(DwpState.HEARING_POSTPONED);
                    sscsCaseData.setIgnoreCallbackWarnings(YES);
                    sscsCaseData.setPostponement(Postponement.builder()
                            .unprocessedPostponement(NO)
                            .build());
                    log.info("Updated case v2 with {} event for id {}", postponementEvent, caseId);
                }
        );
        response.setData(updatedSscsCaseDetails.getData());

        return response;
    }
}
