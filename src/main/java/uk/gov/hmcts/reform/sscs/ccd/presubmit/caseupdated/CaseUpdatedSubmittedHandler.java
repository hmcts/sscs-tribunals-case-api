package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;


@Service
@Slf4j
public class CaseUpdatedSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private IdamService idamService;
    private final UpdateCcdCaseService updateCcdCaseService;

    @Autowired
    public CaseUpdatedSubmittedHandler(IdamService idamService, UpdateCcdCaseService updateCcdCaseService) {
        this.idamService = idamService;
        this.updateCcdCaseService = updateCcdCaseService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        return callbackType.equals(CallbackType.SUBMITTED)
                && callback.getEvent() == EventType.CASE_UPDATED
                && READY_TO_LIST.getId().equals(callback.getCaseDetails().getCaseData().getCreatedInGapsFrom());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            log.info("Cannot handle this event");
            throw new IllegalStateException("Cannot handle callback");
        }

        if (callback.getCaseDetails().getCaseData().getAppeal() == null
                || callback.getCaseDetails().getCaseData().getAppeal().getBenefitType() == null) {
            log.info("Cannot handle this event as no data");
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        BenefitType benefitType = caseData.getAppeal().getBenefitType();
        log.info("Benefit type {} for case id {} ", benefitType, callback.getCaseDetails().getId());

        if (StringUtils.equalsIgnoreCase(benefitType.getCode(), "uc") && isANewJointParty(callback, caseData)) {
            log.info("Pre Calling JOINT_PARTY_ADDED event V2 for case id {}", callback.getCaseDetails().getId());
            SscsCaseDetails sscsCaseDetails = updateCcdCaseService.triggerCaseEventV2(callback.getCaseDetails().getId(),
                    EventType.JOINT_PARTY_ADDED.getCcdType(), "Joint party added",
                    "", idamService.getIdamTokens());
            log.info("jointPartyAdded event updated V2 for case id {}", callback.getCaseDetails().getId());
            return new PreSubmitCallbackResponse<>(sscsCaseDetails.getData());
        }
        return new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
    }

    protected static boolean isANewJointParty(Callback<SscsCaseData> callback, SscsCaseData caseData) {
        boolean wasNotAlreadyJointParty = false;
        CaseDetails oldCaseDetails = callback.getCaseDetailsBefore().orElse(null);
        if (oldCaseDetails != null) {
            SscsCaseData oldCaseData = (SscsCaseData) oldCaseDetails.getCaseData();
            if (isNoOrNull(oldCaseData.getJointParty().getHasJointParty())) {
                wasNotAlreadyJointParty = true;
            }
        } else {
            wasNotAlreadyJointParty = true;
        }
        return wasNotAlreadyJointParty && isYes(caseData.getJointParty().getHasJointParty());
    }
}
