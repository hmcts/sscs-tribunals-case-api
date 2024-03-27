package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority.LATEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.helper.CaseAccessManagementFieldsHelper.setCaseAccessManagementFields;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManualCaseCreatedHandler implements CallbackHandler<SscsCaseData> {

    private final CcdService ccdService;
    private final IdamService idamService;

    @Value("${feature.case-access-management.enabled}")
    private boolean caseAccessManagementFeature;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(SUBMITTED)
            && (callback.getEvent() == NON_COMPLIANT
            || callback.getEvent() == INCOMPLETE_APPLICATION_RECEIVED
            || callback.getEvent() == VALID_APPEAL_CREATED);
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        Long caseId = callback.getCaseDetails().getId();
        log.info("Manually created case handler for case id {}", caseId);

        IdamTokens idamTokens = idamService.getIdamTokens();

        try {
            log.info("Setting supplementary data for: {}", caseId);
            setSupplementaryData(caseId, idamTokens);
            if (caseAccessManagementFeature) {
                log.info("Setting case access management fields for: {}", caseId);
                setCaseAccessManagementFields(callback
                    .getCaseDetails()
                    .getCaseData());
                ccdService.updateCase(
                    callback.getCaseDetails().getCaseData(),
                    callback.getCaseDetails().getId(),
                    UPDATE_CASE_ONLY.getCcdType(),
                    "Case Update - Manual Case Created",
                    "Case was updated in SSCS-Evidence-Share",
                    idamService.getIdamTokens()
                );
            }
        } catch (Exception e) {
            log.error("Error sending supplementary for caseId {}", caseId, e);
        }
    }

    @Override
    public DispatchPriority getPriority() {
        return LATEST;
    }

    private void setSupplementaryData(Long caseId, IdamTokens idamTokens) {
        Map<String, Map<String, Map<String, Object>>> supplementaryDataUpdates = new HashMap<>();
        supplementaryDataUpdates.put("supplementary_data_updates", singletonMap("$set", singletonMap("HMCTSServiceId", "BBA3")));
        ccdService.setSupplementaryData(idamTokens, caseId, supplementaryDataUpdates);
    }
}
