package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.createcase;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.callback.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
public class CreateCaseAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final List<EventType> CREATE_CASE_EVENTS =
            List.of(VALID_APPEAL_CREATED, NON_COMPLIANT, INCOMPLETE_APPLICATION_RECEIVED);

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return isCreateCaseStartCallback(callbackType, callback.getEvent());
    }

    public static boolean isCreateCaseStartCallback(CallbackType callbackType, EventType event) {
        return callbackType.equals(ABOUT_TO_START) && CREATE_CASE_EVENTS.contains(event);
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

        appeal.getBenefitType().setDescriptionSelection(SscsUtil.getBenefitDescriptions());

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
