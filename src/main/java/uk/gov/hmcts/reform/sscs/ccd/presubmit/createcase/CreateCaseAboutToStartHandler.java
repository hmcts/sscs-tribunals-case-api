package uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
public class CreateCaseAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.infected-blood-compensation.enabled}")
    private boolean isInfectedBloodCompensationEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && ((callback.getEvent() == EventType.VALID_APPEAL_CREATED
                || callback.getEvent() == EventType.NON_COMPLIANT
                || callback.getEvent() == EventType.INCOMPLETE_APPLICATION_RECEIVED));
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData caseData = caseDetails.getCaseData();
        log.info("Handling create about to start event for case [" + caseData.getCcdCaseId() + "]");

        Appeal appeal = caseData.getAppeal();
        if (isNull(appeal)) {
            appeal = Appeal.builder().build();
            caseData.setAppeal(appeal);
        }

        if (isNull(appeal.getBenefitType())) {
            BenefitType type = BenefitType.builder().build();
            appeal.setBenefitType(type);
        }

        appeal.getBenefitType().setDescriptionSelection(SscsUtil.getBenefitDescriptions(isInfectedBloodCompensationEnabled));

        if (isNull(appeal.getAppellant())) {
            Appellant appellant = Appellant.builder().build();
            appeal.setAppellant(appellant);
        }

        if (isNull(appeal.getAppellant().getAddress())) {
            Address address = Address.builder().build();
            appeal.getAppellant().setAddress(address);
        }

        appeal.getAppellant().getAddress().setUkPortOfEntryList(SscsUtil.getPortsOfEntry());

        return new PreSubmitCallbackResponse<>(caseData);
    }
}
